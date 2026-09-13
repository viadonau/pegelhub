import { Routes } from '@angular/router';

import { ErrorPageComponent } from './features/error/error-page.component';
import { AppShellComponent } from './shell/app-shell.component';
import { adminGuard, monitoringGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: '',
    component: AppShellComponent,
    children: [
      {
        path: 'quality/profiles',
        title: 'QA-Profile · PegelHub',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/quality/quality-profiles.component').then(
            (m) => m.QualityProfilesComponent,
          ),
      },
      {
        path: 'quality',
        title: 'Qualität · PegelHub',
        canActivate: [monitoringGuard],
        loadComponent: () =>
          import('./features/quality/quality-runs.component').then((m) => m.QualityRunsComponent),
      },
      {
        path: 'notifications',
        title: 'Benachrichtigungen · PegelHub',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(
            (m) => m.NotificationsComponent,
          ),
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview',
      },
      {
        path: 'overview',
        title: 'Messreihen · PegelHub',
        loadComponent: () =>
          import('./features/time-series-overview/time-series-overview.component').then(
            (module) => module.TimeSeriesOverviewComponent,
          ),
      },
      {
        path: 'overview/:timeSeriesId',
        title: 'Messreihe · PegelHub',
        loadComponent: () =>
          import('./features/time-series-detail/time-series-detail.component').then(
            (module) => module.TimeSeriesDetailComponent,
          ),
      },
      {
        path: 'forbidden',
        component: ErrorPageComponent,
        title: 'Zugriff nicht möglich · PegelHub',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
