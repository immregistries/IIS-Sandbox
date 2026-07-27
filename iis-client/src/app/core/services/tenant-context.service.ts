import {computed, effect, Injectable, signal} from '@angular/core';

export interface TenantInfo {
  orgId: number;
  organizationName: string;
}

@Injectable({providedIn: 'root'})
export class TenantContextService {
  private _currentTenant = signal<TenantInfo | null>(null);

  readonly currentTenant = this._currentTenant.asReadonly();
  readonly tenantName = computed(() => this._currentTenant()?.organizationName ?? null);
  readonly hasTenant = computed(() => this._currentTenant() !== null);

  setTenant(tenant: TenantInfo): void {
    this._currentTenant.set(tenant);
  }

  clearTenant(): void {
    this._currentTenant.set(null);
  }
}
