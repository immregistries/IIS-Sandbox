import {Component, computed, inject, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {getActiveFlavors, ProcessingFlavor} from '../../models/flavor.model';
import {FlavorListComponent} from '../flavor-list/flavor-list.component';
import {TenantApiService} from '../../services/tenant-api.service';

@Component({
  selector: 'app-tenant-create-dialog',
  standalone: true,
  imports: [FormsModule, Dialog, InputText, Button, Tag, Tooltip, FlavorListComponent],
  template: `
    <p-dialog header="Create Tenant" [(visible)]="visible" [modal]="true" [style]="{ width: '850px' }">
      <div class="dialog-layout">
        <div class="form-side">
          <div class="form-group">
            <label for="tenantName">
              Tenant Name
              <p-tag
                value="?"
                [rounded]="true"
                severity="info"
                pTooltip="Tenants are separated testing environments. One Tenant ≘ One IIS equivalent. Different Facilities can be registered as information sources to the Tenants."
                tooltipPosition="right"
              />
            </label>
            <input pInputText id="tenantName" [(ngModel)]="tenantName" placeholder="Enter tenant name" class="w-full" />
          </div>
        </div>
        @if (flavors().length) {
          <div class="flavor-side">
            <label>
              Processing Flavors
              <p-tag
                value="?"
                [rounded]="true"
                severity="info"
                pTooltip="If any of the following words appear in the name of the tenant then special processing rules will apply. These processing rules can be used to simulate specific IIS behavior."
                tooltipPosition="left"
              />
            </label>
            <app-flavor-list
              [flavors]="flavors()"
              [activeFlavors]="activeFlavors()"
              [selectable]="true"
              (selectionChange)="onFlavorsChanged($event)"
            />
          </div>
        }
      </div>
      <ng-template #footer>
        <p-button label="Cancel" severity="secondary" [text]="true" (onClick)="visible.set(false)" />
        <p-button label="Create" icon="pi pi-check" (onClick)="onCreate()" [loading]="creating()" [disabled]="!tenantName()" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .dialog-layout {
      display: flex;
      gap: 1.5rem;
    }
    .form-side {
      flex: 1;
      min-width: 250px;
    }
    .flavor-side {
      flex: 1;
      min-width: 300px;
    }
    .form-group, .flavor-side > label {
      label {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        margin-bottom: 0.375rem;
        font-weight: 500;
        font-size: 0.875rem;
      }
    }
    .flavor-side > label {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      margin-bottom: 0.375rem;
      font-weight: 500;
      font-size: 0.875rem;
    }
    .w-full { width: 100%; }
  `,
})
export class TenantCreateDialogComponent {
  private tenantApi = inject(TenantApiService);

  flavors = input<ProcessingFlavor[]>([]);

  visible = signal(false);
  tenantName = signal('');
  creating = signal(false);
  created = output<void>();

  activeFlavors = computed(() => getActiveFlavors(this.tenantName(), this.flavors()));

  open(): void {
    this.tenantName.set('');
    this.visible.set(true);
  }

  onFlavorsChanged(selected: ProcessingFlavor[]): void {
    const baseName = this.getBaseName();
    const flavorKeys = selected.map((f) => f.key);
    this.tenantName.set(flavorKeys.length ? `${baseName}_${flavorKeys.join('_')}` : baseName);
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

  private getBaseName(): string {
    const name = this.tenantName();
    const allKeys = new Set(this.flavors().map((f) => f.key.toLowerCase()));
    return name.split(/[_]/).filter((seg) => !allKeys.has(seg.toLowerCase())).join('_') || name.split('_')[0] || '';
  }
}
