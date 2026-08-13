import { Component, input } from '@angular/core';

@Component({
  selector: 'ph-page-header',
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
export class PhPageHeaderComponent {
  readonly title = input.required<string>();
}
