import {Routes} from '@angular/router';
import {RecommendationPageComponent} from './components/recommendation-page/recommendation-page.component';

export const RECOMMENDATION_ROUTES: Routes = [{path: ':patientId', component: RecommendationPageComponent}];
