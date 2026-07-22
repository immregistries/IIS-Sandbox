import {Component, computed, inject, signal} from '@angular/core';
import {forkJoin} from 'rxjs';
import {Dialog} from 'primeng/dialog';
import {TabsModule} from 'primeng/tabs';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {MessageReceived} from '../../../message/models/message.model';
import {MessageApiService} from '../../../message/services/message-api.service';
import {MessageDetailComponent} from '../../../message/components/message-detail/message-detail.component';
import {PatientApiService} from '../../services/patient-api.service';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {environment} from '../../../../../environments/environment';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-history-dialog',
  standalone: true,
  imports: [Dialog, TabsModule, TableModule, Button, MessageDetailComponent, LoadingSpinnerComponent, DateFormatPipe],
  template: `
    <p-dialog header="Patient History" [(visible)]="visible" [modal]="true" [style]="{ width: '900px', maxHeight: '85vh' }">
      <p-tabs value="0">
        <p-tablist>
          <p-tab value="0">Messages ({{ messages().length }})</p-tab>
          <p-tab value="1">FHIR History</p-tab>
        </p-tablist>
        <p-tabpanels>
          <p-tabpanel value="0">
            @if (loadingMessages()) {
              <app-loading-spinner />
            } @else {
              <p-table [value]="messages()" [paginator]="messages().length > 10" [rows]="10" [rowHover]="true" styleClass="p-datatable-sm">
                <ng-template #header>
                  <tr>
                    <th style="width: 80px">ID</th>
                    <th>Date</th>
                    <th>Category</th>
                    <th style="width: 80px"></th>
                  </tr>
                </ng-template>
                <ng-template #body let-msg>
                  <tr>
                    <td>{{ msg.messageReceivedId }}</td>
                    <td>{{ msg.reportedDate | iisDate }}</td>
                    <td>{{ msg.categoryRequest }}</td>
                    <td>
                      <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="viewMessage(msg)" />
                    </td>
                  </tr>
                </ng-template>
                <ng-template #emptymessage>
                  <tr><td colspan="4" style="text-align: center; color: var(--p-text-muted-color);">No messages found for this patient.</td></tr>
                </ng-template>
              </p-table>
            }
          </p-tabpanel>
          <p-tabpanel value="1">
            <a [href]="fhirHistoryUrl()" target="_blank">
              <p-button label="Open FHIR History" icon="pi pi-external-link" severity="secondary" [outlined]="true" />
            </a>
          </p-tabpanel>
        </p-tabpanels>
      </p-tabs>
    </p-dialog>

    <p-dialog header="Message Detail" [(visible)]="messageDialogVisible" [modal]="true" [style]="{ width: '800px', maxHeight: '80vh' }">
      <app-message-detail [message]="selectedMessage()" />
    </p-dialog>
  `,
})
export class PatientHistoryDialogComponent {
  private messageApi = inject(MessageApiService);
  private patientApi = inject(PatientApiService);
  private tenantContext = inject(TenantContextService);

  visible = signal(false);
  patientId = signal('');
  fhirHistoryUrl = computed(() =>
    `${environment.apiBaseUrl}/fhir/${this.tenantContext.tenantName()}/Patient/${this.patientId()}/_history`
  );
  messages = signal<MessageReceived[]>([]);
  loadingMessages = signal(false);
  selectedMessage = signal<MessageReceived | null>(null);
  messageDialogVisible = signal(false);

  open(patientId: string): void {
    this.patientId.set(patientId);
    this.messages.set([]);
    this.visible.set(true);
    this.loadingMessages.set(true);

    this.patientApi.getRelatedPatients(patientId, true).subscribe({
      next: (related) => {
        const ids = [patientId, ...related.map((p) => p.patientId)];
        const unique = [...new Set(ids)];
        forkJoin(unique.map((id) => this.messageApi.getPatientMessages(id))).subscribe({
          next: (results) => {
            const all = results.flat().sort((a, b) =>
              new Date(b.reportedDate ?? 0).getTime() - new Date(a.reportedDate ?? 0).getTime());
            this.messages.set(all);
            this.loadingMessages.set(false);
          },
          error: () => this.loadingMessages.set(false),
        });
      },
      error: () => {
        this.messageApi.getPatientMessages(patientId).subscribe({
          next: (msgs) => {
            this.messages.set(msgs);
            this.loadingMessages.set(false);
          },
          error: () => this.loadingMessages.set(false),
        });
      },
    });
  }

  viewMessage(msg: MessageReceived): void {
    this.selectedMessage.set(msg);
    this.messageDialogVisible.set(true);
  }
}
