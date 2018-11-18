## JavaScript Smart Contracts Engine on a pBFT+PoA-inspired blockchain consensus algorithm

---

#### The project currently is under global refactoring, sources will be available later.
   

[Telegram](https://t.me/joinchat/B45otRIISgdmc6u2AwC9Cg)
[Twitter](https://twitter.com/slava_snezhkov)

## What's this?

Bloqly is a written from the scratch smart contracts engine with JavaScript support.

Small contract code example:

```JavaScript
const fractions = new Long('10').pow(8);
const maxSupply = new Long('1_000_000_000').multiply(fractions);

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

function set(context, orig, dest, key, value) {

    return [ property(dest, key, value) ];
}

function setSigned(context, orig, dest, key, value, signature, publicKey) {

    let message = Crypto.sha256(value)

    if (!Crypto.verify(message, signature, publicKey)) {
        throw "Invalid signature"
    }

    return [ property(dest, key + ":" + publicKey, value) ];
}
```

## Documentation
### [Bloqly consensus algoritm](https://github.com/bloqly/bloqly/wiki/Bloqly-Consensus-Algorithm)
### [Functional smart contracts with JavaScript](https://github.com/bloqly/bloqly/wiki/PureAttributes)
