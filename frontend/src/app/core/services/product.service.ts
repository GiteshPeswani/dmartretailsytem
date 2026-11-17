import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Page, Product, ProductRequest, Category, CategoryRequest, VerificationResult } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly BASE_URL = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getProducts(categoryId?: number, search?: string, page = 0, size = 12, sort = 'id,desc'): Observable<ApiResponse<Page<Product>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (categoryId) params = params.set('categoryId', categoryId.toString());
    if (search) params = params.set('search', search);

    return this.http.get<ApiResponse<Page<Product>>>(`${this.BASE_URL}/products`, { params });
  }

  getProductById(id: number): Observable<ApiResponse<Product>> {
    return this.http.get<ApiResponse<Product>>(`${this.BASE_URL}/products/${id}`);
  }

  verifyProduct(id: number): Observable<ApiResponse<VerificationResult>> {
    return this.http.get<ApiResponse<VerificationResult>>(`${this.BASE_URL}/products/${id}/verify`);
  }

  getCategories(): Observable<ApiResponse<Category[]>> {
    return this.http.get<ApiResponse<Category[]>>(`${this.BASE_URL}/categories`);
  }

  // Admin APIs
  getAllProductsAdmin(page = 0, size = 15): Observable<ApiResponse<Page<Product>>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<Page<Product>>>(`${this.BASE_URL}/admin/products`, { params });
  }

  createProduct(req: ProductRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(`${this.BASE_URL}/admin/products`, req);
  }

  updateProduct(id: number, req: ProductRequest): Observable<ApiResponse<Product>> {
    return this.http.put<ApiResponse<Product>>(`${this.BASE_URL}/admin/products/${id}`, req);
  }

  deactivateProduct(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.BASE_URL}/admin/products/${id}`);
  }

  // Admin Category APIs
  getAllCategoriesAdmin(): Observable<ApiResponse<Category[]>> {
    return this.http.get<ApiResponse<Category[]>>(`${this.BASE_URL}/admin/categories`);
  }

  createCategory(req: CategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.post<ApiResponse<Category>>(`${this.BASE_URL}/admin/categories`, req);
  }

  updateCategory(id: number, req: CategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.put<ApiResponse<Category>>(`${this.BASE_URL}/admin/categories/${id}`, req);
  }

  deactivateCategory(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.BASE_URL}/admin/categories/${id}`);
  }
}
