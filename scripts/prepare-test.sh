#!/usr/bin/env bash

set -x

JSON="Content-Type: application/json"
PUBLIC_KEYS='{"publicKeys":["02146F9679B32142BFDC7326236D1317098E708102293CE4C3DEE9258334CF6564","023F16A9257B0C815B55DECFB298B5311DB2C32D903DF868746F62C54A598F5614","02AC91629114E7636CD2D777CE37EA6389581A5DE60917EB5F59A1CD872CF314B8","03862DAC0CAE714BDED992ED65996C607753BAD27C64606DC7EC9CA921FB41BE6B"]}'

#root
curl -X POST -H "$JSON" \
  -u main_admin:main_password \
  -d '{"privateKey":"1B7B7B3967A819DF91C82F70D99F64143D0F1ACCEF1A4D19825DD09326F432BB","password":"root password"}' \
  http://127.0.0.1:9911/api/v1/admin/accounts

#validator1
curl -X POST -H "$JSON" \
  -d '{"privateKey":"77D030FC19D72CDCF25CD869AB05487C6772F1A75FDA314C73564F835BCA5CBF","password":"validator1 password"}' \
  -u main_admin:main_password \
  http://127.0.0.1:9911/api/v1/admin/accounts

#validator2
curl -X POST -H "$JSON" \
  -u rhea_admin:rhea_password \
  -d '{"privateKey":"5BE738EC7986195E854C0F5D890A5377950EC1DA75B92DBFBD56CBFB765FF80D","password":"validator2 password"}' \
  http://127.0.0.1:9912/api/v1/admin/accounts

#validator3
curl -X POST -H "$JSON" \
  -u loge_admin:loge_password \
  -d '{"privateKey":"D17EE6A9EBD5F34DF2FD50D22D89AB5EDDC80A350440E4F40F88D71725B36BA6","password":"validator3 password"}' \
  http://127.0.0.1:9913/api/v1/admin/accounts

#validator4
curl -X POST -H "$JSON" \
  -u ymir_admin:ymir_password \
  -d '{"privateKey":"D7A50269CE3839A2D1AD47E5BE388CEEF1254EFAD613B17CD2224D0D213043DD","password":"validator4 password"}' \
  http://127.0.0.1:9914/api/v1/admin/accounts

#validators for node 1
curl -X POST -H "$JSON" \
  -u main_admin:main_password \
  -d "${PUBLIC_KEYS}" \
  http://127.0.0.1:9911/api/v1/admin/accounts/publicKeys

#validators for node 2
curl -X POST -H "$JSON" \
  -u rhea_admin:rhea_password \
  -d "${PUBLIC_KEYS}" \
  http://127.0.0.1:9912/api/v1/admin/accounts/publicKeys

#validators for node 3
curl -X POST -H "$JSON" \
  -u loge_admin:loge_password \
  -d "${PUBLIC_KEYS}" \
  http://127.0.0.1:9913/api/v1/admin/accounts/publicKeys

#validators for node 4
curl -X POST -H "$JSON" \
  -u ymir_admin:ymir_password \
  -d "${PUBLIC_KEYS}" \
  http://127.0.0.1:9914/api/v1/admin/accounts/publicKeys

# init blockchain
curl -X POST -H "$JSON" \
  -u main_admin:main_password \
  -d "{\"space\":\"main\", \"path\": \"$(pwd)/src/test/resources/blocks/currency\", \"passphrase\":\"root password\"}" \
  http://127.0.0.1:9911/api/v1/admin/blockchain

# export genesis block
GENESIS=$(curl -u 'main_admin:main_password' -X GET http://localhost:9911/api/v1/admin/genesis/main)

# import genesis
curl -X POST -H "$JSON" \
  -u rhea_admin:rhea_password \
  -d $GENESIS http://127.0.0.1:9912/api/v1/admin/genesis

curl -X POST -H "$JSON" \
  -u loge_admin:loge_password \
  -d $GENESIS http://127.0.0.1:9913/api/v1/admin/genesis

curl -X POST -H "$JSON" \
  -u ymir_admin:ymir_password \
  -d $GENESIS http://127.0.0.1:9914/api/v1/admin/genesis
