const { expect }  = require("chai");
const { ethers }  = require("hardhat");

// ─── Helpers ──────────────────────────────────────────────────────────────────
const rndHash = () => ethers.keccak256(ethers.toUtf8Bytes(Math.random().toString()));
const pid     = (tag) => ethers.keccak256(ethers.toUtf8Bytes(`PROD-${tag}`));
const oid     = (tag) => ethers.keccak256(ethers.toUtf8Bytes(`ORDER-${tag}`));
const dhash   = (s)   => ethers.keccak256(ethers.toUtf8Bytes(s));

// ─── Tests ────────────────────────────────────────────────────────────────────
describe("RetailAuditRegistry", function () {
  let registry;
  let owner, operator, alice, bob;

  beforeEach(async function () {
    [owner, operator, alice, bob] = await ethers.getSigners();
    const Factory = await ethers.getContractFactory("RetailAuditRegistry");
    registry = await Factory.deploy();
    await registry.waitForDeployment();
  });

  // ══════════════════════════════════════════════════════════════════════════
  // OPERATOR MANAGEMENT
  // ══════════════════════════════════════════════════════════════════════════
  describe("Operator Management", function () {
    it("owner is authorised at deployment", async function () {
      expect(await registry.authorizedOperators(owner.address)).to.equal(true);
    });

    it("owner can grant an operator", async function () {
      await registry.grantOperator(operator.address);
      expect(await registry.authorizedOperators(operator.address)).to.equal(true);
    });

    it("emits OperatorGranted", async function () {
      await expect(registry.grantOperator(operator.address))
        .to.emit(registry, "OperatorGranted")
        .withArgs(operator.address, owner.address);
    });

    it("owner can revoke an operator", async function () {
      await registry.grantOperator(operator.address);
      await registry.revokeOperator(operator.address);
      expect(await registry.authorizedOperators(operator.address)).to.equal(false);
    });

    it("non-owner cannot grant operator", async function () {
      await expect(
        registry.connect(alice).grantOperator(operator.address)
      ).to.be.revertedWithCustomError(registry, "Unauthorized");
    });

    it("cannot grant zero address", async function () {
      await expect(
        registry.grantOperator(ethers.ZeroAddress)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });

    it("cannot revoke owner", async function () {
      await expect(
        registry.revokeOperator(owner.address)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // PRODUCT REGISTRATION
  // ══════════════════════════════════════════════════════════════════════════
  describe("Product Registration", function () {
    const PROD_ID  = pid("001");
    const DATA_HASH = dhash('{"sku":"SKU001","name":"Basmati Rice 5kg","price":"299.00"}');

    it("authorised owner can register a product", async function () {
      await expect(registry.registerProduct(PROD_ID, DATA_HASH))
        .to.emit(registry, "ProductRegistered")
        .withArgs(PROD_ID, DATA_HASH, owner.address, anyValue);

      const [hash, by] = await registry.getProduct(PROD_ID);
      expect(hash).to.equal(DATA_HASH);
      expect(by).to.equal(owner.address);
    });

    it("granted operator can register a product", async function () {
      await registry.grantOperator(operator.address);
      await expect(
        registry.connect(operator).registerProduct(pid("GRANT-01"), DATA_HASH)
      ).to.not.be.reverted;
    });

    it("unauthorised address cannot register", async function () {
      await expect(
        registry.connect(alice).registerProduct(PROD_ID, DATA_HASH)
      ).to.be.revertedWithCustomError(registry, "Unauthorized");
    });

    it("cannot register same product twice", async function () {
      await registry.registerProduct(PROD_ID, DATA_HASH);
      await expect(
        registry.registerProduct(PROD_ID, DATA_HASH)
      ).to.be.revertedWithCustomError(registry, "AlreadyRegistered");
    });

    it("rejects empty productId", async function () {
      await expect(
        registry.registerProduct(ethers.ZeroHash, DATA_HASH)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });

    it("rejects empty dataHash", async function () {
      await expect(
        registry.registerProduct(PROD_ID, ethers.ZeroHash)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });

    it("isProductRegistered returns true after registration", async function () {
      await registry.registerProduct(PROD_ID, DATA_HASH);
      expect(await registry.isProductRegistered(PROD_ID)).to.equal(true);
    });

    it("isProductRegistered returns false for unknown product", async function () {
      expect(await registry.isProductRegistered(pid("UNKNOWN"))).to.equal(false);
    });

    it("verifyProduct returns true for matching hash (staticCall)", async function () {
      await registry.registerProduct(PROD_ID, DATA_HASH);
      const result = await registry.verifyProduct.staticCall(PROD_ID, DATA_HASH);
      expect(result).to.equal(true);
    });

    it("verifyProduct returns false for wrong hash (staticCall)", async function () {
      await registry.registerProduct(PROD_ID, DATA_HASH);
      const result = await registry.verifyProduct.staticCall(PROD_ID, rndHash());
      expect(result).to.equal(false);
    });

    it("verifyProduct on unknown product reverts", async function () {
      await expect(
        registry.verifyProduct.staticCall(pid("NONE"), DATA_HASH)
      ).to.be.revertedWithCustomError(registry, "ProductNotFound");
    });

    it("after revocation, ex-operator cannot register", async function () {
      await registry.grantOperator(operator.address);
      await registry.revokeOperator(operator.address);
      await expect(
        registry.connect(operator).registerProduct(pid("REVOKED"), DATA_HASH)
      ).to.be.revertedWithCustomError(registry, "Unauthorized");
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // ORDER LIFECYCLE
  // ══════════════════════════════════════════════════════════════════════════
  describe("Order Lifecycle", function () {
    const ORDER_ID   = oid("001");
    const ORDER_HASH = dhash('{"orderId":"1","customerId":"7","total":"1499.00"}');

    beforeEach(async function () {
      await registry.createOrder(ORDER_ID, ORDER_HASH);
    });

    it("creates order with PENDING status (0)", async function () {
      const [hash, , , status] = await registry.getOrder(ORDER_ID);
      expect(hash).to.equal(ORDER_HASH);
      expect(status).to.equal(0);
    });

    it("emits OrderCreated event", async function () {
      const newId = oid("002");
      await expect(registry.createOrder(newId, ORDER_HASH))
        .to.emit(registry, "OrderCreated")
        .withArgs(newId, ORDER_HASH, owner.address, anyValue);
    });

    it("cannot create duplicate order", async function () {
      await expect(
        registry.createOrder(ORDER_ID, ORDER_HASH)
      ).to.be.revertedWithCustomError(registry, "AlreadyRegistered");
    });

    it("rejects empty orderId", async function () {
      await expect(
        registry.createOrder(ethers.ZeroHash, ORDER_HASH)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });

    it("PENDING → CONFIRMED is valid", async function () {
      const h = rndHash();
      await expect(registry.updateOrderStatus(ORDER_ID, 1, h))
        .to.emit(registry, "OrderStatusUpdated")
        .withArgs(ORDER_ID, 0, 1, h, owner.address, anyValue);
      const [, , , status] = await registry.getOrder(ORDER_ID);
      expect(status).to.equal(1);
    });

    it("PENDING → CANCELLED is valid", async function () {
      await registry.updateOrderStatus(ORDER_ID, 5, rndHash());
      const [, , , status] = await registry.getOrder(ORDER_ID);
      expect(status).to.equal(5);
    });

    it("PENDING → SHIPPED is invalid (skips steps)", async function () {
      await expect(
        registry.updateOrderStatus(ORDER_ID, 3, rndHash())
      ).to.be.revertedWithCustomError(registry, "InvalidStatusTransition");
    });

    it("PENDING → DELIVERED is invalid", async function () {
      await expect(
        registry.updateOrderStatus(ORDER_ID, 4, rndHash())
      ).to.be.revertedWithCustomError(registry, "InvalidStatusTransition");
    });

    it("full happy-path: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED", async function () {
      await registry.updateOrderStatus(ORDER_ID, 1, rndHash()); // CONFIRMED
      await registry.updateOrderStatus(ORDER_ID, 2, rndHash()); // PROCESSING
      await registry.updateOrderStatus(ORDER_ID, 3, rndHash()); // SHIPPED
      await registry.updateOrderStatus(ORDER_ID, 4, rndHash()); // DELIVERED
      const [, , , status] = await registry.getOrder(ORDER_ID);
      expect(status).to.equal(4);
    });

    it("DELIVERED is terminal — no further updates allowed", async function () {
      await registry.updateOrderStatus(ORDER_ID, 1, rndHash());
      await registry.updateOrderStatus(ORDER_ID, 2, rndHash());
      await registry.updateOrderStatus(ORDER_ID, 3, rndHash());
      await registry.updateOrderStatus(ORDER_ID, 4, rndHash());
      await expect(
        registry.updateOrderStatus(ORDER_ID, 5, rndHash())
      ).to.be.revertedWithCustomError(registry, "InvalidStatusTransition");
    });

    it("CANCELLED is terminal", async function () {
      await registry.updateOrderStatus(ORDER_ID, 5, rndHash());
      await expect(
        registry.updateOrderStatus(ORDER_ID, 1, rndHash())
      ).to.be.revertedWithCustomError(registry, "InvalidStatusTransition");
    });

    it("audit trail length grows with each update", async function () {
      // starts at 1 (CREATED event)
      expect(await registry.getOrderAuditTrailLength(ORDER_ID)).to.equal(1);
      await registry.updateOrderStatus(ORDER_ID, 1, rndHash());
      expect(await registry.getOrderAuditTrailLength(ORDER_ID)).to.equal(2);
      await registry.updateOrderStatus(ORDER_ID, 2, rndHash());
      expect(await registry.getOrderAuditTrailLength(ORDER_ID)).to.equal(3);
    });

    it("audit events are chained via prevEventHash", async function () {
      await registry.updateOrderStatus(ORDER_ID, 1, rndHash());
      await registry.updateOrderStatus(ORDER_ID, 2, rndHash());

      const [, hash1, , , ,]        = await registry.getOrderAuditEvent(ORDER_ID, 1);
      const [, , , , , prevHash2]   = await registry.getOrderAuditEvent(ORDER_ID, 2);
      expect(prevHash2).to.equal(hash1);
    });

    it("unauthorised address cannot update order status", async function () {
      await expect(
        registry.connect(alice).updateOrderStatus(ORDER_ID, 1, rndHash())
      ).to.be.revertedWithCustomError(registry, "Unauthorized");
    });

    it("rejects empty eventHash", async function () {
      await expect(
        registry.updateOrderStatus(ORDER_ID, 1, ethers.ZeroHash)
      ).to.be.revertedWithCustomError(registry, "InvalidInput");
    });

    it("verifyOrder returns true for correct hash", async function () {
      const result = await registry.verifyOrder.staticCall(ORDER_ID, ORDER_HASH);
      expect(result).to.equal(true);
    });

    it("verifyOrder returns false for tampered hash", async function () {
      const result = await registry.verifyOrder.staticCall(ORDER_ID, rndHash());
      expect(result).to.equal(false);
    });

    it("verifyOrder on unknown order reverts", async function () {
      await expect(
        registry.verifyOrder.staticCall(oid("NONE"), ORDER_HASH)
      ).to.be.revertedWithCustomError(registry, "OrderNotFound");
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // RECORD NOT FOUND CASES
  // ══════════════════════════════════════════════════════════════════════════
  describe("Not-Found Cases", function () {
    it("getProduct on unknown product reverts", async function () {
      await expect(registry.getProduct(pid("NONE")))
        .to.be.revertedWithCustomError(registry, "ProductNotFound");
    });

    it("getOrder on unknown order reverts", async function () {
      await expect(registry.getOrder(oid("NONE")))
        .to.be.revertedWithCustomError(registry, "OrderNotFound");
    });

    it("updateOrderStatus on unknown order reverts", async function () {
      await expect(
        registry.updateOrderStatus(oid("NONE"), 1, rndHash())
      ).to.be.revertedWithCustomError(registry, "OrderNotFound");
    });
  });
});

// ─── anyValue matcher ─────────────────────────────────────────────────────────
function anyValue(actual) { return true; }
anyValue.asymmetricMatch = () => true;
