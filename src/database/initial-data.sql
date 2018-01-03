insert into org_master (org_id, organization_name) values (1, 'Mercy Healthcare');
insert into org_master (org_id, organization_name) values (2, 'Family Physician');

insert into org_access (org_id, access_name, access_key) values (1, 'Mercy', 'password1234');
insert into org_access (org_id, access_name, access_key) values (2, 'Bob', '1234password1234');

insert into patient_master (patient_id, patient_registry_id, patient_name_last, patient_name_first, patient_birth_date, patient_soundex_last, patient_soundex_first) values (1, 1, 'Jones', 'John', '1991-09-01', 'J520', 'J500');
insert into patient_master (patient_id, patient_registry_id, patient_name_last, patient_name_first, patient_birth_date, patient_soundex_last, patient_soundex_first) values (2, 2, 'Dawson', 'Dave', '1981-07-21', 'D250', 'D100');

insert into patient_reported (reported_patient_id, reported_org_id, reported_mrn, patient_id, patient_data, reported_date, updated_date) values (1, 1, 'Blah', 1, 'Data', '2011-09-01', '2014-05-30');
insert into patient_reported (reported_patient_id, reported_org_id, reported_mrn, patient_id, patient_data, reported_date, updated_date) values (2, 2, 'Mrn', 2, 'Data?', '2012-12-05', '2015-03-17');



insert into vaccination_master (vaccination_id, patient_id, administered_date, vaccine_cvx_code) values (1, 1, '2013-02-24', '1234');
insert into vaccination_master (vaccination_id, patient_id, administered_date, vaccine_cvx_code) values (2, 2, '2015-01-11', '1424');

insert into vaccination_reported (reported_vaccination_id, reported_patient_id, reported_order_id, vaccination_id, vaccination_data, reported_date, updated_date) values (1, 1, 1, 1, 'vaccination data', '2012-12-05', '2012-12-05');
insert into vaccination_reported (reported_vaccination_id, reported_patient_id, reported_order_id, vaccination_id, vaccination_data, reported_date, updated_date) values (2, 1, 2, 2, 'other data', '2014-12-05', '2015-12-05');

insert into patient_match (match_id, reported_patient_a_id, reported_patient_b_id, match_status) values (1, 1, 1, 'a');
insert into patient_match (match_id, reported_patient_a_id, reported_patient_b_id, match_status) values (2, 1, 2, 'b');
