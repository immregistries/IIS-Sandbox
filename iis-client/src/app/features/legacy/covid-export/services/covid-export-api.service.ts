import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';
import {TenantContextService} from '../../../../core/services/tenant-context.service';

export interface CovidExportRequest {
  dateStart: string;
  dateEnd: string;
  cvxCodes: string;
  includePhi: boolean;
}

@Injectable({providedIn: 'root'})
export class CovidExportApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/covid-export`;
  }

  exportData(request: CovidExportRequest): Observable<string> {
    return this.http.post(this.basePath, request, {responseType: 'text'});
  }
}
