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

  constructor() {
    const stored = sessionStorage.getItem('iis_tenant');
    if (stored) {
      try {
        this._currentTenant.set(JSON.parse(stored));
      } catch {
        sessionStorage.removeItem('iis_tenant');
      }
    }

    effect(() => {
      const t = this._currentTenant();
      if (t) {
        sessionStorage.setItem('iis_tenant', JSON.stringify(t));
      } else {
        sessionStorage.removeItem('iis_tenant');
      }
    });
  }

  setTenant(tenant: TenantInfo): void {
    this._currentTenant.set(tenant);
  }

  clearTenant(): void {
    this._currentTenant.set(null);
  }
}
