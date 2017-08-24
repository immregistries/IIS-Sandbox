CREATE USER 'iis_web'@'localhost' IDENTIFIED BY 'SharkBaitHooHaHa';

GRANT ALL PRIVILEGES ON iis.* TO 'iis_web'@'localhost' WITH GRANT OPTION;
