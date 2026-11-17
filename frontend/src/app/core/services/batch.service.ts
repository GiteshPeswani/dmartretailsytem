import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, ProductBatch, BatchCreateRequest, VerificationResult } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class BatchService {
  private readonly BASE_URL = 'http://localhost:8080/api/batches';

  constructor(private http: HttpClient) {}

  private normalize(b: any): ProductBatch {
    if (!b) return b;
    return {
      ...b,
      currentStage: b.currentStage || b.stage || 'MANUFACTURED',
      stage: b.stage || b.currentStage || 'MANUFACTURED',
      manufacturerName: b.manufacturerName || 'Certified D-Mart Mill / Brand Producer'
    };
  }

  getAllBatches(): Observable<ApiResponse<ProductBatch[]>> {
    return this.http.get<ApiResponse<ProductBatch[]>>(this.BASE_URL).pipe(
      map(res => ({
        ...res,
        data: (res.data || []).map(b => this.normalize(b))
      }))
    );
  }

  getBatchById(id: number): Observable<ApiResponse<ProductBatch>> {
    return this.http.get<ApiResponse<ProductBatch>>(`${this.BASE_URL}/${id}`).pipe(
      map(res => ({ ...res, data: this.normalize(res.data) }))
    );
  }

  getBatchByNumber(batchNumber: string): Observable<ApiResponse<ProductBatch>> {
    return this.http.get<ApiResponse<ProductBatch>>(`${this.BASE_URL}/by-number/${encodeURIComponent(batchNumber)}`).pipe(
      map(res => ({ ...res, data: this.normalize(res.data) }))
    );
  }

  verifyBatch(id: number): Observable<ApiResponse<VerificationResult>> {
    return this.http.get<ApiResponse<VerificationResult>>(`${this.BASE_URL}/${id}/verify`);
  }

  createBatch(req: BatchCreateRequest): Observable<ApiResponse<ProductBatch>> {
    return this.http.post<ApiResponse<ProductBatch>>(this.BASE_URL, req).pipe(
      map(res => ({ ...res, data: this.normalize(res.data) }))
    );
  }

  recordWarehouseArrival(id: number): Observable<ApiResponse<ProductBatch>> {
    return this.http.put<ApiResponse<ProductBatch>>(`${this.BASE_URL}/${id}/warehouse-arrival`, {}).pipe(
      map(res => ({ ...res, data: this.normalize(res.data) }))
    );
  }

  recordStoreArrival(id: number): Observable<ApiResponse<ProductBatch>> {
    return this.http.put<ApiResponse<ProductBatch>>(`${this.BASE_URL}/${id}/store-arrival`, {}).pipe(
      map(res => ({ ...res, data: this.normalize(res.data) }))
    );
  }
}
