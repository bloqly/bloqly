
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

function contract(ctx, amount) {

    var callerBalance = getProperty('caller', 'balance', zero);
    var calleeBalance = getProperty('callee', 'balance', zero);

    var newCallerBalance = callerBalance.subtract(amount);
    var newCalleeBalance = calleeBalance.add(amount);

    return [
        { target: 'caller', balance: newCallerBalance },
        { target: 'callee', balance: newCalleeBalance },
    ];
}

function init(ctx) {

    return [
        { target: 'caller', balance: maxSupply.subtract(new BigInteger('3')) },

        // target = validator's id
        { target: '{{address1}}', balance: validatorAmount },
        { target: '{{address2}}', balance: validatorAmount },
        { target: '{{address3}}', balance: validatorAmount },

        { target: '{{address1}}', power: validatorPower },
        { target: '{{address2}}', power: validatorPower },
        { target: '{{address3}}', power: validatorPower },


        // quorum requirement for active validators group
        { target: 'self', 'quorum': 2 },

        // active validators group size
        { target: 'self', 'validators': 3 }

    ];
}
