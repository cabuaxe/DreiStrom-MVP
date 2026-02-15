import { inject, Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SseEvent<T = unknown> {
  type: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class SseService {
  private readonly zone = inject(NgZone);

  /**
   * Connect to an SSE endpoint and return an Observable of typed events.
   * Automatically reconnects on error after a 3-second delay.
   *
   * @param path SSE endpoint path
   * @param eventName Optional named event type to listen for (uses addEventListener).
   *                   If omitted, listens to the default "message" event (onmessage).
   */
  connect<T = unknown>(path: string, eventName?: string): Observable<SseEvent<T>> {
    const url = `${environment.apiUrl}${path}`;

    return new Observable<SseEvent<T>>((subscriber) => {
      let eventSource: EventSource;
      let reconnectTimeout: ReturnType<typeof setTimeout>;

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

      const connect = () => {
        eventSource = new EventSource(url, { withCredentials: true });

        if (eventName) {
          eventSource.addEventListener(eventName, handler as EventListener);
        } else {
          eventSource.onmessage = handler;
        }

        eventSource.onerror = () => {
          eventSource.close();
          reconnectTimeout = setTimeout(() => connect(), 3000);
        };
      };

      connect();

      return () => {
        clearTimeout(reconnectTimeout);
        eventSource?.close();
      };
    });
  }
}
