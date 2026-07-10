import {Component, input} from '@angular/core';
import {Panel} from 'primeng/panel';
import {MessageReceived} from '../../models/message.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-message-detail',
  standalone: true,
  imports: [Panel, DateFormatPipe],
  template: `
    @if (message()) {
      <div class="message-detail">
        <div class="message-meta">
          <span>ID: {{ message()!.messageReceivedId }}</span>
          <span>Date: {{ message()!.reportedDate | iisDate:'long' }}</span>
          @if (message()!.categoryRequest) {
            <span>Category: {{ message()!.categoryRequest }}</span>
          }
        </div>

        <p-panel header="Request" [toggleable]="true">
          <pre class="hl7-content">{{ message()!.messageRequest }}</pre>
        </p-panel>

        <p-panel header="Response" [toggleable]="true" styleClass="mt-3">
          <pre class="hl7-content">{{ message()!.messageResponse }}</pre>
        </p-panel>
      </div>
    }
  `,
  styles: `
    .message-detail { display: flex; flex-direction: column; gap: 0.75rem; }
    .message-meta {
      display: flex;
      gap: 1.5rem;
      font-size: 0.85rem;
      color: var(--p-text-muted-color);
    }
    .hl7-content {
      font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
      font-size: 0.8rem;
      white-space: pre-wrap;
      word-break: break-all;
      background: var(--p-content-background);
      padding: 0.75rem;
      border-radius: 4px;
      margin: 0;
      max-height: 400px;
      overflow-y: auto;
    }
    .mt-3 { margin-top: 0.75rem; }
  `,
})
export class MessageDetailComponent {
  message = input<MessageReceived | null>(null);
}
