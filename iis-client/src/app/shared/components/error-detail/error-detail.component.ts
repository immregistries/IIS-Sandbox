import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

/**
 * Simple dialog component that displays the raw error payload returned by the backend.
 * It is opened by {@link errorInterceptor} via DialogService.
 */
@Component({
  selector: 'app-error-detail',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule],
  template: `
    <p-card>
      <pre style="white-space: pre-wrap; word-break: break-all;">{{ data | json }}</pre>
      <button pButton type="button" label="Close" (click)="close()" class="p-button-text" style="margin-top: 1rem;"></button>
    </p-card>
  `,
})
export class ErrorDetailComponent {
  data: any;
  constructor(public config: DynamicDialogConfig, private ref: DynamicDialogRef) {
    this.data = config.data ?? {};
  }

  close(): void {
    this.ref?.close();
  }
}
