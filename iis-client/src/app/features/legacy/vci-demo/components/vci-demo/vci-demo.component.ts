import {Component, inject, OnInit, signal, viewChild} from '@angular/core';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {VciConversionResult, VciDemoApiService} from '../../services/vci-demo-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';
import {
  JsonViewerDialogComponent
} from '../../../../../shared/components/json-viewer-dialog/json-viewer-dialog.component';

@Component({
  selector: 'app-vci-demo',
  standalone: true,
  imports: [Button, Card, Message, Tooltip, InputEditorComponent, JsonViewerDialogComponent],
  template: `
    <div class="vci-demo">
      <h1>VCI Demonstration</h1>

      <p-message severity="info" styleClass="mb-4 w-full">
        Converts RSP (immunization response) messages to FHIR resources and Verifiable Credentials (SMART Health Card format).
      </p-message>

      <p-card header="RSP Message">
        <div class="form-layout">
          <div class="toolbar">
            <p-button label="Load Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onLoadSample()" [loading]="loadingSample()" />
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" placeholder="Paste RSP message here..." height="360px" />
          <div class="actions">
            <p-button label="Convert to FHIR + VC" icon="pi pi-shield" (onClick)="onConvert()" [loading]="converting()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (result()) {
        <p-card header="FHIR Patient" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(fhirPatientJson())" />
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(fhirPatientJson())" />
          </div>
          <app-input-editor [content]="fhirPatientJson()" [readonly]="true" language="json" height="240px" />
        </p-card>

        <p-card header="FHIR Immunization" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(fhirImmunizationJson())" />
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(fhirImmunizationJson())" />
          </div>
          <app-input-editor [content]="fhirImmunizationJson()" [readonly]="true" language="json" height="240px" />
        </p-card>

        <p-card header="Verifiable Credential" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(vcJson())" />
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" pTooltip="View JSON" (onClick)="jsonViewer().open(vcJson())" />
          </div>
          <app-input-editor [content]="vcJson()" [readonly]="true" language="json" height="288px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }

      <p-message severity="warn" styleClass="mt-4 w-full">
        Note: JWS (JSON Web Signature) generation is not yet implemented.
      </p-message>

      <app-json-viewer-dialog />
    </div>
  `,
  styles: `
    .vci-demo { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
    .form-layout { display: flex; flex-direction: column; gap: 1rem; }
    .toolbar { display: flex; align-items: center; gap: 0.25rem; margin-bottom: -0.5rem; }
    .spacer { flex: 1; }
    .actions { display: flex; gap: 0.5rem; }
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class VciDemoComponent implements OnInit {
  private api = inject(VciDemoApiService);

  jsonViewer = viewChild.required(JsonViewerDialogComponent);

  messageData = signal('');
  result = signal<VciConversionResult | null>(null);
  error = signal<string | null>(null);
  converting = signal(false);
  loadingSample = signal(false);

  fhirPatientJson = signal('');
  fhirImmunizationJson = signal('');
  vcJson = signal('');

  ngOnInit(): void {
    this.onLoadSample();
  }

  onLoadSample(): void {
    this.loadingSample.set(true);
    this.api.getSample().subscribe({
      next: (sample) => {
        this.messageData.set(sample);
        this.loadingSample.set(false);
      },
      error: () => this.loadingSample.set(false),
    });
  }

  onConvert(): void {
    if (!this.messageData()) return;

    this.converting.set(true);
    this.error.set(null);
    this.result.set(null);

    this.api.convert(this.messageData()).subscribe({
      next: (res) => {
        this.result.set(res);
        this.fhirPatientJson.set(JSON.stringify(res.fhirPatient, null, 2));
        this.fhirImmunizationJson.set(JSON.stringify(res.fhirImmunization, null, 2));
        this.vcJson.set(JSON.stringify(res.verifiableCredential, null, 2));
        this.converting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Conversion failed');
        this.converting.set(false);
      },
    });
  }

  onReset(): void {
    this.messageData.set('');
    this.result.set(null);
    this.fhirPatientJson.set('');
    this.fhirImmunizationJson.set('');
    this.vcJson.set('');
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
