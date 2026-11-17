import { Component, OnInit, inject, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { BlockchainService } from '../../../core/services/blockchain.service';
import { ToastService } from '../../../core/services/toast.service';
import { Order, VerificationResult, AuditEvent, BlockchainTransaction } from '../../../core/models/models';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  encapsulation: ViewEncapsulation.None,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.css']
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private blockchainService = inject(BlockchainService);
  private toast = inject(ToastService);

  order: Order | null = null;
  loading = true;

  // Blockchain verification
  verifying = false;
  verificationResult: VerificationResult | null = null;
  tampering = false;
  showTechnicalProof = false;

  toggleTechnicalProof(): void {
    this.showTechnicalProof = !this.showTechnicalProof;
  }

  // Quick lookup input for testing any Order ID
  lookupInput = '';
  lookupLoading = false;
  lookupResult: VerificationResult | null = null;

  // Audit trail (Immutable Event History)
  auditTrail: AuditEvent[] = [];
  transactions: BlockchainTransaction[] = [];

  readonly statusSteps = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadOrder(+id);
    }
  }

  loadOrder(id: number, isSilentRefresh = false): void {
    if (!isSilentRefresh) this.loading = true;
    this.orderService.getOrderById(id).subscribe({
      next: (res) => {
        if (res.success) {
          this.order = res.data;
          this.loadAudit(id);

          // Auto-run verification so user sees VERIFIED immediately
          if (!this.verificationResult) {
            this.verifyOnChain(true);
          }

          // If still pending or not submitted, auto-refresh once after mining delay
          if (!isSilentRefresh && (this.order.blockchainStatus === 'PENDING' || this.order.blockchainStatus === 'NOT_SUBMITTED')) {
            setTimeout(() => {
              this.loadOrder(id, true);
            }, 3000);
          }
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        if (!isSilentRefresh) this.toast.error('Order not found or access denied.');
      }
    });
  }

  loadAudit(orderId: number): void {
    this.blockchainService.getAuditTrail('ORDER', orderId).subscribe(res => {
      if (res.success) this.auditTrail = res.data;
    });

    this.blockchainService.getEntityTransactions('ORDER', orderId).subscribe(res => {
      if (res.success) this.transactions = res.data;
    });
  }

  verifyOnChain(silent = false): void {
    if (!this.order) return;
    this.verifying = true;

    this.orderService.verifyOrder(this.order.id).subscribe({
      next: (res) => {
        this.verifying = false;
        this.verificationResult = res.data;
        if (!silent) {
          if (res.data.verified) {
            this.toast.success('Order integrity verified! On-chain Keccak-256 hash matches MySQL database.');
          } else if (res.data.status === 'TAMPERED' || res.data.isTampered) {
            this.toast.error('TAMPERED DETECTED! Database content does not match blockchain hash.');
          } else {
            this.toast.warning(res.data.message);
          }
        }
      },
      error: () => {
        this.verifying = false;
        if (!silent) this.toast.error('Could not complete blockchain verification.');
      }
    });
  }

  simulateTamper(): void {
    if (!this.order) return;
    this.tampering = true;
    this.orderService.simulateTamper(this.order.id).subscribe({
      next: (res) => {
        this.tampering = false;
        this.toast.warning('Simulated Database Tamper: Total modified to ₹1.00 in MySQL!');
        // Refresh local order data to reflect changed MySQL total, then re-verify
        this.orderService.getOrderById(this.order!.id).subscribe(ordRes => {
          if (ordRes.success) {
            this.order = ordRes.data;
            this.verifyOnChain();
          }
        });
      },
      error: (err) => {
        this.tampering = false;
        this.toast.error(err.error?.message || 'Failed to simulate tamper.');
      }
    });
  }

  restoreTamper(): void {
    if (!this.order) return;
    this.tampering = true;
    this.orderService.restoreTamper(this.order.id).subscribe({
      next: (res) => {
        this.tampering = false;
        this.toast.success('Database Restored: Total amount recalculated from authentic line items.');
        this.orderService.getOrderById(this.order!.id).subscribe(ordRes => {
          if (ordRes.success) {
            this.order = ordRes.data;
            this.verifyOnChain();
          }
        });
      },
      error: (err) => {
        this.tampering = false;
        this.toast.error(err.error?.message || 'Failed to restore order data.');
      }
    });
  }

  lookupOrderByInput(): void {
    if (!this.lookupInput.trim()) return;
    this.lookupLoading = true;
    this.lookupResult = null;

    this.orderService.verifyOrderByLookup(this.lookupInput.trim()).subscribe({
      next: (res) => {
        this.lookupLoading = false;
        this.lookupResult = res.data;
      },
      error: (err) => {
        this.lookupLoading = false;
        this.toast.error(err.error?.message || 'Order lookup failed.');
      }
    });
  }

  isStepCompleted(step: string): boolean {
    if (!this.order || this.order.status === 'CANCELLED') return false;
    const orderIdx = this.statusSteps.indexOf(this.order.status);
    const stepIdx = this.statusSteps.indexOf(step);
    return orderIdx >= stepIdx;
  }
}
