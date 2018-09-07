'use strict';

var Long = Java.type('org.bloqly.machine.lang.BInteger');

const ZERO = new Long('0');

function property(dest, key, value) {
    let result = { target: dest };
    result[key] = value;
    return result;
}