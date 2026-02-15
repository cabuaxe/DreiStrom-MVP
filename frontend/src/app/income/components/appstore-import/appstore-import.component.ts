import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { AppStoreService } from '../../services/appstore.service';
import { PayoutEntry, PayoutPlatform } from '../../models/appstore.model';

@Component({
  selector: 'app-appstore-import',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatTableModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatChipsModule,
  ],
  templateUrl: './appstore-import.component.html',
  styleUrl: './appstore-import.component.scss',
})
export class AppStoreImportComponent {
  private readonly appStoreService = inject(AppStoreService);

  readonly platform = signal<PayoutPlatform>('APPLE');
  readonly selectedFile = signal<File | null>(null);
  readonly reducedFee = signal(false);
  readonly importing = signal(false);
  readonly importComplete = signal(false);
  readonly error = signal('');
  readonly preview = signal<PayoutEntry[]>([]);

  readonly displayedColumns = ['product', 'region', 'quantity', 'gross', 'commission', 'net'];

  get totalGross(): number {
    return this.preview().reduce((s, e) => s + e.grossRevenue, 0);
  }

  get totalCommission(): number {
    return this.preview().reduce((s, e) => s + e.commission, 0);
  }

  get totalNet(): number {
    return this.preview().reduce((s, e) => s + e.netRevenue, 0);
  }

  get totalQuantity(): number {
    return this.preview().reduce((s, e) => s + e.quantity, 0);
  }

  onPlatformChange(platform: PayoutPlatform): void {
    this.platform.set(platform);
    this.resetState();
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
      this.importComplete.set(false);
      this.error.set('');
      this.preview.set([]);
    }
  }

  onImport(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.importing.set(true);
    this.error.set('');

    const obs = this.platform() === 'APPLE'
        ? this.appStoreService.importAppleCsv(file, this.reducedFee())
        : this.appStoreService.importGoogleCsv(file, this.reducedFee());

    obs.subscribe({
      next: (results) => {
        this.preview.set(results);
        this.importComplete.set(true);
        this.importing.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Import fehlgeschlagen');
        this.importing.set(false);
      },
    });
  }

  onReset(): void {
    this.resetState();
  }

  private resetState(): void {
    this.selectedFile.set(null);
    this.preview.set([]);
    this.importComplete.set(false);
    this.error.set('');
    this.importing.set(false);
  }
}
