import {Routes} from '@angular/router';
import {PatientSearchComponent} from './components/patient-search/patient-search.component';
import {PatientDetailComponent} from './components/patient-detail/patient-detail.component';

export const PATIENT_ROUTES: Routes = [
  {path: '', component: PatientSearchComponent},
  {path: ':patientId', component: PatientDetailComponent},
];
