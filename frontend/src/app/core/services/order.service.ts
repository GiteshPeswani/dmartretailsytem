import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Page, Order, OrderCreateRequest, OrderStatusUpdateRequest, OrderStatus, VerificationResult } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private readonly BASE_URL = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  checkout(req: OrderCreateRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${this.BASE_URL}/orders/checkout`, req);
  }

  getMyOrders(page = 0, size = 10): Observable<ApiResponse<Page<Order>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<Page<Order>>>(`${this.BASE_URL}/orders`, { params });
  }

  getOrderById(id: number): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.BASE_URL}/orders/${id}`);
  }

  verifyOrder(id: number): Observable<ApiResponse<VerificationResult>> {
    return this.http.get<ApiResponse<VerificationResult>>(`${this.BASE_URL}/orders/${id}/verify`);
  }

  verifyOrderByLookup(query: string): Observable<ApiResponse<VerificationResult>> {
    const params = new HttpParams().set('query', query);
    return this.http.get<ApiResponse<VerificationResult>>(`${this.BASE_URL}/orders/verify-lookup`, { params });
  }

  simulateTamper(id: number): Observable<ApiResponse<{ originalAmount: number; tamperedAmount: number; message: string }>> {
    return this.http.post<ApiResponse<{ originalAmount: number; tamperedAmount: number; message: string }>>(`${this.BASE_URL}/orders/${id}/simulate-tamper`, {});
  }

  restoreTamper(id: number): Observable<ApiResponse<{ restoredAmount: number; message: string }>> {
    return this.http.post<ApiResponse<{ restoredAmount: number; message: string }>>(`${this.BASE_URL}/orders/${id}/restore-tamper`, {});
  }

  // Admin Order APIs
  getAllOrdersAdmin(status?: OrderStatus, search?: string, page = 0, size = 15): Observable<ApiResponse<Page<Order>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);

    return this.http.get<ApiResponse<Page<Order>>>(`${this.BASE_URL}/admin/orders`, { params });
  }

  updateOrderStatusAdmin(id: number, req: OrderStatusUpdateRequest): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${this.BASE_URL}/admin/orders/${id}/status`, req);
  }

  // Store Manager APIs
  getManagerOrders(status?: OrderStatus, search?: string, page = 0, size = 15): Observable<ApiResponse<Page<Order>>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);

    return this.http.get<ApiResponse<Page<Order>>>(`${this.BASE_URL}/manager/orders`, { params });
  }

  updateOrderStatusManager(id: number, req: OrderStatusUpdateRequest): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${this.BASE_URL}/manager/orders/${id}/status`, req);
  }
}
