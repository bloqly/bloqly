
<p align="center">
<img src="resources/logo.svg" width="40%"/>
</p>    
<p align="center">JavaScript Smart Contracts Engine on a pBFT+PoA-inspired blockchain consensus algorithm</p>


---  
<p align="center">
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="Build Status"></a>
</p> 

<p align="center">
This is a work in progress/prototype, API and implementation detaisl can change.
</p>     

[Chat](https://riot.im/app/#/room/#bloqly:matrix.org)  
[Twitter](https://twitter.com/slava_snezhkov)

## Installation

### Install Bloqly

Download and unpack the latest binary distribution for your platform from https://github.com/bloqly/bloqly/releases

Note 1: right now **MacOS** and **Linux** are supported natively. 
For **Windows** please use Linux Bash Shell for Windows (it works great actually).

Note 2:  Bloqly uses Java under the hood, but you don't need to download and install JVM in order to get it working, 
it is already packed up into the distribution archives.

### Install Postgres

For **Linux** I find these instructions of installing Postgres very well written and easy to follow:

https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-16-04
https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-ubuntu-18-04

For **MacOS** just run `brew install postgres`

After Postgres is installed, lets create test databases and users:

```bash
sudo -su postgres

createdb bloqly_main
createdb bloqly_rhea
createdb bloqly_loge
createdb bloqly_ymir

createuser user_main
createuser user_rhea
createuser user_loge
createuser user_ymir

psql -d bloqly_main -c "alter user user_main with password 'password_main';"
psql -d bloqly_rhea -c "alter user user_rhea with password 'password_rhea';"
psql -d bloqly_loge -c "alter user user_loge with password 'password_loge';"
psql -d bloqly_ymir -c "alter user user_ymir with password 'password_ymir';"
```

## Quick start

Bloqly comes with preconfigured demo settings for a simplest blockchain network consisting of 4 nodes running a simplified cryptocurrency.
It is for demo purposes only and not of a production quality yet. Please don't create an ICO on it.

### Start nodes

In order to test if things are working at all you can use provided demo configuration - four nodes with pre-configured settings.

Lets start nodes first. Open new bash windows and run from the Bloqly installation directory:

```bash
BLOQLY_HOME=./demo/home/main ./bq.sh
```

```bash
BLOQLY_HOME=./demo/home/rhea ./bq.sh
```

```bash
BLOQLY_HOME=./demo/home/loge ./bq.sh
```

```bash
BLOQLY_HOME=./demo/home/ymir ./bq.sh
```

`BLOQLY_HOME` is name of a folder which contains configuration of a node and where log files will be created. 

For these wondering, the names of the configurations are the names of the moons of Saturn (well, except "main").

### Create root user

As scary at it sounds for a decentralized blockchain project, root user here is just a source of signature, so that it is possible to
export/import genesis block and check it's validity.

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"privateKey":"1B7B7B3967A819DF91C82F70D99F64143D0F1ACCEF1A4D19825DD09326F432BB","password":"root password"}' \
  -u main_admin:main_password \
  http://localhost:9911/api/v1/admin/accounts
```
### Create 4 validator accounts, single validator per node.

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"privateKey":"77D030FC19D72CDCF25CD869AB05487C6772F1A75FDA314C73564F835BCA5CBF","password":"validator1 password"}' \
  -u main_admin:main_password \
  http://localhost:9911/api/v1/admin/accounts
```

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"privateKey":"5BE738EC7986195E854C0F5D890A5377950EC1DA75B92DBFBD56CBFB765FF80D","password":"validator2 password"}' \
  -u rhea_admin:rhea_password \
  http://localhost:9912/api/v1/admin/accounts
```

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"privateKey":"D17EE6A9EBD5F34DF2FD50D22D89AB5EDDC80A350440E4F40F88D71725B36BA6","password":"validator3 password"}' \
  -u loge_admin:loge_password \
  http://localhost:9913/api/v1/admin/accounts
```

```bash
curl -X POST \
  -H 'Content-Type: application/json' \
  -d '{"privateKey":"D7A50269CE3839A2D1AD47E5BE388CEEF1254EFAD613B17CD2224D0D213043DD","password":"validator4 password"}' \
  -u ymir_admin:ymir_password \
  http://localhost:9914/api/v1/admin/accounts
```

### Initialize blockchain

The default blockchain smart contract sources can be found in `contract/currency` folder. It contains a very simple toy 
implementation of a cryptocurrency and intended to just demonstrate the idea behind Bloqly as a scriptable blockchain engine
with JavaScript smart contracts support. 

Lets initialize new blockchain using it:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d "{\"space\":\"main\", \"path\": \"$(pwd)/demo/contract/currency\", \"passphrase\":\"root password\"}" \
  -u main_admin:main_password \
  http://localhost:9911/api/v1/admin/blockchain
