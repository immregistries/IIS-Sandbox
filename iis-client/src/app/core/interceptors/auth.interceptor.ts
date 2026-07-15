import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {catchError, throwError} from 'rxjs';
import {TenantContextService} from '../services/tenant-context.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const tenantContext = inject(TenantContextService);
  return next(req).pipe(
    catchError((error) => {
      if (error.status === 401 || error.status === 403) {
        tenantContext.clearTenant();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
