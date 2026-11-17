import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { BlockchainService } from '../../../core/services/blockchain.service';
import { ProductService } from '../../../core/services/product.service';
import { Category } from '../../../core/models/models';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  cartService = inject(CartService);
  private blockchainService = inject(BlockchainService);
  private productService = inject(ProductService);
  private router = inject(Router);

  searchQuery = '';
  categories: Category[] = [];
  blockchainOnline = false;
  userDropdownOpen = false;
  categoryDropdownOpen = false;

  toggleUserDropdown(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.userDropdownOpen = !this.userDropdownOpen;
    this.categoryDropdownOpen = false;
  }

  toggleCategoryDropdown(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.categoryDropdownOpen = !this.categoryDropdownOpen;
    this.userDropdownOpen = false;
  }

  closeDropdowns(): void {
    this.userDropdownOpen = false;
    this.categoryDropdownOpen = false;
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.cartService.fetchCart().subscribe();
    }

    this.productService.getCategories().subscribe(res => {
      if (res.success) {
        this.categories = res.data;
      }
    });

    this.checkBlockchainStatus();
  }

  checkBlockchainStatus(): void {
    this.blockchainService.getStatus().subscribe({
      next: (res) => {
        this.blockchainOnline = res.success && res.data.connected;
      },
      error: () => {
        this.blockchainOnline = false;
      }
    });
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/products'], { queryParams: { search: this.searchQuery.trim() } });
    }
  }

  logout(): void {
    this.closeDropdowns();
    this.authService.logout();
  }
}
