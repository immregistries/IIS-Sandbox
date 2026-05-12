CREATE USER
   'iis_web' IDENTIFIED WITH mysql_native_password
                                    BY 'SharkBaitHooHaHa';
GRANT ALL ON iis_alongside_jpa.* TO 'iis_web';