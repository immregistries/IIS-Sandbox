export interface ObservationReported {
  observationReportedId: string;
  patientReportedId: string;
  valueType: string;
  identifierCode: string;
  valueCode: string;
  observationDate: string | null;
  observationReportedExternalId: string;
}
