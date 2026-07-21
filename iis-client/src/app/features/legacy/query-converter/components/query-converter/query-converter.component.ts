import {Component, inject, OnInit, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {Tooltip} from 'primeng/tooltip';
import {QueryConverterApiService} from '../../services/query-converter-api.service';
import {InputEditorComponent} from '../../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-query-converter',
  standalone: true,
  imports: [Button, Card, Message, Tooltip, InputEditorComponent],
  template: `
    <div class="query-converter">
      <h1>Query Converter</h1>

      <p-message severity="info" styleClass="mb-4 w-full">
        Converts VXU (Vaccine Update) messages to QBP (Query By Parameter) messages.
      </p-message>

      <p-card header="VXU Message">
        <div class="form-layout">
          <div class="toolbar">
            <p-button label="New Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onLoadSample()" [loading]="loadingSample()" />
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(messageData())" />
          </div>
          <app-input-editor [(content)]="messageData" placeholder="Paste HL7 VXU message here..." height="360px" />
          <div class="actions">
            <p-button label="Convert to QBP-Z34" icon="pi pi-arrow-right" (onClick)="onConvert('QBP-Z34')" [loading]="converting()" />
            <p-button label="Convert to QBP-Z44" icon="pi pi-arrow-right" severity="secondary" (onClick)="onConvert('QBP-Z44')" [loading]="converting()" />
            <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="QBP Message" styleClass="mt-4">
          <div class="toolbar">
            <span class="spacer"></span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" pTooltip="Copy" (onClick)="copyToClipboard(response()!)" />
          </div>
          <app-input-editor [content]="response() ?? ''" [readonly]="true" height="288px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .query-converter { max-width: 960px; }
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
export class QueryConverterComponent implements OnInit {
  private api = inject(QueryConverterApiService);

  messageData = signal('');
  response = signal<string | null>(null);
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

  onConvert(queryType: string): void {
    if (!this.messageData()) return;

    this.converting.set(true);
    this.error.set(null);
    this.response.set(null);

    this.api.convert(this.messageData(), queryType).subscribe({
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

  onReset(): void {
    this.response.set(null);
    this.error.set(null);
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text);
  }
}
