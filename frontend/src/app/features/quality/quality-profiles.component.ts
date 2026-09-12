import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MonitoringApiService } from '../../core/api/monitoring-api.service';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import { downloadYaml, operationError, readConfiguration } from '../../ui/operation-format';
import { NotificationsApi } from '../notifications/notifications-api.service';
import { QualityApi } from './quality-api.service';
import { ProfileConfig, QualityProfile } from './quality.dto';

@Component({
  selector: 'app-quality-profiles',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CheckboxModule,
    InputTextModule,
    MultiSelectModule,
    SelectModule,
    TagModule,
    TooltipModule,
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
  ],
  templateUrl: './quality-profiles.component.html',
  styleUrl: '../../ui/operations.scss',
})
export class QualityProfilesComponent {
  private readonly api = inject(QualityApi);

  protected readonly profiles = this.api.profiles();
  protected readonly status = this.api.status();
  private readonly catalog = this.api.catalog();
  // Monitoring metadata improves labels but is optional; its failure must not block administrator edits.
  private readonly monitoring = inject(MonitoringApiService).timeSeriesCollectionResource();
  private readonly destinations = inject(NotificationsApi).destinations();
  private readonly resources = [this.profiles, this.status, this.catalog, this.destinations];

  protected readonly loading = computed(() => this.resources.some((r) => r.isLoading()));
  protected readonly loadError = computed(() =>
    this.resources.some((r) => r.error()) ? 'QA-Konfiguration konnte nicht geladen werden.' : null,
  );
  protected readonly ready = computed(
    () =>
      !this.loading() &&
      !this.loadError() &&
      this.resources.every((r) => r.status() === 'resolved'),
  );
  protected readonly rows = computed(() => (this.profiles.hasValue() ? this.profiles.value() : []));
  protected readonly executionEnabled = computed(
    () => this.status.hasValue() && this.status.value().enabled,
  );

