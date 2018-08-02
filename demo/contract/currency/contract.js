
const maxSupply = new BigInteger('1000000');
const validatorAmount = new BigInteger('1');
const validatorPower = validatorAmount;

const zero = new BigInteger('0');

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