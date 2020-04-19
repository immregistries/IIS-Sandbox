CREATE TABLE observation_master (
  observation_id          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_id              INT          NOT NULL,
  vaccination_id          INT,
  identifier_code         VARCHAR(250) NOT NULL,
  value_code              VARCHAR(250) NOT NULL,
  observation_reported_id INT
);

CREATE TABLE observation_reported (
  observation_reported_id   INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
  patient_reported_id       INT          NOT NULL,
  vaccination_reported_id   INT,
  observation_id            INT          NOT NULL,
  reported_date             DATETIME     NOT NULL,
  updated_date              DATETIME     NOT NULL,
  value_type                VARCHAR(250),
  identifier_code           VARCHAR(250) NOT NULL,
  identifier_label          VARCHAR(250),
  identifier_table          VARCHAR(250),
  value_code                VARCHAR(250) NOT NULL,
  value_label               VARCHAR(250),
  value_table               VARCHAR(250),
  units_code                VARCHAR(250),
  units_label               VARCHAR(250),
  units_table               VARCHAR(250),
  result_status             VARCHAR(250),
  observation_date          DATE,
  method_code               VARCHAR(250),
  method_label              VARCHAR(250),
  method_table              VARCHAR(250)
);