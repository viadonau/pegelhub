import { inject, Injectable } from '@angular/core';

import { RUNTIME_CONFIG } from '../config/runtime-config';

@Injectable({ providedIn: 'root' })
export class CoreApiUrlService {
  private readonly config = inject(RUNTIME_CONFIG);

  url(path: string): string {
    const baseUrl = this.config.apiBaseUrl.replace(/\/$/, '');
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;

    return `${baseUrl}${normalizedPath}`;
  }
}
