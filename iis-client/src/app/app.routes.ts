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
            path: 'dashboard',
            loadChildren: () => import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
          },
          {
            path: 'tenants',
            loadChildren: () => import('./features/tenant/tenant.routes').then((m) => m.TENANT_ROUTES),
          },
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
          {
            path: 'sh-link',
            loadChildren: () => import('./features/shlink/shlink.routes').then((m) => m.SHLINK_ROUTES),
          },
          {
            path: 'subscription',
            loadChildren: () =>
              import('./features/subscription/subscription.routes').then((m) => m.SUBSCRIPTION_ROUTES),
          },
          {
            path: 'clvr',
            loadChildren: () =>
              import('./features/clvr/clvr.routes').then((m) => m.CLVR_ROUTES),
          },
          {
            path: 'recommendation',
            loadChildren: () =>
              import('./features/recommendation/recommendation.routes').then((m) => m.RECOMMENDATION_ROUTES),
          },
          {
            path: 'fhir-messaging',
            loadChildren: () =>
              import('./features/fhir-messaging/fhir-messaging.routes').then((m) => m.FHIR_MESSAGING_ROUTES),
          },
          {
            path: 'v2-to-fhir',
            loadChildren: () =>
              import('./features/v2-to-fhir/v2-to-fhir.routes').then((m) => m.V2_TO_FHIR_ROUTES),
          },
          {
            path: 'wsdl',
            loadChildren: () => import('./features/wsdl/wsdl.routes').then((m) => m.WSDL_ROUTES),
          },
          {
            path: 'legacy',
            loadChildren: () =>
              import('./features/legacy/legacy.routes').then((m) => m.LEGACY_ROUTES),
          },
          {path: '', redirectTo: 'patients', pathMatch: 'full'},
        ],
      },
      {path: '', redirectTo: 'dashboard', pathMatch: 'full'},
    ],
  },
];
