import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {MessageReceived} from '../models/message.model';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class MessageApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/message`;
  }

  getMessages(search?: string): Observable<MessageReceived[]> {
    let params = new HttpParams();
    if (search) params = params.set('search', search);
    return this.http.get<MessageReceived[]>(this.basePath, {params});
  }

  getPatientMessages(patientId: string): Observable<MessageReceived[]> {
    return this.http.get<MessageReceived[]>(`${this.basePath}/${patientId}`);
  }
}
