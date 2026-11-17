import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div class="container py-5 text-center my-5">
      <div class="my-5">
        <i class="bi bi-question-circle text-muted display-1"></i>
        <h2 class="fw-bold text-dark mt-4">404 - Page Not Found</h2>
        <p class="text-muted lead">The page you requested does not exist or has been moved.</p>
        <a routerLink="/" class="btn btn-dmart mt-3">Back to Homepage</a>
      </div>
    </div>
  `
})
export class NotFoundComponent {}
