import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

export interface ShLinkGenerateRequest {
  patientId: string;
  flag?: string;
  exp?: string;
  passcode?: string;
  label?: string;
  description?: string;
}

export interface ShLinkGenerated {
  id: number;
  url: string;
  patientId: string;
  createdAt: string;
  flag: string;
  exp: number;
  encodedQR: string;
  label: string;
  description: string;
}

@Injectable({providedIn: 'root'})
export class ShLinkApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/sh-link`;
  }

  generate(request: ShLinkGenerateRequest): Observable<string> {
    const params = new HttpParams()
      .set('patientId', request.patientId)
      .set('flag', request.flag || '')
      .set('exp', request.exp || '10000000')
        .set('passcode', request.passcode || '')
        .set('label', request.label || '')
        .set('description', request.description || '');
    return this.http.post(this.basePath, null, {params, responseType: 'text'});
  }

  getAll(): Observable<ShLinkGenerated[]> {
    return this.http.get<ShLinkGenerated[]>(this.basePath);
  }

  getByPatientId(patientId: string): Observable<ShLinkGenerated[]> {
    return this.http.get<ShLinkGenerated[]>(
        `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/patient/${patientId}/sh-link`
    );
  }

  getById(id: number): Observable<ShLinkGenerated> {
    return this.http.get<ShLinkGenerated>(`${this.basePath}/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.basePath}/${id}`);
  }
}
