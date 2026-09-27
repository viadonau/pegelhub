import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { MeasurementApiService } from '../../../core/api/measurement-api.service';
import {
  observedPropertyLabel,
  observedPropertyUnit,
} from '../../../core/time-series/parameter-legend';
import { PhChartReferenceLine } from '../../../ui/chart/line-chart.component';
import { PhMessageComponent } from '../../../ui/message/message.component';
import { MeasurementChartPreferences } from '../data-access/measurement-chart-preferences.service';
import {
  AnalysisInterval,
  AnalysisRange,
  AnalysisTimeBasis,
  analysisChartSeries,
  basisDateTimeInput,
  buildAnalysisQuery,
  formatBasisDateTime,
  formatRawNumber,
  LoadedAnalysisResult,
  MAX_ANALYSIS_INTERVALS,
  resolveRequestedRange,
} from '../model/interval-analysis';
import { PhMeasurementChartComponent } from '../measurement-chart/measurement-chart.component';

const PAGE_SIZE = 50;
const MAX_RAW_SPAN_MS = 30 * 24 * 60 * 60_000;

@Component({
  selector: 'ph-time-series-measurements',
  imports: [
    FormsModule,
    SelectModule,
    ToggleSwitchModule,
    TooltipModule,
    PhMeasurementChartComponent,
    PhMessageComponent,
  ],
  templateUrl: './time-series-measurements.component.html',
  styleUrl: './time-series-measurements.component.scss',
})
export class PhTimeSeriesMeasurementsComponent {
  readonly timeSeriesId = input('');
  readonly observedProperty = input('');
  readonly unit = input('');
  readonly referenceLines = input<readonly PhChartReferenceLine[]>([]);
  readonly refresh = output<void>();

  private readonly measurementApi = inject(MeasurementApiService);
  protected readonly preferences = inject(MeasurementChartPreferences);
  private readonly normalizedTimeSeriesId = computed(() => this.timeSeriesId().trim());
  protected readonly ranges = [
    { label: '24 Stunden', value: '24h' },
    { label: '7 Tage', value: '7d' },
    { label: '30 Tage', value: '30d' },
    { label: 'Eigener Zeitraum', value: 'custom' },
  ];
  protected readonly intervals = [
    { label: '15 Minuten', value: '15m' },
    { label: '1 Stunde', value: '1h' },
    { label: 'Kalendertag', value: '1d' },
  ];
  protected readonly timeBases = [
    { label: 'MEZ (UTC+1)', value: '+01:00' },
    { label: 'UTC', value: 'UTC' },
  ];
  protected readonly selectedRange = signal<AnalysisRange>('24h');
  protected readonly selectedInterval = signal<AnalysisInterval>('15m');
  protected readonly selectedTimeBasis = signal<AnalysisTimeBasis>('+01:00');
  protected readonly selectedView = signal<'chart' | 'raw'>('chart');
  protected readonly customFrom = signal(
    basisDateTimeInput(Date.now() - 24 * 60 * 60_000, '+01:00'),
  );
  protected readonly customTo = signal(basisDateTimeInput(Date.now(), '+01:00'));
  protected readonly now = signal(Date.now());
  protected readonly page = signal(0);

  protected readonly queryResult = computed(() =>
    buildAnalysisQuery(
      this.selectedRange(),
      this.selectedInterval(),
      this.selectedTimeBasis(),
      this.customFrom(),
      this.customTo(),
      this.now(),
    ),
  );
  protected readonly requestedRangeResult = computed(() =>
    resolveRequestedRange(
      this.selectedRange(),
      this.selectedTimeBasis(),
      this.customFrom(),
      this.customTo(),
      this.now(),
    ),
  );
  protected readonly query = computed(() => this.queryResult().query);
  private readonly intervalsResource = this.measurementApi.measurementIntervalsResource(
    this.normalizedTimeSeriesId,
    this.query,
  );
  private readonly rawRangeError = computed(() => {
    const requested = this.requestedRangeResult();
    if (!requested.range) return requested.error;
    return Date.parse(requested.range.to) - Date.parse(requested.range.from) > MAX_RAW_SPAN_MS
      ? 'Rohwerte können für höchstens 30 Tage auf einmal geprüft werden. Zeitraum verkürzen.'
      : null;
  });
  private readonly rawQuery = computed(() =>
    this.selectedView() === 'raw' && !this.rawRangeError()
      ? this.requestedRangeResult().range
      : null,
  );
  private readonly rawResource = this.measurementApi.rawMeasurementsResource(
    this.normalizedTimeSeriesId,
    this.rawQuery,
  );

