import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class QueryConverterApiService {
  private http = inject(HttpClient);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/rest/query-converter`;
  }

  getSample(): Observable<string> {
    return this.http.get(`${this.basePath}/sample`, {responseType: 'text'});
  }

  convert(message: string, queryType: string): Observable<string> {
    return this.http.post(this.basePath, {message, queryType}, {responseType: 'text'});
  }
}
