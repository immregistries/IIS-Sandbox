import {Component, inject, OnInit, signal} from '@angular/core';
import {Router} from '@angular/router';
import {Select} from 'primeng/select';
import {FormsModule} from '@angular/forms';
import {Tenant} from '../../../features/tenant/models/tenant.model';
import {TenantApiService} from '../../../features/tenant/services/tenant-api.service';
import {TenantContextService} from '../../../core/services/tenant-context.service';

@Component({
  selector: 'app-tenant-selector',
  standalone: true,
  imports: [Select, FormsModule],
  template: `
    <p-select
      [options]="tenants()"
      [ngModel]="tenantContext.currentTenant()"
      (ngModelChange)="onSelect($event)"
      optionLabel="organizationName"
      placeholder="Select tenant"
      [style]="{ minWidth: '180px' }"
      size="small"
    />
  `,
})
export class TenantSelectorComponent implements OnInit {
  private tenantApi = inject(TenantApiService);
  private router = inject(Router);
  tenantContext = inject(TenantContextService);

  tenants = signal<Tenant[]>([]);

  ngOnInit(): void {
    this.tenantApi.getTenants().subscribe((t) => this.tenants.set(t));
  }

  onSelect(tenant: Tenant): void {
    this.tenantContext.setTenant(tenant);
    this.router.navigate(['/t', tenant.organizationName, 'patients']);
  }
}
