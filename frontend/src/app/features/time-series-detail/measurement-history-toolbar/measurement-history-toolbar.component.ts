import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

interface MeasurementRange {
  label: string;
  value: string;
}

const MEASUREMENT_RANGES: readonly MeasurementRange[] = [
  { label: '1 Minute', value: '1m' },
  { label: '5 Minuten', value: '5m' },
  { label: '15 Minuten', value: '15m' },
  { label: '1 Stunde', value: '1h' },
  { label: '3 Stunden', value: '3h' },
  { label: '24 Stunden', value: '24h' },
  { label: '7 Tage', value: '7d' },
  { label: '30 Tage', value: '30d' },
];

@Component({
  selector: 'ph-measurement-history-toolbar',
  imports: [FormsModule, SelectModule, ToggleSwitchModule],
  host: {
    class: 'ph-measurement-history-toolbar',
    role: 'group',
    'aria-label': 'Zeitraum für den Messverlauf steuern',
  },
  templateUrl: './measurement-history-toolbar.component.html',
  styleUrl: './measurement-history-toolbar.component.scss',
})
export class PhMeasurementHistoryToolbarComponent {
  readonly selectedRange = input('24h');
  readonly loading = input(false);
  readonly referenceLevelsAvailable = input(false);
  readonly referenceLevelsVisible = input(false);
  readonly rangeChange = output<string>();
  readonly referenceLevelsVisibleChange = output<boolean>();
  readonly refresh = output<void>();

  protected readonly ranges = [...MEASUREMENT_RANGES];

  protected selectRange(range: string): void {
    if (range !== this.selectedRange()) {
      this.rangeChange.emit(range);
    }
  }

  protected reload(): void {
    if (!this.loading()) {
      this.refresh.emit();
    }
  }

  protected setReferenceLevelsVisible(visible: boolean): void {
    this.referenceLevelsVisibleChange.emit(visible);
  }
}
