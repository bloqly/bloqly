#!/usr/bin/env bash

set -x

#root
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"1B7B7B3967A819DF91C82F70D99F64143D0F1ACCEF1A4D19825DD09326F432BB","password":"root password"}' \
  http://localhost:9901/api/v1/admin/accounts

#validator1
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"77D030FC19D72CDCF25CD869AB05487C6772F1A75FDA314C73564F835BCA5CBF","password":"validator1 password"}' \
  http://localhost:9901/api/v1/admin/accounts

#validator2
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"5BE738EC7986195E854C0F5D890A5377950EC1DA75B92DBFBD56CBFB765FF80D","password":"validator2 password"}' \
  http://localhost:9902/api/v1/admin/accounts

#validator3
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"D17EE6A9EBD5F34DF2FD50D22D89AB5EDDC80A350440E4F40F88D71725B36BA6","password":"validator3 password"}' \
  http://localhost:9903/api/v1/admin/accounts

#validator4
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"D7A50269CE3839A2D1AD47E5BE388CEEF1254EFAD613B17CD2224D0D213043DD","password":"validator4 password"}' \
  http://localhost:9904/api/v1/admin/accounts

# init blockchain
curl -X POST \
  -H "Content-Type: application/json" \
  -d "{\"space\":\"main\", \"path\": \"$(pwd)/src/test/resources/blocks/currency-js\", \"passphrase\":\"root password\"}" \
  http://localhost:9901/api/v1/admin/blockchain

# export genesis block
GENESIS=$(curl -X GET http://localhost:9901/api/v1/admin/genesis/main)

# import genesis
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9902/api/v1/admin/genesis
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9903/api/v1/admin/genesis
curl -X POST -H "Content-Type: application/json" -d "$GENESIS" http://localhost:9904/api/v1/admin/genesis
