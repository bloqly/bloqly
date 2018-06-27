#!/usr/bin/env bash

set -x

#root
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"78CA9AE2A589CB7D0E151E153F2CCCC760CBDAA05FE74C802FBAC6B0CDDCA534"}' \
  http://localhost:9901/api/v1/admin/accounts

#validator1
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"7869F85C5F8138512E75C2E8469DDDA0A6A4EFBCD5FE3F068EECD9B0439601C9"}' \
  http://localhost:9901/api/v1/admin/accounts

#validator2
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"00B8898555BE47960016E3B5E37D83520188E442BADF3397E4D7CD07D9D54099F4"}' \
  http://localhost:9902/api/v1/admin/accounts

#validator3
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"3890ACF98A004E166A864F1444F4425003275DBD1BF84DFCA9F55847BF5E84B4"}' \
  http://localhost:9903/api/v1/admin/accounts

#validator4
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"A115E92725C45AD12D054EDBBFEAA071D12D20512602A66CB6B401D3CD459944"}' \
  http://localhost:9904/api/v1/admin/accounts

# init blockchain
curl -X POST \
  -H "Content-Type: application/json" \
  -d "{\"space\":\"main\", \"path\": \"$(pwd)/src/test/resources/blocks/currency-js\"}" \
  http://localhost:9901/api/v1/admin/blockchain

# export genesis block
GENESIS=$(curl -X GET http://localhost:9901/api/v1/admin/genesis/main)

# import genesis
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9902/api/v1/admin/genesis/main
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9903/api/v1/admin/genesis/main
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9904/api/v1/admin/genesis/main
