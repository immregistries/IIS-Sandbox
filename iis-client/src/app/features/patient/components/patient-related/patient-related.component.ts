import {Component, input, output} from '@angular/core';
import {TableModule} from 'primeng/table';
import {Button} from 'primeng/button';
import {IisPatient} from '../../models/patient.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-related',
  standalone: true,
  imports: [TableModule, Button, DateFormatPipe],
  template: `
    <p-table [value]="patients()" styleClass="p-datatable-sm">
      <ng-template #header>
        <tr>
          <th>ID</th>
          <th>Last Name</th>
          <th>First Name</th>
          <th>DOB</th>
          <th style="width: 80px"></th>
        </tr>
      </ng-template>
      <ng-template #body let-patient>
        <tr>
          <td>{{ patient.patientId }}</td>
          <td>{{ patient.patientNames?.[0]?.nameLast }}</td>
          <td>{{ patient.patientNames?.[0]?.nameFirst }}</td>
          <td>{{ patient.birthDate | iisDate }}</td>
          <td>
            <p-button icon="pi pi-eye" [rounded]="true" [text]="true" size="small" (onClick)="selected.emit(patient)" />
          </td>
        </tr>
      </ng-template>
      <ng-template #emptymessage>
        <tr><td colspan="5" style="text-align: center; color: #64748b;">No related patients.</td></tr>
      </ng-template>
    </p-table>
  `,
})
export class PatientRelatedComponent {
  patients = input.required<IisPatient[]>();
  selected = output<IisPatient>();
}
