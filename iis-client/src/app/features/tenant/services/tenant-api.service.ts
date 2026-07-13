import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Tenant} from '../models/tenant.model';
import {ProcessingFlavor} from '../models/flavor.model';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class TenantApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/rest/tenant`;

  getTenants(): Observable<Tenant[]> {
    return this.http.get<Tenant[]>(this.base);
  }

  getTenant(tenantId: number): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.base}/${tenantId}`);
  }

  createTenant(tenant: Partial<Tenant>): Observable<Tenant> {
    return this.http.post<Tenant>(this.base, tenant);
  }

  getFlavors(): Observable<ProcessingFlavor[]> {
    return this.http.get<ProcessingFlavor[]>(`${environment.apiBaseUrl}/rest/flavors`);
  }
}
