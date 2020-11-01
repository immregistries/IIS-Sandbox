CREATE DATABASE iis;


CREATE USER
  'iis_web'@'localhost' IDENTIFIED WITH mysql_native_password
                                   BY 'SharkBaitHooHaHa';

GRANT ALL ON iis.* TO 'iis_web'@'localhost';


-- MySQL dump 10.13  Distrib 8.0.13, for Win64 (x86_64)
--
-- Host: localhost    Database: iis
-- ------------------------------------------------------
-- Server version	8.0.13

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8mb4 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `message_received`
--

DROP TABLE IF EXISTS `message_received`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `message_received` (
  `message_received_id` int(11) NOT NULL AUTO_INCREMENT,
  `org_id` int(11) NOT NULL,
  `message_request` mediumtext NOT NULL,
  `message_response` mediumtext NOT NULL,
  `reported_date` datetime NOT NULL,
  `category_request` varchar(250) NOT NULL,
  `category_response` varchar(250) NOT NULL,
  `patient_reported_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`message_received_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `message_received`
--

LOCK TABLES `message_received` WRITE;
/*!40000 ALTER TABLE `message_received` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_received` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `observation_master`
--

DROP TABLE IF EXISTS `observation_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `observation_master` (
  `observation_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_id` int(11) NOT NULL,
  `vaccination_id` int(11) DEFAULT NULL,
  `identifier_code` varchar(250) NOT NULL,
  `value_code` varchar(250) NOT NULL,
  `observation_reported_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`observation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `observation_master`
--

LOCK TABLES `observation_master` WRITE;
/*!40000 ALTER TABLE `observation_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `observation_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `observation_reported`
--

DROP TABLE IF EXISTS `observation_reported`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `observation_reported` (
  `observation_reported_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_reported_id` int(11) NOT NULL,
  `vaccination_reported_id` int(11) DEFAULT NULL,
  `observation_id` int(11) NOT NULL,
  `reported_date` datetime NOT NULL,
  `updated_date` datetime NOT NULL,
  `value_type` varchar(250) DEFAULT NULL,
  `identifier_code` varchar(250) NOT NULL,
  `identifier_label` varchar(250) DEFAULT NULL,
  `identifier_table` varchar(250) DEFAULT NULL,
  `value_code` varchar(250) NOT NULL,
  `value_label` varchar(250) DEFAULT NULL,
  `value_table` varchar(250) DEFAULT NULL,
  `units_code` varchar(250) DEFAULT NULL,
  `units_label` varchar(250) DEFAULT NULL,
  `units_table` varchar(250) DEFAULT NULL,
  `result_status` varchar(250) DEFAULT NULL,
  `observation_date` date DEFAULT NULL,
  `method_code` varchar(250) DEFAULT NULL,
  `method_label` varchar(250) DEFAULT NULL,
  `method_table` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`observation_reported_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `observation_reported`
--

LOCK TABLES `observation_reported` WRITE;
/*!40000 ALTER TABLE `observation_reported` DISABLE KEYS */;
/*!40000 ALTER TABLE `observation_reported` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_access`
--

DROP TABLE IF EXISTS `org_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `org_access` (
  `org_access_id` int(11) NOT NULL AUTO_INCREMENT,
  `org_id` int(11) NOT NULL,
  `access_name` varchar(30) NOT NULL,
  `access_key` varchar(30) NOT NULL,
  PRIMARY KEY (`org_access_id`),
  KEY `org_id` (`org_id`),
  CONSTRAINT `org_access_ibfk_1` FOREIGN KEY (`org_id`) REFERENCES `org_master` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_access`
--

LOCK TABLES `org_access` WRITE;
/*!40000 ALTER TABLE `org_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `org_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_master`
--

DROP TABLE IF EXISTS `org_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `org_master` (
  `org_id` int(11) NOT NULL AUTO_INCREMENT,
  `organization_name` varchar(255) NOT NULL,
  PRIMARY KEY (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_master`
--

LOCK TABLES `org_master` WRITE;
/*!40000 ALTER TABLE `org_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `org_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_master`
--

DROP TABLE IF EXISTS `patient_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `patient_master` (
  `patient_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_external_link` varchar(30) NOT NULL,
  `patient_name_last` varchar(30) NOT NULL,
  `patient_name_first` varchar(30) NOT NULL,
  `patient_name_middle` varchar(30) DEFAULT NULL,
  `patient_birth_date` date NOT NULL,
  `patient_phone_frag` varchar(30) DEFAULT NULL,
  `patient_address_frag` varchar(30) DEFAULT NULL,
  `patient_soundex_last` varchar(30) NOT NULL,
  `patient_soundex_first` varchar(30) NOT NULL,
  `org_id` int(11) NOT NULL,
  PRIMARY KEY (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_master`
--

LOCK TABLES `patient_master` WRITE;
/*!40000 ALTER TABLE `patient_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_match`
--

DROP TABLE IF EXISTS `patient_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `patient_match` (
  `match_id` int(11) NOT NULL AUTO_INCREMENT,
  `reported_patient_a_id` int(11) NOT NULL,
  `reported_patient_b_id` int(11) NOT NULL,
  `match_status` varchar(1) NOT NULL,
  PRIMARY KEY (`match_id`),
  KEY `reported_patient_a_id` (`reported_patient_a_id`),
  KEY `reported_patient_b_id` (`reported_patient_b_id`),
  CONSTRAINT `patient_match_ibfk_1` FOREIGN KEY (`reported_patient_a_id`) REFERENCES `patient_master` (`patient_id`),
  CONSTRAINT `patient_match_ibfk_2` FOREIGN KEY (`reported_patient_b_id`) REFERENCES `patient_master` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_match`
--

LOCK TABLES `patient_match` WRITE;
/*!40000 ALTER TABLE `patient_match` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_match` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patient_reported`
--

DROP TABLE IF EXISTS `patient_reported`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `patient_reported` (
  `patient_reported_id` int(11) NOT NULL AUTO_INCREMENT,
  `org_reported_id` int(11) NOT NULL,
  `patient_reported_external_link` varchar(60) NOT NULL,
  `patient_id` int(11) NOT NULL,
  `reported_date` datetime NOT NULL,
  `updated_date` datetime NOT NULL,
  `patient_reported_authority` varchar(250) DEFAULT NULL,
  `patient_reported_type` varchar(250) DEFAULT NULL,
  `patient_name_last` varchar(250) DEFAULT NULL,
  `patient_name_first` varchar(250) DEFAULT NULL,
  `patient_name_middle` varchar(250) DEFAULT NULL,
  `patient_mother_maiden` varchar(250) DEFAULT NULL,
  `patient_birth_date` date NOT NULL,
  `patient_sex` varchar(250) DEFAULT NULL,
  `patient_race` varchar(250) DEFAULT NULL,
  `patient_address_line1` varchar(250) DEFAULT NULL,
  `patient_address_line2` varchar(250) DEFAULT NULL,
  `patient_address_city` varchar(250) DEFAULT NULL,
  `patient_address_state` varchar(250) DEFAULT NULL,
  `patient_address_zip` varchar(250) DEFAULT NULL,
  `patient_address_country` varchar(250) DEFAULT NULL,
  `patient_address_county_parish` varchar(250) DEFAULT NULL,
  `patient_phone` varchar(250) DEFAULT NULL,
  `patient_email` varchar(250) DEFAULT NULL,
  `patient_ethnicity` varchar(250) DEFAULT NULL,
  `patient_birth_flag` varchar(1) DEFAULT NULL,
  `patient_birth_order` varchar(250) DEFAULT NULL,
  `patient_death_flag` varchar(1) DEFAULT NULL,
  `patient_death_date` date DEFAULT NULL,
  `publicity_indicator` varchar(250) DEFAULT NULL,
  `publicity_indicator_date` date DEFAULT NULL,
  `protection_indicator` varchar(250) DEFAULT NULL,
  `protection_indicator_date` date DEFAULT NULL,
  `registry_status_indicator` varchar(250) DEFAULT NULL,
  `registry_status_indicator_date` date DEFAULT NULL,
  `guardian_last` varchar(250) DEFAULT NULL,
  `guardian_first` varchar(250) DEFAULT NULL,
  `guardian_middle` varchar(250) DEFAULT NULL,
  `guardian_relationship` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`patient_reported_id`),
  KEY `patient_id` (`patient_id`),
  KEY `org_reported_id` (`org_reported_id`),
  CONSTRAINT `patient_reported_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patient_master` (`patient_id`),
  CONSTRAINT `patient_reported_ibfk_2` FOREIGN KEY (`org_reported_id`) REFERENCES `org_master` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patient_reported`
--

LOCK TABLES `patient_reported` WRITE;
/*!40000 ALTER TABLE `patient_reported` DISABLE KEYS */;
/*!40000 ALTER TABLE `patient_reported` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vaccination_master`
--

DROP TABLE IF EXISTS `vaccination_master`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `vaccination_master` (
  `vaccination_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_id` int(11) NOT NULL,
  `administered_date` date NOT NULL,
  `vaccine_cvx_code` varchar(80) NOT NULL,
  `vaccination_reported_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`vaccination_id`),
  KEY `vaccination_reported_id` (`vaccination_reported_id`),
  CONSTRAINT `vaccination_master_ibfk_1` FOREIGN KEY (`vaccination_reported_id`) REFERENCES `vaccination_reported` (`vaccination_reported_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vaccination_master`
--

LOCK TABLES `vaccination_master` WRITE;
/*!40000 ALTER TABLE `vaccination_master` DISABLE KEYS */;
/*!40000 ALTER TABLE `vaccination_master` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vaccination_reported`
--

DROP TABLE IF EXISTS `vaccination_reported`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `vaccination_reported` (
  `vaccination_reported_id` int(11) NOT NULL AUTO_INCREMENT,
  `patient_reported_id` int(11) NOT NULL,
  `vaccination_reported_external_link` varchar(60) NOT NULL,
  `vaccination_id` int(11) NOT NULL,
  `reported_date` datetime NOT NULL,
  `updated_date` datetime NOT NULL,
  `administered_date` date NOT NULL,
  `vaccine_cvx_code` varchar(250) NOT NULL,
  `vaccine_ndc_code` varchar(250) DEFAULT NULL,
  `vaccine_mvx_code` varchar(250) DEFAULT NULL,
  `administered_amount` varchar(250) DEFAULT NULL,
  `information_source` varchar(250) DEFAULT NULL,
  `lot_number` varchar(250) DEFAULT NULL,
  `expiration_date` date DEFAULT NULL,
  `completion_status` varchar(250) DEFAULT NULL,
  `action_code` varchar(250) DEFAULT NULL,
  `refusal_reason_code` varchar(250) DEFAULT NULL,
  `body_site` varchar(250) DEFAULT NULL,
  `body_route` varchar(250) DEFAULT NULL,
  `funding_source` varchar(250) DEFAULT NULL,
  `funding_eligibility` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`vaccination_reported_id`),
  KEY `patient_reported_id` (`patient_reported_id`),
  KEY `vaccination_id` (`vaccination_id`),
  CONSTRAINT `vaccination_reported_ibfk_1` FOREIGN KEY (`patient_reported_id`) REFERENCES `patient_reported` (`patient_reported_id`),
  CONSTRAINT `vaccination_reported_ibfk_2` FOREIGN KEY (`vaccination_id`) REFERENCES `vaccination_master` (`vaccination_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vaccination_reported`
--

LOCK TABLES `vaccination_reported` WRITE;
/*!40000 ALTER TABLE `vaccination_reported` DISABLE KEYS */;
/*!40000 ALTER TABLE `vaccination_reported` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-10-01  6:47:02