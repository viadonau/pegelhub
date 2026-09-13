import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const adminGuard: CanActivateFn = () =>
  inject(AuthStateService).isAdmin() || inject(Router).createUrlTree(['/forbidden']);

export const monitoringGuard: CanActivateFn = () =>
  inject(AuthStateService).canMonitor() || inject(Router).createUrlTree(['/forbidden']);
