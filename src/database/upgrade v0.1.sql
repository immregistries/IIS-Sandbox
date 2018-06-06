DROP DATABASE iis;

CREATE DATABASE iis;

use iis;

CREATE TABLE org_master  (
  org_id                    int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY, 
  organization_name         varchar(255)   NOT NULL 
);

CREATE TABLE org_access          (
  org_access_id            int            NOT NULL AUTO_INCREMENT PRIMARY KEY, 
  org_id                   int            NOT NULL,           
  access_name              varchar(30)    NOT NULL,           
  access_key               varchar(30)    NOT NULL,
  FOREIGN KEY (org_id)     REFERENCES     Org_Master(org_id) 
);

CREATE TABLE patient_master      (
  patient_id               int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY,  
  patient_external_link    varchar(30)    NOT NULL,
  patient_name_last        varchar(30)    NOT NULL,
  patient_name_first       varchar(30)    NOT NULL,
  patient_name_middle      varchar(30)            ,
  patient_birth_date       date           NOT NULL,
  patient_phone_frag       varchar(30)            ,
  patient_address_frag     varchar(30)            ,
  patient_soundex_last     varchar(30)    NOT NULL,
  patient_soundex_first    varchar(30)    NOT NULL
);

CREATE TABLE patient_reported    (
  patient_reported_id      int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY, 
  org_reported_id                 int            NOT NULL,           
  patient_reported_external_link  varchar(60)    NOT NULL,           
  patient_id                      int            NOT NULL,
  reported_date                   datetime       NOT NULL,
  updated_date                    datetime       NOT NULL,
  patient_reported_authority      varchar(250),
  patient_reported_type           varchar(250),
  patient_name_last               varchar(250),
  patient_name_first              varchar(250),
  patient_name_middle             varchar(250),
  patient_mother_maiden           varchar(250),
  patient_birth_date              date           NOT NULL,      
  patient_sex                     varchar(250),
  patient_race                    varchar(250),
  patient_address_line1           varchar(250),
  patient_address_line2           varchar(250),
  patient_address_city            varchar(250),
  patient_address_state           varchar(250),
  patient_address_zip             varchar(250),
  patient_address_country         varchar(250),
  patient_address_county_parish   varchar(250),
  patient_phone                   varchar(250),
  patient_email                   varchar(250),
  patient_ethnicity               varchar(250),
  patient_birth_flag              varchar(1),
  patient_birth_order             varchar(250),
  patient_death_flag              varchar(1),
  patient_death_date              date,
  publicity_indicator             varchar(250),
  publicity_indicator_date        date,
  protection_indicator            varchar(250),
  protection_indicator_date       date,
  registry_status_indicator       varchar(250),
  registry_status_indicator_date  date,
  guardian_last                   varchar(250),
  guardian_first                  varchar(250),
  guardian_middle                 varchar(250),
  guardian_relationship           varchar(250),
  FOREIGN KEY (patient_id)        REFERENCES     Patient_Master(patient_id),
  FOREIGN KEY (org_reported_id)   REFERENCES     Org_Master(org_id) 
);
             
CREATE TABLE Patient_Match       (
  match_id                 int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY,
  reported_patient_a_id    int            NOT NULL,
  reported_patient_b_id    int            NOT NULL,
  match_status             varchar(1)     NOT NULL,
  FOREIGN KEY (reported_patient_a_id)     REFERENCES     Patient_Master(patient_id),
  FOREIGN KEY (reported_patient_b_id)     REFERENCES     Patient_Master(patient_id) 
);
 
CREATE TABLE Vaccination_Master  (
  vaccination_id           int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY,
  patient_id               int            NOT NULL,
  administered_date        date           NOT NULL,
  vaccine_cvx_code         varchar(80)    NOT NULL,
  vaccination_reported_id  int            		
);

CREATE TABLE Vaccination_Reported (
  vaccination_reported_id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY, 
  patient_reported_id                 int            NOT NULL,           
  vaccination_reported_external_link  varchar(60)    NOT NULL,           
  vaccination_id                      int            NOT NULL,
  reported_date                       datetime       NOT NULL,
  updated_date                        datetime       NOT NULL,
  administered_date                   date           NOT NULL,
  vaccine_cvx_code                    varchar(250)   NOT NULL,
  vaccine_ndc_code                    varchar(250),
  vaccine_mvx_code                    varchar(250),
  administered_amount                 varchar(250),
  information_source                  varchar(250),
  lot_number                          varchar(250),
  expiration_date                     date,
  completion_status                   varchar(250),
  action_code                         varchar(250),
  refusal_reason_code                 varchar(250),
  body_site                           varchar(250),
  body_route                          varchar(250),
  funding_source                      varchar(250),
  funding_eligibility                 varchar(250),
  FOREIGN KEY (patient_reported_id)   REFERENCES     Patient_Reported(patient_reported_id), 
  FOREIGN KEY (vaccination_id)        REFERENCES     Vaccination_Master(vaccination_id) 
);

ALTER TABLE Vaccination_Master ADD FOREIGN KEY (vaccination_reported_id)  REFERENCES Vaccination_Reported(vaccination_reported_id);

