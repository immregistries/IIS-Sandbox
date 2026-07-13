import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {IisPatient, PatientMaster} from '../models/patient.model';
import {ObservationReported} from '../models/observation.model';
import {MdmLink} from '../models/mdm-link.model';
import {ShLinkPayload} from '../models/shlink.model';
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

  getPatientVaccinations(patientId: string, isGolden = true): Observable<VaccinationMaster[]> {
    const params = new HttpParams().set('isGolden', isGolden);
    return this.http.get<VaccinationMaster[]>(`${this.basePath}/${patientId}/vaccination`, {params});
  }

  getPatientObservations(patientId: string, isGolden = true): Observable<ObservationReported[]> {
    const params = new HttpParams().set('isGolden', isGolden);
    return this.http.get<ObservationReported[]>(`${this.basePath}/${patientId}/observations`, {params});
  }

  getRelatedPatients(patientId: string, isGolden = true): Observable<IisPatient[]> {
    const params = new HttpParams().set('isGolden', isGolden);
    return this.http.get<IisPatient[]>(`${this.basePath}/${patientId}/related`, {params});
  }

  getPatientRecommendations(patientId: string): Observable<unknown> {
    return this.http.get(`${this.basePath}/${patientId}/recommendation`);
  }

  getShLinkPayload(patientId: string): Observable<ShLinkPayload> {
    return this.http.get<ShLinkPayload>(`${this.basePath}/${patientId}/sh-link-payload`);
  }

  getShLinkIpsPayload(patientId: string): Observable<ShLinkPayload> {
    return this.http.get<ShLinkPayload>(`${this.basePath}/${patientId}/sh-link-payload/ips`);
  }

  getShLinkQrCodeUrl(patientId: string): string {
    return `${this.basePath}/${patientId}/patient-sh-link`;
  }

  getClvrPdf(patientId: string): Observable<Blob> {
    return this.http.get(`${this.basePath}/${patientId}/clvr/pdf`, {responseType: 'blob'});
  }

  getMdmLinks(goldenResourceId: string): Observable<MdmLink[]> {
    const fhirBase = `${environment.apiBaseUrl}/fhir/${this.tenantContext.tenantName()}`;
    return this.http.get<any>(`${fhirBase}/$mdm-query-links`, {
      params: new HttpParams().set('goldenResourceId', goldenResourceId),
    }).pipe(
      map((params) => {
        const links: MdmLink[] = [];
        for (const param of params.parameter || []) {
          if (param.name !== 'link') continue;
          const parts = param.part || [];
          const get = (name: string) => parts.find((p: any) => p.name === name);
          links.push({
            goldenResourceId: (get('goldenResourceId')?.valueString || '').replace('Patient/', ''),
            sourceResourceId: (get('sourceResourceId')?.valueString || '').replace('Patient/', ''),
            matchResult: get('matchResult')?.valueString || '',
            score: get('score')?.valueDecimal,
          });
        }
        return links;
      }),
    );
  }
}
