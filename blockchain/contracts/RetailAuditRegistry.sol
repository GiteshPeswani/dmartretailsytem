// SPDX-License-Identifier: MIT
pragma solidity ^0.8.27;

/**
 * @title RetailAuditRegistry
 * @notice Immutable audit log for D-Mart retail product registrations
 *         and order lifecycle events on a local EVM blockchain.
 * @dev Only authorized operators can write records.
 *      Anyone can read and verify records.
 */
contract RetailAuditRegistry {

    // ===================== ERRORS =====================
    error Unauthorized(address caller);
    error AlreadyRegistered(bytes32 id);
    error InvalidInput(string reason);
    error InvalidStatusTransition(uint8 fromStatus, uint8 toStatus);
    error OrderNotFound(bytes32 orderId);
    error ProductNotFound(bytes32 productId);
    error BatchNotFound(bytes32 batchId);

    // ===================== ENUMS =====================
    /// @notice Order status codes matching backend OrderStatus enum
    enum OrderStatus {
        PENDING,     // 0
        CONFIRMED,   // 1
        PROCESSING,  // 2
        SHIPPED,     // 3
        DELIVERED,   // 4
        CANCELLED    // 5
    }

    /// @notice Product batch supply chain stages
    enum BatchStage {
        MANUFACTURED,       // 0
        WAREHOUSE_ARRIVED,  // 1
        STORE_ARRIVED       // 2
    }

    // ===================== STRUCTS =====================
    struct ProductRecord {
        bytes32 productId;
        bytes32 dataHash;
        address registeredBy;
        uint256 registeredAt;
        bool exists;
    }

    struct OrderRecord {
        bytes32 orderId;
        bytes32 orderHash;
        address createdBy;
        uint256 createdAt;
        OrderStatus status;
        bool exists;
    }

    struct BatchRecord {
        bytes32 batchId;
        bytes32 productId;
        bytes32 dataHash;
        address registeredBy;
        uint256 registeredAt;
        BatchStage currentStage;
        bool exists;
    }

    struct AuditEvent {
        bytes32 recordId;
        bytes8  eventType;
        bytes32 eventHash;
        address actor;
        uint8   newStatus;
        uint256 timestamp;
        bytes32 prevEventHash;
    }

    // ===================== STATE =====================
    address public owner;
    mapping(address => bool) public authorizedOperators;

    mapping(bytes32 => ProductRecord) private _products;
    mapping(bytes32 => OrderRecord)   private _orders;
    mapping(bytes32 => BatchRecord)   private _batches;

    mapping(bytes32 => AuditEvent[]) private _orderAuditTrail;
    mapping(bytes32 => AuditEvent[]) private _productAuditTrail;
    mapping(bytes32 => AuditEvent[]) private _batchAuditTrail;

    // ===================== EVENTS =====================
    event OperatorGranted(address indexed operator, address indexed grantedBy);
    event OperatorRevoked(address indexed operator, address indexed revokedBy);

    event ProductRegistered(
        bytes32 indexed productId,
        bytes32         dataHash,
        address indexed registeredBy,
        uint256         timestamp
    );

    event OrderCreated(
        bytes32 indexed orderId,
        bytes32         orderHash,
        address indexed createdBy,
        uint256         timestamp
    );

    event OrderStatusUpdated(
        bytes32 indexed orderId,
        uint8   indexed previousStatus,
        uint8   indexed newStatus,
        bytes32         eventHash,
        address         actor,
        uint256         timestamp
    );

    event BatchRegistered(
        bytes32 indexed batchId,
        bytes32 indexed productId,
        bytes32         dataHash,
        address indexed registeredBy,
        uint256         timestamp
    );

    event BatchStageUpdated(
        bytes32 indexed batchId,
        uint8   indexed previousStage,
        uint8   indexed newStage,
        bytes32         eventHash,
        address         actor,
        uint256         timestamp
    );

    event RecordVerified(
        bytes32 indexed recordId,
        bool            matched,
        address indexed verifier,
        uint256         timestamp
    );

    // ===================== MODIFIERS =====================
    modifier onlyOwner() {
        if (msg.sender != owner) revert Unauthorized(msg.sender);
        _;
    }

    modifier onlyOperator() {
        if (!authorizedOperators[msg.sender] && msg.sender != owner)
            revert Unauthorized(msg.sender);
        _;
    }

    // ===================== CONSTRUCTOR =====================
    constructor() {
        owner = msg.sender;
        authorizedOperators[msg.sender] = true;
    }

    // ===================== OPERATOR MANAGEMENT =====================

    function grantOperator(address operator) external onlyOwner {
        if (operator == address(0)) revert InvalidInput("Zero address");
        authorizedOperators[operator] = true;
        emit OperatorGranted(operator, msg.sender);
    }

    function revokeOperator(address operator) external onlyOwner {
        if (operator == owner) revert InvalidInput("Cannot revoke owner");
        authorizedOperators[operator] = false;
        emit OperatorRevoked(operator, msg.sender);
    }

    // ===================== PRODUCT FUNCTIONS =====================

    function registerProduct(bytes32 productId, bytes32 dataHash) external onlyOperator {
        if (productId == bytes32(0)) revert InvalidInput("Empty productId");
        if (dataHash == bytes32(0))  revert InvalidInput("Empty dataHash");
        if (_products[productId].exists) revert AlreadyRegistered(productId);

        _products[productId] = ProductRecord({
            productId:    productId,
            dataHash:     dataHash,
            registeredBy: msg.sender,
            registeredAt: block.timestamp,
            exists:       true
        });

        _productAuditTrail[productId].push(AuditEvent({
            recordId:      productId,
            eventType:     "REGISTER",
            eventHash:     dataHash,
            actor:         msg.sender,
            newStatus:     0,
            timestamp:     block.timestamp,
            prevEventHash: bytes32(0)
        }));

        emit ProductRegistered(productId, dataHash, msg.sender, block.timestamp);
    }

    function getProduct(bytes32 productId)
        external view
        returns (bytes32 dataHash, address registeredBy, uint256 registeredAt)
    {
        if (!_products[productId].exists) revert ProductNotFound(productId);
        ProductRecord storage p = _products[productId];
        return (p.dataHash, p.registeredBy, p.registeredAt);
    }

    function isProductRegistered(bytes32 productId) external view returns (bool) {
        return _products[productId].exists;
    }

    function verifyProduct(bytes32 productId, bytes32 dataHash) external view returns (bool matched) {
        if (!_products[productId].exists) return false;
        return (_products[productId].dataHash == dataHash);
    }

    // ===================== ORDER FUNCTIONS =====================

    function createOrder(bytes32 orderId, bytes32 orderHash) external onlyOperator {
        if (orderId == bytes32(0))    revert InvalidInput("Empty orderId");
        if (orderHash == bytes32(0))  revert InvalidInput("Empty orderHash");
        if (_orders[orderId].exists)  revert AlreadyRegistered(orderId);

        _orders[orderId] = OrderRecord({
            orderId:   orderId,
            orderHash: orderHash,
            createdBy: msg.sender,
            createdAt: block.timestamp,
            status:    OrderStatus.PENDING,
            exists:    true
        });

        bytes32 initEventHash = keccak256(
            abi.encodePacked(orderId, orderHash, block.timestamp, uint8(0))
        );

        _orderAuditTrail[orderId].push(AuditEvent({
            recordId:      orderId,
            eventType:     "CREATED",
            eventHash:     initEventHash,
            actor:         msg.sender,
            newStatus:     uint8(OrderStatus.PENDING),
            timestamp:     block.timestamp,
            prevEventHash: bytes32(0)
        }));

        emit OrderCreated(orderId, orderHash, msg.sender, block.timestamp);
    }

    function updateOrderStatus(
        bytes32 orderId,
        uint8   newStatus,
        bytes32 eventHash
    ) external onlyOperator {
        if (!_orders[orderId].exists)            revert OrderNotFound(orderId);
        if (newStatus > uint8(OrderStatus.CANCELLED)) revert InvalidInput("Invalid status");
        if (eventHash == bytes32(0))             revert InvalidInput("Empty eventHash");

        OrderStatus current = _orders[orderId].status;
        OrderStatus next    = OrderStatus(newStatus);

        if (!_isValidTransition(current, next))
            revert InvalidStatusTransition(uint8(current), newStatus);

        AuditEvent[] storage trail = _orderAuditTrail[orderId];
        bytes32 prevHash = trail.length > 0 ? trail[trail.length - 1].eventHash : bytes32(0);

        trail.push(AuditEvent({
            recordId:      orderId,
            eventType:     _statusToEventType(next),
            eventHash:     eventHash,
            actor:         msg.sender,
            newStatus:     newStatus,
            timestamp:     block.timestamp,
            prevEventHash: prevHash
        }));

        uint8 oldStatus = uint8(current);
        _orders[orderId].status = next;

        emit OrderStatusUpdated(orderId, oldStatus, newStatus, eventHash, msg.sender, block.timestamp);
    }

    function getOrder(bytes32 orderId)
        external view
        returns (bytes32 orderHash, address createdBy, uint256 createdAt, uint8 status)
    {
        if (!_orders[orderId].exists) revert OrderNotFound(orderId);
        OrderRecord storage o = _orders[orderId];
        return (o.orderHash, o.createdBy, o.createdAt, uint8(o.status));
    }

    function isOrderRegistered(bytes32 orderId) external view returns (bool) {
        return _orders[orderId].exists;
    }

    function verifyOrder(bytes32 orderId, bytes32 orderHash) external view returns (bool matched) {
        if (!_orders[orderId].exists) return false;
        return (_orders[orderId].orderHash == orderHash);
    }

    function getOrderAuditTrailLength(bytes32 orderId) external view returns (uint256) {
        return _orderAuditTrail[orderId].length;
    }

    function getOrderAuditEvent(bytes32 orderId, uint256 index)
        external view
        returns (
            bytes8  eventType,
            bytes32 eventHash,
            address actor,
            uint8   newStatus,
            uint256 timestamp,
            bytes32 prevEventHash
        )
    {
        AuditEvent[] storage trail = _orderAuditTrail[orderId];
        require(index < trail.length, "Index out of bounds");
        AuditEvent storage evt = trail[index];
        return (evt.eventType, evt.eventHash, evt.actor, evt.newStatus, evt.timestamp, evt.prevEventHash);
    }

    // ===================== BATCH FUNCTIONS =====================

    function registerBatch(
        bytes32 batchId,
        bytes32 productId,
        bytes32 dataHash
    ) external onlyOperator {
        if (batchId == bytes32(0))   revert InvalidInput("Empty batchId");
        if (productId == bytes32(0)) revert InvalidInput("Empty productId");
        if (dataHash == bytes32(0))  revert InvalidInput("Empty dataHash");
        if (_batches[batchId].exists) revert AlreadyRegistered(batchId);

        _batches[batchId] = BatchRecord({
            batchId: batchId,
            productId: productId,
            dataHash: dataHash,
            registeredBy: msg.sender,
            registeredAt: block.timestamp,
            currentStage: BatchStage.MANUFACTURED,
            exists: true
        });

        _batchAuditTrail[batchId].push(AuditEvent({
            recordId: batchId,
            eventType: "MFG",
            eventHash: dataHash,
            actor: msg.sender,
            newStatus: 0,
            timestamp: block.timestamp,
            prevEventHash: bytes32(0)
        }));

        emit BatchRegistered(batchId, productId, dataHash, msg.sender, block.timestamp);
    }

    function updateBatchStage(
        bytes32 batchId,
        uint8 newStage,
        bytes32 eventHash
    ) external onlyOperator {
        if (!_batches[batchId].exists) revert BatchNotFound(batchId);
        if (newStage > uint8(BatchStage.STORE_ARRIVED)) revert InvalidInput("Invalid stage");
        if (eventHash == bytes32(0)) revert InvalidInput("Empty eventHash");

        BatchStage current = _batches[batchId].currentStage;
        require(newStage > uint8(current), "Stage must advance");

        AuditEvent[] storage trail = _batchAuditTrail[batchId];
        bytes32 prevHash = trail.length > 0 ? trail[trail.length - 1].eventHash : bytes32(0);

        bytes8 stageType = newStage == 1 ? bytes8("WH_RCV") : bytes8("ST_RCV");

        trail.push(AuditEvent({
            recordId: batchId,
            eventType: stageType,
            eventHash: eventHash,
            actor: msg.sender,
            newStatus: newStage,
            timestamp: block.timestamp,
            prevEventHash: prevHash
        }));

        uint8 oldStage = uint8(current);
        _batches[batchId].currentStage = BatchStage(newStage);

        emit BatchStageUpdated(batchId, oldStage, newStage, eventHash, msg.sender, block.timestamp);
    }

    function getBatch(bytes32 batchId)
        external view
        returns (
            bytes32 productId,
            bytes32 dataHash,
            address registeredBy,
            uint256 registeredAt,
            uint8 currentStage
        )
    {
        if (!_batches[batchId].exists) revert BatchNotFound(batchId);
        BatchRecord storage b = _batches[batchId];
        return (b.productId, b.dataHash, b.registeredBy, b.registeredAt, uint8(b.currentStage));
    }

    function isBatchRegistered(bytes32 batchId) external view returns (bool) {
        return _batches[batchId].exists;
    }

    function verifyBatch(bytes32 batchId, bytes32 dataHash) external view returns (bool matched) {
        if (!_batches[batchId].exists) return false;
        return (_batches[batchId].dataHash == dataHash);
    }

    function getBatchAuditTrailLength(bytes32 batchId) external view returns (uint256) {
        return _batchAuditTrail[batchId].length;
    }

    function getBatchAuditEvent(bytes32 batchId, uint256 index)
        external view
        returns (
            bytes8 eventType,
            bytes32 eventHash,
            address actor,
            uint8 newStatus,
            uint256 timestamp,
            bytes32 prevEventHash
        )
    {
        AuditEvent[] storage trail = _batchAuditTrail[batchId];
        require(index < trail.length, "Index out of bounds");
        AuditEvent storage evt = trail[index];
        return (evt.eventType, evt.eventHash, evt.actor, evt.newStatus, evt.timestamp, evt.prevEventHash);
    }

    // ===================== INTERNAL HELPERS =====================

    function _isValidTransition(OrderStatus from, OrderStatus to) internal pure returns (bool) {
        if (from == OrderStatus.DELIVERED || from == OrderStatus.CANCELLED) return false;
        if (to == OrderStatus.CANCELLED) return true;
        return uint8(to) >= uint8(from);
    }

    function _statusToEventType(OrderStatus status) internal pure returns (bytes8) {
        if (status == OrderStatus.PENDING)    return "PENDING";
        if (status == OrderStatus.CONFIRMED)  return "CONFIRM";
        if (status == OrderStatus.PROCESSING) return "PROCESS";
        if (status == OrderStatus.SHIPPED)    return "SHIPPED";
        if (status == OrderStatus.DELIVERED)  return "DELIVER";
        if (status == OrderStatus.CANCELLED)  return "CANCEL";
        return "UNKNOWN";
    }
}
