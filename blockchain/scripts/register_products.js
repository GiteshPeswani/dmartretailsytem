const { ethers } = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  const contractAddress = "0x0165878A594ca255338adfa4d48449f69242Eb8F";
  console.log("Connecting to RetailAuditRegistry at:", contractAddress);

  const registry = await ethers.getContractAt("RetailAuditRegistry", contractAddress);
  const products = JSON.parse(
    fs.readFileSync(path.join(__dirname, "products_to_register.json"), "utf8")
  );

  const results = [];

  for (const p of products) {
    const canonical = `${p.id}|${p.sku}|${p.name}|${Number(p.price).toFixed(2)}`;
    const dataHash = ethers.keccak256(ethers.toUtf8Bytes(canonical));
    const productId = ethers.encodeBytes32String(p.id.toString());

    console.log(`Processing Product ID ${p.id}: ${p.name} (${p.sku})...`);

    // Check if already registered
    try {
      const isVerified = await registry.verifyProduct(productId, dataHash);
      if (isVerified) {
        console.log(`  -> Already registered & verified on-chain.`);
        results.push({
          id: p.id,
          productId,
          dataHash,
          txHash: "0x" + "0".repeat(64),
          status: "ALREADY_REGISTERED"
        });
        continue;
      }
    } catch (e) {
      // Not registered
    }

    try {
      const tx = await registry.registerProduct(productId, dataHash);
      const receipt = await tx.wait();
      console.log(`  -> Registered! Tx: ${receipt.hash}, Block: ${receipt.blockNumber}`);
      results.push({
        id: p.id,
        productId,
        dataHash,
        txHash: receipt.hash,
        blockNumber: Number(receipt.blockNumber),
        gasUsed: Number(receipt.gasUsed),
        status: "CONFIRMED"
      });
    } catch (err) {
      console.error(`  -> Failed for product ${p.id}:`, err.message);
      results.push({
        id: p.id,
        productId,
        dataHash,
        error: err.message,
        status: "FAILED"
      });
    }
  }

  fs.writeFileSync(
    path.join(__dirname, "registration_results.json"),
    JSON.stringify(results, null, 2)
  );
  console.log(`\nFinished! Successfully processed ${results.length} products.`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
