CREATE TABLE org_location
(
  org_location_id         INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
  org_facility_code       VARCHAR(250)   NOT NULL,
  org_id                  INT            NOT NULL,
  org_facility_name       VARCHAR(250),
  location_type           VARCHAR(250),
  address_line1           VARCHAR(250),
  address_line2           VARCHAR(250),
  address_city            VARCHAR(250),
  address_state           VARCHAR(250),
  address_zip             VARCHAR(250),
  address_country         VARCHAR(250),
  address_county_parish   VARCHAR(250), 
  vfc_provider_pin        VARCHAR(250)
);

ALTER TABLE vaccination_reported ADD COLUMN org_location_id INT;

ALTER TABLE patient_reported ADD COLUMN patient_race2 VARCHAR(250);
ALTER TABLE patient_reported ADD COLUMN patient_race3 VARCHAR(250);
ALTER TABLE patient_reported ADD COLUMN patient_race4 VARCHAR(250);
ALTER TABLE patient_reported ADD COLUMN patient_race5 VARCHAR(250);
ALTER TABLE patient_reported ADD COLUMN patient_race6 VARCHAR(250);

ALTER TABLE vaccination_reported ADD COLUMN entered_by INT;
ALTER TABLE vaccination_reported ADD COLUMN ordering_provider INT;
ALTER TABLE vaccination_reported ADD COLUMN administering_provider INT;


CREATE TABLE person
(
  person_id               INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
  person_external_link    VARCHAR(250)   NOT NULL,
  org_id                  INT            NOT NULL,
  name_last               VARCHAR(250),
  name_first              VARCHAR(250),
  name_middle             VARCHAR(250),
  assigning_authority     VARCHAR(250),
  name_type_code          VARCHAR(250),
  identifier_type_code    VARCHAR(250),
  professional_suffix     VARCHAR(250)
);
