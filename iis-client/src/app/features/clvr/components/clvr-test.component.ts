import {InputEditorComponent} from './../../../shared/components/input-editor/input-editor.component';
import {Component, inject, Input, signal} from '@angular/core';
import {CommonModule, JsonPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';

// PrimeNG 19 Modules
import {CardModule} from 'primeng/card';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {TextareaModule} from 'primeng/textarea';
import {DialogModule} from 'primeng/dialog';
import {ToastModule} from 'primeng/toast';
import {MessageModule} from 'primeng/message';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {MessageService} from 'primeng/api';

// Services & Models
import {
  ClvrTestApiService,
  ConvertFhirRequest,
  ParseQrRequest,
  PdfRequest,
  SignCompressRequest
} from '../services/clvr-test-api.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-clvr-test',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    DialogModule,
    ToastModule,
    MessageModule,
    ProgressSpinnerModule,
    InputEditorComponent,
  ],
  providers: [MessageService, ClvrTestApiService, JsonPipe],
  template: `
  <p-toast></p-toast>

<div class="clvr-container">
  <div class="header-banner p-3 mb-4 text-center surface-card border-round shadow-1">
    <h1 class="m-0 text-xl font-bold text-900">Test IPS to CLVR Generator</h1>
  </div>

  <div class="grid p-fluid">
    <!-- Panel 1: Signing Keys -->
    <div class="col-12 md:col-6 lg:col-3">

      <p-card header="Signing Keys" class="h-full flex flex-column">
        <div class="form-layout">
            <p-button
              label="New Sample"
              icon="pi pi-file-plus"
              severity="secondary"
              outlined="true"
               size="small"
              [loading]="loadingKey()"
              (onClick)="loadExampleKey()"
              class="w-full">
            </p-button>
          <app-input-editor [(content)]="jwk" placeholder="JWK key (EC)" height="360px" />

          <div class="options-bar">
            <!-- <div class="facility-field">
              <label for="facilityName">Sending organization name</label>
              <input pInputText id="facilityName" [(ngModel)]="facilityName" placeholder="Overrides the message segments" />
            </div> -->
            <div class="actions">
            <p-button
              label="Load Key Pair"
              icon="pi pi-key"
              [loading]="loadingKey()"
              (onClick)="loadKeyPair()"
              >
            </p-button>
            </div>
          </div>
        </div>
        <p-message
            *ngIf="keyStatus()"
            [severity]="keyStatus()?.type || 'info'"
            [text]="keyStatus()?.text || ''"
            class="w-full">
          </p-message>
      </p-card>
    </div>

    <!-- Panel 2: Bundle Processor -->
    <div class="col-12 md:col-6 lg:col-3">
      <p-card header="Bundle Processor" class="h-full flex flex-column">
        <div class="form-layout">
          <p-button label="New Sample" icon="pi pi-file-plus" severity="secondary" outlined="true" [loading]="loadingFhir()" (onClick)="loadExampleFhir()"></p-button>
          <label for="fhirArea" class="font-bold block mb-2">FHIR Bundle JSON:</label>
          <app-input-editor id="fhirArea" [(content)]="fhirBundle" placeholder="Paste FHIR Bundle JSON here..." height="360px" />

          <div class="options-bar">
            <div class="issuer-field">
              <label for="issuerField">Issuer (3 letter code):</label>
              <input pInputText  id="issuerField"  type="text" [(ngModel)]="issuer" maxlength="3"/>
            </div>
            <div class="actions">
              <p-button label="Convert Bundle" icon="pi pi-arrow-down" [loading]="loadingFhir()" (onClick)="convertFhirBundle()"></p-button>
            </div>
          </div>
        </div>
         <p-message
            *ngIf="fhirStatus()"
            [severity]="fhirStatus()?.type || 'info'"
            [text]="fhirStatus()?.text || ''"
            class="w-full">
          </p-message>
      </p-card>
    </div>

    <!-- Panel 3: CLVR Token -->
    <div class="col-12 md:col-6 lg:col-3">
      <p-card header="CLVR Token" class="h-full flex flex-column">
        <div class="form-layout">
          <label for="clvrArea" class="font-bold block mb-2">CLVR Token JSON:</label>
          <app-input-editor id="clvrArea" [(content)]="clvrTokenJson" placeholder="CLVR Token JSON will appear here..." height="360px" />
        </div>
        <div class="options-bar">
          <div class="actions">
            <p-button
              label="Sign and Compress"
              icon="pi pi-shield pi-arrow-down"
              severity="success"
              [loading]="loadingClvr()"
              (onClick)="signAndCompress()">
            </p-button>
          </div>
          <p-message
            *ngIf="clvrStatus()"
            [severity]="clvrStatus()?.type || 'info'"
            [text]="clvrStatus()?.text || ''"
            class="w-full">
          </p-message>
        </div>
      </p-card>
    </div>

    <!-- Panel 4: CLVR Results & QR -->
    <div class="col-12 md:col-6 lg:col-3">
      <p-card header="CLVR Results" class="h-full flex flex-column">
        <div class="form-layout">
          <label for="qrArea" class="font-bold block mb-2">QR Code (String):</label>
          <app-input-editor id="qrArea" [(content)]="qrCodeString" placeholder="QR string..." height="360px" />

          <div class="options-bar">
            <div class="actions">
              <p-button
                label="Parse Token"
                icon="pi pi-arrow-up"
                severity="secondary"
                [loading]="loadingQr()"
                (onClick)="parseQrToToken()"
                class="w-full">
              </p-button>

              <p-button
                label="Check Signature"
                icon="pi pi-check-circle"
                severity="info"
                [loading]="loadingQr()"
                (onClick)="checkSignature()"
                class="w-full">
              </p-button>

              <p-button label="Open PDF" icon="pi pi-file-pdf" [loading]="loadingQr()" (onClick)="exportPdfFile()" />

            <p-dialog header="European Vaccine Certificate" [(visible)]="pdfDialogVisible" [modal]="true" [style]="{ width: '800px', height: '85vh' }">
              @if (pdfUrl()) {
                <iframe [src]="pdfUrl()!" class="pdf-viewer"></iframe>
              }
            </p-dialog>
            </div>
          </div>

          <p-message
            *ngIf="qrStatus()"
            [severity]="qrStatus()?.type || 'info'"
            [text]="qrStatus()?.text || ''"
            class="w-full mt-2">
          </p-message>
        </div>
      </p-card>
    </div>
  </div>
</div>

<!-- Modal 1: QR Code Image Dialog -->
<p-dialog
  header="Generated Health QR Code"
  [(visible)]="showQrModal"
  [modal]="true"
  [style]="{ width: '350px' }">
  <div class="flex justify-content-center align-items-center p-3">
    <img
      *ngIf="qrImageUrl()"
      [src]="qrImageUrl()"
      alt="Generated Health QR Code"
      class="max-w-full border-round shadow-2" />
  </div>
</p-dialog>

<!-- Modal 2: PDF Preview Dialog -->
<p-dialog
  header="Generated PDF Preview"
  [(visible)]="showPdfModal"
  [modal]="true"
  [style]="{ width: '600px' }">
  <div class="flex justify-content-center align-items-center p-3">
    <img
      *ngIf="pdfImageUrl()"
      [src]="pdfImageUrl()"
      alt="Generated PDF Render"
      class="max-w-full border-round shadow-2" />
  </div>
</p-dialog>`,
  styles: `
  .clvr-container {
    padding: 1rem;
    background-color: var(--surface-ground, #f8f9fa);
    min-height: 100vh;
  }

  textarea {
    resize: vertical;
  }

  .font-mono {
    font-family: monospace;
  }

  :host ::ng-deep .p-card {
    display: flex;
    flex-direction: column;
  }

  :host ::ng-deep .p-card-body {
    display: flex;
    flex-direction: column;
    flex-grow: 1;
  }
  .options-bar {
      display: flex;
      align-items: flex-end;
      gap: 1rem;
      flex-wrap: wrap;
    }
  .issuer-field {
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

  :host ::ng-deep .p-card-content { // TODO remove ng-deep
    display: flex;
    flex-direction: column;
    flex-grow: 1;
  }

  .pdf-viewer {
    width: 100%;
    height: calc(85vh - 130px);
    border: none;
  }
  `

})
export class ClvrTestComponent {
  private clvrTestService = inject(ClvrTestApiService);
  private messageService = inject(MessageService);
  private jsonPipe = inject(JsonPipe);
  private sanitizer = inject(DomSanitizer);


