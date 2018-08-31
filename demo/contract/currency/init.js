function init() {
    return [
        {
            target: 'self',
            root: '58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607'
        }, {
            target: '58BF325AF01CCC78265EB715C1EB10EEA455905D4B50C2AC6541950D97DF8607',
            balance: maxValue.safeSubtract(4)
        }, {
            target: '10CA5388D7637B9A280D6E5BC3DBA5C71D55F6C155C9B9B2F35BCB149386EDFF',
            balance: new BigInteger('1')
        }, {
            target: 'F7974E0C93F5B1B98FE87E6791FE2203AF9D33E870C6DDAA3398D55243F7DAF7',
            balance: new BigInteger('1')
        }, {
            target: '9CBDDA161B690697550C552B6994DAC2B6628B6AF5F0337F931FA7F671DF8013',
            balance: new BigInteger('1')
        }, {
             target: 'DA4A181CBA4B51875A6C83015B2067DFC3EA3A3A4F9BCF20AEE953674ECA909C',
             balance: new BigInteger('1')
        }, {
            target: '10CA5388D7637B9A280D6E5BC3DBA5C71D55F6C155C9B9B2F35BCB149386EDFF',
            power: new BigInteger('1')
        }, {
            target: 'F7974E0C93F5B1B98FE87E6791FE2203AF9D33E870C6DDAA3398D55243F7DAF7',
            power: new BigInteger('1')
        }, {
            target: '9CBDDA161B690697550C552B6994DAC2B6628B6AF5F0337F931FA7F671DF8013',
            power: new BigInteger('1')
        }, {
            target: 'DA4A181CBA4B51875A6C83015B2067DFC3EA3A3A4F9BCF20AEE953674ECA909C',
            power: new BigInteger('1')
        }, {
            target: 'self',
            quorum: 3
        }, {
            target: 'self',
            validators: 4
        }
    ];
}

