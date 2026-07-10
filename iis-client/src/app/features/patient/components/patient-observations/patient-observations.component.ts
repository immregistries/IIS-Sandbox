import {Component, input} from '@angular/core';
import {TableModule} from 'primeng/table';
import {ObservationReported} from '../../models/observation.model';
import {DateFormatPipe} from '../../../../shared/pipes/date-format.pipe';

@Component({
  selector: 'app-patient-observations',
  standalone: true,
  imports: [TableModule, DateFormatPipe],
  template: `
    <p-table [value]="observations()" styleClass="p-datatable-sm" [paginator]="true" [rows]="10">
      <ng-template #header>
        <tr>
          <th>Identifier Code</th>
          <th>Value Code</th>
          <th>Value Type</th>
          <th>Date</th>
        </tr>
      </ng-template>
      <ng-template #body let-obs>
        <tr>
          <td>{{ obs.identifierCode }}</td>
          <td>{{ obs.valueCode }}</td>
          <td>{{ obs.valueType }}</td>
          <td>{{ obs.observationDate | iisDate }}</td>
        </tr>
      </ng-template>
      <ng-template #emptymessage>
        <tr><td colspan="4" style="text-align: center; color: #64748b;">No observations.</td></tr>
      </ng-template>
    </p-table>
  `,
})
export class PatientObservationsComponent {
  observations = input.required<ObservationReported[]>();
}
