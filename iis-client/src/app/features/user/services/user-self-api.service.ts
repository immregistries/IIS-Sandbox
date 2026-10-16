import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../../environments/environment';
import {ChangePasswordRequest, UserProfile} from '../models/user.model';

/**
 * Self service for the signed-in user, backed by UserSelfRestController.
 * <p>
 * Every endpoint requires an authenticated session: the /rest/user/** matcher in
 * ServerSecurityConfig answers 401 rather than redirecting to the login page, so a
 * rejected call surfaces as an HttpErrorResponse and not as an HTML body.
 */
@Injectable({providedIn: 'root'})
export class UserSelfApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/rest/user/me`;

  getOwnProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.base);
  }

  /**
   * The server rotates the stored hash and refreshes the session copy, so the current
   * session stays usable. Responds 204, 401 when the current password is wrong, and 400
   * when the new one is too short or unchanged.
   */
  changeOwnPassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/password`, request);
  }

  /**
   * @param email a blank value clears the address; anything else must parse as an
   * address or the server answers 400.
   */
  updateOwnEmail(email: string | null): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.base}/email`, {email});
  }
}
