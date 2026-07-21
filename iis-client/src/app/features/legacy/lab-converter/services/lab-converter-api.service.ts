import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';
import {TenantContextService} from '../../../../core/services/tenant-context.service';

export interface LabConversionResult {
  vxuMessage: string;
  testCount: number;
}

@Injectable({providedIn: 'root'})
export class LabConverterApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/lab-converter`;
  }

  getSample(): Observable<string> {
    return this.http.get(`${this.basePath}/sample`, {responseType: 'text'});
  }

  convert(oruMessage: string): Observable<LabConversionResult> {
    return this.http.post<LabConversionResult>(this.basePath, oruMessage, {
      headers: {'Content-Type': 'text/plain'},
    });
  }
}
