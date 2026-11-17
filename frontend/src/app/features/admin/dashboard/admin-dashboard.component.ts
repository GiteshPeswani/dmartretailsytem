import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { BlockchainService } from '../../../core/services/blockchain.service';
import { DashboardSummary, BlockchainStats } from '../../../core/models/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  authService = inject(AuthService);
  private dashboardService = inject(DashboardService);
  private blockchainService = inject(BlockchainService);

  summary: DashboardSummary | null = null;
  blockchainStats: BlockchainStats | null = null;
  loading = true;

  logout(): void {
    this.authService.logout();
  }

  ngOnInit(): void {
    this.loadSummary();
    this.loadBlockchainStats();
  }

  loadSummary(): void {
    this.loading = true;
    this.dashboardService.getSummary().subscribe({
      next: (res) => {
        if (res.success) {
          this.summary = res.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadBlockchainStats(): void {
    this.blockchainService.getStats().subscribe({
      next: (res) => {
        if (res.success) {
          this.blockchainStats = res.data;
        }
      },
      error: (err) => {
        console.error('Failed to load blockchain dashboard stats:', err);
      }
    });
  }
}
