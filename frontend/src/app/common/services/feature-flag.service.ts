import { inject, Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { UserFeatureFlags } from '../models/feature-flags.model';
import { SseService } from './sse.service';
import { Subscription } from 'rxjs';

/**
 * Progressive disclosure service (architecture doc §4.2.4).
 *
 * Fetches feature flags computed from the user's actual business volume
 * and keeps them updated in real-time via SSE.
 *
 * Usage in components:
 *   readonly flags = inject(FeatureFlagService).flags;
 *   @if (flags()?.showGewerbesteuerCard) { ... }
 */
@Injectable({ providedIn: 'root' })
export class FeatureFlagService {
  private readonly http = inject(HttpClient);
  private readonly sseService = inject(SseService);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  private sseSub?: Subscription;

  /** Current feature flags (null until first load). */
  readonly flags = signal<UserFeatureFlags | null>(null);

  /** Whether initial load has completed. */
  readonly loaded = signal(false);

  /** Convenience computed signals for common checks. */
  readonly complexityLevel = computed(() => this.flags()?.complexityLevel ?? 1);

  /**
   * Load feature flags from the backend and subscribe to SSE updates.
   * Call once after authentication (e.g., from an app initializer or auth guard).
   */
  initialize(year?: number): void {
    this.loadFlags(year);
    this.subscribeToUpdates();
  }

  /**
   * Force reload flags (e.g., after year change).
   */
  refresh(year?: number): void {
    this.loadFlags(year);
  }

  /**
   * Clean up SSE subscription.
   */
  destroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadFlags(year?: number): void {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    this.http.get<UserFeatureFlags>(`${this.baseUrl}/user/feature-flags`, { params })
      .subscribe({
        next: (flags) => {
          this.flags.set(flags);
          this.loaded.set(true);
        },
        error: () => this.loaded.set(true),
      });
  }

  private subscribeToUpdates(): void {
    this.sseSub?.unsubscribe();
    this.sseSub = this.sseService
      .connect<UserFeatureFlags>('/api/v1/dashboard/events', 'feature-flags')
      .subscribe({
        next: (event) => this.flags.set(event.data),
      });
  }
}
