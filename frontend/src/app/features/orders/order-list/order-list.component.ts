import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/models';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);

  orders: Order[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 0;

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page = 0): void {
    this.loading = true;
    this.orderService.getMyOrders(page, 10).subscribe({
      next: (res) => {
        if (res.success) {
          this.orders = res.data.content;
          this.currentPage = res.data.number;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.loadOrders(page);
    }
  }
}
