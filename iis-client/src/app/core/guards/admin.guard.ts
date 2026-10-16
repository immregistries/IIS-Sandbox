import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {map} from 'rxjs';
import {AuthService} from '../services/auth.service';

/**
 * Keeps non-administrators off the admin screens. This only decides what to render:
 * the /rest/admin/** matcher on the server is the real check and answers 403 whatever
 * the client does.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.ensureLoaded().pipe(
    map(() => {
      if (!authService.isAuthenticated()) return router.createUrlTree(['/login']);
      if (authService.isAdmin()) return true;
      return router.createUrlTree(['/dashboard']);
    }),
  );
};
