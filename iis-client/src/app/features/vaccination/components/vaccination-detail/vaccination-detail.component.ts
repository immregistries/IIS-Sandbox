import {Component, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Card} from 'primeng/card';
import {Fieldset} from 'primeng/fieldset';
import {Tag} from 'primeng/tag';
import {Button} from 'primeng/button';
import {IisVaccination} from '../../models/vaccination.model';
import {VaccinationApiService} from '../../services/vaccination-api.service';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {VaccinationRelatedComponent} from '../vaccination-related/vaccination-related.component';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-vaccination-detail',
  standalone: true,
  imports: [Card, Fieldset, Tag, Button, VaccinationRelatedComponent, LoadingSpinnerComponent, DateFormatPipe],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    } @else if (vaccination()) {
      <div class="vaccination-detail">
        <div class="page-header">
          <h1>Vaccination Record</h1>
          <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" [outlined]="true" (onClick)="goBack()" />
        </div>

        <p-card>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">Vaccination ID</span>
              <span class="value">{{ vaccination()!.vaccinationId }}</span>
            </div>
            <div class="detail-item">
              <span class="label">CVX Code</span>
              <span class="value">{{ vaccination()!.vaccineCvxCode }}</span>
            </div>
            <div class="detail-item">
              <span class="label">NDC Code</span>
              <span class="value">{{ vaccination()!.vaccineNdcCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">MVX Code</span>
              <span class="value">{{ vaccination()!.vaccineMvxCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Administered Date</span>
              <span class="value">{{ vaccination()!.administeredDate | iisDate:'long' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Lot Number</span>
              <span class="value">{{ vaccination()!.lotnumber || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Expiration Date</span>
              <span class="value">{{ vaccination()!.expirationDate | iisDate }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Amount</span>
              <span class="value">{{ vaccination()!.administeredAmount || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Information Source</span>
              <span class="value">{{ vaccination()!.informationSource || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Completion Status</span>
              <p-tag [value]="vaccination()!.completionStatus || 'N/A'" [severity]="vaccination()!.completionStatus === 'CP' ? 'success' : 'info'" />
            </div>
            <div class="detail-item">
              <span class="label">Action Code</span>
              <span class="value">{{ vaccination()!.actionCode || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Body Site</span>
              <span class="value">{{ vaccination()!.bodySite || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Body Route</span>
              <span class="value">{{ vaccination()!.bodyRoute || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Funding Source</span>
              <span class="value">{{ vaccination()!.fundingSource || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Funding Eligibility</span>
              <span class="value">{{ vaccination()!.fundingEligibility || '-' }}</span>
            </div>
          </div>
        </p-card>

        @if (relatedVaccinations().length > 0) {
          <p-fieldset legend="Related Vaccinations" styleClass="mt-4">
            <app-vaccination-related [vaccinations]="relatedVaccinations()" />
          </p-fieldset>
        }
      </div>
    }
  `,
  styles: `
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
      h1 { margin: 0; color: #1e293b; }
    }
    .detail-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1.25rem;
    }
    .detail-item {
      .label { display: block; font-size: 0.75rem; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.125rem; }
      .value { font-size: 0.9rem; color: #1e293b; }
    }
    .mt-4 { margin-top: 1rem; }
  `,
})
export class VaccinationDetailComponent implements OnInit {
  private vaccinationApi = inject(VaccinationApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tenantContext = inject(TenantContextService);

  vaccination = signal<IisVaccination | null>(null);
  relatedVaccinations = signal<IisVaccination[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('vaccinationId')!;
    this.vaccinationApi.getVaccination(id).subscribe({
      next: (v) => {
        this.vaccination.set(v);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.vaccinationApi.getRelatedVaccinations(id).subscribe((r) => this.relatedVaccinations.set(r));
  }

  goBack(): void {
    this.router.navigate(['/t', this.tenantContext.tenantName(), 'patients']);
  }
}
