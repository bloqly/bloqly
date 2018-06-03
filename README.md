
<p align="center">
<img src="resources/logo.svg" width="40%"/>
</p>    
<p align="center">JavaScript Smart Contracts Engine on PBFT blockchain.</p>


---  
<p align="center">
<a href="https://travis-ci.org/slavasn/bloqly"><img src="https://travis-ci.org/slavasn/bloqly.svg?branch=master" alt="Build Status"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="Build Status"></a>
</p>    

[Chat](https://riot.im/app/#/room/#bloqly:matrix.org)  
[Twitter](https://twitter.com/slava_snezhkov)

<p align="center">
This is a work in progress/prototype.
</p>     

## Consensus engine prototype, functional smart contracts

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

Corresponding genesis configuration:

```JSON
{
  "parameters": [
    {
      "target": "self",
      "key": "root",
      "value": "0A83C9CD3F1CA7DC1CA8AFA1727D64E9B1FAC66321403136EB2F1CB86DC93736"
    },
    {
      "target": "0A83C9CD3F1CA7DC1CA8AFA1727D64E9B1FAC66321403136EB2F1CB86DC93736",
      "key": "balance",
      "value": "BigInteger(999997)"
    },
    {
      "target": "A3DDB47B4849BF1DD90580604405ABC91DECCFF68F6E787BDBEACED3F640B669",
      "key": "balance",
      "value": "BigInteger(1)"
    },
    {
      "target": "32AC890D2E3ECA40888749A099524E23458598B764CAA752EF07A773AE479E91",
      "key": "balance",
      "value": "BigInteger(1)"
    },
    {
      "target": "9EB1858D8BBBDD10BA3E4EE83BE51EEF1BCB95CAFDA89FA57E5AE8342AB97A3F",
      "key": "balance",
      "value": "BigInteger(1)"
    },
    {
      "target": "A3DDB47B4849BF1DD90580604405ABC91DECCFF68F6E787BDBEACED3F640B669",
      "key": "power",
      "value": "BigInteger(1)"
    },
    {
      "target": "32AC890D2E3ECA40888749A099524E23458598B764CAA752EF07A773AE479E91",
      "key": "power",
      "value": "BigInteger(1)"
    },
    {
      "target": "9EB1858D8BBBDD10BA3E4EE83BE51EEF1BCB95CAFDA89FA57E5AE8342AB97A3F",
      "key": "power",
      "value": "BigInteger(1)"
    },
    {
      "target": "self",
      "key": "quorum",
      "value": 2
    },
    {
      "target": "self",
      "key": "validators",
      "value": 3
    }
  ]
}
```
