import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse, CartSummary } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly API_URL = 'http://localhost:8080/api/cart';

  cart = signal<CartSummary | null>(null);
  itemCount = computed(() => this.cart()?.totalItemCount ?? 0);
  grandTotal = computed(() => this.cart()?.grandTotal ?? 0);

  constructor(private http: HttpClient) {}

  fetchCart(): Observable<ApiResponse<CartSummary>> {
    return this.http.get<ApiResponse<CartSummary>>(this.API_URL).pipe(
      tap(res => {
        if (res.success) {
          this.cart.set(res.data);
        }
      })
    );
  }

  addToCart(productId: number, quantity = 1): Observable<ApiResponse<CartSummary>> {
    return this.http.post<ApiResponse<CartSummary>>(`${this.API_URL}/items`, { productId, quantity }).pipe(
      tap(res => {
        if (res.success) {
          this.cart.set(res.data);
        }
      })
    );
  }

  updateQuantity(productId: number, quantity: number): Observable<ApiResponse<CartSummary>> {
    return this.http.put<ApiResponse<CartSummary>>(`${this.API_URL}/items/${productId}?quantity=${quantity}`, {}).pipe(
      tap(res => {
        if (res.success) {
          this.cart.set(res.data);
        }
      })
    );
  }

  removeFromCart(productId: number): Observable<ApiResponse<CartSummary>> {
    return this.http.delete<ApiResponse<CartSummary>>(`${this.API_URL}/items/${productId}`).pipe(
      tap(res => {
        if (res.success) {
          this.cart.set(res.data);
        }
      })
    );
  }

  clearCart(): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(this.API_URL).pipe(
      tap(res => {
        if (res.success) {
          this.cart.set(null);
        }
      })
    );
  }
}
