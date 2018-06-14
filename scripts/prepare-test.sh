#!/usr/bin/env bash

#root
./release/run.sh -database bloqly_main -command "account.import('78CA9AE2A589CB7D0E151E153F2CCCC760CBDAA05FE74C802FBAC6B0CDDCA534')"

#validator1
./release/run.sh -database bloqly_main -command "account.import('7869F85C5F8138512E75C2E8469DDDA0A6A4EFBCD5FE3F068EECD9B0439601C9')"

#validator2
./release/run.sh -database bloqly_second -command "account.import('00B8898555BE47960016E3B5E37D83520188E442BADF3397E4D7CD07D9D54099F4')"

#validator3
./release/run.sh -database bloqly_third -command "account.import('3890ACF98A004E166A864F1444F4425003275DBD1BF84DFCA9F55847BF5E84B4')"

./release/run.sh -database bloqly_main -command "chain.init('main','../src/test/resources/blocks/currency-js')"
./release/run.sh -database bloqly_main -command "block.exportFirst('main')"