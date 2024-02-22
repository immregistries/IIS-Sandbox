DROP DATABASE IF EXISTS iis_alongside_jpa;
CREATE DATABASE iis_alongside_jpa;

USE iis_alongside_jpa;


GRANT ALL ON iis_alongside_jpa.* TO 'iis_web';

DROP TABLE IF EXISTS `message_received`;
DROP TABLE IF EXISTS `user_access`;
DROP TABLE IF EXISTS `tenant`;

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


CREATE TABLE `user_access` (
  `user_access_id` int NOT NULL AUTO_INCREMENT,
  `access_name` varchar(30) NOT NULL,
  `access_key` varchar(250) NOT NULL,
  PRIMARY KEY (`user_access_id`),
  KEY `user_access_id` (`user_access_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `tenant` (
  `org_id` int NOT NULL AUTO_INCREMENT,
  `organization_name` varchar(255) NOT NULL,
  `user_access_id` int NOT NULL,
  PRIMARY KEY (`org_id`),
  CONSTRAINT `tenant_ibfk_1` FOREIGN KEY (`user_access_id`) REFERENCES `user_access` (`user_access_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

insert into user_access (user_access_id, access_name, access_key) values (1, 'Mercy', 'password1234');
insert into user_access (user_access_id, access_name, access_key) values (2, 'Bob', '1234password1234');
insert into user_access (user_access_id, access_name, access_key) values (3, 'Connectathon', 'SundaysR0ck!');
insert into user_access (user_access_id, access_name, access_key) values (4, 'admin', '?whooosM0reSorino?');
--insert into user_access (user_access_id, access_name, access_key) values (5, 'DEFAULT', 'BabySharkJaws');

insert into tenant (org_id, organization_name, user_access_id) values (1, 'Mercy-Healthcare', 1);
insert into tenant (org_id, organization_name, user_access_id) values (2, 'Family-Physician', 2);
insert into tenant (org_id, organization_name, user_access_id) values (3, 'Connectathon', 3);
insert into tenant (org_id, organization_name, user_access_id) values (4, 'admin', 4);
--insert into tenant (org_id, organization_name, user_access_id) values (5, 'DEFAULT', 5);