  /** Patient ID parameter passed to the component */
  @Input() patientId: string = '';

  // --- State Variables ---
  jwk = signal<string>('');
  kid = signal<string>('');
  issuer = signal<string>('SYA');
  fhirBundle = signal<string>('');
  clvrTokenJson = signal<string>('');
  qrCodeString = signal<string>('');

  // Status & Panel Loading States
  keyStatus = signal<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  fhirStatus = signal<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  clvrStatus = signal<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  qrStatus = signal<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  loadingKey = signal<boolean>(false);
  loadingFhir = signal<boolean>(false);
  loadingClvr = signal<boolean>(false);
  loadingQr = signal<boolean>(false);

  // Dialog & Image Preview States
  showQrModal = signal<boolean>(false);
  showPdfModal = signal<boolean>(false);
  qrImageUrl = signal<string | null>(null);
  pdfImageUrl = signal<string | null>(null);

  // =========================================================================
  // Panel 1: Key Operations
  // =========================================================================

  pdfDialogVisible = signal(false);
  pdfUrl = signal<SafeResourceUrl | null>(null);

  loadExampleKey(): void {
    this.loadingKey.set(true);
    this.clvrTestService.getExampleKey().subscribe({
      next: (res) => {
        this.jwk.set(this.jsonPipe.transform(res));
        this.keyStatus.set({type: 'info', text: 'Example key loaded into text area'});
        this.loadingKey.set(false);
      },
      error: (err) => this.handleError(err, this.keyStatus, 'Failed to fetch example key', this.loadingKey)
    });
  }

