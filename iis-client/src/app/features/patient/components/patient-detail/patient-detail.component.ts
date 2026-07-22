import {Component, inject, input, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {TabsModule} from 'primeng/tabs';
import {Fieldset} from 'primeng/fieldset';
import {Tag} from 'primeng/tag';
import {Tooltip} from 'primeng/tooltip';
import {Button} from 'primeng/button';
import {TableModule} from 'primeng/table';
import {IisPatient} from '../../models/patient.model';
import {ObservationReported} from '../../models/observation.model';
import {MdmLink} from '../../models/mdm-link.model';
import {VaccinationMaster} from '../../../vaccination/models/vaccination.model';
import {PatientApiService} from '../../services/patient-api.service';
import {TenantContextService} from '../../../../core/services/tenant-context.service';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';
import {PatientObservationsComponent} from '../patient-observations/patient-observations.component';
import {PatientRelatedComponent} from '../patient-related/patient-related.component';
import {PatientMdmChartComponent} from '../patient-mdm-chart/patient-mdm-chart.component';
import {PatientHealthCardsComponent} from '../patient-health-cards/patient-health-cards.component';
import {PatientFhirLinksComponent} from '../patient-fhir-links/patient-fhir-links.component';
import {
  PatientRecommendationsComponent
} from '../../../recommendation/components/patient-recommendations/patient-recommendations.component';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';
import {PatientHistoryDialogComponent} from '../patient-history-dialog/patient-history-dialog.component';
import {Card} from "primeng/card";

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    TabsModule,
    Fieldset,
    Tag,
    Tooltip,
    Button,
    TableModule,
    LoadingSpinnerComponent,
    PatientObservationsComponent,
    PatientRelatedComponent,
    PatientMdmChartComponent,
    PatientHealthCardsComponent,
    PatientFhirLinksComponent,
    PatientRecommendationsComponent,
    DateFormatPipe,
    PatientHistoryDialogComponent,
    Card
  ],
  template: `
    @if (loading()) {
      <app-loading-spinner />
    } @else if (patient()) {
      <div class="patient-detail">
        <div class="page-header">
          <div>
            <h1>{{ patient()!.patientNames?.[0]?.nameLast }}, {{ patient()!.patientNames?.[0]?.nameFirst }}</h1>
            <span class="patient-id">
              ID: {{ patient()!.patientId }}
              @if (asNonGolden()) {
                <p-tag
                  value="Reported Record"
                  severity="info"
                  [rounded]="true"
                  pTooltip="Reported (Non-golden) record. As part of the Master Data Management (MDM), this record represents the information as it was first received, before a merging process, and is kept separated from the consolidated record for preserving history and later potential merging."
                  tooltipPosition="bottom"
                />
              } @else {
                <p-tag
                  value="Golden Record"
                  severity="warn"
                  [rounded]="true"
                  pTooltip="Consolidated (Golden) record. As part of the Master Data Management (MDM), this record was generated aggregating the information across records identified as potential duplicates."
                  tooltipPosition="bottom"
                />
              }
            </span>
          </div>
          <div class="header-actions">
            <p-button label="History" icon="pi pi-clock" severity="secondary" [outlined]="true" (onClick)="historyDialog.open(patient()!.patientId)" />
            <p-button label="Back" icon="pi pi-arrow-left" severity="secondary" [outlined]="true" (onClick)="goBack()" />
          </div>
        </div>

        <p-tabs value="0">
          <p-tablist>
            <p-tab value="0">Demographics</p-tab>
            <p-tab value="1">Vaccinations ({{ vaccinations().length }})</p-tab>
            <p-tab value="2">Observations ({{ observations().length }})</p-tab>
            <p-tab value="3">Related Patients ({{ relatedPatients().length }})</p-tab>
            <p-tab value="4">Health Cards</p-tab>
            <p-tab value="5">Recommendations</p-tab>
            <p-tab value="6">FHIR API</p-tab>
          </p-tablist>
          <p-tabpanels>
            <p-tabpanel value="0">
              <div class="demographics-grid">
                <p-fieldset legend="Personal Information">
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="label">Date of Birth</span>
                      <span class="value">{{ patient()!.birthDate | iisDate:'long' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">Sex</span>
                      <span class="value">{{ patient()!.sex }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">Ethnicity</span>
                      <span class="value">{{ patient()!.ethnicity || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">Race</span>
                      <span class="value">{{ patient()!.races?.join(', ') || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">Mother Maiden Name</span>
                      <span class="value">{{ patient()!.motherMaidenName || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">Email</span>
                      <span class="value">{{ patient()!.email || '-' }}</span>
                    </div>
                  </div>
                </p-fieldset>

                @if (patient()!.addresses?.length) {
                  <p-fieldset legend="Address">
                    @for (addr of patient()!.addresses; track $index) {
                      <div class="info-grid">
                        <div class="info-item">
                          <span class="label">Address</span>
                          <span class="value">{{ addr.addressLine1 }} {{ addr.addressLine2 }}</span>
                        </div>
                        <div class="info-item">
                          <span class="label">City / State / Zip</span>
                          <span class="value">{{ addr.addressCity }}, {{ addr.addressState }} {{ addr.addressZip }}</span>
                        </div>
                      </div>
                    }
                  </p-fieldset>
                }

                @if (patient()!.businessIdentifiers?.length) {
                  <p-fieldset legend="Identifiers">
                    <p-table [value]="patient()!.businessIdentifiers" styleClass="p-datatable-sm">
                      <ng-template #header>
                        <tr><th>System</th><th>Value</th><th>Type</th></tr>
                      </ng-template>
                      <ng-template #body let-id>
                        <tr><td>{{ id.system }}</td><td>{{ id.value }}</td><td>{{ id.type }}</td></tr>
                      </ng-template>
                    </p-table>
                  </p-fieldset>
                }
              </div>
            </p-tabpanel>

            <p-tabpanel value="1">
              <p-table [value]="vaccinations()" styleClass="p-datatable-sm" [paginator]="true" [rows]="10" [rowHover]="true">
                <ng-template #header>
                  <tr>
                    <th>CVX</th>
                    <th>Administered Date</th>
                    <th>MVX</th>
                    <th>Lot #</th>
                    <th>Status</th>
                    <th style="width: 80px"></th>
                  </tr>
                </ng-template>
                <ng-template #body let-vax>
                  <tr>
                    <td>{{ vax.vaccineCvxCode }}</td>
                    <td>{{ vax.administeredDate | iisDate }}</td>
                    <td>{{ vax.vaccineMvxCode }}</td>
                    <td>{{ vax.lotnumber }}</td>
                    <td><p-tag [value]="vax.completionStatus || 'N/A'" [severity]="vax.completionStatus === 'CP' ? 'success' : 'info'" /></td>
                    <td>
                      <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="viewVaccination(vax.vaccinationId)" />
                    </td>
                  </tr>
                </ng-template>
                <ng-template #emptymessage>
                  <tr><td colspan="6" class="text-muted" style="text-align: center;">No vaccinations recorded.</td></tr>
                </ng-template>
              </p-table>
            </p-tabpanel>

            <p-tabpanel value="2">
              <app-patient-observations [observations]="observations()" />
            </p-tabpanel>

            <p-tabpanel value="3">

              <app-patient-related [patients]="relatedPatients()" [isGolden]="!asNonGolden()" (selected)="viewRelatedPatient($event)" />
             @if (relatedPatients().length) {
              <p-card>
                <app-patient-mdm-chart
                  [patient]="patient()!"
                  [relatedPatients]="relatedPatients()"
                  [mdmLinks]="mdmLinks()"
                  [isGolden]="!asNonGolden()"
                  (selected)="viewRelatedPatient($event)"
                />
              </p-card>


              }
            </p-tabpanel>

            <p-tabpanel value="4">
              <app-patient-health-cards [patientId]="patient()!.patientId" />
            </p-tabpanel>

            <p-tabpanel value="5">
              <app-patient-recommendations [patientId]="patient()!.patientId" />
            </p-tabpanel>

            <p-tabpanel value="6">
              <app-patient-fhir-links [patientId]="patient()!.patientId" />
            </p-tabpanel>
          </p-tabpanels>
        </p-tabs>
      </div>
      <app-patient-history-dialog #historyDialog />
    }
  `,
  styles: `
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1.25rem;
      h1 { margin: 0 0 0.25rem; }
    }
    .header-actions { display: flex; gap: 0.5rem; }
    .patient-id {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.85rem;
      color: var(--p-text-muted-color);
    }
    .demographics-grid { display: flex; flex-direction: column; gap: 1rem; }
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1rem;
    }
    .info-item {
      .label { display: block; font-size: 0.75rem; color: var(--p-text-muted-color); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.125rem; }
      .value { font-size: 0.9rem; }
    }
  `,
})
export class PatientDetailComponent implements OnInit {
  private patientApi = inject(PatientApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tenantContext = inject(TenantContextService);

  asNonGolden = input(false, {transform: (v: string | boolean) => v === true || v === 'true'});

  patient = signal<IisPatient | null>(null);
  vaccinations = signal<VaccinationMaster[]>([]);
  observations = signal<ObservationReported[]>([]);
  relatedPatients = signal<IisPatient[]>([]);
  mdmLinks = signal<MdmLink[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    const patientId = this.route.snapshot.paramMap.get('patientId')!;
    this.loadPatient(patientId);
  }

  private loadPatient(patientId: string): void {
    const isGolden = !this.asNonGolden();
    this.loading.set(true);
    this.patientApi.getPatient(patientId).subscribe({
      next: (patient) => {
        this.patient.set(patient);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.patientApi.getPatientVaccinations(patientId, isGolden).subscribe((v) => this.vaccinations.set(v));
    this.patientApi.getPatientObservations(patientId, isGolden).subscribe((o) => this.observations.set(o));
    this.patientApi.getRelatedPatients(patientId, isGolden).subscribe((r) => this.relatedPatients.set(r));
    if (isGolden) {
      this.patientApi.getMdmLinks(patientId).subscribe((links) => this.mdmLinks.set(links));
    }
  }

  viewVaccination(vaccinationId: string): void {
    this.router.navigate(['/t', this.tenantContext.tenantName(), 'vaccinations', vaccinationId]);
  }

  viewRelatedPatient(patient: IisPatient): void {
    this.router.navigate(
      ['/t', this.tenantContext.tenantName(), 'patients', patient.patientId],
      {queryParams: {asNonGolden: !this.asNonGolden()}},
    );
  }

  goBack(): void {
    this.router.navigate(['/t', this.tenantContext.tenantName(), 'patients']);
  }
}
