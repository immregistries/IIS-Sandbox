import {Component, inject, input, OnInit, signal, viewChild} from '@angular/core';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {Button} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {ShLinkPayload} from '../../models/shlink.model';
import {PatientApiService} from '../../services/patient-api.service';
import {QrCodeCardComponent} from '../../../../shared/components/qr-code-card/qr-code-card.component';
import {ShLinkGenerateComponent} from '../../../shlink/components/shlink-generate/shlink-generate.component';
import {ShLinkTableComponent} from '../../../shlink/components/shlink-table/shlink-table.component';

@Component({
  selector: 'app-patient-health-cards',
  standalone: true,
  imports: [Button, Dialog, QrCodeCardComponent, ShLinkGenerateComponent, ShLinkTableComponent],
  template: `
    <div class="health-cards">
      <h3>Smart Health Link</h3>
      <div class="cards-grid">
        @if (patientShLink()) {
          <app-qr-code-card
            header="Patient Static Link"
            flag="L"
            description="Long term, statically defined Smart Health Link to the Patient Resource"
            [codeUri]="shlinkUri(patientShLink()!)"
            [manifestUrl]="patientShLink()!.url"
          />
        }
        @if (ipsShLink()) {
          <app-qr-code-card
            flag="L"
            header="IPS Static Link"
            description="Long term, statically defined Smart Health Link to the IPS"
            [codeUri]="shlinkUri(ipsShLink()!)"
            [manifestUrl]="ipsShLink()!.url"
          />
        }
      </div>

      <div class="table-header">
        <h3>Generated Links</h3>
      <p-button label="Generate New Smart Health Link" icon="pi pi-link" [outlined]="true" (onClick)="shlinkDialog().open(patientId())" />

      </div>

      <app-shlink-table [patientId]="patientId()" [showPatientColumn]="false" />

      <app-shlink-generate (generated)="onGenerated()" />

      <h3>European Vaccine Certificate (EVC) - CLVR</h3>
      <p-button label="Generate EVC with IPS" icon="pi pi-file-pdf" [loading]="pdfLoading()" (onClick)="generatePdf()" />

      <p-dialog header="European Vaccine Certificate" [(visible)]="pdfDialogVisible" [modal]="true" [style]="{ width: '800px', height: '85vh' }">
        @if (pdfUrl()) {
          <iframe [src]="pdfUrl()!" class="pdf-viewer"></iframe>
        }
        <ng-template #footer>
          <p-button label="Download PDF" icon="pi pi-download" (onClick)="downloadPdf()" />
        </ng-template>
      </p-dialog>
    </div>
  `,
  styles: `
    .health-cards { display: flex; flex-direction: column; gap: 1rem; }
    h3 { margin: 0.5rem 0; }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 1rem;
    }
    .pdf-viewer {
      width: 100%;
      height: calc(85vh - 130px);
      border: none;
    }
    .table-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 1rem;
      h3 { margin: 0; }
    }
  `,
})
export class PatientHealthCardsComponent implements OnInit {
  private patientApi = inject(PatientApiService);
  private sanitizer = inject(DomSanitizer);

  patientId = input.required<string>();
  shlinkDialog = viewChild.required(ShLinkGenerateComponent);
  shlinkTable = viewChild.required(ShLinkTableComponent);

  patientShLink = signal<ShLinkPayload | null>(null);
  ipsShLink = signal<ShLinkPayload | null>(null);
  pdfDialogVisible = signal(false);
  pdfUrl = signal<SafeResourceUrl | null>(null);
  pdfLoading = signal(false);

  private pdfBlob: Blob | null = null;

  ngOnInit(): void {
    const id = this.patientId();
    this.patientApi.getShLinkPayload(id).subscribe({
      next: (payload) => this.patientShLink.set(payload),
    });
    this.patientApi.getShLinkIpsPayload(id).subscribe({
      next: (payload) => this.ipsShLink.set(payload),
    });
  }

  onGenerated(): void {
    this.shlinkTable().reload();
  }

  generatePdf(): void {
    this.pdfLoading.set(true);
    this.patientApi.getClvrPdf(this.patientId()).subscribe({
      next: (blob) => {
        this.pdfBlob = blob;
        const objectUrl = URL.createObjectURL(blob);
        this.pdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl));
        this.pdfLoading.set(false);
        this.pdfDialogVisible.set(true);
      },
      error: () => this.pdfLoading.set(false),
    });
  }

  downloadPdf(): void {
    if (!this.pdfBlob) return;
    const url = URL.createObjectURL(this.pdfBlob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `evc-${this.patientId()}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  }

  shlinkUri(payload: ShLinkPayload): string {
    return `shlink:/${btoa(JSON.stringify(payload))}`;
  }
}