  loadKeyPair(): void {
    if (!this.jwk().trim()) {
      this.keyStatus.set({type: 'error', text: 'JWK field cannot be empty'});
      return;
    }
    this.loadingKey.set(true);
    this.clvrTestService.loadKey(this.jwk()).subscribe({
      next: (res: string) => {
        this.kid.set(res);
        this.keyStatus.set({type: 'success', text: `Key stored and ready (KID: ${res})`});
        this.loadingKey.set(false);
      },
      error: (err) => this.handleError(err, this.keyStatus, 'Key pair could not be loaded', this.loadingKey)
    });
  }

  // =========================================================================
  // Panel 2: Bundle Processor Operations
  // =========================================================================

  loadExampleFhir(): void {
    this.loadingFhir.set(true);
    this.clvrTestService.getExampleFhir(this.patientId).subscribe({
      next: (res) => {
        this.issuer.set(res.issuer);
        this.fhirBundle.set(res.fhirBundle);
        this.fhirStatus.set({type: 'info', text: 'Example FHIR bundle loaded'});
        this.loadingFhir.set(false);
      },
      error: (err) => this.handleError(err, this.fhirStatus, 'Failed to load example FHIR bundle', this.loadingFhir)
    });
  }

  convertFhirBundle(): void {
    if (!this.fhirBundle().trim()) {
      this.fhirStatus.set({type: 'error', text: 'FHIR Bundle cannot be empty!'});
      return;
    }
    this.loadingFhir.set(true);
    const request: ConvertFhirRequest = {
      issuer: this.issuer(),
      fhirBundle: this.fhirBundle()
    };

    this.clvrTestService.convertFhir(request).subscribe({
      next: (res) => {
        const prettyToken = JSON.stringify(res, null, 2).replace(/"(-?\d+)":/g, '$1:');
        this.clvrTokenJson.set(prettyToken);
        this.fhirStatus.set({type: 'success', text: 'Parsed and Converted FHIR Bundle'});
        this.loadingFhir.set(false);
      },
      error: (err) => this.handleError(err, this.fhirStatus, 'Failed to convert FHIR Bundle', this.loadingFhir)
    });
  }

  // =========================================================================
  // Panel 3: CLVR Operations
  // =========================================================================