  readonly loadedAnalysisResult = computed<LoadedAnalysisResult | null>(() => {
    const query = this.query();
    return query &&
      this.intervalsResource.status() === 'resolved' &&
      this.intervalsResource.hasValue()
      ? { query, response: this.intervalsResource.value() }
      : null;
  });
  protected readonly chartSeries = computed(() => {
    const result = this.loadedAnalysisResult();
    return result ? analysisChartSeries(result, this.chartParameter()) : null;
  });
  protected readonly emptyIntervalCount = computed(
    () =>
      this.loadedAnalysisResult()?.response.intervals.filter(
        (item) => item.supportStatus !== 'full',
      ).length ?? 0,
  );
  protected readonly chartEmptyMessage = computed(() => {
    const count = this.emptyIntervalCount();
    return count
      ? `${count} abgeschlossene ${count === 1 ? 'Intervall' : 'Intervalle'} ohne vollständige Stützung.`
      : 'Keine abgeschlossenen Intervalle im Zeitraum.';
  });
  protected readonly chartParameter = computed(() =>
    observedPropertyLabel(this.observedProperty()),
  );
  protected readonly chartUnit = computed(() => {
    const responseUnit = this.loadedAnalysisResult()?.response.unit;
    return responseUnit ? this.displayUnit(responseUnit) : this.unit().trim() || null;
  });
  protected readonly chartYLabel = computed(() => {
    const unit = this.chartUnit();
    return unit ? `${this.chartParameter()} (${unit})` : this.chartParameter();
  });
  protected readonly visibleReferenceLines = computed(() =>
    this.preferences.showReferenceLevels() ? this.referenceLines() : [],
  );
  protected readonly loading = this.intervalsResource.isLoading;
  protected readonly rawLoading = this.rawResource.isLoading;
  protected readonly error = computed(() => {
    if (this.selectedView() === 'raw') return null;
    if (this.queryResult().error) return this.queryResult().error;
    if (this.intervalsResource.status() !== 'error') return null;
    const response = this.intervalsResource.error();
    return response instanceof HttpErrorResponse && response.status === 400
      ? `Die Analyse wurde vom Datendienst abgelehnt. Zeitraum und Intervall prüfen; maximal ${MAX_ANALYSIS_INTERVALS.toLocaleString('de-DE')} Intervalle und 100.000 Beobachtungen sind erlaubt.`
      : 'Die Intervallanalyse konnte nicht geladen werden.';
  });
  protected readonly rawError = computed(
    () =>
      this.rawRangeError() ??
      (this.rawResource.status() === 'error' ? 'Rohwerte konnten nicht geladen werden.' : null),
  );
  protected readonly rawResult = computed(() =>
    this.rawResource.status() === 'resolved' && this.rawResource.hasValue()
      ? this.rawResource.value()
      : null,
  );
  private readonly rawMeasurements = computed(() => {
    const result = this.rawResult();
    return result && !result.truncated ? result.measurements : [];
  });
  protected readonly pageCount = computed(() =>
    Math.max(1, Math.ceil(this.rawMeasurements().length / PAGE_SIZE)),
  );
  protected readonly currentPage = computed(() => Math.min(this.page(), this.pageCount() - 1));
  protected readonly rawRows = computed(() => {
    const start = this.currentPage() * PAGE_SIZE;
    return this.rawMeasurements().slice(start, start + PAGE_SIZE);
  });
  protected readonly basisLabel = computed(() =>
    this.selectedTimeBasis() === '+01:00' ? 'MEZ (UTC+1)' : 'UTC',
  );
  protected readonly rangeTooltip = computed(() => {
    const requested = this.requestedRangeResult().range;
    if (!requested) {
      return 'Bestimmt den angezeigten Zeitraum.';
    }

    const selected = this.selectedView() === 'raw' ? null : this.query();
    const requestedText = `Ausgewählt: ${this.formatTime(requested.from)} bis ${this.formatTime(requested.to)} ${this.basisLabel()}.`;
    if (!selected) {
      return requestedText;
    }

    return `${requestedText} Volle Intervalle: ${this.formatTime(selected.from)} bis ${this.formatTime(selected.to)}.`;
  });
  protected readonly intervalTooltip =
    'Jeder Punkt ist das zeitgewichtete Mittel des vorherigen Intervalls. Fehlende Stützung bleibt als Lücke sichtbar; vollständige Stützung bewertet nicht die Sensorqualität.';
  protected readonly timeBasisTooltip = computed(() =>
    this.selectedTimeBasis() === '+01:00'
      ? 'Fixes MEZ (UTC+1), ohne Sommerzeitumstellung.'
      : 'Alle Zeitangaben werden in UTC dargestellt.',
  );
  protected readonly formatRawNumber = formatRawNumber;

  constructor() {
    effect(() => {
      this.normalizedTimeSeriesId();
      this.page.set(0);
    });
  }

  protected setRange(value: AnalysisRange): void {
    if (value === 'custom' && this.selectedRange() !== 'custom') {
      this.updateCustomRange(this.selectedTimeBasis());
    }
    this.selectedRange.set(value);
    this.page.set(0);
  }

  protected setInterval(value: AnalysisInterval): void {
    this.selectedInterval.set(value);
    this.page.set(0);
  }

  protected setTimeBasis(value: AnalysisTimeBasis): void {
    if (this.selectedRange() === 'custom') {
      this.updateCustomRange(value);
    }
    this.selectedTimeBasis.set(value);
    this.page.set(0);
  }

  protected setCustomFrom(value: string): void {
    this.customFrom.set(value);
    this.page.set(0);
  }
  protected setCustomTo(value: string): void {
    this.customTo.set(value);
    this.page.set(0);
  }
  protected setView(value: 'chart' | 'raw'): void {
    this.selectedView.set(value);
    this.page.set(0);
  }
  protected previousPage(): void {
    this.page.set(Math.max(0, this.currentPage() - 1));
  }
  protected nextPage(): void {
    this.page.set(Math.min(this.pageCount() - 1, this.currentPage() + 1));
  }
  protected formatTime(value: string): string {
    return formatBasisDateTime(value, this.selectedTimeBasis());
  }
  protected formatExactTime(value: string): string {
    return formatBasisDateTime(value, this.selectedTimeBasis(), true, true);
  }
  protected displayUnit(unit: string): string {
    return observedPropertyUnit(this.observedProperty(), unit);
  }

  protected reload(): void {
    this.page.set(0);
    this.now.set(Date.now());
    this.intervalsResource.reload();
    if (this.selectedView() === 'raw') this.rawResource.reload();
    this.refresh.emit();
  }

  private updateCustomRange(timeBasis: AnalysisTimeBasis): void {
    const current = this.requestedRangeResult().range;
    if (!current) return;
    this.customFrom.set(basisDateTimeInput(Date.parse(current.from), timeBasis));
    this.customTo.set(basisDateTimeInput(Date.parse(current.to), timeBasis));
  }
}
