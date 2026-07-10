import {Component, inject, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {TenantApiService} from '../../services/tenant-api.service';

@Component({
  selector: 'app-tenant-create-dialog',
  standalone: true,
  imports: [FormsModule, Dialog, InputText, Button],
  template: `
    <p-dialog header="Create Tenant" [(visible)]="visible" [modal]="true" [style]="{ width: '400px' }">
      <div class="form-group">
        <label for="tenantName">Tenant Name</label>
        <input pInputText id="tenantName" [(ngModel)]="tenantName" placeholder="Enter tenant name" class="w-full" />
      </div>
      <ng-template #footer>
        <p-button label="Cancel" severity="secondary" [text]="true" (onClick)="visible.set(false)" />
        <p-button label="Create" icon="pi pi-check" (onClick)="onCreate()" [loading]="creating()" [disabled]="!tenantName()" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .form-group {
      margin-bottom: 1rem;
      label { display: block; margin-bottom: 0.375rem; font-weight: 500; font-size: 0.875rem; }
    }
    .w-full { width: 100%; }
  `,
})
export class TenantCreateDialogComponent {
  private tenantApi = inject(TenantApiService);

  visible = signal(false);
  tenantName = signal('');
  creating = signal(false);
  created = output<void>();

  open(): void {
    this.tenantName.set('');
    this.visible.set(true);
  }

  onCreate(): void {
    const name = this.tenantName();
    if (!name) return;
    this.creating.set(true);
    this.tenantApi.createTenant({organizationName: name}).subscribe({
      next: () => {
        this.creating.set(false);
        this.visible.set(false);
        this.created.emit();
      },
      error: () => this.creating.set(false),
    });
  }
}
