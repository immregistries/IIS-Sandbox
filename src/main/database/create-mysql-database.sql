DROP DATABASE iis_alongside_jpa;
CREATE DATABASE iis_alongside_jpa;

USE iis_alongside_jpa;


GRANT ALL ON iis_alongside_jpa.* TO 'iis_web'@'localhost';

DROP TABLE IF EXISTS `message_received`;
DROP TABLE IF EXISTS `org_access`;
DROP TABLE IF EXISTS `org_master`;

CREATE TABLE `message_received` (
  `message_received_id` int NOT NULL AUTO_INCREMENT,
  `org_id` int NOT NULL,
  `message_request` mediumtext NOT NULL,
  `message_response` mediumtext NOT NULL,
  `reported_date` datetime NOT NULL,
  `category_request` varchar(250) NOT NULL,
  `category_response` varchar(250) NOT NULL,
  `patient_reported_id` int DEFAULT NULL,
  PRIMARY KEY (`message_received_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `org_master` (
  `org_id` int NOT NULL AUTO_INCREMENT,
  `organization_name` varchar(255) NOT NULL,
  PRIMARY KEY (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `org_access` (
  `org_access_id` int NOT NULL AUTO_INCREMENT,
  `org_id` int NOT NULL,
  `access_name` varchar(30) NOT NULL,
  `access_key` varchar(250) NOT NULL,
  PRIMARY KEY (`org_access_id`),
  KEY `org_id` (`org_id`),
  CONSTRAINT `org_access_ibfk_1` FOREIGN KEY (`org_id`) REFERENCES `Org_Master` (`org_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


insert into org_master (org_id, organization_name) values (1, 'Mercy-Healthcare');
insert into org_master (org_id, organization_name) values (2, 'Family-Physician');
insert into org_master (org_id, organization_name) values (3, 'DEFAULT');
insert into org_master (org_id, organization_name) values (4, 'Connectathon');

insert into org_access (org_id, access_name, access_key) values (1, 'Mercy', 'password1234');
insert into org_access (org_id, access_name, access_key) values (2, 'Bob', '1234password1234');
insert into org_access (org_id, access_name, access_key) values (3, 'DEFAULT', 'BabySharkJaws');
insert into org_access (org_id, access_name, access_key) values (4, 'Connectathon', 'SundaysR0ck!');