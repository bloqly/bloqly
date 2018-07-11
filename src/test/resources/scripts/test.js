var BigInteger = Java.type("org.bloqly.machine.math.BInteger");

function contract(context, orig, dest, arg1, arg2, arg3, arg4) {

    return [
        { target: orig, value1: arg1 },
        { target: dest, value2: arg2 },

        { target: context.self, value3: arg3 },
        { target: context.self, value4: arg4.add(new BigInteger("1")) }
    ];
}

function init() {
    return [
        { target: 'owner id', value1: 'test1' },
        { target: 'test.js.self', value3: false }
    ];
}