  protected readonly editing = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly busy = signal(false);
  protected readonly message = signal('');
  protected readonly error = signal('');

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    enabled: this.fb.nonNullable.control(false),
    sources: this.fb.nonNullable.control<string[]>([], Validators.required),
    output: this.fb.control<string | null>(null),
    destinations: this.fb.nonNullable.control<string[]>([]),
    lookbackSeconds: this.fb.nonNullable.control(1200, [
      Validators.required,
      Validators.min(1),
      Validators.max(604800),
    ]),
    intervalSeconds: this.fb.nonNullable.control(60, [
      Validators.required,
      Validators.min(1),
      Validators.max(86400),
    ]),
    rangeEnabled: this.fb.nonNullable.control(false),
    min: this.fb.control<number | null>(null),
    max: this.fb.control<number | null>(null),
    jumpEnabled: this.fb.nonNullable.control(false),
    jump: this.fb.control<number | null>(null),
    frozenEnabled: this.fb.nonNullable.control(false),
    frozen: this.fb.control<number | null>(null),
    deviationEnabled: this.fb.nonNullable.control(false),
    deviation: this.fb.control<number | null>(null),
  });

  private readonly selectedSources = toSignal(this.form.controls.sources.valueChanges, {
    initialValue: [],
  });
  protected readonly sourceOptions = computed(() => {
    const metadata = this.monitoring.hasValue() ? this.monitoring.value().items : [];

    return (this.catalog.hasValue() ? this.catalog.value() : [])
      .filter((s) => s.status === 'active')
      .map((s) => {
        const detail = metadata.find((item) => item.id === s.id);

        return {
          ...s,
          label: detail
            ? `${detail.station.name} · ${detail.measuringPoint.name} · ${s.observedProperty} (${s.unit})`
            : `${s.observedProperty} · ${s.id}`,
        };
      });
  });
  protected readonly outputOptions = computed(() => {
    const sources = this.selectedSources();
    const property = this.sourceOptions().find((s) => sources.includes(s.id))?.observedProperty;

    return this.sourceOptions().filter(
      (s) =>
        !sources.includes(s.id) &&
        s.observedProperty === property &&
        (!s.sourceAssignment ||
          (this.editing() !== null && s.sourceAssignment.internalProducerId === this.editing())),
    );
  });
  protected readonly destinationOptions = computed(() =>
    (this.destinations.hasValue() ? this.destinations.value() : []).map((d) => ({
      id: d.id,
      label: `${d.configuration.name}${d.configuration.enabled ? '' : ' (inaktiv)'}`,
    })),
  );

  protected reload(): void {
    this.resources.forEach((r) => r.reload());
  }

  protected edit(profile?: QualityProfile): void {
    this.editing.set(profile?.id ?? null);
    this.form.reset();

    if (profile) {
      const config = profile.configuration;
      this.form.patchValue({
        ...config,
        rangeEnabled: !!config.rules.range,
        min: config.rules.range?.min ?? null,
        max: config.rules.range?.max ?? null,
        jumpEnabled: config.rules.jumpThreshold !== null,
        jump: config.rules.jumpThreshold,
        frozenEnabled: config.rules.frozenSeconds !== null,
        frozen: config.rules.frozenSeconds,
        deviationEnabled: config.rules.maxDeviation !== null,
        deviation: config.rules.maxDeviation,
      });
    }

    this.error.set('');
    this.message.set('');
    this.editorOpen.set(true);
  }

  protected async save(): Promise<void> {
    if (this.busy() || !this.ready()) {
      return;
    }

    const config = this.configurationFromForm();
    if (!config) {
      this.form.markAllAsTouched();
      this.error.set(
        'Bitte Quellen, Zeitfenster und aktivierte Regeln vollständig und gültig ausfüllen.',
      );
      return;
    }

    this.busy.set(true);
    this.error.set('');

    try {
      await firstValueFrom(this.api.save(this.editing(), config));

      this.editorOpen.set(false);
      this.profiles.reload();
      // Saving may claim or release an output series; selectors must not keep stale ownership.
      this.catalog.reload();
      this.message.set('Profil gespeichert.');
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      this.busy.set(false);
    }
  }

  /** Null means invalid input. Disabled rules stay null; zero remains an enabled threshold. */
  private configurationFromForm(): ProfileConfig | null {
    const values = this.form.getRawValue();
    const isFiniteNumber = (value: number | null) => value !== null && Number.isFinite(value);

    if (
      this.form.invalid ||
      (values.rangeEnabled &&
        (!isFiniteNumber(values.min) ||
          !isFiniteNumber(values.max) ||
          values.min! > values.max!)) ||
      (values.jumpEnabled && (!isFiniteNumber(values.jump) || values.jump! < 0)) ||
      (values.frozenEnabled &&
        (!isFiniteNumber(values.frozen) ||
          values.frozen! < 1 ||
          values.frozen! > values.lookbackSeconds)) ||
      (values.deviationEnabled && (!isFiniteNumber(values.deviation) || values.deviation! < 0))
    ) {
      return null;
    }

    return {
      name: values.name.trim(),
      enabled: values.enabled,
      sources: values.sources,
      output: values.output,
      destinations: values.destinations,
      lookbackSeconds: values.lookbackSeconds,
      intervalSeconds: values.intervalSeconds,
      rules: {
        range: values.rangeEnabled ? { min: values.min!, max: values.max! } : null,
        jumpThreshold: values.jumpEnabled ? values.jump : null,
        frozenSeconds: values.frozenEnabled ? values.frozen : null,
        maxDeviation: values.deviationEnabled ? values.deviation : null,
      },
    };
  }

  protected async run(id: string): Promise<void> {
    this.busy.set(true);
    this.error.set('');

    try {
      await firstValueFrom(this.api.run(id));

      this.profiles.reload();
      this.message.set('Lauf angefordert.');
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected async export(id: string): Promise<void> {
    try {
      downloadYaml(await firstValueFrom(this.api.export(id)), `qa-${id}`);
    } catch (error) {
      this.error.set(operationError(error));
    }
  }

  protected async importFile(input: HTMLInputElement): Promise<void> {
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.busy.set(true);
    this.error.set('');

    try {
      await firstValueFrom(this.api.import(await readConfiguration(file)));

      this.profiles.reload();
      this.catalog.reload();
      this.message.set('Profil importiert.');
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      input.value = '';
      this.busy.set(false);
    }
  }
}
