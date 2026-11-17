import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'products',
    loadComponent: () => import('./features/products/product-list/product-list.component').then(m => m.ProductListComponent)
  },
  {
    path: 'products/:id',
    loadComponent: () => import('./features/products/product-detail/product-detail.component').then(m => m.ProductDetailComponent)
  },
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cart/cart.component').then(m => m.CartComponent)
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./features/checkout/checkout.component').then(m => m.CheckoutComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-list/order-list.component').then(m => m.OrderListComponent)
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent)
  },
  // Admin Routes
  {
    path: 'admin/dashboard',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/products',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/products/admin-products.component').then(m => m.AdminProductsComponent)
  },
  {
    path: 'admin/categories',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/categories/admin-categories.component').then(m => m.AdminCategoriesComponent)
  },
  {
    path: 'admin/orders',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/admin/orders/admin-orders.component').then(m => m.AdminOrdersComponent)
  },
  // Store Manager Routes
  {
    path: 'store-manager',
    canActivate: [roleGuard(['STORE_MANAGER', 'ADMIN'])],
    loadComponent: () => import('./features/store-manager/store-manager.component').then(m => m.StoreManagerComponent)
  },
  // Batch Tracking & Verification
  {
    path: 'batch-tracking',
    canActivate: [roleGuard(['STORE_MANAGER', 'ADMIN'])],
    loadComponent: () => import('./features/batch-tracking/batch-tracking.component').then(m => m.BatchTrackingComponent)
  },
  // Blockchain Explorer (Admin Only)
  {
    path: 'blockchain-explorer',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/blockchain-explorer/blockchain-explorer.component').then(m => m.BlockchainExplorerComponent)
  },
  {
    path: 'access-denied',
    loadComponent: () => import('./features/access-denied/access-denied.component').then(m => m.AccessDeniedComponent)
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
