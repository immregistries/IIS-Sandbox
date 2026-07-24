// ==========================================
// Wire Format Interfaces (Jackson / JSON)
// Matches exact output of the Java REST Controller
// ==========================================

/** Nested class representing the "nam" structure. */
export interface ClvrNameWire {
  /** Mapped from @JsonProperty("fnt") */
  fnt?: string;

  /** Mapped from @JsonProperty("gnt") */
  gnt?: string;
}

/** Nested class representing the vaccination record within the "v" array. */
export interface ClvrVaccinationRecordWire {
  /** Mapped from @JsonProperty("reg") */
  reg?: string;

  /** Mapped from @JsonProperty("rep") */
  rep?: number;

  /** Mapped from @JsonProperty("i") */
  i?: number;

  /** Mapped from @JsonProperty("a") */
  a?: number;

  /** Mapped from @JsonProperty("mp") */
  mp?: number;
}

/** Represents the EvC payload structure */
export interface ClvrPayloadWire {
  /** Mapped from @JsonProperty("ver") */
  ver?: string;

  /** Mapped from @JsonProperty("nam") */
  nam?: ClvrNameWire;

  /** Mapped from @JsonGetter("dob") */
  dob?: string;

  /** Mapped from @JsonProperty("v") */
  v?: ClvrVaccinationRecordWire[];
}

/** The top level CLVR Token structure */
export interface ClvrTokenWire {
  /** Issuer (ISSUER_KEY: "1") */
  '1'?: string;

  /** UNIX Expiration time (EXPIRATION_TIME_KEY: "4") */
  '4'?: number;

  /** Unix Issued time (ISSUED_TIME_KEY: "6") */
  '6'?: number;

  /** Payload (PAYLOAD_KEY: "-260") */
  '-260'?: ClvrPayloadWire;
}

// ==========================================
// Application Format Interfaces (CamelCase)
// Standard frontend data models
// ==========================================

export interface ClvrName {
  familyName?: string;
  givenName?: string;
}

export interface ClvrVaccinationRecord {
  registryCode?: string;
  repositoryIndex?: number;
  reference?: number;
  ageInDays?: number;
  nuvaCode?: number;
}

export interface ClvrPayload {
  version?: string;
  clvrName?: ClvrName;
  dateOfBirth?: string;
  clvrVaccinationRecords?: ClvrVaccinationRecord[];
}

export interface ClvrToken {
  issuer?: string;
  expirationTime?: number;
  issuedTime?: number;
  clvrPayload?: ClvrPayload;
}
