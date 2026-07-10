import {Component, inject, OnInit, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TableModule} from 'primeng/table';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Dialog} from 'primeng/dialog';
import {MessageReceived} from '../../models/message.model';
import {MessageApiService} from '../../services/message-api.service';
import {MessageDetailComponent} from '../message-detail/message-detail.component';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-message-list',
  standalone: true,
  imports: [FormsModule, TableModule, InputText, Button, Dialog, MessageDetailComponent, LoadingSpinnerComponent, DateFormatPipe],
  template: `
    <div class="message-list-page">
      <h1>Messages</h1>

      <div class="search-bar">
        <input pInputText [(ngModel)]="searchTerm" placeholder="Search messages..." (keyup.enter)="onSearch()" class="search-input" />
        <p-button label="Search" icon="pi pi-search" (onClick)="onSearch()" [loading]="loading()" />
        <p-button label="Show Recent" severity="secondary" [outlined]="true" (onClick)="loadRecent()" [loading]="loading()" />
      </div>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        <p-table [value]="messages()" [paginator]="true" [rows]="10" [rowHover]="true" styleClass="p-datatable-sm">
          <ng-template #header>
            <tr>
              <th style="width: 80px">ID</th>
              <th>Date</th>
              <th>Category</th>
              <th>Patient ID</th>
              <th style="width: 80px"></th>
            </tr>
          </ng-template>
          <ng-template #body let-msg>
            <tr>
              <td>{{ msg.messageReceivedId }}</td>
              <td>{{ msg.reportedDate | iisDate }}</td>
              <td>{{ msg.categoryRequest }}</td>
              <td>{{ msg.patientReportedId || '-' }}</td>
              <td>
                <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="viewMessage(msg)" />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr><td colspan="5" class="text-muted" style="text-align: center;">No messages found.</td></tr>
          </ng-template>
        </p-table>
      }

      <p-dialog header="Message Detail" [(visible)]="dialogVisible" [modal]="true" [style]="{ width: '800px', maxHeight: '80vh' }">
        <app-message-detail [message]="selectedMessage()" />
      </p-dialog>
    </div>
  `,
  styles: `
    h1 { margin: 0 0 1rem; }
    .search-bar {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .search-input { flex: 1; max-width: 400px; }
  `,
})
export class MessageListComponent implements OnInit {
  private messageApi = inject(MessageApiService);

  messages = signal<MessageReceived[]>([]);
  loading = signal(false);
  searchTerm = signal('');
  selectedMessage = signal<MessageReceived | null>(null);
  dialogVisible = signal(false);

  ngOnInit(): void {
    this.loadRecent();
  }

  loadRecent(): void {
    this.loading.set(true);
    this.messageApi.getMessages().subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSearch(): void {
    this.loading.set(true);
    this.messageApi.getMessages(this.searchTerm() || undefined).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  viewMessage(msg: MessageReceived): void {
    this.selectedMessage.set(msg);
    this.dialogVisible.set(true);
  }
}
