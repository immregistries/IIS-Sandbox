import {computed, inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {map, Observable, of, tap} from 'rxjs';
import {AuthInfo} from '../models/auth.model';
import {environment} from '../../../environments/environment';

@Injectable({providedIn: 'root'})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private _auth = signal<AuthInfo | null>(null);
  private _loaded = signal(false);
  private _loading = signal(false);
  private _error = signal<string | null>(null);
  private _authCheck$: Observable<AuthInfo> | null = null;

  readonly auth = this._auth.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly isAuthenticated = computed(() => {
    const auth = this._auth();
    return auth?.authenticated === true && auth?.name !== 'anonymousUser';
  });
  readonly username = computed(() => {
    const name = this._auth()?.name;
    return name === 'anonymousUser' ? '' : (name ?? '');
  });

  ensureLoaded(): Observable<void> {
    if (this._loaded()) {
      return of(undefined);
    }
    if (!this._authCheck$) {
      this._authCheck$ = this.http.get<AuthInfo>(`${environment.apiBaseUrl}/rest/authentication`).pipe(
        tap({
          next: (auth) => {
            this._auth.set(auth);
            this._loaded.set(true);
            this._authCheck$ = null;
          },
          error: () => {
            this._auth.set(null);
            this._loaded.set(true);
            this._authCheck$ = null;
          },
        }),
      );
    }
    return this._authCheck$.pipe(map(() => undefined));
  }

  checkAuth(onSuccess?: () => void): void {
    this._loading.set(true);
    this._authCheck$ = null;
    this._loaded.set(false);
    this.http.get<AuthInfo>(`${environment.apiBaseUrl}/rest/authentication`).subscribe({
      next: (auth) => {
        console.info("auth", auth)
        this._auth.set(auth);
        this._loaded.set(true);
        this._loading.set(false);
        onSuccess?.();
      },
      error: () => {
        this._auth.set(null);
        this._loaded.set(true);
        this._loading.set(false);
      },
    });
  }

  loginWithForm(userId: string, password: string, tenantName?: string): void {
    this._loading.set(true);
    this._error.set(null);

    const body = new URLSearchParams();
    body.set('USERID', userId);
    body.set('PASSWORD', password);
    if (tenantName) {
      body.set('TENANTID', tenantName);
    }

    this.http
      .post(`${environment.apiBaseUrl}/login`, body.toString(), {
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-Requested-With': 'XMLHttpRequest',
        },
        responseType: 'text',
        withCredentials: true,
      })
      .subscribe({
        next: () => {
          this.checkAuth(() => {
            console.info(this._auth())
            console.info(this.isAuthenticated())
            if (this.isAuthenticated()) {
              this.router.navigate(['/dashboard']);
            } else {
              this._error.set('Invalid credentials');
            }
          });
        },
        error: () => {
          this._error.set('Invalid credentials');
          this._loading.set(false);
        },
      });
  }

  getGitHubLoginUrl(): string {
    return `${environment.apiBaseUrl}/oauth2/authorization/github`;
  }

  logout(): void {
    this.http
      .post(`${environment.apiBaseUrl}/logout`, null, {withCredentials: true, responseType: 'text'})
      .subscribe({
        next: () => {
          this._auth.set(null);
          this.router.navigate(['/login']);
        },
        error: () => {
          this._auth.set(null);
          this.router.navigate(['/login']);
        },
      });
  }
}
