import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Product, Category, Page } from '../../../core/models/models';
import { ProductImagePipe } from '../../../shared/pipes/product-image.pipe';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ProductImagePipe],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.css']
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  products: Product[] = [];
  categories: Category[] = [];
  selectedCategoryId: number | null = null;
  searchQuery = '';
  selectedSort = 'id,desc';

  currentPage = 0;
  pageSize = 12;
  totalElements = 0;
  totalPages = 0;
  loading = false;

  ngOnInit(): void {
    this.productService.getCategories().subscribe(res => {
      if (res.success) this.categories = res.data;
    });

    this.route.queryParams.subscribe(params => {
      this.selectedCategoryId = params['categoryId'] ? +params['categoryId'] : null;
      this.searchQuery = params['search'] || '';
      this.currentPage = params['page'] ? +params['page'] : 0;
      this.loadProducts();
    });
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getProducts(
      this.selectedCategoryId || undefined,
      this.searchQuery || undefined,
      this.currentPage,
      this.pageSize,
      this.selectedSort
    ).subscribe({
      next: (res) => {
        if (res.success) {
          this.products = res.data.content;
          this.totalElements = res.data.totalElements;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  filterByCategory(catId: number | null): void {
    this.selectedCategoryId = catId;
    this.currentPage = 0;
    this.updateRoute();
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.updateRoute();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.updateRoute();
    }
  }

  private updateRoute(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        categoryId: this.selectedCategoryId || null,
        search: this.searchQuery || null,
        page: this.currentPage > 0 ? this.currentPage : null
      },
      queryParamsHandling: 'merge'
    });
  }

  addToCart(product: Product): void {
    if (!this.authService.isAuthenticated()) {
      this.toast.warning('Please log in to add items to your cart.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: '/products' } });
      return;
    }

    this.cartService.addToCart(product.id, 1).subscribe({
      next: () => {
        this.toast.success(`Added ${product.name} to cart!`);
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Failed to add item to cart.');
      }
    });
  }
}
