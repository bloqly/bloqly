
<p align="center">
<img src="resources/logo.svg" width="40%"/>
</p>    
<p align="center">JavaScript Smart Contracts Engine on a pBFT+PoA-inspired blockchain consensus algorithm</p>


---  
<p align="center">
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="Build Status"></a>
</p> 

<p align="center">
This is a work in progress/prototype, API and implementation details can change.
</p>     

[Telegram](https://t.me/joinchat/B45otRIISgdmc6u2AwC9Cg)
[Twitter](https://twitter.com/slava_snezhkov)

## What's this?

Bloqly is a written from the scratch smart contracts engine with JavaScript support.

First, small contract code sample:

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

const fractions = new Long('10').pow(8);
const maxSupply = new Long('1_000_000_000').add(fractions);

function main(context, orig, dest, amount) {

    let origBalance = getProperty(orig, 'balance', ZERO);
    let destBalance = getProperty(dest, 'balance', ZERO);

    let newOrigBalance = origBalance.safeSubtract(amount);
    let newDestBalance = destBalance.safeAdd(amount, maxSupply);

    return [
        { target: orig, balance: newOrigBalance },
        { target: dest, balance: newDestBalance },
    ];
}
```

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

## Documentation
### [Quick start](https://github.com/bloqly/bloqly/wiki/Quick-Start)
### [Bloqly consensus algoritm](https://github.com/bloqly/bloqly/wiki/Bloqly-Consensus-Algorithm)
### [Functional smart contracts with JavaScript](https://github.com/bloqly/bloqly/wiki/PureAttributes)