``` 

### Export genesis block

Now we have new blockchain initialized on node `main` and want to export it to other nodes so that they all can work together.

It can be done in 2 steps:

1. Export genesis block

```bash
GENESIS=$(curl -u 'main_admin:main_password' -X GET http://localhost:9911/api/v1/admin/genesis/main)
```

2. Import genesis block to other nodes.

```bash
curl -X POST \
  -u rhea_admin:rhea_password \
  -H 'Content-Type: application/json' \
  -d ${GENESIS} http://localhost:9912/api/v1/admin/genesis
```

```bash
curl -X POST \
  -u loge_admin:loge_password \
  -H 'Content-Type: application/json' \
  -d ${GENESIS} http://localhost:9913/api/v1/admin/genesis
```

```bash
curl -X POST \
  -u ymir_admin:ymir_password \
  -H 'Content-Type: application/json' \
  -d ${GENESIS} http://localhost:9914/api/v1/admin/genesis
```

### Checking account balance

Now lets check the value of `root` account's property `balance` :

```bash
curl -X GET 'http://localhost:9901/api/v1/data/properties?space=main&self=self&target=58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607&key=balance'
```

Should output:

```json
{"value":"999996","type":"BIGINT"}
```

### Creating transaction

```bash
curl -X PUT \
-H 'Content-Type: application/json' \
-d '
{
    "space": "main",
    "origin": "58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607",
    "passphrase": "root password",
    "destination": "5CA1EEF9AA50625F3B7AC637D35655174CAA2C4FAB559B294D6E7C924C9AA6D4",
    "transactionType": "CALL",
    "self": "self",
    "key": "contract",
    "args": [
        {
            "type": "BIGINT",
            "value": "100"
        }
    ]
}
' http://localhost:9901/api/v1/data/transactions    
```

In response you should receive JSON representation of created transaction similar to the following:

```json
{
  "space": "main",
  "destination": "5CA1EEF9AA50625F3B7AC637D35655174CAA2C4FAB559B294D6E7C924C9AA6D4",
  "self": "self",
  "key": "contract",
  "value": "AwAAAAAAAABk",
  "transactionType": "CALL",
  "referencedBlockHash": "0611039429DB47FCE0E5B9E59B6D530300448A8463386565DC794F5FF0E759BA",
  "timestamp": 1533214649345,
  "signature": "QLS0XbcB6gwrZ2ec/8Zwl0Fm9WpxiGzjiZW/Hybt0YcKk/nCOMsDV7AfBKa+vCfPuu/NzKXWQaMaRZDTKbqzxQ==",
  "publicKey": "0360F47BACA2C9AF1CCD2B12A08B9C169B703142B2D45AD9DB783070CBD8011BE5",
  "hash": "0FBC70E170DA7ABD04268DF8B52A7D773D9F9FD0C0024B5DE44879CA5698B050",
  "nonce": "DA848B0A05C57E23328F87459AFE9B36E6220AA4A423F31726E0587CFA427A70"
}
```

Now, lets check balances of the first user again:

```bash
curl -X GET 'http://localhost:9901/api/v1/data/properties?space=main&self=self&target=58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607&key=balance'
```

Outputs:

```json
{"value":"999896","type":"BIGINT"}
```

And balance of a user we moved funds to:

```bash
curl -X GET 'http://localhost:9901/api/v1/data/properties?space=main&self=self&target=5CA1EEF9AA50625F3B7AC637D35655174CAA2C4FAB559B294D6E7C924C9AA6D4&key=balance'
```

Outputs:

```json
{"value":"100","type":"BIGINT"}
```

That's it for now!

## Consensus engine prototype, functional smart contracts, code samples

Smart contract code sample:

```JavaScript

/**
 * Move balance contract
 *
 * Simplified implementation of a cryptocurrency
 *
 * @param {Object}      ctx        Execution context
 * @param {BigInteger}  amount     Amount to move
 *
 * @return {Object}  Array of properties to set after function execution
 */

function contract(context, orig, dest, amount) {

    let origBalance = getProperty(orig, 'balance', zero);
    let destBalance = getProperty(dest, 'balance', zero);

    let newOrigBalance = origBalance.subtract(amount);
    let newDestBalance = destBalance.add(amount);

    return [
        { target: orig, balance: newOrigBalance },
        { target: dest, balance: newDestBalance },
    ];
}

```


```JavaScript
/**
 * Initialize genesis properties 
 *
 */
function init() {
    return [
        {
            target: 'self',
            root: '58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607'
        }, {
            target: '58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607',
            balance: new BigInteger('999996')
        }, {
            target: '10CA5388D7637B9A280D6E5BC3DBA5C71D55F6C155C9B9B2F35BCB149386EDFF',
            balance: new BigInteger('1')
        }, {
            target: 'F7974E0C93F5B1B98FE87E6791FE2203AF9D33E870C6DDAA3398D55243F7DAF7',
            balance: new BigInteger('1')
        }, {
            target: '9CBDDA161B690697550C552B6994DAC2B6628B6AF5F0337F931FA7F671DF8013',
            balance: new BigInteger('1')
        }, {
             target: 'DA4A181CBA4B51875A6C83015B2067DFC3EA3A3A4F9BCF20AEE953674ECA909C',
             balance: new BigInteger('1')
        }, {
            target: '10CA5388D7637B9A280D6E5BC3DBA5C71D55F6C155C9B9B2F35BCB149386EDFF',
            power: new BigInteger('1')
        }, {
            target: 'F7974E0C93F5B1B98FE87E6791FE2203AF9D33E870C6DDAA3398D55243F7DAF7',
            power: new BigInteger('1')
        }, {
            target: '9CBDDA161B690697550C552B6994DAC2B6628B6AF5F0337F931FA7F671DF8013',
            power: new BigInteger('1')
        }, {
            target: 'DA4A181CBA4B51875A6C83015B2067DFC3EA3A3A4F9BCF20AEE953674ECA909C',
            power: new BigInteger('1')
        }, {
            target: 'self',
            quorum: 3
        }, {
            target: 'self',
            validators: 4
        }
    ];
}

```
