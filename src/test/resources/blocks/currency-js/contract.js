
const fractions = new BigInteger('10').pow(8);
const maxSupply = new BigInteger('1_000_000_000').add(fractions);

const validatorAmount = new BigInteger('1');
const validatorPower = validatorAmount;

const zero = new BigInteger('0');

function main(context, orig, dest, amount) {

    let origBalance = getProperty(orig, 'balance', zero);
    let destBalance = getProperty(dest, 'balance', zero);

    let newOrigBalance = origBalance.safeSubtract(amount);
    let newDestBalance = destBalance.safeAdd(amount, maxSupply);

    return [
        { target: orig, balance: newOrigBalance },
        { target: dest, balance: newDestBalance },
    ];
}

function set(context, orig, key, value) {

    return [
        { target: context.self, key: value }
    ];
}