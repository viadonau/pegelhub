import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';
import { CoreApiUrlService } from '../../core/api/core-api-url.service';
import { PageDto } from '../../core/api/page.dto';
import { Delivery, Destination, DestinationConfig, NotificationStatus } from './notifications.dto';

@Injectable({ providedIn: 'root' })
export class NotificationsApi {
  private readonly http = inject(HttpClient);
  private readonly urls = inject(CoreApiUrlService);

  destinations() {
    return httpResource<Destination[]>(() => this.urls.url('/notifications/destinations'));
  }

  status() {
    return httpResource<NotificationStatus>(() => this.urls.url('/notifications/status'));
  }

  deliveries(offset: Signal<number>) {
    return httpResource<PageDto<Delivery>>(() => ({
      url: this.urls.url('/notifications/deliveries'),
      params: { offset: offset(), limit: 25 },
    }));
  }

  save(id: string | null, configuration: DestinationConfig) {
    return id
      ? this.http.put<Destination>(
          this.urls.url(`/notifications/destinations/${id}`),
          configuration,
        )
      : this.http.post<Destination>(this.urls.url('/notifications/destinations'), configuration);
  }

  test(id: string) {
    return this.http.post<{ requestId: string }>(this.urls.url('/notifications'), {
      destinations: [id],
      subject: 'PH Testnachricht',
      body: 'Testnachricht aus PegelHub.',
    });
  }

  export(id: string) {
    return this.http.get(this.urls.url(`/notifications/destinations/${id}/export`), {
      responseType: 'text',
    });
  }

  import(document: string) {
    return this.http.post<Destination>(
      this.urls.url('/notifications/destinations/import'),
      document,
      { headers: { 'Content-Type': 'application/yaml' } },
    );
  }
}
