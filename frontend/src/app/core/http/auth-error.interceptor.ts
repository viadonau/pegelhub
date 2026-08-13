import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthStateService } from '../auth/auth-state.service';

export const authErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthStateService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          void auth.login(router.url);
        }

        if (error.status === 403 && router.url !== '/forbidden') {
          void router.navigateByUrl('/forbidden', { replaceUrl: true });
        }
      }

      return throwError(() => error);
    }),
  );
};
