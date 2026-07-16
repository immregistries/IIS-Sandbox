import {Component, inject, input, OnInit, signal} from '@angular/core';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {ConfirmDialog} from 'primeng/confirmdialog';
import {ConfirmationService} from 'primeng/api';
import {Dialog} from 'primeng/dialog';
import {ShLinkApiService, ShLinkGenerated} from '../../services/shlink-api.service';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';
import {QrCodeCardComponent} from '../../../../shared/components/qr-code-card/qr-code-card.component';

@Component({
  selector: 'app-shlink-table',
  standalone: true,
  imports: [Button, TableModule, Tag, ConfirmDialog, Dialog, LoadingSpinnerComponent, DateFormatPipe, QrCodeCardComponent],
  providers: [ConfirmationService],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    } @else {
      <p-table [value]="shLinks()" [paginator]="true" [rows]="10" [rowHover]="true" styleClass="p-datatable-sm">
        <ng-template #header>
          <tr>
            <th style="width: 60px">ID</th>
            @if (showPatientColumn()) {
              <th>Patient</th>
            }
            <th>Label</th>
            <th>Flag</th>
            <th>Created</th>
            <th style="width: 100px"></th>
          </tr>
        </ng-template>
        <ng-template #body let-link>
          <tr>
            <td>{{ link.id }}</td>
            @if (showPatientColumn()) {
              <td>{{ link.patientId }}</td>
            }
            <td>{{ link.label || '-' }}</td>
            <td>
              @if (link.flag) {
                <p-tag [value]="link.flag" severity="info" />
              } @else {
                -
              }
            </td>
            <td>{{ link.createdAt | iisDate }}</td>
            <td>
              <p-button icon="pi pi-qrcode" [rounded]="true" [text]="true" size="small" (onClick)="viewQr(link)" />
              <p-button icon="pi pi-trash" [rounded]="true" [text]="true" size="small" severity="danger" (onClick)="confirmDelete(link)" />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td [attr.colspan]="showPatientColumn() ? 6 : 5" style="text-align: center;" class="text-muted">No Smart Health Links generated yet.</td></tr>
        </ng-template>
      </p-table>
    }

    <p-dialog [(visible)]="qrDialogVisible" [modal]="true" [style]="{ width: '400px' }">
      @if (selectedLink()) {
        <div class="qr-view">
          <app-qr-code-card
            [flag]="selectedLink()?.flag ?? ''"
            [codeUri]="selectedLink()?.encodedQR ?? ''"
            [description]="selectedLink()?.description"
            [header]="selectedLink()?.label"
            [manifestUrl]="selectedLink()?.url"
            [exp]="selectedLink()?.exp"
            [createdAt]="selectedLink()?.createdAt"
          ></app-qr-code-card>
        </div>
      }
    </p-dialog>

    <p-confirmDialog />
  `,
  styles: `
    .qr-view {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
    }
  `,
})
export class ShLinkTableComponent implements OnInit {
  private shLinkApi = inject(ShLinkApiService);
  private confirmationService = inject(ConfirmationService);

  patientId = input<string | undefined>();
  showPatientColumn = input(true);

  shLinks = signal<ShLinkGenerated[]>([]);
  loading = signal(false);
  selectedLink = signal<ShLinkGenerated | null>(null);
  qrDialogVisible = signal(false);

  ngOnInit(): void {
    this.loadShLinks();
  }

  reload(): void {
    this.loadShLinks();
  }

  private loadShLinks(): void {
    this.loading.set(true);
    const source$ = this.patientId()
      ? this.shLinkApi.getByPatientId(this.patientId()!)
      : this.shLinkApi.getAll();
    source$.subscribe({
      next: (links) => {
        this.shLinks.set(links);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  viewQr(link: ShLinkGenerated): void {
    this.selectedLink.set(link);
    this.qrDialogVisible.set(true);
  }

  confirmDelete(link: ShLinkGenerated): void {
    this.confirmationService.confirm({
      message: `Delete Smart Health Link #${link.id}?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.shLinkApi.delete(link.id).subscribe({
          next: () => this.loadShLinks(),
        });
      },
    });
  }
}
