import {Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {PatientRecommendationsComponent} from '../patient-recommendations/patient-recommendations.component';

@Component({
  selector: 'app-recommendation-page',
  standalone: true,
  imports: [PatientRecommendationsComponent],
  template: `
    <div class="recommendation-page">
      <h1>Recommendations</h1>
      <app-patient-recommendations [patientId]="patientId" />
    </div>
  `,
  styles: `
    .recommendation-page { max-width: 960px; }
    h1 { margin: 0 0 1rem; }
  `,
})
export class RecommendationPageComponent {
  private route = inject(ActivatedRoute);
  patientId = this.route.snapshot.paramMap.get('patientId')!;
}
