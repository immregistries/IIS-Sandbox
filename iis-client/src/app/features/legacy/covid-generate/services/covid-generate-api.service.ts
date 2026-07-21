import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';

export interface CovidGenerateRequest {
  messageCount: number;
  includeAdmin: boolean;
  includeRefusal: boolean;
  includeComorbidity: boolean;
  includeMissed: boolean;
  includeSerology: boolean;
}

@Injectable({providedIn: 'root'})
export class CovidGenerateApiService {
  private http = inject(HttpClient);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/covid-generate`;
  }

  generate(request: CovidGenerateRequest): Observable<string> {
    return this.http.post(this.basePath, request, {responseType: 'text'});
  }
}
