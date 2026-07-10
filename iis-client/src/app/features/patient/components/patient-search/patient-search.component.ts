import {Component, inject, signal} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {Button} from 'primeng/button';
import {Card} from 'primeng/card';
import {PatientMaster} from '../../models/patient.model';
import {PatientApiService} from '../../services/patient-api.service';
import {PatientListComponent} from '../patient-list/patient-list.component';
import {LoadingSpinnerComponent} from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-patient-search',
  standalone: true,
  imports: [FormsModule, InputText, Button, Card, PatientListComponent, LoadingSpinnerComponent],
  template: `
    <div class="patient-search">
      <h1>Patients</h1>

      <p-card>
        <div class="search-form">
          <div class="search-field">
            <label for="family">Last Name</label>
            <input pInputText id="family" [(ngModel)]="family" placeholder="Search by last name" (keyup.enter)="onSearch()" />
          </div>
          <div class="search-field">
            <label for="name">First Name</label>
            <input pInputText id="name" [(ngModel)]="firstName" placeholder="Search by first name" (keyup.enter)="onSearch()" />
          </div>
          <div class="search-field">
            <label for="identifier">Identifier</label>
            <input pInputText id="identifier" [(ngModel)]="identifier" placeholder="Medical record #" (keyup.enter)="onSearch()" />
          </div>
          <div class="search-actions">
            <p-button label="Search" icon="pi pi-search" (onClick)="onSearch()" [loading]="loading()" />
            <p-button label="Show All" severity="secondary" [outlined]="true" (onClick)="loadAll()" [loading]="loading()" />
          </div>
        </div>
      </p-card>

      @if (loading()) {
        <app-loading-spinner />
      } @else if (patients().length > 0 || searched()) {
        <app-patient-list [patients]="patients()" (selected)="onSelectPatient($event)" />
      }
    </div>
  `,
  styles: `
    h1 { margin: 0 0 1rem; color: #1e293b; }
    .search-form {
      display: flex;
      gap: 1rem;
      align-items: flex-end;
      flex-wrap: wrap;
    }
    .search-field {
      flex: 1;
      min-width: 160px;
      label { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; }
      input { width: 100%; }
    }
    .search-actions {
      display: flex;
      gap: 0.5rem;
      align-self: flex-end;
    }
  `,
})
export class PatientSearchComponent {
  private patientApi = inject(PatientApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  family = signal('');
  firstName = signal('');
  identifier = signal('');
  patients = signal<PatientMaster[]>([]);
  loading = signal(false);
  searched = signal(false);

  onSearch(): void {
    this.loading.set(true);
    this.searched.set(true);
    this.patientApi
      .searchPatients({
        family: this.family() || undefined,
        name: this.firstName() || undefined,
        identifier: this.identifier() || undefined,
      })
      .subscribe({
        next: (patients) => {
          this.patients.set(patients);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  loadAll(): void {
    this.loading.set(true);
    this.searched.set(true);
    this.patientApi.getPatients().subscribe({
      next: (patients) => {
        this.patients.set(patients);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSelectPatient(patient: PatientMaster): void {
    this.router.navigate([patient.patientId], {relativeTo: this.route});
  }
}
