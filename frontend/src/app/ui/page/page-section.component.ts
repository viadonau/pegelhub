import { Component, input } from '@angular/core';

import { PhContentStateComponent } from '../content-state/content-state.component';

@Component({
  selector: 'ph-page-section',
  imports: [PhContentStateComponent],
  templateUrl: './page-section.component.html',
  styleUrl: './page-section.component.scss',
})
export class PhPageSectionComponent {
  readonly ariaLabel = input<string>();
  readonly loading = input(false);
  readonly error = input<string | null | undefined>(null);
}
