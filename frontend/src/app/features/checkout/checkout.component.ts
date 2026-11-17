import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  cartService = inject(CartService);
  private orderService = inject(OrderService);
  private toast = inject(ToastService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  checkoutForm: FormGroup = this.fb.group({
    street: ['', [Validators.required, Validators.minLength(5)]],
    city: ['', [Validators.required]],
    state: ['', [Validators.required]],
    postalCode: ['', [Validators.required, Validators.pattern('^[0-9]{6}$')]],
    notes: ['']
  });

  submitting = false;

  ngOnInit(): void {
    if (this.cartService.itemCount() === 0) {
      this.toast.warning('Your cart is empty. Please add items before checkout.');
      this.router.navigate(['/cart']);
    }
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      return;
    }

    this.submitting = true;

    const f = this.checkoutForm.value;
    const fullAddress = `${f.street}, ${f.city}, ${f.state} - ${f.postalCode}`;

    this.orderService.checkout({
      deliveryAddress: fullAddress,
      notes: f.notes
    }).subscribe({
      next: (res) => {
        this.submitting = false;
        this.cartService.cart.set(null); // Clear cart state in service
        this.toast.success('Order placed successfully! Submitting to blockchain...');
        this.router.navigate(['/orders', res.data.id]);
      },
      error: (err) => {
        this.submitting = false;
        this.toast.error(err.error?.message || 'Checkout failed. Please check stock and try again.');
      }
    });
  }
}
