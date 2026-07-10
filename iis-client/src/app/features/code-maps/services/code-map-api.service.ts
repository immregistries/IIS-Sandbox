import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class CodeMapApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/rest/code-maps`;

  getCodeMaps(): Observable<unknown> {
    return this.http.get(this.base);
  }

  searchCodesByTable(tableName: string): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.base}/search`, {
      params: new HttpParams().set('tableName', tableName),
    });
  }
}
