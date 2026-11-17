import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { InventoryService } from '../../core/services/inventory.service';
import { OrderService } from '../../core/services/order.service';
import { BatchService } from '../../core/services/batch.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/services/auth.service';
import { Product, Order, InventoryMovement, InventoryUpdateRequest, OrderStatus, ProductBatch, BatchCreateRequest } from '../../core/models/models';

@Component({
  selector: 'app-store-manager',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule],
  templateUrl: './store-manager.component.html',
  styleUrls: ['./store-manager.component.css']
})
export class StoreManagerComponent implements OnInit {
  authService = inject(AuthService);
  private productService = inject(ProductService);
  private inventoryService = inject(InventoryService);
  private orderService = inject(OrderService);
  private batchService = inject(BatchService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  logout(): void {
    this.authService.logout();
  }

  products: Product[] = [];
  orders: Order[] = [];
  movements: InventoryMovement[] = [];
  batches: ProductBatch[] = [];

  activeTab: 'inventory' | 'orders' | 'history' | 'batches' = 'inventory';
  loading = true;

  // Inventory adjustment modal
  selectedProduct: Product | null = null;
  showAdjustModal = false;
  adjusting = false;

  adjustForm: FormGroup = this.fb.group({
    movementType: ['PURCHASE', [Validators.required]],
    quantityChange: [10, [Validators.required]],
    reason: ['', [Validators.required, Validators.minLength(3)]]
  });

  // Order status update modal
  selectedOrder: Order | null = null;
  targetOrderStatus: OrderStatus | '' = '';
  orderReason = '';
  showOrderStatusModal = false;
  updatingOrder = false;

  // Batch creation & QR Modal
  showCreateBatchModal = false;
  creatingBatch = false;
  selectedBatchForQR: ProductBatch | null = null;
  showQRModal = false;

  batchForm: FormGroup = this.fb.group({
    batchNumber: ['', [Validators.required, Validators.minLength(4)]],
    productId: [null],
    productSku: ['', [Validators.required]],
    productName: ['', [Validators.required]],
    manufacturerName: ['', [Validators.required]],
    manufactureDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    expiryDate: ['', [Validators.required]],
    quantity: [100, [Validators.required, Validators.min(1)]],
    warehouseLocation: ['Central Hub - Bay A4'],
    storeLocation: ['Main D-Mart Outlet - Shelf 3B']
  });

  ngOnInit(): void {
    this.loadData();
    this.loadBatches();
  }

  loadData(): void {
    this.loading = true;
    this.productService.getAllProductsAdmin(0, 50).subscribe({
      next: (res) => {
        if (res.success && res.data?.content?.length) {
          this.products = res.data.content;
          this.loading = false;
        } else {
          this.fallbackLoadProducts();
        }
      },
      error: () => this.fallbackLoadProducts()
    });

    this.orderService.getManagerOrders(undefined, undefined, 0, 50).subscribe({
      next: (res) => {
        if (res.success) this.orders = res.data.content;
      }
    });
  }

  private fallbackLoadProducts(): void {
    this.productService.getProducts(undefined, undefined, 0, 50).subscribe({
      next: (res) => {
        if (res.success) this.products = res.data.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadBatches(): void {
    this.batchService.getAllBatches().subscribe({
      next: (res) => {
        if (res.success) {
          this.batches = res.data;
        }
      }
    });
  }

  openAdjustModal(product: Product): void {
    this.selectedProduct = product;
    this.adjustForm.reset({
      movementType: 'PURCHASE',
      quantityChange: 10,
      reason: ''
    });
    this.showAdjustModal = true;
  }

  closeAdjustModal(): void {
    this.showAdjustModal = false;
    this.selectedProduct = null;
  }

  submitAdjustment(): void {
    if (!this.selectedProduct || this.adjustForm.invalid) {
      this.adjustForm.markAllAsTouched();
      return;
    }

    this.adjusting = true;
    const f = this.adjustForm.value;
    const req: InventoryUpdateRequest = {
      productId: this.selectedProduct.id,
      movementType: f.movementType,
      quantityChange: f.quantityChange,
      reason: f.reason
    };

    this.inventoryService.adjustInventory(req).subscribe({
      next: (res) => {
        this.adjusting = false;
        this.toast.success(`Inventory updated. New stock: ${res.data.stockQuantity}`);
        this.closeAdjustModal();
        this.loadData();
      },
      error: (err) => {
        this.adjusting = false;
        this.toast.error(err.error?.message || 'Failed to update inventory');
      }
    });
  }

  viewMovements(product: Product): void {
    this.selectedProduct = product;
    this.inventoryService.getProductMovements(product.id).subscribe({
      next: (res) => {
        if (res.success) {
          this.movements = res.data.content;
          this.activeTab = 'history';
        }
      }
    });
  }

  openOrderStatusModal(order: Order): void {
    this.selectedOrder = order;
    this.targetOrderStatus = '';
    this.orderReason = '';
    this.showOrderStatusModal = true;
  }

  closeOrderStatusModal(): void {
    this.showOrderStatusModal = false;
    this.selectedOrder = null;
  }

  getPermittedNextStatuses(current: OrderStatus): OrderStatus[] {
    switch (current) {
      case 'PENDING': return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED': return ['PROCESSING', 'CANCELLED'];
      case 'PROCESSING': return ['SHIPPED', 'CANCELLED'];
      case 'SHIPPED': return ['DELIVERED'];
      default: return [];
    }
  }

  submitOrderStatusUpdate(): void {
    if (!this.selectedOrder || !this.targetOrderStatus) return;

    this.updatingOrder = true;
    this.orderService.updateOrderStatusManager(this.selectedOrder.id, {
      newStatus: this.targetOrderStatus as OrderStatus,
      reason: this.orderReason
    }).subscribe({
      next: () => {
        this.updatingOrder = false;
        this.toast.success(`Order updated to ${this.targetOrderStatus} and broadcast to blockchain!`);
        this.closeOrderStatusModal();
        this.loadData();
      },
      error: (err) => {
        this.updatingOrder = false;
        this.toast.error(err.error?.message || 'Status update failed');
      }
    });
  }

  // Feature 3: Product Batch Tracking Methods
  openCreateBatchModal(): void {
    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    const today = new Date();
    const nextYear = new Date();
    nextYear.setFullYear(today.getFullYear() + 1);

    const firstProduct = this.products[0];
    this.batchForm.patchValue({
      batchNumber: `BATCH-2026-${randomSuffix}`,
      productId: firstProduct?.id || 1,
      productSku: firstProduct?.sku || 'SKU-DAI-001',
      productName: firstProduct?.name || 'Amul Milk 1 L',
      manufacturerName: 'D-Mart Certified Producer Ltd.',
      manufactureDate: today.toISOString().split('T')[0],
      expiryDate: nextYear.toISOString().split('T')[0],
      quantity: 100,
      warehouseLocation: 'Central Logistics Bay 4',
      storeLocation: 'D-Mart Main Store Shelf 2A'
    });
    this.showCreateBatchModal = true;
  }

  closeCreateBatchModal(): void {
    this.showCreateBatchModal = false;
  }

  onProductSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const selectedSku = target.value;
    const prod = this.products.find(p => p.sku === selectedSku);
    if (prod) {
      this.batchForm.patchValue({
        productId: prod.id,
        productSku: prod.sku,
        productName: prod.name
      });
    }
  }

  submitCreateBatch(): void {
    if (this.batchForm.invalid) {
      this.batchForm.markAllAsTouched();
      this.toast.warning('Please fill in all required batch fields.');
      return;
    }

    this.creatingBatch = true;
    const formVal = this.batchForm.value;
    let productId = formVal.productId;
    if (!productId && formVal.productSku) {
      const p = this.products.find(x => x.sku === formVal.productSku);
      if (p) productId = p.id;
    }

    const req: BatchCreateRequest = {
      ...formVal,
      productId: Number(productId || 1)
    };

    this.batchService.createBatch(req).subscribe({
      next: (res) => {
        this.creatingBatch = false;
        this.toast.success(`Batch ${res.data.batchNumber} created and queued for blockchain confirmation!`);
        this.closeCreateBatchModal();
        this.loadBatches();
      },
      error: (err) => {
        this.creatingBatch = false;
        this.toast.error(err.error?.message || 'Failed to create batch');
      }
    });
  }

  recordWarehouseArrival(batchId: number): void {
    this.batchService.recordWarehouseArrival(batchId).subscribe({
      next: (res) => {
        this.toast.success(`Batch ${res.data.batchNumber} recorded: WAREHOUSE ARRIVED on-chain!`);
        this.loadBatches();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Failed to record warehouse arrival');
      }
    });
  }

  recordStoreArrival(batchId: number): void {
    this.batchService.recordStoreArrival(batchId).subscribe({
      next: (res) => {
        this.toast.success(`Batch ${res.data.batchNumber} recorded: STORE SHELF ARRIVED on-chain!`);
        this.loadBatches();
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Failed to record store arrival');
      }
    });
  }

  openQRModal(batch: ProductBatch): void {
    this.selectedBatchForQR = batch;
    this.showQRModal = true;
  }

  closeQRModal(): void {
    this.showQRModal = false;
    this.selectedBatchForQR = null;
  }

  getBatchQrUrl(batch: ProductBatch | null): string {
    if (!batch) return '';
    const details = [
      `📦 D-MART BATCH PROVENANCE`,
      `Batch Lot: ${batch.batchNumber}`,
      `Product: ${batch.productName} (${batch.productSku})`,
      `Manufacturer: ${batch.manufacturerName || 'Certified Supplier'}`,
      `Stage: ${batch.currentStage || 'MANUFACTURED'}`,
      `Blockchain Status: ${batch.blockchainStatus || 'CONFIRMED'}`,
      `Mfd: ${batch.manufactureDate || 'N/A'} | Exp: ${batch.expiryDate || 'N/A'}`,
      `Authenticity: 100% Genuine Direct Sourcing`
    ].join('\n');
    return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(details)}`;
  }
}
