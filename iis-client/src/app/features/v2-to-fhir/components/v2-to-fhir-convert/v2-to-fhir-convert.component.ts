import {Component, inject, OnInit, signal, viewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {V2ToFhirApiService} from '../../services/v2-to-fhir-api.service';
import {PopApiService} from '../../../pop/services/pop-api.service';
import {JsonViewerDialogComponent} from '../../../../shared/components/json-viewer-dialog/json-viewer-dialog.component';
import {InputEditorComponent} from '../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-v2-to-fhir-convert',
  standalone: true,
  imports: [FormsModule, InputText, Button, Card, Message, Tooltip, JsonViewerDialogComponent, InputEditorComponent],
  template: `
    <div class="v2-to-fhir">
      <h1>V2 to FHIR Converter</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        Test Data Only — Do not submit real patient data.
      </p-message>

      <p-card header="HL7 V2 Message">
        <div class="form-layout">
          <div class="textarea-toolbar">
            <p-button label="New Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onNewSample()" [loading]="loadingSample()" />
            <span class="toolbar-spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" placeholder="Paste HL7 V2 message here..." height="360px" />

          <div class="options-bar">
            <div class="facility-field">
              <label for="facilityName">Sending organization name</label>
              <input pInputText id="facilityName" [(ngModel)]="facilityName" placeholder="Overrides the message segments" />
            </div>
            <div class="actions">
              <p-button label="Convert" icon="pi pi-arrows-h" (onClick)="onConvert()" [loading]="converting()" />
            </div>
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="FHIR Bundle" styleClass="mt-4">
          <div class="form-layout">
            <div class="textarea-toolbar">
              <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
              <span class="toolbar-spacer"></span>
              <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(response()!)" />
              <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(response()!)" />
            </div>
            <app-input-editor [content]="response() ?? ''" [readonly]="true" language="json" height="288px" />
          </div>
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }

      <app-json-viewer-dialog />
    </div>
  `,
  styles: `
    .v2-to-fhir { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
.options-bar {
      display: flex;
      align-items: flex-end;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .facility-field {
      flex: 1;
      min-width: 200px;
      label {
        display: block;
        margin-bottom: 0.375rem;
        font-size: 0.875rem;
        font-weight: 500;
      }
      input { width: 100%; }
    }
    .actions {
      display: flex;
      gap: 0.5rem;
    }
    .textarea-toolbar {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      margin-bottom: -0.5rem;
    }
    .toolbar-spacer { flex: 1; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class V2ToFhirConvertComponent implements OnInit {
  private v2ToFhirApi = inject(V2ToFhirApiService);
  private popApi = inject(PopApiService);

  jsonViewer = viewChild.required(JsonViewerDialogComponent);

  messageData = signal('');
  facilityName = signal('');
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  converting = signal(false);
  loadingSample = signal(false);

  private sampleMessage = '';

  ngOnInit(): void {
    this.popApi.getSampleMessage().subscribe({
      next: (sample) => {
        this.sampleMessage = sample;
        this.messageData.set(sample);
      },
    });
  }

  onConvert(): void {
    if (!this.messageData()) return;

    this.converting.set(true);
    this.error.set(null);
    this.response.set(null);

    this.v2ToFhirApi.convert(this.messageData(), this.facilityName() || undefined).subscribe({
      next: (result) => {
        this.response.set(result);
        this.converting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Conversion failed');
        this.converting.set(false);
      },
    });
  }

  onNewSample(): void {
    this.loadingSample.set(true);
    this.popApi.getSampleMessage().subscribe({
      next: (sample) => {
        this.sampleMessage = sample;
        this.messageData.set(sample);
        this.loadingSample.set(false);
      },
      error: () => this.loadingSample.set(false),
    });
  }

  onReset(): void {
    this.messageData.set(this.sampleMessage);
    this.facilityName.set('');
    this.response.set(null);
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
