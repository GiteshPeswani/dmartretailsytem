import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);

  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  loading = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.loading = false;
        this.toast.success('Welcome back, ' + res.data.firstName + '!');

        const returnUrl = this.route.snapshot.queryParams['returnUrl'];
        if (returnUrl) {
          this.router.navigateByUrl(returnUrl);
        } else if (res.data.roles.includes('ADMIN')) {
          this.router.navigate(['/admin/dashboard']);
        } else if (res.data.roles.includes('STORE_MANAGER')) {
          this.router.navigate(['/store-manager']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Invalid email or password.';
        this.toast.error(this.errorMessage);
      }
    });
  }

  // Convenience helper for academic demo to autofill test accounts
  quickFill(role: string): void {
    if (role === 'admin') {
      this.loginForm.patchValue({ email: 'admin@dmart.local', password: 'Admin@123' });
    } else if (role === 'manager') {
      this.loginForm.patchValue({ email: 'manager@dmart.local', password: 'Manager@123' });
    }
  }
}
