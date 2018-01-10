CREATE TABLE Org_Master          (org_id                    int            NOT NULL       AUTO_INCREMENT   PRIMARY KEY, 
                                  organization_name         varchar(255)   NOT NULL );

CREATE TABLE Org_Access          (org_access_id            int            NOT NULL       AUTO_Increment   PRIMARY KEY, 
                                  org_id                   int            NOT NULL,           
                                  access_name              varchar(30)    NOT NULL,           
                                  access_key               varchar(30)    NOT NULL,
                     FOREIGN KEY (org_id)                  REFERENCES     Org_Master(org_id) );

CREATE TABLE Patient_Master      (patient_id               int            NOT NULL       AUTO_Increment   PRIMARY KEY,  
                                  patient_registry_id      varchar(30)    NOT NULL,
                                  patient_name_last        varchar(30)    NOT NULL,
                                  patient_name_first       varchar(30)    NOT NULL,
                                  patient_name_middle      varchar(30)            ,
                                  patient_birth_date       date           NOT NULL,
                                  patient_phone_frag       varchar(30)            ,
                                  patient_address_frag     varchar(30)            ,
                                  patient_soundex_last     varchar(30)    NOT NULL,
                                  patient_soundex_first    varchar(30)    NOT NULL);

CREATE TABLE Patient_Reported    (reported_patient_id      int            NOT NULL       AUTO_Increment   PRIMARY KEY, 
                                  reported_org_id          int            NOT NULL,           
                                  reported_mrn             varchar(60)    NOT NULL,           
                                  patient_id               int            NOT NULL,
                                  patient_data             varchar(21000) NOT NULL,
                                  reported_date            datetime       NOT NULL,
                                  updated_date             datetime       NOT NULL,
                     FOREIGN KEY (patient_id)              REFERENCES     Patient_Master(patient_id),
                     FOREIGN KEY (reported_org_id)         REFERENCES     Org_Master(org_id) );
                                
CREATE TABLE Patient_Match       (match_id                 int            NOT NULL       AUTO_Increment   PRIMARY KEY,
                                  reported_patient_a_id    int            NOT NULL,
                                  reported_patient_b_id    int            NOT NULL,
                                  match_status             varchar(1)     NOT NULL,
                     FOREIGN KEY (reported_patient_a_id)   REFERENCES     Patient_Master(patient_id),
                     FOREIGN KEY (reported_patient_b_id)   REFERENCES     Patient_Master(patient_id) );
 
CREATE TABLE Vaccination_Master  (vaccination_id           int            NOT NULL       AUTO_Increment   PRIMARY KEY,
                                  patient_id               int            NOT NULL,
                                  administered_date        date           NOT NULL,
                                  vaccine_cvx_code         varchar(80)    NOT NULL);

CREATE TABLE Vaccination_Reported (reported_vaccination_id int            NOT NULL       AUTO_Increment   PRIMARY KEY, 
                                  reported_patient_id      int            NOT NULL,           
                                  reported_order_id        varchar(60)    NOT NULL,           
                                  vaccination_id           int            NOT NULL,
                                  vaccination_data         varchar(20000) NOT NULL,
                                  reported_date            datetime       NOT NULL,
                                  updated_date             datetime       NOT NULL,
                     FOREIGN KEY (reported_patient_id)     REFERENCES     Patient_Reported(reported_patient_id), 
                     FOREIGN KEY (vaccination_id)          REFERENCES     Vaccination_Master(vaccination_id) );
