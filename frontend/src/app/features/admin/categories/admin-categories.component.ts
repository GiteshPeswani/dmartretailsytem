import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { ToastService } from '../../../core/services/toast.service';
import { Category, CategoryRequest } from '../../../core/models/models';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-categories.component.html',
  styleUrls: ['./admin-categories.component.css']
})
export class AdminCategoriesComponent implements OnInit {
  private productService = inject(ProductService);
  private toast = inject(ToastService);
  private fb = inject(FormBuilder);

  categories: Category[] = [];
  loading = true;
  showModal = false;
  editingCategory: Category | null = null;
  submitting = false;

  categoryForm: FormGroup = this.fb.group({
    name: ['', [Validators.required]],
    slug: [''],
    description: [''],
    active: [true]
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
    this.productService.getAllCategoriesAdmin().subscribe({
      next: (res) => {
        if (res.success) this.categories = res.data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  openCreateModal(): void {
    this.editingCategory = null;
    this.categoryForm.reset({ name: '', slug: '', description: '', active: true });
    this.showModal = true;
  }

  openEditModal(cat: Category): void {
    this.editingCategory = cat;
    this.categoryForm.patchValue({
      name: cat.name,
      slug: cat.slug,
      description: cat.description,
      active: cat.active
    });
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editingCategory = null;
  }

  onSubmit(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const req: CategoryRequest = this.categoryForm.value;

    if (this.editingCategory) {
      this.productService.updateCategory(this.editingCategory.id, req).subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Category updated successfully');
          this.closeModal();
          this.loadCategories();
        },
        error: (err) => {
          this.submitting = false;
          this.toast.error(err.error?.message || 'Update failed');
        }
      });
    } else {
      this.productService.createCategory(req).subscribe({
        next: () => {
          this.submitting = false;
          this.toast.success('Category created successfully');
          this.closeModal();
          this.loadCategories();
        },
        error: (err) => {
          this.submitting = false;
          this.toast.error(err.error?.message || 'Creation failed');
        }
      });
    }
  }

  deactivateCategory(cat: Category): void {
    if (confirm(`Deactivate category "${cat.name}"?`)) {
      this.productService.deactivateCategory(cat.id).subscribe({
        next: () => {
          this.toast.success('Category deactivated');
          this.loadCategories();
        }
      });
    }
  }
}