  signAndCompress(): void {
    if (!this.clvrTokenJson().trim() || !this.jwk().trim()) {
      this.clvrStatus.set({type: 'error', text: 'CLVR Token and JWK Key are required!'});
      return;
    }
    this.loadingClvr.set(true);
    const request: SignCompressRequest = {
      clvrTokenJson: this.clvrTokenJson(),
      kid: this.kid()
    };
    console.info(this.clvrTokenJson(), request)


    this.clvrTestService.signAndCompress(request).subscribe({
      next: (res) => {
        this.qrCodeString.set(res);
        this.clvrStatus.set({type: 'success', text: 'Generated Health QR Code'});
        this.loadingClvr.set(false);
      },
      error: (err) => this.handleError(err, this.clvrStatus, 'Signing/compression failed', this.loadingClvr)
    });
  }

  // =========================================================================
  // Panel 4: QR & PDF Actions
  // =========================================================================

  parseQrToToken(): void {
    if (!this.qrCodeString().trim() || !this.jwk().trim()) {
      this.qrStatus.set({type: 'error', text: 'QR String and JWK key are required!'});
      return;
    }
    this.loadingQr.set(true);
    const request: ParseQrRequest = {
      qrCodeString: this.qrCodeString(),
      kid: this.kid()
    };

    this.clvrTestService.parseQr(request).subscribe({
      next: (res) => {
        const prettyToken = JSON.stringify(res, null, 2).replace(/"(-?\d+)":/g, '$1:');
        this.clvrTokenJson.set(prettyToken);
        this.qrStatus.set({type: 'success', text: 'Parsed and Converted CLVR Token from QR'});
        this.loadingQr.set(false);
      },
      error: (err) => this.handleError(err, this.qrStatus, 'Failed to parse QR Code', this.loadingQr)
    });
  }

  checkSignature(): void {
    if (!this.qrCodeString().trim() || !this.jwk().trim()) {
      this.qrStatus.set({type: 'error', text: 'QR String and JWK key are required!'});
      return;
    }
    this.loadingQr.set(true);
    const request: ParseQrRequest = {
      qrCodeString: this.qrCodeString(),
      kid: this.kid()
    };

    this.clvrTestService.checkSignature(request).subscribe({
      next: (res) => {
        if (res) {
          this.qrStatus.set({type: 'success', text: "Signature validated by provided key"});
          this.messageService.add({
            severity: 'success',
            summary: 'Signature Valid',
            detail: "Signature validated by provided key"
          });
        } else {
          this.qrStatus.set({type: 'error', text: "Signature invalid"});
        }
        this.loadingQr.set(false);
      },
      error: (err) => this.handleError(err, this.qrStatus, 'Signature check failed', this.loadingQr)
    });
  }

  showQrCodeModal(): void {
    if (!this.qrCodeString().trim()) {
      this.qrStatus.set({type: 'error', text: 'QR Code String is empty!'});
      return;
    }
    this.loadingQr.set(true);

  }

  private pdfBlob: Blob | null = null;

  exportPdfFile(): void {
    if (!this.clvrTokenJson().trim() || !this.qrCodeString().trim()) {
      this.qrStatus.set({type: 'error', text: 'Both CLVR Token and QR String are required for PDF!'});
      return;
    }
    this.loadingQr.set(true);
    const request: PdfRequest = {
      clvrTokenJson: this.clvrTokenJson(),
      qrCodeString: this.qrCodeString()
    };
    this.clvrTestService.exportPdf(request).subscribe({
      next: (blob) => {
        this.pdfBlob = blob;
        const objectUrl = URL.createObjectURL(blob);
        this.pdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl));
        this.loadingQr.set(false);
        this.pdfDialogVisible.set(true);
      },
      error: () => this.loadingQr.set(false),
    });
  }

  // --- Helper Methods ---

  private handleError(err: any, statusSignal: any, fallbackMsg: string, loadingSignal: any): void {
    loadingSignal.set(false);
    const errorDetail = err?.error?.error || err?.message || fallbackMsg;
    statusSignal.set({type: 'error', text: errorDetail});
    this.messageService.add({severity: 'error', summary: 'Error', detail: errorDetail});
  }
}
