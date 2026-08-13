import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';

import { PhPageHeaderComponent } from '../../ui/page/page-header.component';
import { PhPageSectionComponent } from '../../ui/page/page-section.component';
import { PhPageComponent } from '../../ui/page/page.component';

@Component({
  selector: 'app-error-page',
  imports: [
    RouterLink,
    ButtonModule,
    PhPageComponent,
    PhPageHeaderComponent,
    PhPageSectionComponent,
  ],
  templateUrl: './error-page.component.html',
  styleUrl: './error-page.component.scss',
})
export class ErrorPageComponent {}
