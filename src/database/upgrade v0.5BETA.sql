CREATE TABLE patient_link
(
  id integer primary key auto_increment,
  patient_master_ID varchar(60),
  patient_reported_ID varchar(60),
  level_confidence integer
);
