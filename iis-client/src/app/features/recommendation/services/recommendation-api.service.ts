import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';
import {RecommendationItem} from '../../patient/models/recommendation.model';

@Injectable({providedIn: 'root'})
export class RecommendationApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}`;
  }

  getPatientRecommendations(patientId: string): Observable<RecommendationItem[]> {
    return this.http.get<any>(`${this.basePath}/patient/${patientId}/recommendation`).pipe(
      map((bundle) => this.parseFhirBundle(bundle)),
    );
  }

  generateRecommendation(patientId: string): Observable<void> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http.post<void>(`${this.basePath}/recommendation/random`, null, {params});
  }

  private parseFhirBundle(bundle: any): RecommendationItem[] {
    const items: RecommendationItem[] = [];
    for (const entry of bundle?.entry || []) {
      const resource = entry.resource;
      if (!resource) continue;
      const recommendationId = resource.id || '';
      for (const component of resource.recommendation || []) {
        const coding = component.vaccineCode?.[0]?.coding?.[0];
        const dateCriterion = component.dateCriterion?.[0];
        items.push({
          vaccineCode: coding?.code || '',
          vaccineDisplay: coding?.display || '',
          date: dateCriterion?.value || '',
          dateCriterion: dateCriterion?.code?.coding?.[0]?.display || '',
          recommendationId,
        });
      }
    }
    return items;
  }
}
