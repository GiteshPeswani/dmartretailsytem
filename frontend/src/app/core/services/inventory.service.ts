import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Page, Product, InventoryMovement, InventoryUpdateRequest } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class InventoryService {
  private readonly BASE_URL = 'http://localhost:8080/api/manager';

  constructor(private http: HttpClient) {}

  adjustInventory(req: InventoryUpdateRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(`${this.BASE_URL}/inventory/adjust`, req);
  }

  getProductMovements(productId: number, page = 0, size = 15): Observable<ApiResponse<Page<InventoryMovement>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<Page<InventoryMovement>>>(`${this.BASE_URL}/inventory/${productId}/movements`, { params });
  }
}
