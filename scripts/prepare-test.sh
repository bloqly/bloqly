#!/usr/bin/env bash

set -x

JSON="Content-Type: application/json"
PUBLIC_KEYS='{"publicKeys":["02146F9679B32142BFDC7326236D1317098E708102293CE4C3DEE9258334CF6564","023F16A9257B0C815B55DECFB298B5311DB2C32D903DF868746F62C54A598F5614","02AC91629114E7636CD2D777CE37EA6389581A5DE60917EB5F59A1CD872CF314B8","03862DAC0CAE714BDED992ED65996C607753BAD27C64606DC7EC9CA921FB41BE6B"]}'

#root
curl -X POST -H "$JSON" \
  -u main_admin:main_password \
  -d '{"privateKeyEncrypted":"93428F80F96F7903679473931D2D772B388F8D7FDCE7833F22728A0317A5ACC1305B93E5B791FD174DA832C564ADE6B0F500522AE5AA3E51D0F27BF3DE947382","publicKey":"0360F47BACA2C9AF1CCD2B12A08B9C169B703142B2D45AD9DB783070CBD8011BE5"}' \
  http://127.0.0.1:9911/api/v1/admin/accounts

#validator1
curl -X POST -H "$JSON" \
  -d '{"privateKeyEncrypted":"43A05C521ED05E8702273F4DC3EF04CC1D7A5E8B646D453CB117B94F8820DDDA17E49A141852DB8DF113BFC0D8FFEED10944193B21EC8B073D492556CBF91F7F","publicKey":"02146F9679B32142BFDC7326236D1317098E708102293CE4C3DEE9258334CF6564"}' \
  -u main_admin:main_password \
  http://127.0.0.1:9911/api/v1/admin/accounts

#validator2
curl -X POST -H "$JSON" \
  -u rhea_admin:rhea_password \
  -d '{"privateKeyEncrypted":"7BC02095EE014655D3EC541D408B7A7690DDC3A224246144EF05CE6DAF697FDE264AA99BD7D682E7948ADFDC024B3F11F9D8F7DC23E07E890CFF386FE677161A","publicKey":"023F16A9257B0C815B55DECFB298B5311DB2C32D903DF868746F62C54A598F5614"}' \
  http://127.0.0.1:9912/api/v1/admin/accounts

#validator3
curl -X POST -H "$JSON" \
  -u loge_admin:loge_password \
  -d '{"privateKeyEncrypted":"E72C1132E6CC1C821189CBF7CCA768828BF63F8CE0E5BCA40E61BD02309A7531FABCB4153B7644383F7461A7A76C778CB8B4D5FDCFBB1BA0155C38B9BE83DF6F","publicKey":"02AC91629114E7636CD2D777CE37EA6389581A5DE60917EB5F59A1CD872CF314B8"}' \
  http://127.0.0.1:9913/api/v1/admin/accounts

#validator4
curl -X POST -H "$JSON" \
  -u ymir_admin:ymir_password \
  -d '{"privateKeyEncrypted":"BA2B68A887CAE2D85318B176A876D8B8C15D757D8D6D2AB9D74EF5161D60FDCA3AA5240C1AE018018E8779D767DE084E936CF774C71207F430954AC5F8A02A19","publicKey":"03862DAC0CAE714BDED992ED65996C607753BAD27C64606DC7EC9CA921FB41BE6B"}' \
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
  -d "{\"space\":\"main\", \"path\": \"$(pwd)/demo/contract/currency\", \"passphrase\":\"root password\"}" \
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
