import {Routes} from '@angular/router';

export const LEGACY_ROUTES: Routes = [
  {
    path: 'query-converter',
    loadComponent: () =>
      import('./query-converter/components/query-converter/query-converter.component').then(
        (m) => m.QueryConverterComponent,
      ),
  },
  {
    path: 'covid-generate',
    loadComponent: () =>
      import('./covid-generate/components/covid-generate/covid-generate.component').then(
        (m) => m.CovidGenerateComponent,
      ),
  },
  {
    path: 'lab-converter',
    loadComponent: () =>
      import('./lab-converter/components/lab-converter/lab-converter.component').then(
        (m) => m.LabConverterComponent,
      ),
  },
  {
    path: 'vci-demo',
    loadComponent: () =>
      import('./vci-demo/components/vci-demo/vci-demo.component').then((m) => m.VciDemoComponent),
  },
  {
    path: 'vac-dedup',
    loadComponent: () =>
      import('./vac-dedup/components/vac-dedup/vac-dedup.component').then((m) => m.VacDedupComponent),
  },
  {
    path: 'fits',
    loadComponent: () =>
      import('./fits/components/fits/fits.component').then((m) => m.FitsComponent),
  },
  {
    path: 'vxu-download',
    loadComponent: () =>
      import('./vxu-download/components/vxu-download/vxu-download.component').then(
        (m) => m.VxuDownloadComponent,
      ),
  },
  {
    path: 'covid-export',
    loadComponent: () =>
      import('./covid-export/components/covid-export/covid-export.component').then(
        (m) => m.CovidExportComponent,
      ),
  },
  {path: '', redirectTo: 'query-converter', pathMatch: 'full'},
];
