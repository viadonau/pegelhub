import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import { operationLabel, operationSeverity } from '../../ui/operation-format';
import { QualityApi } from './quality-api.service';
import { QualityRun } from './quality.dto';

@Component({
  selector: 'app-quality-runs',
  imports: [
    DatePipe,
    RouterLink,
    ButtonModule,
    TagModule,
    TooltipModule,
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
  ],
  templateUrl: './quality-runs.component.html',
  styleUrl: '../../ui/operations.scss',
})
export class QualityRunsComponent {
  protected readonly auth = inject(AuthStateService);
  private readonly api = inject(QualityApi);

  protected readonly offset = signal(0);
  protected readonly runs = this.api.runs(this.offset);
  protected readonly rows = computed(() => (this.runs.hasValue() ? this.runs.value().items : []));
  protected readonly total = computed(() => (this.runs.hasValue() ? this.runs.value().total : 0));

  protected readonly selected = signal<QualityRun | null>(null);
  private readonly selectedId = computed(() => this.selected()?.id ?? null);
  protected readonly findingOffset = signal(0);
  protected readonly findings = this.api.findings(this.selectedId, this.findingOffset);
  protected readonly findingRows = computed(() =>
    this.findings.hasValue() ? this.findings.value().items : [],
  );
  protected readonly findingTotal = computed(() =>
    this.findings.hasValue() ? this.findings.value().total : 0,
  );

  protected readonly label = operationLabel;
  protected readonly severity = operationSeverity;

  protected evidence(values: Record<string, number>): string {
    const labels: Record<string, string> = {
      value: 'Wert',
      previous: 'Vorheriger Wert',
      min: 'Minimum',
      max: 'Maximum',
    };

    return Object.entries(values)
      .map(
        ([key, value]) =>
          `${labels[key] ?? key}: ${value.toLocaleString('de-AT', { maximumFractionDigits: 6 })}`,
      )
      .join(' · ');
  }

  protected select(run: QualityRun): void {
    this.findingOffset.set(0);
    this.selected.set(run);
  }

  protected reload(): void {
    this.selected.set(null);
    this.runs.reload();
  }

  protected page(offset: number): void {
    this.selected.set(null);
    this.offset.set(offset);
  }
}
