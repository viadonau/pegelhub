import { Component, input } from '@angular/core';
import { MessageModule } from 'primeng/message';

export type PhMessageSeverity = 'info' | 'success' | 'warn' | 'error';

@Component({
  selector: 'ph-message',
  imports: [MessageModule],
  host: {
    class: 'block',
  },
  template: `
    <p-message [severity]="severity()" [closable]="false">
      <span>{{ text() }}</span>
    </p-message>
  `,
})
export class PhMessageComponent {
  readonly severity = input<PhMessageSeverity>('info');
  readonly text = input.required<string>();
}
