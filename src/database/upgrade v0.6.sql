USE iis;
ALTER TABLE `iis`.`org_access` 
CHANGE COLUMN `access_key` `access_key` VARCHAR(250) NOT NULL ;

CREATE TABLE `subscription_store` (
  `identifier` varchar(45) NOT NULL,
  `name` varchar(45) DEFAULT NULL,
  `status` varchar(45) NOT NULL,
  `topic` varchar(90) NOT NULL,
  `end` datetime DEFAULT NULL,
  `reason` varchar(90) DEFAULT NULL,
  `channelType` varchar(45) DEFAULT NULL,
  `header` varchar(45) DEFAULT NULL,
  `heartbeatPeriod` int DEFAULT NULL,
  `timeout` int DEFAULT NULL,
  `contentType` varchar(45) DEFAULT NULL,
  `content` varchar(45) DEFAULT NULL,
  `notificationUrlLocation` varchar(45) DEFAULT NULL,
  `maxCount` int DEFAULT NULL,
  PRIMARY KEY (`identifier`)
)
