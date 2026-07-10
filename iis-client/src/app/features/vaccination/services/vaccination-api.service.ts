import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {IisVaccination} from '../models/vaccination.model';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class VaccinationApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/vaccination`;
  }

  getVaccination(vaccinationId: string): Observable<IisVaccination> {
    return this.http.get<IisVaccination>(`${this.basePath}/${vaccinationId}`);
  }

  getRelatedVaccinations(vaccinationId: string): Observable<IisVaccination[]> {
    return this.http.get<IisVaccination[]>(`${this.basePath}/${vaccinationId}/related`);
  }
}
