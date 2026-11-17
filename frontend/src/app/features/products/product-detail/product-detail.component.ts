import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Product, VerificationResult } from '../../../core/models/models';
import { ProductImagePipe } from '../../../shared/pipes/product-image.pipe';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ProductImagePipe],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.css']
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  product: Product | null = null;
  quantity = 1;
  loading = true;

  // Blockchain verification state
  verifying = false;
  verificationResult: VerificationResult | null = null;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(+id);
    }
  }

  loadProduct(id: number): void {
    this.loading = true;
    this.productService.getProductById(id).subscribe({
      next: (res) => {
        if (res.success) {
          this.product = res.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Product not found');
        this.router.navigate(['/products']);
      }
    });
  }

  verifyOnChain(): void {
    if (!this.product) return;
    this.verifying = true;
    this.verificationResult = null;

    this.productService.verifyProduct(this.product.id).subscribe({
      next: (res) => {
        this.verifying = false;
        this.verificationResult = res.data;
        if (res.data.verified) {
          this.toast.success('Blockchain record verified! Hash matches on-chain state.');
        } else if (!res.data.blockchainAvailable) {
          this.toast.warning('Local blockchain node is offline.');
        } else {
          this.toast.error('Verification failed! Hash mismatch detected.');
        }
      },
      error: () => {
        this.verifying = false;
        this.toast.error('Could not verify on blockchain.');
      }
    });
  }

  addToCart(): void {
    if (!this.product) return;
    if (!this.authService.isAuthenticated()) {
      this.toast.warning('Please sign in to add items to your shopping cart.');
      this.router.navigate(['/auth/login'], { queryParams: { returnUrl: `/products/${this.product.id}` } });
      return;
    }

    this.cartService.addToCart(this.product.id, this.quantity).subscribe({
      next: () => {
        this.toast.success(`Added ${this.quantity} × ${this.product?.name} to cart!`);
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Failed to add item to cart.');
      }
    });
  }
}
