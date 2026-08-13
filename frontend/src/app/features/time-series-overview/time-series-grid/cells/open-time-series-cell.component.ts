import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';

import { TimeSeriesOverviewView } from '../../model/time-series-overview';

@Component({
  selector: 'ph-open-time-series-cell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (row(); as timeSeries) {
      <button
        type="button"
        class="ph-open-time-series-cell"
        [attr.aria-label]="
          'Messreihe öffnen: ' +
          timeSeries.measuringPointName +
          ', ' +
          timeSeries.measurementTypeLabel
        "
        [title]="
          'Messreihe öffnen: ' +
          timeSeries.measuringPointName +
          ', ' +
          timeSeries.measurementTypeLabel
        "
        (click)="open($event)"
      >
        <i class="pi pi-chevron-right" aria-hidden="true"></i>
      </button>
    }
  `,
  styleUrl: './open-time-series-cell.component.scss',
})
export class OpenTimeSeriesCellComponent implements ICellRendererAngularComp {
  private readonly router = inject(Router);

  protected readonly row = signal<TimeSeriesOverviewView | null>(null);

  agInit(params: ICellRendererParams<TimeSeriesOverviewView>): void {
    this.row.set(params.data ?? null);
  }

  refresh(params: ICellRendererParams<TimeSeriesOverviewView>): boolean {
    this.row.set(params.data ?? null);

    return true;
  }

  protected open(event: Event): void {
    event.stopPropagation();

    const timeSeries = this.row();
    if (timeSeries) {
      void this.router.navigate(['/overview', timeSeries.id]);
    }
  }
}
