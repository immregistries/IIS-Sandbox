import {Component, inject, OnInit, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {LabConversionResult, LabConverterApiService} from '../../services/lab-converter-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-lab-converter',
  standalone: true,
  imports: [Button, Card, Message, Tooltip, InputEditorComponent],
  template: `
    <div class="lab-converter">
      <h1>ORU to VXU Lab Converter</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        Test Data Only — Do not submit real patient data.
      </p-message>

      <p-card header="ORU^R01 Message">
        <div class="form-layout">
          <div class="toolbar">
            <p-button label="Load Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onLoadSample()" [loading]="loadingSample()" />
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" placeholder="Paste ORU^R01 lab message here..." height="360px" />
          <div class="actions">
            <p-button label="Convert" icon="pi pi-arrow-right" (onClick)="onConvert()" [loading]="converting()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (result()) {
        <p-message severity="success" [text]="'Found ' + result()!.testCount + ' SARS-CoV-2 test(s)'" styleClass="mt-4 w-full" />
        <p-card header="VXU Message" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(result()!.vxuMessage)" />
          </div>
          <app-input-editor [content]="result()!.vxuMessage" [readonly]="true" height="288px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .lab-converter { max-width: 960px; }
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
export class LabConverterComponent implements OnInit {
  private api = inject(LabConverterApiService);

  messageData = signal('');
  result = signal<LabConversionResult | null>(null);
  error = signal<string | null>(null);
  converting = signal(false);
  loadingSample = signal(false);

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
        this.converting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Conversion failed');
        this.converting.set(false);
      },
    });
  }

  onReset(): void {
    this.result.set(null);
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
