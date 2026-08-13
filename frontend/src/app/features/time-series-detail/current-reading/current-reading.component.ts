import { Component, input } from '@angular/core';

@Component({
  selector: 'ph-current-reading',
  host: {
    class: 'ph-current-reading',
    role: 'region',
    'aria-label': 'Aktueller Messwert',
  },
  templateUrl: './current-reading.component.html',
  styleUrl: './current-reading.component.scss',
})
export class PhCurrentReadingComponent {
  readonly label = input<string>();
  readonly value = input.required<string>();
  readonly unit = input<string | null>(null);
  readonly timestamp = input<string | null>(null);
  readonly relativeTimestamp = input<string | null>(null);
}
