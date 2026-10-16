import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {AdminUserSummary, CreateUserRequest, UserRole} from '../models/user.model';

/**
 * Administration of every account, backed by AdminUserRestController.
 * <p>
 * The /rest/admin/** matcher in ServerSecurityConfig requires ROLE_ADMIN, so these calls
 * answer 401 when unauthenticated and 403 for a signed-in non-admin.
 */
@Injectable({providedIn: 'root'})
export class AdminUserApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/rest/admin/users`;

  /** Sorted by accessName on the server, each entry carrying the tenants it owns. */
  getAllUsers(): Observable<AdminUserSummary[]> {
    return this.http.get<AdminUserSummary[]>(this.base);
  }

  getUser(userId: number): Observable<AdminUserSummary> {
    return this.http.get<AdminUserSummary>(`${this.base}/${userId}`);
  }

  /**
   * Answers 400 for a duplicate accessName, a password under the minimum length, or a
   * name using the github- prefix reserved for OAuth logins.
   */
  createUser(request: CreateUserRequest): Observable<AdminUserSummary> {
    return this.http.post<AdminUserSummary>(this.base, request);
  }

  /**
   * Resets a password without knowing the old one. Responds 204.
   * <p>
   * Any session the target already holds keeps working for the UI, but its cached copy of
   * the old hash goes stale, so their internal FHIR calls fail until they sign in again.
   */
  setPassword(userId: number, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.base}/${userId}/password`, {newPassword});
  }

  /**
   * Disabling closes self service immediately for a user who already holds a session.
   * The server answers 400 rather than disabling the last enabled ADMIN.
   */
  setEnabled(userId: number, enabled: boolean): Observable<AdminUserSummary> {
    return this.http.put<AdminUserSummary>(`${this.base}/${userId}/enabled`, {enabled});
  }

  /**
   * An empty list falls back to [USER] on the server, and revoking ADMIN from the last
   * enabled administrator answers 400.
   */
  setRoles(userId: number, roles: UserRole[]): Observable<AdminUserSummary> {
    return this.http.put<AdminUserSummary>(`${this.base}/${userId}/roles`, {roles});
  }

  /** A blank value clears the address, see UserSelfApiService.updateOwnEmail. */
  setEmail(userId: number, email: string | null): Observable<AdminUserSummary> {
    return this.http.put<AdminUserSummary>(`${this.base}/${userId}/email`, {email});
  }
}
