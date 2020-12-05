CREATE TABLE vaccination_link
(
    id integer primary key auto_increment,
    vaccination_master_ID integer,
    vaccination_reported_ID integer,
    level_confidence integer
);
