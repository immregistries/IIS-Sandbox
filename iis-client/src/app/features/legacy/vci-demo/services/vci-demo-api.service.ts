import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';

export interface VciConversionResult {
  fhirPatient: Record<string, unknown>;
  fhirImmunization: Record<string, unknown>;
  verifiableCredential: Record<string, unknown>;
}

@Injectable({providedIn: 'root'})
export class VciDemoApiService {
  private http = inject(HttpClient);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/vci-demo`;
  }

  getSample(): Observable<string> {
    return this.http.get(`${this.basePath}/sample`, {responseType: 'text'});
  }

  convert(rspMessage: string): Observable<VciConversionResult> {
    return this.http.post<VciConversionResult>(`${this.basePath}/convert`, rspMessage, {
      headers: {'Content-Type': 'text/plain'},
    });
  }
}
