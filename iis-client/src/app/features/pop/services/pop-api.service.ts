import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class PopApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/pop`;
  }

  getSampleMessage(): Observable<string> {
    return this.http.get(`${this.basePath}/sample`, {responseType: 'text'});
  }

  sendMessage(message: string, facilityName?: string): Observable<string> {
    let params = new HttpParams();
    if (facilityName) params = params.set('FACILITY_NAME', facilityName);
    return this.http.post(this.basePath, message, {
      params,
      responseType: 'text',
      headers: {'Content-Type': 'text/plain'},
    });
  }
}
