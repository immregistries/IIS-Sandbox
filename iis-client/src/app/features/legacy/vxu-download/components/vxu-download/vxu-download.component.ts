import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {InputText} from 'primeng/inputtext';
import {Checkbox} from 'primeng/checkbox';
import {DatePicker} from 'primeng/datepicker';
import {Tooltip} from 'primeng/tooltip';
import {VxuDownloadApiService} from '../../services/vxu-download-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-vxu-download',
  standalone: true,
  imports: [FormsModule, Button, Card, Message, InputText, Checkbox, DatePicker, Tooltip, InputEditorComponent],
  template: `
    <div class="vxu-download">
      <h1>VXU Download for CDC Reporting</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        This feature is marked as deprecated. Test Data Only — Do not submit real patient data.
      </p-message>

      <p-card header="Export Parameters">
        <div class="form-layout">
          <div class="field-row">
            <div class="field">
              <label for="startDate">Start Date</label>
              <p-datepicker id="startDate" [(ngModel)]="startDate" dateFormat="mm/dd/yy" [showIcon]="true" />
            </div>
            <div class="field">
              <label for="endDate">End Date</label>
              <p-datepicker id="endDate" [(ngModel)]="endDate" dateFormat="mm/dd/yy" [showIcon]="true" />
            </div>
          </div>
          <div class="field">
            <label for="cvxCodes">CVX Codes (comma-separated)</label>
            <input pInputText id="cvxCodes" [(ngModel)]="cvxCodes" placeholder="208,207,210,212,213" />
          </div>
          <div class="checkbox-field">
            <p-checkbox [(ngModel)]="includePhi" [binary]="true" inputId="includePhi" />
            <label for="includePhi">Include PHI (Protected Health Information)</label>
          </div>
          <div class="actions">
            <p-button label="Generate" icon="pi pi-download" (onClick)="onGenerate()" [loading]="generating()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="Generated VXU" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(response()!)" />
            <p-button icon="pi pi-download" [rounded]="true" [text]="true" size="small" pTooltip="Download" (onClick)="download()" />
          </div>
          <app-input-editor [content]="response() ?? ''" [readonly]="true" height="400px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .vxu-download { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .field-row { display: flex; gap: 1rem; flex-wrap: wrap; }
    .field {
      flex: 1; min-width: 200px;
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.875rem; font-weight: 500; }
      input { width: 100%; }
    }
    .toolbar { display: flex; align-items: center; gap: 0.25rem; margin-bottom: -0.5rem; }
    .spacer { flex: 1; }
    .checkbox-field { display: flex; align-items: center; gap: 0.5rem; }
    .actions { display: flex; gap: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class VxuDownloadComponent {
  private api = inject(VxuDownloadApiService);

  startDate = signal<Date | null>(null);
  endDate = signal<Date | null>(null);
  cvxCodes = signal('208,207,210,212,213');
  includePhi = signal(false);
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  generating = signal(false);

  onGenerate(): void {
    this.generating.set(true);
    this.error.set(null);
    this.response.set(null);

    this.api.generate({
      dateStart: this.formatDate(this.startDate()),
      dateEnd: this.formatDate(this.endDate()),
      cvxCodes: this.cvxCodes(),
      includePhi: this.includePhi(),
    }).subscribe({
      next: (result) => {
        this.response.set(result);
        this.generating.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Generation failed');
        this.generating.set(false);
      },
    });
  }

  onReset(): void {
    this.startDate.set(null);
    this.endDate.set(null);
    this.cvxCodes.set('208,207,210,212,213');
    this.includePhi.set(false);
    this.response.set(null);
    this.error.set(null);
  }

  download(): void {
    const text = this.response();
    if (!text) return;
    const blob = new Blob([text], {type: 'text/plain'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'vxu-download.txt';
    a.click();
    URL.revokeObjectURL(url);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }

  private formatDate(date: Date | null): string {
    if (!date) return '';
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const y = date.getFullYear();
    return `${m}/${d}/${y} 00:00:00`;
  }
}
