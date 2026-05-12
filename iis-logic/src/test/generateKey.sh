#ssh-keygen -t rsa -b 4096 -E SHA384 -m PEM -P "" -f RS384.key;
# ssh-keygen -t rsa -b 4096 -E SHA384 -m PKCS8 -P "" -f RS384.key;
# openssl rsa -in RS384.key -pubout -outform PKCS8 -out RS384.key.pub;
openssl ecparam -name prime256v1 -genkey -noout -out ES256.key;
openssl ec -in ES256.key -pubout -out ES256.key.pub;