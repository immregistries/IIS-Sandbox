CREATE TABLE patient_link
(
  id integer primary key auto_increment,
  patient_master_ID integer,
  patient_reported_ID integer,
  level_confidence integer
);
