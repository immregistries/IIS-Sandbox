import {inject} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, Router} from '@angular/router';
import {TenantContextService} from '../services/tenant-context.service';

export const tenantGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const tenantContext = inject(TenantContextService);
  const router = inject(Router);

  const tenantName = route.paramMap.get('tenantName');
  if (!tenantName) {
    return router.createUrlTree(['/tenants']);
  }

  const current = tenantContext.currentTenant();
  if (!current || current.organizationName !== tenantName) {
    tenantContext.setTenant({orgId: -1, organizationName: tenantName});
  }

  return true;
};
