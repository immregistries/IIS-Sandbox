import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';
import {TenantContextService} from '../../../../core/services/tenant-context.service';

export interface VxuDownloadRequest {
  dateStart: string;
  dateEnd: string;
  cvxCodes: string;
  includePhi: boolean;
}

@Injectable({providedIn: 'root'})
export class VxuDownloadApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/vxu-download`;
  }

  generate(request: VxuDownloadRequest): Observable<string> {
    return this.http.post(this.basePath, request, {responseType: 'text'});
  }
}
