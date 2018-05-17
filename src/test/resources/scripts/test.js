var BigInteger = Java.type("org.bloqly.machine.math.BInteger");

function contract(ctx, arg1, arg2, arg3, arg4) {

    return [
        { target: 'caller', value1: arg1 },
        { target: 'caller', value2: arg2 },

        { target: 'self', value3: arg3 },
        { target: 'self', value4: arg4.add(new BigInteger("1")) }
    ];
}

function init(ctx) {

    return [
        { target: 'caller', value1: 'test1'},

        { target: 'self', value3: false }
    ];
}
