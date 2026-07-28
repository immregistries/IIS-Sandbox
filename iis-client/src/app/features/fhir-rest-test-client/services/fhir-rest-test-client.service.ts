import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {TenantContextService} from '../../../core/services/tenant-context.service';
import {environment} from '../../../../environments/environment';

@Injectable({providedIn: 'root'})
export class FhirRestTestClientService {
  private http = inject(HttpClient);
  private tenantContext = inject(TenantContextService);

  private get basePath(): string {
    return `${environment.apiBaseUrl}/fhir/${this.tenantContext.tenantName()}`;
  }

  create(resource: string, body: string): Observable<{}> {
    const headers = new HttpHeaders({"Content-Type": `application/fhir+json;charset=UTF-8`});
    return this.http.post<{}>(`${this.basePath}/${resource}`, body, {headers: headers});
  }

  read(resource: string, id: string): Observable<{}> {
    return this.http.get(`${this.basePath}/${resource}/${id}`);
  }

  update(resource: string, id: string, body: string): Observable<{}> {
    const headers = new HttpHeaders({"Content-Type": `application/fhir+json;charset=UTF-8`});
    return this.http.put<{}>(`${this.basePath}/${resource}/${id}`, body, {headers: headers});
  }

  delete(resource: string, id: string): Observable<{}> {
    return this.http.delete(`${this.basePath}/${resource}/${id}`);
  }

  search(resource: string, params: HttpParams = new HttpParams()): Observable<{}> {
    const headers = new HttpHeaders({"Content-Type": `application/fhir+json;charset=UTF-8`});
    return this.http.get(`${this.basePath}/${resource}`, {params: params, headers: headers});
  }

  execute(
    operation: 'create' | 'read' | 'update' | 'delete' | 'search',
    resource: string,
    id: string = '',
    body: string = ''
  ): Observable<string | {}> {
    switch (operation) {
      case 'create':
        return this.create(resource, body);
      case 'read':
        return this.read(resource, id);
      case 'update':
        return this.update(resource, id, body);
      case 'delete':
        return this.delete(resource, id);
      case 'search':
        // For simplicity, treat body as URL query string parameters
        const params = new HttpParams({fromString: body});
        return this.search(resource, params);
      default:
        throw new Error(`Unsupported operation: ${operation}`);
    }
  }
}
