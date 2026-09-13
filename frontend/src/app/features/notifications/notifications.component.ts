import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { PhPageComponent } from '../../ui/page/page.component';
import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import {
  downloadYaml,
  operationError,
  operationLabel,
  operationSeverity,
  readConfiguration,
} from '../../ui/operation-format';
import { NotificationsApi } from './notifications-api.service';
import { Delivery, Destination, DestinationConfig } from './notifications.dto';

@Component({
  selector: 'app-notifications',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    ButtonModule,
    CheckboxModule,
    InputTextModule,
    SelectModule,
    SelectButtonModule,
    TagModule,
    TooltipModule,
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
  ],
  templateUrl: './notifications.component.html',
  styleUrl: '../../ui/operations.scss',
})
export class NotificationsComponent {
  private readonly api = inject(NotificationsApi);

  protected readonly destinations = this.api.destinations();
  protected readonly status = this.api.status();
  protected readonly offset = signal(0);
  protected readonly deliveries = this.api.deliveries(this.offset);

  protected readonly rows = computed(() =>
    this.destinations.hasValue() ? this.destinations.value() : [],
  );
  protected readonly deliveryRows = computed(() =>
    this.deliveries.hasValue() ? this.deliveries.value().items : [],
  );
  protected readonly deliveryTotal = computed(() =>
    this.deliveries.hasValue() ? this.deliveries.value().total : 0,
  );
  protected readonly loading = computed(
    () => this.destinations.isLoading() || this.status.isLoading(),
  );
  protected readonly loadError = computed(() =>
    this.destinations.error() || this.status.error()
      ? 'Benachrichtigungsziele konnten nicht geladen werden.'
      : null,
  );
  protected readonly ready = computed(
    () =>
      !this.loading() &&
      !this.loadError() &&
      this.destinations.hasValue() &&
      this.status.hasValue(),
  );
  protected readonly executionEnabled = computed(
    () => this.status.hasValue() && this.status.value().enabled,
  );

  protected readonly selected = signal<Delivery | null>(null);
  protected readonly editing = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly message = signal('');

  protected readonly label = operationLabel;
  protected readonly severity = operationSeverity;
  protected readonly transports = ['SMTP', 'SNMP'];
  protected readonly versions = ['v2c', 'v3'];

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    enabled: this.fb.nonNullable.control(false),
    transport: this.fb.nonNullable.control<'SMTP' | 'SNMP'>('SMTP'),
    credentialRef: this.fb.nonNullable.control('', Validators.required),
    from: this.fb.nonNullable.control(''),
    recipients: this.fb.nonNullable.control(''),
    signature: this.fb.nonNullable.control(''),
    host: this.fb.nonNullable.control(''),
    port: this.fb.nonNullable.control(162),
    version: this.fb.nonNullable.control<'v2c' | 'v3'>('v3'),
    trapOid: this.fb.nonNullable.control(''),
    messageOid: this.fb.nonNullable.control(''),
    enterpriseOid: this.fb.nonNullable.control(''),
  });

  private readonly transport = toSignal(this.form.controls.transport.valueChanges, {
    initialValue: 'SMTP',
  });
  private readonly version = toSignal(this.form.controls.version.valueChanges, {
    initialValue: 'v3',
  });
  protected readonly credentialOptions = computed(() => {
    const kind =
      this.transport() === 'SMTP' ? 'SMTP' : this.version() === 'v3' ? 'SNMP_V3' : 'SNMP_V2C';
    const options = (this.status.hasValue() ? this.status.value().credentials : [])
      .filter((c) => c.kind === kind)
      .map((c) => ({
        name: c.name,
        label: `${c.name} · ${c.available ? 'verfügbar' : 'nicht verfügbar'}`,
      }));

    const original = this.rows().find((d) => d.id === this.editing())?.configuration;
    // A removed deployment credential must remain selectable so its existing destination can be disabled.
    if (original && !options.some((c) => c.name === original.credentialRef)) {
      options.push({
        name: original.credentialRef,
        label: `${original.credentialRef} · nicht verfügbar`,
      });
    }

    return options;
  });

  protected reload(): void {
    this.destinations.reload();
    this.status.reload();
    this.deliveries.reload();
    this.selected.set(null);
  }

  protected edit(destination?: Destination): void {
    this.editing.set(destination?.id ?? null);
    this.form.reset();

    if (destination) {
      const config = destination.configuration;
      this.form.patchValue({
        name: config.name,
        enabled: config.enabled,
        transport: config.transport,
        credentialRef: config.credentialRef,
        from: config.mail?.from ?? '',
        recipients: config.mail?.recipients.join('\n') ?? '',
        signature: config.mail?.signature ?? '',
        host: config.snmp?.host ?? '',
        port: config.snmp?.port ?? 162,
        version: config.snmp?.version ?? 'v3',
        trapOid: config.snmp?.trapOid ?? '',
        messageOid: config.snmp?.messageOid ?? '',
        enterpriseOid: config.snmp?.enterpriseOid ?? '',
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
      this.error.set('Bitte Name, Zugang und Transportziel vollständig und gültig ausfüllen.');
      return;
    }

    this.busy.set(true);
    this.error.set('');

    try {
      await firstValueFrom(this.api.save(this.editing(), config));

      this.editorOpen.set(false);
      this.destinations.reload();
      this.deliveries.reload();
      this.message.set('Benachrichtigungsziel gespeichert.');
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      this.busy.set(false);
    }
  }

  /** Builds only the selected route, or returns null. Core remains authoritative for address/OID and allowlist checks. */
  private configurationFromForm(): DestinationConfig | null {
    const values = this.form.getRawValue();
    const recipients = values.recipients
      .split(/\r?\n/)
      .map((recipient) => recipient.trim())
      .filter(Boolean);

    if (
      this.form.invalid ||
      !this.credentialOptions().some((credential) => credential.name === values.credentialRef) ||
      (values.transport === 'SMTP' && (!values.from.trim() || recipients.length === 0)) ||
      (values.transport === 'SNMP' &&
        (!values.host.trim() ||
          !values.trapOid.trim() ||
          !values.messageOid.trim() ||
          !Number.isInteger(values.port) ||
          values.port < 1 ||
          values.port > 65535))
    ) {
      return null;
    }

    return {
      name: values.name.trim(),
      enabled: values.enabled,
      transport: values.transport,
      credentialRef: values.credentialRef,
      mail:
        values.transport === 'SMTP'
          ? { from: values.from.trim(), recipients, signature: values.signature || null }
          : null,
      snmp:
        values.transport === 'SNMP'
          ? {
              host: values.host.trim(),
              port: values.port,
              version: values.version,
              trapOid: values.trapOid.trim(),
              messageOid: values.messageOid.trim(),
              enterpriseOid: values.enterpriseOid.trim() || null,
            }
          : null,
    };
  }

  protected async test(id: string): Promise<void> {
    this.busy.set(true);
    this.error.set('');

    try {
      const response = await firstValueFrom(this.api.test(id));

      this.deliveries.reload();
      this.message.set(`Testnachricht eingereiht: ${response.requestId}`);
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected async export(id: string): Promise<void> {
    try {
      downloadYaml(await firstValueFrom(this.api.export(id)), `benachrichtigung-${id}`);
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

      this.destinations.reload();
      this.message.set('Benachrichtigungsziel importiert.');
    } catch (error) {
      this.error.set(operationError(error));
    } finally {
      input.value = '';
      this.busy.set(false);
    }
  }
}
