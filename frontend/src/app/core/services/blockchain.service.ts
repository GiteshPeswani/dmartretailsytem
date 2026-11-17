import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, BlockchainTransaction, AuditEvent, BlockchainStats } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class BlockchainService {
  private readonly BASE_URL = 'http://localhost:8080/api/blockchain';

  constructor(private http: HttpClient) {}

  getStatus(): Observable<ApiResponse<{ connected: boolean; contractReady: boolean }>> {
    return this.http.get<ApiResponse<{ connected: boolean; contractReady: boolean }>>(`${this.BASE_URL}/status`);
  }

  getStats(): Observable<ApiResponse<BlockchainStats>> {
    return this.http.get<ApiResponse<BlockchainStats>>(`${this.BASE_URL}/stats`);
  }

  getRecentTransactions(): Observable<ApiResponse<BlockchainTransaction[]>> {
    return this.http.get<ApiResponse<BlockchainTransaction[]>>(`${this.BASE_URL}/transactions`);
  }

  getAuditTrail(entityType: 'PRODUCT' | 'ORDER', entityId: number): Observable<ApiResponse<AuditEvent[]>> {
    return this.http.get<ApiResponse<AuditEvent[]>>(`${this.BASE_URL}/audit/${entityType}/${entityId}`);
  }

  getEntityTransactions(entityType: 'PRODUCT' | 'ORDER', entityId: number): Observable<ApiResponse<BlockchainTransaction[]>> {
    return this.http.get<ApiResponse<BlockchainTransaction[]>>(`${this.BASE_URL}/transactions/${entityType}/${entityId}`);
  }
}
