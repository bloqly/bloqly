
<p align="center">
<img src="https://raw.githubusercontent.com/slavasn/bloqly/master/resources/logo.png"/>
</p>    
<p align="center">JavaScript Smart Contracts Engine on PBFT blockchain.</p>


---  
<p align="center">
<a href="https://travis-ci.org/slavasn/bloqly"><img src="https://travis-ci.org/slavasn/bloqly.svg?branch=master" alt="Build Status"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="Build Status"></a>
</p>    

<p align="center">
This is a work in progress/prototype.
</p>     

## Consensus engine prototype, functional smart contracts

Smart contract code sample:

```JavaScript

const maxSupply = new BigInteger('1000000');
const validatorAmount = new BigInteger('1');
const validatorPower = validatorAmount;

const zero = new BigInteger('0');

/**
 * Move balance contract
 *
 * Simplified implementation of a cryptocurrency  defining  functional  smart contract
 *
 * @param {Object}      ctx        Execution context
 * @param {BigInteger}  amount     Amount to move
 *
 * @return {Object}  Array of properties to set after function execution
 */

function contract(context, amount) {

    var callerBalance = getProperty('caller', 'balance', zero);
    var calleeBalance = getProperty('callee', 'balance', zero);

    var newCallerBalance = callerBalance.subtract(amount);
    var newCalleeBalance = calleeBalance.add(amount);

    return [
        { target: 'caller', balance: newCallerBalance },
        { target: 'callee', balance: newCalleeBalance },
    ];
}

function init(context, genesisParameters) {

    return [
        { target: 'caller', balance: new BigInteger('999997') },

        // target = validator's id
        { target: genesisParameters.validators[0].id, balance: validatorAmount },
        { target: genesisParameters.validators[1].id, balance: validatorAmount },
        { target: genesisParameters.validators[2].id, balance: validatorAmount },
        
        { target: genesisParameters.validators[0].id, power: validatorAmount },
        { target: genesisParameters.validators[1].id, power: validatorAmount },
        { target: genesisParameters.validators[2].id, power: validatorAmount },

        // quorum requirement for active validators group
        { target: 'self', quorum: 2 },

        // active validators group size
        { target: 'self', validators: 3 }

    ];
}

```

[Chat](https://riot.im/app/#/room/#bloqly:matrix.org)
[Twitter](https://twitter.com/slava_snezhkov)
