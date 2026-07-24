import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';
import {ClvrTokenWire} from '../models/clvr.model';

// ==========================================
// API Request & Response Interfaces
// ==========================================

export interface ConvertFhirRequest {
  issuer: string;
  fhirBundle: string;
}

export interface SignCompressRequest {
  clvrTokenJson: string;
  jwk: string;
}

export interface ParseQrRequest {
  qrCodeString: string;
  jwk: string;
}

export interface PdfRequest {
  clvrTokenJson: string;
  qrCodeString: string;
}

export interface LoadKeyResponse {
  message: string;
  kid: string;
}

export interface ExampleFhirResponse {
  issuer: string;
  fhirBundle: string;
}

export interface ConvertFhirResponse {
  clvrToken: ClvrTokenWire;
}

export interface SignCompressResponse {
  qrCodeString: string;
}

export interface ParseQrResponse {
  clvrTokenPretty: string;
}

export interface CheckSignatureResponse {
  valid: boolean;
  message: string;
}

@Injectable({providedIn: 'root'})
export class ClvrTestApiService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private getBasePath(patientId: string): string {
    return `${environment.apiBaseUrl}/rest/tenant/${this.tenantContext.tenantName()}/patient/${patientId}/clvr/test`;
  }

  getExampleKey(patientId: string): Observable<{ jwk: string }> {
    return this.http.get<{ jwk: string }>(`${this.getBasePath(patientId)}/example-key`);
  }

  loadKey(patientId: string, jwk: string): Observable<LoadKeyResponse> {
    return this.http.post<LoadKeyResponse>(`${this.getBasePath(patientId)}/load-key`, {jwk});
  }

  getExampleFhir(patientId: string): Observable<ExampleFhirResponse> {
    return this.http.get<ExampleFhirResponse>(`${this.getBasePath(patientId)}/example-fhir`);
  }

  convertFhir(patientId: string, request: ConvertFhirRequest): Observable<ConvertFhirResponse> {
    return this.http.post<ConvertFhirResponse>(`${this.getBasePath(patientId)}/convert-fhir`, request);
  }

  signAndCompress(patientId: string, request: SignCompressRequest): Observable<SignCompressResponse> {
    return this.http.post<SignCompressResponse>(`${this.getBasePath(patientId)}/sign-and-compress`, request);
  }

  parseQr(patientId: string, request: ParseQrRequest): Observable<ParseQrResponse> {
    return this.http.post<ParseQrResponse>(`${this.getBasePath(patientId)}/parse-qr`, request);
  }

  checkSignature(patientId: string, request: ParseQrRequest): Observable<CheckSignatureResponse> {
    return this.http.post<CheckSignatureResponse>(`${this.getBasePath(patientId)}/check-signature`, request);
  }

  generateQrImage(patientId: string, qrCodeString: string): Observable<Blob> {
    return this.http.post(`${this.getBasePath(patientId)}/generate-qr-image`, {qrCodeString}, {responseType: 'blob'});
  }

  renderPdfImage(patientId: string, request: PdfRequest): Observable<Blob> {
    return this.http.post(`${this.getBasePath(patientId)}/render-pdf-image`, request, {responseType: 'blob'});
  }

  exportPdf(patientId: string, request: PdfRequest): Observable<Blob> {
    return this.http.post(`${this.getBasePath(patientId)}/export-pdf`, request, {responseType: 'blob'});
  }
}
