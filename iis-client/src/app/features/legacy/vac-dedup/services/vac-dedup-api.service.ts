import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';
import {TenantContextService} from '../../../../core/services/tenant-context.service';

export interface ImmunizationRecord {
  date: string;
  cvx: string;
  mvx: string;
  lot: string;
  org: string;
  source: 'SOURCE' | 'HISTORICAL';
}

export interface ImmunizationResult {
  date: string;
  cvx: string;
  mvx: string;
  lotNumber: string;
  organisationID: string;
  source: string;
  vaccineGroupList: string[];
  productCode: string;
  immunizationID: string | null;
}

export interface VacDedupRequest {
  immunizations: ImmunizationRecord[];
  algorithm: 'DETERMINISTIC' | 'WEIGHTED' | 'HYBRID';
}

@Injectable({providedIn: 'root'})
export class VacDedupApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/vacDedup`;
  }

  deduplicate(request: VacDedupRequest): Observable<ImmunizationResult[][]> {
    return this.http.post<ImmunizationResult[][]>(this.basePath, request);
  }
}
