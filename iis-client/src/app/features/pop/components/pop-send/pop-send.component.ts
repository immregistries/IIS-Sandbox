import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {Message} from 'primeng/message';
import {PopApiService} from '../../services/pop-api.service';
import {InputEditorComponent} from '../../../../shared/components/input-editor/input-editor.component';

@Component({
  selector: 'app-pop-send',
  standalone: true,
  imports: [FormsModule, InputText, Button, Card, Message, InputEditorComponent],
  template: `
    <div class="pop-send">
      <h1>Send Now</h1>

      <p-message severity="warn" styleClass="mb-4 w-full">
        <ng-template #messageicon>
          <i class="pi pi-exclamation-triangle"></i>
        </ng-template>
        Test Data Only — Do not submit real patient data.
      </p-message>

      <p-card header="VXU Message">
        <div class="form-layout">
          <p-button label="New Sample" icon="pi pi-file-plus" severity="secondary" [outlined]="true" size="small" (onClick)="onNewSample()" [loading]="loadingSample()" />
          <app-input-editor [(content)]="messageData" placeholder="Paste HL7 VXU message here..." height="360px" />

          <div class="options-bar">
            <div class="facility-field">
              <label for="facilityName">Sending organization name</label>
              <input pInputText id="facilityName" [(ngModel)]="facilityName" placeholder="Overrides the message segments" />
            </div>
            <div class="actions">
              <p-button label="Submit" icon="pi pi-send" (onClick)="onSubmit()" [loading]="submitting()" />
              <p-button label="Reset" icon="pi pi-refresh" severity="secondary" [outlined]="true" (onClick)="onReset()" />
            </div>
          </div>
        </div>
      </p-card>

      @if (response()) {
        <p-card header="ACK Response" styleClass="mt-4">
          <app-input-editor [content]="response() ?? ''" [readonly]="true" height="288px" />
        </p-card>
      }

      @if (error()) {
        <p-message severity="error" [text]="error()!" styleClass="mt-4 w-full" />
      }
    </div>
  `,
  styles: `
    .pop-send { max-width: 960px; }
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
    .mb-4 { margin-bottom: 1rem; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
  `,
})
export class PopSendComponent implements OnInit {
  private popApi = inject(PopApiService);

  messageData = signal('');
  facilityName = signal('');
  response = signal<string | null>(null);
  error = signal<string | null>(null);
  submitting = signal(false);
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

  onSubmit(): void {
    if (!this.messageData()) return;

    this.submitting.set(true);
    this.error.set(null);
    this.response.set(null);

    this.popApi.sendMessage(this.messageData(), this.facilityName() || undefined).subscribe({
      next: (ack) => {
        this.response.set(ack);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Failed to send message');
        this.submitting.set(false);
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
}
