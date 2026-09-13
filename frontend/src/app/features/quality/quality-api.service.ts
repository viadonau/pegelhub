import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';
import { CoreApiUrlService } from '../../core/api/core-api-url.service';
import { PageDto } from '../../core/api/page.dto';
import {
  CatalogSeries,
  Finding,
  ProfileConfig,
  QualityProfile,
  QualityRun,
  QualityStatus,
} from './quality.dto';

@Injectable({ providedIn: 'root' })
export class QualityApi {
  private readonly http = inject(HttpClient);
  private readonly urls = inject(CoreApiUrlService);

  profiles() {
    return httpResource<QualityProfile[]>(() => this.urls.url('/quality/profiles'));
  }

  status() {
    return httpResource<QualityStatus>(() => this.urls.url('/quality/status'));
  }

  catalog() {
    return httpResource<CatalogSeries[]>(() => this.urls.url('/time-series'));
  }

  runs(offset: Signal<number>) {
    return httpResource<PageDto<QualityRun>>(() => ({
      url: this.urls.url('/quality/runs'),
      params: { offset: offset(), limit: 25 },
    }));
  }

  findings(id: Signal<string | null>, offset: Signal<number>) {
    return httpResource<PageDto<Finding>>(() =>
      id()
        ? {
            url: this.urls.url(`/quality/runs/${id()}/findings`),
            params: { offset: offset(), limit: 25 },
          }
        : undefined,
    );
  }

  save(id: string | null, configuration: ProfileConfig) {
    return id
      ? this.http.put<QualityProfile>(this.urls.url(`/quality/profiles/${id}`), configuration)
      : this.http.post<QualityProfile>(this.urls.url('/quality/profiles'), configuration);
  }

  run(id: string) {
    return this.http.post<void>(this.urls.url(`/quality/profiles/${id}/run`), {});
  }

  export(id: string) {
    return this.http.get(this.urls.url(`/quality/profiles/${id}/export`), { responseType: 'text' });
  }

  import(document: string) {
    return this.http.post<QualityProfile>(this.urls.url('/quality/profiles/import'), document, {
      headers: { 'Content-Type': 'application/yaml' },
    });
  }
}
