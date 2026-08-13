import { Component, input } from '@angular/core';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { PhMessageComponent } from '../message/message.component';

@Component({
  selector: 'ph-content-state',
  imports: [PhMessageComponent, ProgressSpinnerModule],
  template: `
    @if (loading()) {
      <div class="ph-content-loading" role="status" aria-live="polite">
        <p-progress-spinner class="ph-content-loading-spinner" strokeWidth="4" ariaLabel="Lädt" />
        <span>Inhalte werden geladen</span>
      </div>
    } @else if (error(); as errorText) {
      <ph-message severity="error" [text]="errorText" />
    } @else {
      <ng-content />
    }
  `,
  styleUrl: './content-state.component.scss',
})
export class PhContentStateComponent {
  readonly loading = input(false);
  readonly error = input<string | null | undefined>(null);
}
