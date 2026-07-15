import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';
import {SubscriptionItem, TriggerRequest} from '../models/subscription.model';

@Injectable({providedIn: 'root'})
export class SubscriptionApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get restPath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/subscription`;
  }

  private get fhirPath(): string {
    return `${environment.apiBaseUrl}/fhir/${this.tenantContext.tenantName()}/Subscription`;
  }

  getAll(): Observable<SubscriptionItem[]> {
    return this.http.get<any>(this.restPath).pipe(
      map((bundle) => this.parseFhirBundle(bundle)),
    );
  }

  trigger(request: TriggerRequest): Observable<string> {
    return this.http.post(`${this.restPath}/trigger`, request, {responseType: 'text'});
  }

  create(subscription: any): Observable<any> {
    return this.http.post(this.fhirPath, subscription);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.fhirPath}/${id}`);
  }

  private parseFhirBundle(bundle: any): SubscriptionItem[] {
    return (bundle?.entry || []).map((entry: any) => {
      const r = entry.resource;
      return {
        id: r?.id || '',
        name: r?.name || '',
        identifier: r?.identifier?.[0]?.value || '',
        endpoint: r?.endpoint || '',
        status: r?.status || '',
        topic: r?.topic || '',
        contentType: r?.contentType || '',
      };
    });
  }
}
