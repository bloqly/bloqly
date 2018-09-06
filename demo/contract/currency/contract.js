
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

    if (key == 'balance' || key == 'power') {
        throw 'Illegal property access attempt: ' + key;
    }

    if (orig !=  dest) {
        throw 'Origin and destination don\'t match: ' + orig + ', ' + dest
    }

    return [ { target: dest, key: value } ];
}