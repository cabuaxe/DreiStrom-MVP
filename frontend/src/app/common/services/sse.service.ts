import { inject, Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface SseEvent<T = unknown> {
  type: string;
  data: T;
}

/**
 * Unified SSE service — maintains a single shared EventSource connection
 * to /api/v1/events/stream and multiplexes named events to subscribers.
 *
 * Components subscribe by event name; the service opens the connection
 * on the first subscriber and closes it when the last unsubscribes.
 */
@Injectable({ providedIn: 'root' })
export class SseService implements OnDestroy {
  private readonly zone = inject(NgZone);

  private static readonly SSE_PATH = '/api/v1/events/stream';
  private static readonly RECONNECT_DELAY = 3000;

  private eventSource: EventSource | null = null;
  private reconnectTimeout: ReturnType<typeof setTimeout> | null = null;
  private refCount = 0;

  /** Internal bus for all incoming SSE events. */
  private readonly eventBus = new Subject<SseEvent>();

  /** All event names we're currently listening for. */
  private readonly registeredEvents = new Set<string>();

  /**
   * Subscribe to a named SSE event type.
   * The shared connection is opened on the first call and closed
   * when the last subscriber unsubscribes.
   *
   * @param eventName Named event type (e.g., 'kleinunternehmer', 'notification')
   */
  on<T = unknown>(eventName: string): Observable<SseEvent<T>> {
    return new Observable<SseEvent<T>>((subscriber) => {
      this.addRef(eventName);

      const sub = this.eventBus.pipe(
        filter((e) => e.type === eventName),
        map((e) => e as SseEvent<T>),
      ).subscribe(subscriber);

      return () => {
        sub.unsubscribe();
        this.removeRef(eventName);
      };
    });
  }

  /**
   * Legacy API — kept for backward compatibility.
   * If eventName is provided, delegates to on(). Otherwise listens to default messages.
   */
  connect<T = unknown>(path: string, eventName?: string): Observable<SseEvent<T>> {
    if (eventName) {
      return this.on<T>(eventName);
    }
    // Fallback: direct connection for non-named events (rare)
    return this.connectDirect<T>(path);
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventBus.complete();
  }

  private addRef(eventName: string): void {
    this.refCount++;
    if (!this.registeredEvents.has(eventName)) {
      this.registeredEvents.add(eventName);
      // If connection is already open, add the listener immediately
      if (this.eventSource?.readyState === EventSource.OPEN
          || this.eventSource?.readyState === EventSource.CONNECTING) {
        this.addEventSourceListener(eventName);
      }
    }
    if (this.refCount === 1) {
      this.openConnection();
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private removeRef(eventName: string): void {
    this.refCount = Math.max(0, this.refCount - 1);
    if (this.refCount === 0) {
      this.disconnect();
      this.registeredEvents.clear();
    }
  }

  private openConnection(): void {
    if (this.eventSource) return;

    const url = `${environment.apiUrl}${SseService.SSE_PATH}`;

    this.zone.runOutsideAngular(() => {
      const connect = () => {
        this.eventSource = new EventSource(url, { withCredentials: true });

        // Register listeners for all known event names
        for (const name of this.registeredEvents) {
          this.addEventSourceListener(name);
        }

        this.eventSource.onerror = () => {
          this.eventSource?.close();
          this.eventSource = null;
          this.reconnectTimeout = setTimeout(() => connect(), SseService.RECONNECT_DELAY);
        };
      };

      connect();
    });
  }

  private addEventSourceListener(eventName: string): void {
    this.eventSource?.addEventListener(eventName, ((event: MessageEvent) => {
      this.zone.run(() => {
        try {
          const data = JSON.parse(event.data);
          this.eventBus.next({ type: eventName, data });
        } catch {
          this.eventBus.next({ type: eventName, data: event.data });
        }
      });
    }) as EventListener);
  }

  private disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    this.eventSource?.close();
    this.eventSource = null;
  }

  /** Direct connection for non-shared use cases (single endpoint without named events). */
  private connectDirect<T>(path: string): Observable<SseEvent<T>> {
    const url = `${environment.apiUrl}${path}`;
    return new Observable<SseEvent<T>>((subscriber) => {
      let es: EventSource;
      let timeout: ReturnType<typeof setTimeout>;

      const handler = (event: MessageEvent) => {
        this.zone.run(() => {
          try {
            const data = JSON.parse(event.data) as T;
            subscriber.next({ type: event.type, data });
          } catch {
            subscriber.next({ type: event.type, data: event.data as T });
          }
        });
      };

      const doConnect = () => {
        es = new EventSource(url, { withCredentials: true });
        es.onmessage = handler;
        es.onerror = () => {
          es.close();
          timeout = setTimeout(() => doConnect(), SseService.RECONNECT_DELAY);
        };
      };

      doConnect();
      return () => {
        clearTimeout(timeout);
        es?.close();
      };
    });
  }
}
