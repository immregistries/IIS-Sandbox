import {Routes} from '@angular/router';
import {authGuard} from './core/guards/auth.guard';
import {tenantGuard} from './core/guards/tenant.guard';
import {MainLayoutComponent} from './shared/layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'tenants',
        loadChildren: () => import('./features/tenant/tenant.routes').then((m) => m.TENANT_ROUTES),
      },
      {
        path: 't/:tenantName',
        canActivate: [tenantGuard],
        children: [
          {
            path: 'patients',
            loadChildren: () => import('./features/patient/patient.routes').then((m) => m.PATIENT_ROUTES),
          },
          {
            path: 'vaccinations',
            loadChildren: () =>
              import('./features/vaccination/vaccination.routes').then((m) => m.VACCINATION_ROUTES),
          },
          {
            path: 'messages',
            loadChildren: () => import('./features/message/message.routes').then((m) => m.MESSAGE_ROUTES),
          },
          {
            path: 'pop',
            loadChildren: () => import('./features/pop/pop.routes').then((m) => m.POP_ROUTES),
          },
          {path: '', redirectTo: 'patients', pathMatch: 'full'},
        ],
      },
      {path: '', redirectTo: 'dashboard', pathMatch: 'full'},
    ],
  },
];
