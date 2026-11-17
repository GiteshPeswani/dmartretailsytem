import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BatchService } from '../../core/services/batch.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductBatch, VerificationResult } from '../../core/models/models';

@Component({
  selector: 'app-batch-tracking',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './batch-tracking.component.html',
  styleUrls: ['./batch-tracking.component.css']
})
export class BatchTrackingComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private batchService = inject(BatchService);
  private toast = inject(ToastService);

  searchQuery = 'BATCH-2026-MILK-01';
  loading = false;
  batch: ProductBatch | null = null;
  verificationResult: VerificationResult | null = null;
  verifying = false;

  allBatches: ProductBatch[] = [];

  ngOnInit(): void {
    this.loadAllBatches();
    this.route.queryParams.subscribe(params => {
      const q = params['batchNumber'] || params['batchId'];
      if (q) {
        this.searchQuery = q;
        this.trackBatch();
      } else {
        this.trackBatch();
      }
    });
  }

  loadAllBatches(): void {
    this.batchService.getAllBatches().subscribe({
      next: (res) => {
        if (res.success) {
          this.allBatches = res.data;
        }
      }
    });
  }

  trackBatch(): void {
    if (!this.searchQuery.trim()) return;
    this.loading = true;
    this.batch = null;
    this.verificationResult = null;

    this.batchService.getBatchByNumber(this.searchQuery.trim()).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success && res.data) {
          this.batch = res.data;
          this.verifyProvenance(this.batch.id);
        } else {
          this.toast.error('Batch record not found.');
        }
      },
      error: () => {
        this.loading = false;
        this.toast.error(`Batch "${this.searchQuery}" not found on blockchain registry.`);
      }
    });
  }

  selectSample(batchNumber: string): void {
    this.searchQuery = batchNumber;
    this.trackBatch();
  }

  verifyProvenance(batchId: number): void {
    this.verifying = true;
    this.batchService.verifyBatch(batchId).subscribe({
      next: (res) => {
        this.verifying = false;
        this.verificationResult = res.data;
      },
      error: () => {
        this.verifying = false;
      }
    });
  }

  getQrCodeUrl(batch: ProductBatch | string | null): string {
    if (!batch) return '';
    let details = '';
    if (typeof batch === 'object') {
      details = [
        `📦 D-MART BATCH PROVENANCE`,
        `Batch Lot: ${batch.batchNumber}`,
        `Product: ${batch.productName} (${batch.productSku})`,
        `Manufacturer: ${batch.manufacturerName || 'Certified Supplier'}`,
        `Stage: ${batch.currentStage || 'MANUFACTURED'}`,
        `Blockchain Status: ${batch.blockchainStatus || 'CONFIRMED'}`,
        `Mfd: ${batch.manufactureDate || 'N/A'} | Exp: ${batch.expiryDate || 'N/A'}`,
        `Authenticity: 100% Genuine Direct Sourcing`
      ].join('\n');
    } else {
      details = `D-MART BATCH: ${batch} | Genuine Sourced`;
    }
    return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(details)}`;
  }
}
