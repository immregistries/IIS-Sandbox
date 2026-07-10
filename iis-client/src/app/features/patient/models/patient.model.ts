export interface ModelName {
  nameType: string;
  nameLast: string;
  nameFirst: string;
  nameMiddle: string;
  namePrefix: string;
  nameSuffix: string;
}

export interface ModelAddress {
  addressLine1: string;
  addressLine2: string;
  addressCity: string;
  addressState: string;
  addressZip: string;
  addressCountry: string;
  addressCountyParish: string;
  addressType: string;
}

export interface ModelPhone {
  phoneNumber: string;
  phoneType: string;
}

export interface BusinessIdentifier {
  system: string;
  value: string;
  type: string;
}

export interface PatientGuardian {
  guardianRelationship: string;
  guardianName: ModelName;
}

export interface IisPatient {
  patientId: string;
  businessIdentifiers: BusinessIdentifier[];
  reportedDate: string | null;
  updatedDate: string | null;
  patientNames: ModelName[];
  motherMaidenName: string;
  birthDate: string | null;
  sex: string;
  races: string[];
  addresses: ModelAddress[];
  phones: ModelPhone[];
  email: string;
  ethnicity: string;
  birthFlag: string;
  birthOrder: string;
  deathFlag: string;
  deathDate: string | null;
  publicityIndicator: string;
  publicityIndicatorDate: string | null;
  protectionIndicator: string;
  protectionIndicatorDate: string | null;
  registryStatusIndicator: string;
  registryStatusIndicatorDate: string | null;
  patientGuardians: PatientGuardian[];
  managingOrganizationId: string;
  generalPractitionerId: string;
}

export interface PatientMaster extends IisPatient {
}
