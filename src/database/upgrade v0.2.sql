CREATE TABLE message_received (
  message_received_id   INT        NOT NULL AUTO_INCREMENT PRIMARY KEY, 
  org_id                INT        NOT NULL,
  message_request       MEDIUMTEXT NOT NULL,
  message_response      MEDIUMTEXT NOT NULL,
  reported_date         DATETIME   NOT NULL,
  category_request      VARCHAR(250) NOT NULL,
  category_response     VARCHAR(250) NOT NULL,
  patient_reported_id   INT
);

ALTER TABLE patient_master ADD COLUMN org_id INT NOT NULL;