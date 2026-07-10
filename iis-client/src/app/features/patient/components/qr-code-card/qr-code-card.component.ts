import {Component, input} from '@angular/core';
import {Card} from 'primeng/card';
import {Button} from 'primeng/button';

@Component({
  selector: 'app-qr-code-card',
  standalone: true,
  imports: [Card, Button],
  template: `
    <p-card [header]="header()" styleClass="text-center-header">
      <div class="card-body">
        <img [src]="pictureUrl()" [alt]="header()" class="qr-code" />
        <div class="code-uri">
          <span class="label">
            SHLink URI
            <p-button icon="pi pi-copy" [rounded]="true" [text]="true" size="small" (onClick)="copyToClipboard()" />
          </span>
          <code class="code-value">{{ codeUri() }}</code>
        </div>
      </div>
      <ng-template #footer>
        <span class="card-description">
          {{ description() }}
          @if (manifestUrl()) {
            (<a [href]="manifestUrl()" target="_blank">manifest</a>)
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
      max-width: 200px;
      height: auto;
      border-radius: 4px;
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
  `,
})
export class QrCodeCardComponent {
  header = input('');
  description = input('');
  codeUri = input('');
  pictureUrl = input('');
  manifestUrl = input('');

  copyToClipboard(): void {
    navigator.clipboard.writeText(this.codeUri());
  }
}
