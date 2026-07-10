import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {IisPatient, PatientMaster} from '../models/patient.model';
import {ObservationReported} from '../models/observation.model';
import {VaccinationMaster} from '../../vaccination/models/vaccination.model';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class PatientApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/patient`;
  }

  getPatients(): Observable<PatientMaster[]> {
    return this.http.get<PatientMaster[]>(this.basePath);
  }

  searchPatients(params: {
    family?: string;
    name?: string;
    identifier?: string;
  }): Observable<PatientMaster[]> {
    let httpParams = new HttpParams();
    if (params.family) httpParams = httpParams.set('family', params.family);
    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.identifier) httpParams = httpParams.set('identifier', params.identifier);
    return this.http.get<PatientMaster[]>(`${this.basePath}/search`, {params: httpParams});
  }

  getPatient(patientId: string): Observable<IisPatient> {
    return this.http.get<IisPatient>(`${this.basePath}/${patientId}`);
  }

  getPatientVaccinations(patientId: string): Observable<VaccinationMaster[]> {
    return this.http.get<VaccinationMaster[]>(`${this.basePath}/${patientId}/vaccination`);
  }

  getPatientObservations(patientId: string): Observable<ObservationReported[]> {
    return this.http.get<ObservationReported[]>(`${this.basePath}/${patientId}/observations`);
  }

  getRelatedPatients(patientId: string): Observable<IisPatient[]> {
    return this.http.get<IisPatient[]>(`${this.basePath}/${patientId}/related`);
  }

  getPatientRecommendations(patientId: string): Observable<unknown> {
    return this.http.get(`${this.basePath}/${patientId}/recommendation`);
  }
}
