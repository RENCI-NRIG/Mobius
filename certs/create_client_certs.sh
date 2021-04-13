#!/bin/bash

set -o nounset \
    -o errexit \
    -o verbose \
    -o xtrace


# Kafkacat
openssl genrsa -des3 -passout "pass:exogeni" -out kafkacat.client.key 2048
openssl req -passin "pass:exogeni" -passout "pass:exogeni" -key kafkacat.client.key -new -out kafkacat.client.req -subj '/CN=kafkacat.test.exogeni.io/OU=RENCI/O=UNC/L=ChapelHill/S=NC/C=US'
openssl x509 -req -CA ca-cert -CAkey ca-key -in kafkacat.client.req -out kafkacat-ca1-signed.pem -days 9999 -CAcreateserial -passin "pass:exogeni"

