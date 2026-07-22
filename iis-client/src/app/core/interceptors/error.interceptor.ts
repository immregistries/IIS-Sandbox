import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

/**
 * Functional HTTP interceptor (HttpInterceptorFn) compatible with Angular's
 * `provideHttpClient(withInterceptors([...]))` API. It shows a PrimeNG toast on
 * any error response and opens a dialog with the full payload.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const msgSrv = inject(MessageService);
  
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const body = err.error ?? {};
      const summary = body.message ?? err.statusText ?? 'Server error';
      const detail = `${body.type ?? err.name} – ${body.traceId ?? 'no-trace'}`;

      // Show a toast with a key so we can render a custom template
      msgSrv.add({
        key: 'globalError',
        severity: 'error',
        summary,
        detail,
        data: body,
        sticky: true,
        life: 0,
      });

      // Re‑throw so callers can still handle the error if needed
      return throwError(() => err);
    })
  );
};
