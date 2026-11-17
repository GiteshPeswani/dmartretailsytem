import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div class="container py-5 text-center my-5">
      <div class="my-5">
        <i class="bi bi-shield-slash text-danger display-1"></i>
        <h2 class="fw-bold text-dark mt-4">403 - Access Denied</h2>
        <p class="text-muted lead">You do not have the required permissions to view this administrative page.</p>
        <a routerLink="/" class="btn btn-dmart mt-3">Return to Storefront</a>
      </div>
    </div>
  `
})
export class AccessDeniedComponent {}
