import {BusinessIdentifier} from '../../patient/models/patient.model';

export interface IisVaccination {
  vaccinationId: string;
  businessIdentifiers: BusinessIdentifier[];
  patientReportedId: string;
  reportedDate: string | null;
  updatedDate: string | null;
  administeredDate: string | null;
  vaccineCvxCode: string;
  vaccineNdcCode: string;
  vaccineMvxCode: string;
  administeredAmount: string;
  informationSource: string;
  lotnumber: string;
  expirationDate: string | null;
  completionStatus: string;
  actionCode: string;
  refusalReasonCode: string;
  bodySite: string;
  bodyRoute: string;
  fundingSource: string;
  fundingEligibility: string;
  orgLocationId: string;
  enteredById: string;
  orderingProviderId: string;
  administeringProviderId: string;
}

export interface VaccinationMaster extends IisVaccination {
}
