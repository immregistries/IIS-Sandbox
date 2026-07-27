import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

// ==========================================
// API Request & Response Interfaces
// ==========================================

export interface ConvertFhirRequest {
  issuer: string;
  fhirBundle: string;
}

export interface SignCompressRequest {
  clvrTokenJson: string;
  kid: string;
}

export interface ParseQrRequest {
  qrCodeString: string;
  kid: string;
}

export interface PdfRequest {
  clvrTokenJson: string;
  qrCodeString: string;
}

export interface ExampleFhirResponse {
  issuer: string;
  fhirBundle: string;
}

export interface CheckSignatureResponse {
  valid: boolean;
  message: string;
}

@Injectable({providedIn: 'root'})
export class ClvrTestApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private getBasePath(): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/clvr/test`;
  }

  getExampleKey(): Observable<string> {
    return this.http.get<string>(`${this.getBasePath()}/example-key`);
  }

  loadKey(jwk: string): Observable<string> {
    const headers = new HttpHeaders({'Accept': 'text/plain'});
    return this.http.post(`${this.getBasePath()}/load-key`, jwk, {
      headers: headers,
      responseType: 'text'
    });
  }

  getExampleFhir(patientId: string): Observable<ExampleFhirResponse> {
    return this.http.get<ExampleFhirResponse>(`${this.getBasePath()}/example-fhir`, {params: patientId ? {patientId: patientId} : {}});
  }

  convertFhir(request: ConvertFhirRequest): Observable<string> {
    const headers = new HttpHeaders({'Accept': 'text/plain'});
    return this.http.post(`${this.getBasePath()}/convert-fhir`, request, {
      headers: headers,
      responseType: 'text'
    });
  }

  signAndCompress(request: SignCompressRequest): Observable<string> {
    return this.http.post<string>(`${this.getBasePath()}/sign-and-compress`, request);
  }

  parseQr(request: ParseQrRequest): Observable<string> {
    return this.http.post<string>(`${this.getBasePath()}/parse-qr`, request);
  }

  checkSignature(request: ParseQrRequest): Observable<CheckSignatureResponse> {
    return this.http.post<CheckSignatureResponse>(`${this.getBasePath()}/check-signature`, request);
  }

  exportPdf(request: PdfRequest): Observable<Blob> {
    return this.http.post(`${this.getBasePath()}/export-pdf`, request, {responseType: 'blob'});
  }
}
