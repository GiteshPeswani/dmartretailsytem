import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, DashboardSummary } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly BASE_URL = 'http://localhost:8080/api/admin/dashboard';

  constructor(private http: HttpClient) {}

  getSummary(): Observable<ApiResponse<DashboardSummary>> {
    return this.http.get<ApiResponse<DashboardSummary>>(`${this.BASE_URL}/summary`);
  }
}
