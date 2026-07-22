import {Component, computed, input} from '@angular/core';
import {DatePipe} from '@angular/common';
import {Card} from 'primeng/card';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {QRCodeComponent} from 'angularx-qrcode';

@Component({
  selector: 'app-qr-code-card',
  standalone: true,
  imports: [Card, Button, Tag, DatePipe, QRCodeComponent],
  template: `
    <p-card [header]="header()" styleClass="text-center-header">
      <div class="card-body">
        <qrcode
          [qrdata]="codeUri()"
          [errorCorrectionLevel]="'M'"
          [width]="250"
          [elementType]="'img'"
          class="qr-code">
        </qrcode>
        <div class="tags">
          @if (passcodeProtected()) {
            <p-tag value="Passcode protected" icon="pi pi-lock" severity="warn" />
          }
          @if (directFile()) {
            <p-tag value="Direct File" icon="pi pi-file" severity="info" />
          }
          @if (longTerm()) {
            <p-tag value="Long Term" icon="pi pi-clock" severity="success" />
          }
        </div>
        @if (expirationDate()) {
          <span class="expiration">Expires: {{ expirationDate() | date:'medium' }}</span>
        }
        <div class="code-uri">
          <span class="label">
            SHLink URI
            <span class="spacer"> </span>
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" (onClick)="copyToClipboard()" />
          </span>
          <code class="code-value">{{ codeUri() }}</code>
        </div>
      </div>
      <ng-template #footer>
        <span class="card-description">
          {{ description() }}
          @if (manifestUrl()) {
            <a [href]="manifestUrl()" target="_blank">
              <p-button [label]="directFile() ? 'File' : 'Manifest'" icon="pi pi-external-link" severity="secondary" [outlined]="true" />
            </a>
          }
        </span>
      </ng-template>
    </p-card>
  `,
  styles: `
    .card-body {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
    }
    .qr-code {
      display: block;
      max-width: 250px;
      margin: 0 auto;
    }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      justify-content: center;
    }
    .expiration {
      font-size: 0.8rem;
      color: var(--p-text-muted-color);
    }
    .code-uri {
      width: 100%;
      .label {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-size: 0.75rem;
        color: var(--p-text-muted-color);
        text-transform: uppercase;
        letter-spacing: 0.05em;
        margin-bottom: 0.25rem;
      }
    }
    .code-value {
      display: block;
      font-size: 0.75rem;
      word-break: break-all;
      background: var(--p-content-background);
      padding: 0.5rem;
      border-radius: 4px;
      max-height: 60px;
      overflow-y: auto;
    }
    .card-description {
      font-size: 0.8rem;
      color: var(--p-text-muted-color);
    }
    .spacer { flex: 1; }
  `,
})
export class QrCodeCardComponent {
  header = input<string | undefined>('');
  description = input<string | undefined>('');
  codeUri = input('');
  manifestUrl = input<string | undefined>('');
  flag = input('');
  exp = input<number | undefined>();
  createdAt = input<string | undefined>();

  passcodeProtected = computed(() => this.flag().includes('P'));
  directFile = computed(() => this.flag().includes('U'));
  longTerm = computed(() => this.flag().includes('L'));
  expirationDate = computed(() => {
    const exp = this.exp();
    const createdAt = this.createdAt();
    if (!exp || !createdAt) return null;
    const createdMs = new Date(createdAt).getTime();
    return new Date(createdMs + exp * 1000);
  });


  copyToClipboard(): void {
    navigator.clipboard.writeText(this.codeUri());
  }
}
