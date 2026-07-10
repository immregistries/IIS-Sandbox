import {Routes} from '@angular/router';
import {VaccinationDetailComponent} from './components/vaccination-detail/vaccination-detail.component';

export const VACCINATION_ROUTES: Routes = [{path: ':vaccinationId', component: VaccinationDetailComponent}];
