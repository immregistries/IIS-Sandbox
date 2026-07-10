import {Component, input} from '@angular/core';
import {TableModule} from 'primeng/table';
import {IisVaccination} from '../../models/vaccination.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-vaccination-related',
  standalone: true,
  imports: [TableModule, DateFormatPipe],
  template: `
    <p-table [value]="vaccinations()" styleClass="p-datatable-sm">
      <ng-template #header>
        <tr>
          <th>ID</th>
          <th>CVX Code</th>
          <th>Administered Date</th>
          <th>Lot #</th>
          <th>Status</th>
        </tr>
      </ng-template>
      <ng-template #body let-vax>
        <tr>
          <td>{{ vax.vaccinationId }}</td>
          <td>{{ vax.vaccineCvxCode }}</td>
          <td>{{ vax.administeredDate | iisDate }}</td>
          <td>{{ vax.lotnumber }}</td>
          <td>{{ vax.completionStatus }}</td>
        </tr>
      </ng-template>
      <ng-template #emptymessage>
        <tr><td colspan="5" style="text-align: center; color: #64748b;">No related vaccinations.</td></tr>
      </ng-template>
    </p-table>
  `,
})
export class VaccinationRelatedComponent {
  vaccinations = input.required<IisVaccination[]>();
}
