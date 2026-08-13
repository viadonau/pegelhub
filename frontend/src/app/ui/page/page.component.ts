import { Component } from '@angular/core';

@Component({
  selector: 'ph-page',
  host: {
    class: 'ph-page',
  },
  template: '<ng-content />',
  styleUrl: './page.component.scss',
})
export class PhPageComponent {}
