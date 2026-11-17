import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiResponse, AuthResponse, User, UserProfileResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'dmart_auth_token';
  private readonly USER_KEY = 'dmart_user';

  // Reactive state using Angular Signals
  currentUser = signal<User | null>(this.getStoredUser());
  token = signal<string | null>(localStorage.getItem(this.TOKEN_KEY));

  isAuthenticated = computed(() => !!this.token());
  isAdmin = computed(() => this.currentUser()?.roles.includes('ADMIN') ?? false);
  isStoreManager = computed(() => this.currentUser()?.roles.includes('STORE_MANAGER') ?? false);
  isStaff = computed(() => this.isAdmin() || this.isStoreManager());
  isCustomer = computed(() => this.currentUser()?.roles.includes('CUSTOMER') ?? false);

  constructor(private http: HttpClient, private router: Router) {}

  register(data: any): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API_URL}/register`, data).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  login(credentials: any): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API_URL}/login`, credentials).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.setSession(res.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  refreshProfile(): Observable<ApiResponse<UserProfileResponse>> {
    return this.http.get<ApiResponse<UserProfileResponse>>(`${this.API_URL}/me`).pipe(
      tap(res => {
        if (res.success && res.data) {
          const user: User = {
            id: res.data.id,
            email: res.data.email,
            firstName: res.data.firstName,
            lastName: res.data.lastName,
            phone: res.data.phone,
            roles: Array.from(res.data.roles)
          };
          this.currentUser.set(user);
          localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        }
      })
    );
  }

  private setSession(auth: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, auth.token);
    const user: User = {
      id: auth.id,
      email: auth.email,
      firstName: auth.firstName,
      lastName: auth.lastName,
      roles: auth.roles
    };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.token.set(auth.token);
    this.currentUser.set(user);
  }

  private getStoredUser(): User | null {
    try {
      const stored = localStorage.getItem(this.USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }
}
