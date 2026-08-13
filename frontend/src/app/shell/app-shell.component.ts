import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { AuthStateService } from '../core/auth/auth-state.service';
import { ThemeService } from '../core/theme/theme.service';

@Component({
  selector: 'app-shell',
  imports: [ButtonModule, RouterLink, RouterOutlet, ToolbarModule, TooltipModule],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  protected readonly auth = inject(AuthStateService);
  protected readonly theme = inject(ThemeService);
  protected readonly userInitials = computed(() => initials(this.auth.userName()));

  protected logout(): void {
    void this.auth.logout();
  }
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);

  if (parts.length === 0) {
    return '?';
  }

  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts.at(-1)?.[0] ?? '') : '';

  return `${first}${last}`.toLocaleUpperCase('de-AT');
}
