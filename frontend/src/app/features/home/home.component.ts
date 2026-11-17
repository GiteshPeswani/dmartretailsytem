import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProductImagePipe } from '../../shared/pipes/product-image.pipe';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Product, Category } from '../../core/models/models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductImagePipe],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  categories: Category[] = [];
  featuredProducts: Product[] = [];
  discountedProducts: Product[] = [];
  loading = true;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.productService.getCategories().subscribe(res => {
      if (res.success) this.categories = res.data;
    });

    this.productService.getProducts(undefined, undefined, 0, 8, 'id,desc').subscribe({
      next: (res) => {
        if (res.success) {
          this.featuredProducts = res.data.content;
          this.discountedProducts = res.data.content.filter(p => p.discountPercent > 0);
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  addToCart(product: Product): void {
    if (!this.authService.isAuthenticated()) {
      this.toast.warning('Please log in to add items to your cart.');
      return;
    }

    this.cartService.addToCart(product.id, 1).subscribe({
      next: () => {
        this.toast.success(`Added ${product.name} to cart!`);
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Could not add to cart.');
      }
    });
  }
}
