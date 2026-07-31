import {Component, inject, signal, viewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {FhirMessagingApiService} from '../../services/fhir-messaging-api.service';
import {JsonViewerDialogComponent} from '../../../../shared/components/json-viewer-dialog/json-viewer-dialog.component';
import {InputEditorComponent} from '../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-fhir-messaging-send',
  standalone: true,
  imports: [FormsModule, InputText, Button, Card, Message, Tooltip, JsonViewerDialogComponent, InputEditorComponent],
  template: `
    <div class="fhir-messaging">
      <h1>FHIR Messaging</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        Test Data Only — Do not submit real patient data.
      </p-message>

      <p-card header="FHIR Bundle Message">
        <div class="form-layout">
          <div class="textarea-toolbar">
            <p-button label="Load Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onLoadSample()" [loading]="loadingSample()" />
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" language="json" placeholder="Paste a FHIR R4 Bundle (JSON) here..." height="360px" />

          <div class="options-bar">
            <div class="facility-field">
              <label for="facilityName">Sending organization name</label>
              <input pInputText id="facilityName" [(ngModel)]="facilityName" placeholder="Overrides the message segments" />
            </div>
            <div class="actions">
              <p-button label="Submit" icon="pi pi-send" (onClick)="onSubmit()" [loading]="submitting()" />
            </div>
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="Response" styleClass="mt-4">
        <div class="form-layout">
          <div class="textarea-toolbar">
              <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />

            <span class="spacer"></span>

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
    .fhir-messaging { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .textarea-toolbar {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      margin-bottom: -0.5rem;
    }
    .spacer { flex: 1; }
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
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class FhirMessagingSendComponent {
  private fhirMessagingApi = inject(FhirMessagingApiService);

  jsonViewer = viewChild.required(JsonViewerDialogComponent);

  messageData = signal('');
  facilityName = signal('');
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  submitting = signal(false);
  loadingSample = signal(false);

  onLoadSample(): void {
    this.loadingSample.set(true);
    this.fhirMessagingApi.getSample().subscribe({
      next: (sample) => {
        this.messageData.set(sample);
        this.loadingSample.set(false);
      },
      error: () => this.loadingSample.set(false),
    });
  }

  onSubmit(): void {
    if (!this.messageData()) return;

    this.submitting.set(true);
    this.error.set(null);
    this.response.set(null);

    this.fhirMessagingApi.sendMessage(this.messageData(), this.facilityName() || undefined).subscribe({
      next: (result) => {
        this.response.set(result);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set((err.error? JSON.parse(err.error).message : err.message) || 'Failed to send message');
        this.submitting.set(false);
      },
    });
  }

  onReset(): void {
    this.response.set(null);
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
