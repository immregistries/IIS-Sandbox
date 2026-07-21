import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';

export interface ForecastActual {
  vaccineGroup: string;
  adminStatus: string;
  validDate: string;
  dueDate: string;
  overdueDate: string;
  vaccineCvx: string;
}

export interface ParseDebugLine {
  lineStatus: string;
  line: string;
  lineStatusReason: string;
}

export interface FitsInspectResult {
  forecastActuals: ForecastActual[];
  parseDebugLines: ParseDebugLine[];
  familyMapping: Record<string, { label: string; count: number }[]>;
  vaccineGroupCounts: { label: string; count: number }[];
  junitCode: string;
}

@Injectable({providedIn: 'root'})
export class FitsApiService {
  private http = inject(HttpClient);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/fits`;
  }

  getExamples(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.basePath}/example/all`);
  }

  getExample(name: string): Observable<string> {
    return this.http.get(`${this.basePath}/example`, {
      params: {name},
      responseType: 'text',
    });
  }

  inspect(rspMessage: string, messageName?: string): Observable<FitsInspectResult> {
    return this.http.post<FitsInspectResult>(`${this.basePath}/inspect`, {rspMessage, messageName});
  }
}
