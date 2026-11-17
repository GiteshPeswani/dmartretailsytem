import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BlockchainService } from '../../core/services/blockchain.service';
import { ProductService } from '../../core/services/product.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';
import { BlockchainTransaction, VerificationResult } from '../../core/models/models';

@Component({
  selector: 'app-blockchain-explorer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './blockchain-explorer.component.html',
  styleUrls: ['./blockchain-explorer.component.css']
})
export class BlockchainExplorerComponent implements OnInit {
  private blockchainService = inject(BlockchainService);
  private productService = inject(ProductService);
  private orderService = inject(OrderService);
  private toast = inject(ToastService);

  connected = false;
  contractReady = false;
  transactions: BlockchainTransaction[] = [];
  loading = true;

  // Search Verification
  searchType: 'PRODUCT' | 'ORDER' = 'ORDER';
  searchId: number | null = null;
  verifying = false;
  verificationResult: VerificationResult | null = null;

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.blockchainService.getStatus().subscribe({
      next: (res) => {
        this.connected = res.data.connected;
        this.contractReady = res.data.contractReady;
      },
      error: () => {
        this.connected = false;
        this.contractReady = false;
      }
    });

    this.blockchainService.getRecentTransactions().subscribe({
      next: (res) => {
        if (res.success) this.transactions = res.data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  executeVerification(): void {
    if (!this.searchId) return;

    this.verifying = true;
    this.verificationResult = null;

    if (this.searchType === 'PRODUCT') {
      this.productService.verifyProduct(this.searchId).subscribe({
        next: (res) => {
          this.verifying = false;
          this.verificationResult = res.data;
        },
        error: () => {
          this.verifying = false;
          this.toast.error('Product not found or verification error');
        }
      });
    } else {
      this.orderService.verifyOrder(this.searchId).subscribe({
        next: (res) => {
          this.verifying = false;
          this.verificationResult = res.data;
        },
        error: () => {
          this.verifying = false;
          this.toast.error('Order not found or verification error');
        }
      });
    }
  }
}
