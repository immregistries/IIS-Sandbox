import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {Checkbox} from 'primeng/checkbox';
import {InputNumber} from 'primeng/inputnumber';
import {CovidGenerateApiService} from '../../services/covid-generate-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-covid-generate',
  standalone: true,
  imports: [FormsModule, Button, Card, Message, Tooltip, Checkbox, InputNumber, InputEditorComponent],
  template: `
    <div class="covid-generate">
      <h1>Generate COVID-19 HL7 Messages</h1>

      <p-message severity="info" styleClass="mb-4 w-full">
        Generates synthetic HL7 messages containing COVID-19 vaccination events.
      </p-message>

      <p-card header="Generation Options">
        <div class="form-layout">
          <div class="field">
            <label for="messageCount">Number of messages</label>
            <p-inputnumber id="messageCount" [(ngModel)]="messageCount" [min]="1" [max]="1000" [showButtons]="true" />
          </div>
          <div class="checkbox-group">
            <div class="checkbox-field">
              <p-checkbox [(ngModel)]="includeAdmin" [binary]="true" inputId="admin" />
              <label for="admin">Include Administered</label>
            </div>
            <div class="checkbox-field">
              <p-checkbox [(ngModel)]="includeRefusal" [binary]="true" inputId="refusal" />
              <label for="refusal">Include Refusals</label>
            </div>
            <div class="checkbox-field">
              <p-checkbox [(ngModel)]="includeComorbidity" [binary]="true" inputId="comorbidity" />
              <label for="comorbidity">Include Comorbidity</label>
            </div>
            <div class="checkbox-field">
              <p-checkbox [(ngModel)]="includeMissed" [binary]="true" inputId="missed" />
              <label for="missed">Include Missed Appointments</label>
            </div>
            <div class="checkbox-field">
              <p-checkbox [(ngModel)]="includeSerology" [binary]="true" inputId="serology" />
              <label for="serology">Include Serology</label>
            </div>
          </div>
          <div class="actions">
            <p-button label="Generate" icon="pi pi-bolt" (onClick)="onGenerate()" [loading]="generating()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="Generated Messages" styleClass="mt-4">
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
    .covid-generate { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .field {
      display: flex; flex-direction: column; gap: 0.375rem;
      label { font-size: 0.875rem; font-weight: 500; }
    }
    .checkbox-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .checkbox-field { display: flex; align-items: center; gap: 0.5rem; }
    .toolbar { display: flex; align-items: center; gap: 0.25rem; margin-bottom: -0.5rem; }
    .spacer { flex: 1; }
    .actions { display: flex; gap: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class CovidGenerateComponent {
  private api = inject(CovidGenerateApiService);

  messageCount = signal(100);
  includeAdmin = signal(true);
  includeRefusal = signal(true);
  includeComorbidity = signal(true);
  includeMissed = signal(true);
  includeSerology = signal(true);
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  generating = signal(false);

  onGenerate(): void {
    this.generating.set(true);
    this.error.set(null);
    this.response.set(null);

    this.api.generate({
      messageCount: this.messageCount(),
      includeAdmin: this.includeAdmin(),
      includeRefusal: this.includeRefusal(),
      includeComorbidity: this.includeComorbidity(),
      includeMissed: this.includeMissed(),
      includeSerology: this.includeSerology(),
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
    this.messageCount.set(100);
    this.includeAdmin.set(true);
    this.includeRefusal.set(true);
    this.includeComorbidity.set(true);
    this.includeMissed.set(true);
    this.includeSerology.set(true);
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
    a.download = 'covid-hl7-messages.txt';
    a.click();
    URL.revokeObjectURL(url);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
