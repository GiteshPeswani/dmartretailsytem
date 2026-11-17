import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { CartSummary } from '../../core/models/models';
import { ProductImagePipe } from '../../shared/pipes/product-image.pipe';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductImagePipe],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cartService = inject(CartService);
  private toast = inject(ToastService);
  private router = inject(Router);

  loading = true;

  ngOnInit(): void {
    this.loadCart();
  }

  loadCart(): void {
    this.loading = true;
    this.cartService.fetchCart().subscribe({
      next: () => {
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  updateQuantity(productId: number, newQty: number): void {
    if (newQty < 1) {
      this.removeItem(productId);
      return;
    }

    this.cartService.updateQuantity(productId, newQty).subscribe({
      next: () => {
        this.toast.info('Cart updated');
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Could not update quantity');
      }
    });
  }

  removeItem(productId: number): void {
    this.cartService.removeFromCart(productId).subscribe({
      next: () => {
        this.toast.info('Item removed from cart');
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Could not remove item');
      }
    });
  }

  clearCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.cartService.clearCart().subscribe({
        next: () => {
          this.toast.info('Cart cleared');
        }
      });
    }
  }

  proceedToCheckout(): void {
    this.router.navigate(['/checkout']);
  }
}
