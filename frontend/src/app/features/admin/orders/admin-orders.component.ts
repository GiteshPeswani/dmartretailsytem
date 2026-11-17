import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order, OrderStatus } from '../../../core/models/models';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-orders.component.html',
  styleUrls: ['./admin-orders.component.css']
})
export class AdminOrdersComponent implements OnInit {
  private orderService = inject(OrderService);
  private toast = inject(ToastService);

  orders: Order[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 0;

  filterStatus: OrderStatus | '' = '';
  searchQuery = '';

  // Status transition modal
  selectedOrder: Order | null = null;
  targetStatus: OrderStatus | '' = '';
  transitionReason = '';
  updatingStatus = false;
  showModal = false;

  readonly allStatuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page = 0): void {
    this.loading = true;
    this.orderService.getAllOrdersAdmin(
      this.filterStatus || undefined,
      this.searchQuery || undefined,
      page,
      15
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.orders = res.data.content;
          this.currentPage = res.data.number;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  openStatusModal(order: Order): void {
    this.selectedOrder = order;
    this.targetStatus = '';
    this.transitionReason = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedOrder = null;
  }

  getPermittedNextStatuses(current: OrderStatus): OrderStatus[] {
    switch (current) {
      case 'PENDING': return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED': return ['PROCESSING', 'CANCELLED'];
      case 'PROCESSING': return ['SHIPPED', 'CANCELLED'];
      case 'SHIPPED': return ['DELIVERED'];
      default: return []; // DELIVERED and CANCELLED are terminal
    }
  }

  submitStatusUpdate(): void {
    if (!this.selectedOrder || !this.targetStatus) return;

    this.updatingStatus = true;
    this.orderService.updateOrderStatusAdmin(this.selectedOrder.id, {
      newStatus: this.targetStatus as OrderStatus,
      reason: this.transitionReason
    }).subscribe({
      next: () => {
        this.updatingStatus = false;
        this.toast.success(`Order status updated to ${this.targetStatus} and audit event recorded on blockchain`);
        this.closeModal();
        this.loadOrders(this.currentPage);
      },
      error: (err) => {
        this.updatingStatus = false;
        this.toast.error(err.error?.message || 'Status transition rejected');
      }
    });
  }
}
