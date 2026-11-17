import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../core/services/toast.service';
import { Product, Category, ProductRequest } from '../../../core/models/models';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './admin-products.component.html',
  styleUrls: ['./admin-products.component.css']
})
export class AdminProductsComponent implements OnInit {
  private productService = inject(ProductService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  products: Product[] = [];
  categories: Category[] = [];
  loading = true;
  currentPage = 0;
  totalPages = 0;

  showModal = false;
  editingProduct: Product | null = null;
  submitting = false;

  productForm: FormGroup = this.fb.group({
    sku: ['', [Validators.required]],
    name: ['', [Validators.required]],
    description: [''],
    categoryId: [null, [Validators.required]],
    price: [null, [Validators.required, Validators.min(0.01)]],
    discountPercent: [0, [Validators.min(0), Validators.max(100)]],
    stockQuantity: [0, [Validators.required, Validators.min(0)]],
    imageUrl: [''],
    active: [true]
  });

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.productService.getCategories().subscribe(res => {
      if (res.success) this.categories = res.data;
    });
  }

  loadProducts(page = 0): void {
    this.loading = true;
    this.productService.getAllProductsAdmin(page, 15).subscribe({
      next: (res) => {
        if (res.success) {
          this.products = res.data.content;
          this.currentPage = res.data.number;
          this.totalPages = res.data.totalPages;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.editingProduct = null;
    this.productForm.reset({
      sku: '',
      name: '',
      description: '',
      categoryId: this.categories.length > 0 ? this.categories[0].id : null,
      price: null,
      discountPercent: 0,
      stockQuantity: 50,
      active: true
    });
    this.showModal = true;
  }

  openEditModal(product: Product): void {
    this.editingProduct = product;
    this.productForm.patchValue({
      sku: product.sku,
      name: product.name,
      description: product.description,
      categoryId: product.categoryId,
      price: product.price,
      discountPercent: product.discountPercent,
      stockQuantity: product.stockQuantity,
      imageUrl: product.imageUrl,
      active: product.active
    });
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editingProduct = null;
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const req: ProductRequest = this.productForm.value;

    if (this.editingProduct) {
      this.productService.updateProduct(this.editingProduct.id, req).subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Product updated successfully!');
          this.closeModal();
          this.loadProducts(this.currentPage);
        },
        error: (err) => {
          this.submitting = false;
          this.toast.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      this.productService.createProduct(req).subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Product created and submitted to blockchain!');
          this.closeModal();
          this.loadProducts(0);
        },
        error: (err) => {
          this.submitting = false;
          this.toast.error(err.error?.message || 'Creation failed');
        }
      });
    }
  }

  deactivateProduct(product: Product): void {
    if (confirm(`Are you sure you want to deactivate "${product.name}"?`)) {
      this.productService.deactivateProduct(product.id).subscribe({
        next: () => {
          this.toast.success('Product deactivated');
          this.loadProducts(this.currentPage);
        }
      });
    }
  }
}
