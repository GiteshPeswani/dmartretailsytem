export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
}

export interface UserProfileResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  active: boolean;
}

export interface CategoryRequest {
  name: string;
  slug?: string;
  description?: string;
  imageUrl?: string;
  active?: boolean;
}

export type BlockchainStatus = 'NOT_REGISTERED' | 'NOT_SUBMITTED' | 'PENDING' | 'CONFIRMED' | 'FAILED';

export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  categoryName?: string;
  price: number;
  discountPercent: number;
  effectivePrice: number;
  stockQuantity: number;
  imageUrl?: string;
  active: boolean;
  blockchainProductId?: string;
  blockchainTxHash?: string;
  blockchainStatus: BlockchainStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  price: number;
  discountPercent?: number;
  stockQuantity: number;
  imageUrl?: string;
  active?: boolean;
}

export interface CartItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  productImageUrl?: string;
  unitPrice: number;
  discountPercent: number;
  effectivePrice: number;
  quantity: number;
  lineTotal: number;
  availableStock: number;
}

export interface CartSummary {
  items: CartItem[];
  totalItemCount: number;
  subtotal: number;
  totalSavings: number;
  grandTotal: number;
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  customerEmail: string;
  deliveryAddress: string;
  status: OrderStatus;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  notes?: string;
  blockchainOrderId?: string;
  blockchainTxHash?: string;
  blockchainStatus: BlockchainStatus;
  orderHash?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderCreateRequest {
  deliveryAddress: string;
  notes?: string;
}

export interface OrderStatusUpdateRequest {
  newStatus: OrderStatus;
  reason?: string;
}

export interface InventoryMovement {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  userId?: number;
  userName?: string;
  orderId?: number;
  movementType: 'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'DAMAGED';
  quantityChange: number;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  createdAt: string;
}

export interface InventoryUpdateRequest {
  productId: number;
  movementType: 'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'DAMAGED';
  quantityChange: number;
  reason: string;
}

export interface BlockchainTransaction {
  id: number;
  txHash: string;
  entityType: 'PRODUCT' | 'ORDER';
  entityId: number;
  eventType: string;
  status: 'PENDING' | 'CONFIRMED' | 'FAILED';
  blockNumber?: number;
  gasUsed?: number;
  errorMessage?: string;
  retryCount: number;
  submittedAt: string;
  confirmedAt?: string;
}

export interface AuditEvent {
  id: number;
  entityType: 'PRODUCT' | 'ORDER';
  entityId: number;
  eventType: string;
  actorId?: number;
  actorName?: string;
  actorRole?: string;
  description: string;
  txHash?: string;
  createdAt: string;
}

export interface VerificationResult {
  verified: boolean;
  blockchainAvailable: boolean;
  entityType: string;
  entityId: number;
  localHash: string;
  onChainHash?: string;
  message: string;
  txHash?: string;
  blockNumber?: number;
  status?: 'VERIFIED' | 'TAMPERED' | 'ERROR' | 'UNKNOWN';
  isTampered?: boolean;
}

export type BatchStage = 'MANUFACTURED' | 'WAREHOUSE_ARRIVED' | 'STORE_ARRIVED';

export interface ProductBatch {
  id: number;
  batchNumber: string;
  productSku: string;
  productName: string;
  manufacturerName: string;
  manufactureDate: string;
  expiryDate?: string;
  quantity: number;
  currentStage: BatchStage;
  warehouseLocation?: string;
  storeLocation?: string;
  blockchainTxHash?: string;
  blockchainBatchId?: string;
  batchHash?: string;
  blockchainStatus: BlockchainStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BatchCreateRequest {
  productId?: number;
  batchNumber: string;
  productSku?: string;
  productName?: string;
  manufacturerName?: string;
  manufactureDate: string;
  expiryDate?: string;
  quantity: number;
  warehouseLocation?: string;
  storeLocation?: string;
}

export interface BlockchainStats {
  totalTransactions: number;
  confirmedTransactions: number;
  pendingTransactions: number;
  failedTransactions: number;
  recentTransactions: BlockchainTransaction[];
}

export interface DashboardSummary {
  totalProducts: number;
  totalCustomers: number;
  totalOrders: number;
  totalRevenue: number;
  lowStockCount: number;
  pendingOrdersCount: number;
  blockchainConfirmedOrders: number;
  blockchainFailedOrders: number;
  recentOrders: Order[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
