import {Component, inject, OnInit, signal, viewChild} from '@angular/core';
import {Router} from '@angular/router';
import {TableModule} from 'primeng/table';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {Button} from 'primeng/button';
import {Tenant} from '../../models/tenant.model';
import {getActiveFlavors, ProcessingFlavor} from '../../models/flavor.model';
import {TenantApiService} from '../../services/tenant-api.service';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {TenantCreateDialogComponent} from '../tenant-create-dialog/tenant-create-dialog.component';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [TableModule, Tag, Tooltip, Button, TenantCreateDialogComponent, LoadingSpinnerComponent],
  template: `
    <div class="tenant-list-page">
      <div class="page-header">
        <h1>
          Tenants
          <p-tag
            value="?"
            [rounded]="true"
            severity="info"
            pTooltip="Tenants are separated testing environments. One Tenant ≘ One IIS equivalent. Different Facilities can be registered as information sources to the Tenants."
            tooltipPosition="right"
          />
        </h1>
        <p-button label="Create Tenant" icon="pi pi-plus" (onClick)="createDialog().open()" />
      </div>

      @if (loading()) {
        <app-loading-spinner />
      } @else {
        <p-table [value]="tenants()" [paginator]="true" [rows]="10" [rowHover]="true" styleClass="p-datatable-sm">
          <ng-template #header>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Flavors</th>
              <th style="width: 120px">Actions</th>
            </tr>
          </ng-template>
          <ng-template #body let-tenant>
            <tr>
              <td>{{ tenant.orgId }}</td>
              <td>{{ tenant.organizationName }}</td>
              <td>
                <div class="flavor-tags">
                  @for (key of getActiveFlavorKeys(tenant.organizationName); track key) {
                    <p-tag
                      [value]="key"
                      severity="success"
                      [rounded]="true"
                      [pTooltip]="getFlavorDescription(key)"
                      tooltipPosition="top"
                    />
                  }
                </div>
              </td>
              <td>
                <p-button label="Select" icon="pi pi-arrow-right" size="small" [text]="true" (onClick)="selectTenant(tenant)" />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="4" class="text-center">No tenants found. Create one to get started.</td>
            </tr>
          </ng-template>
        </p-table>
      }

      <app-tenant-create-dialog [flavors]="flavors()" (created)="loadTenants()" />
    </div>
  `,
  styles: `
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
      h1 { margin: 0; }
    }
    .text-center { text-align: center; color: var(--p-text-muted-color); }
    .flavor-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem;
    }
  `,
})
export class TenantListComponent implements OnInit {
  private tenantApi = inject(TenantApiService);
  private tenantContext = inject(TenantContextService);
  private router = inject(Router);

  createDialog = viewChild.required(TenantCreateDialogComponent);

  tenants = signal<Tenant[]>([]);
  flavors = signal<ProcessingFlavor[]>([]);
  loading = signal(false);

  private flavorMap = new Map<string, string>();

  ngOnInit(): void {
    this.loadTenants();
    this.tenantApi.getFlavors().subscribe({
      next: (flavors) => {
        this.flavors.set(flavors);
        this.flavorMap.clear();
        for (const f of flavors) this.flavorMap.set(f.key, f.behaviorDescription);
      },
    });
  }

  loadTenants(): void {
    this.loading.set(true);
    this.tenantApi.getTenants().subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  getActiveFlavorKeys(tenantName: string): string[] {
    return [...getActiveFlavors(tenantName, this.flavors())];
  }

  getFlavorDescription(key: string): string {
    return this.flavorMap.get(key) || '';
  }

  selectTenant(tenant: Tenant): void {
    this.tenantContext.setTenant(tenant);
    this.router.navigate(['/t', tenant.organizationName, 'patients']);
  }
}
