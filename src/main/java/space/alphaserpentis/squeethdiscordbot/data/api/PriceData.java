// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api;

import java.math.BigInteger;

public class PriceData {

    public enum Prices {
        ETHUSD,
        OSQTHETH,
        CRABV1ETH,
        CRABV2ETH,
        NORMFACTOR
    }

    public BigInteger ethUsdc = BigInteger.ZERO;
    public BigInteger osqthEth = BigInteger.ZERO;
    public BigInteger crabEth = BigInteger.ZERO;
    public BigInteger crabV2Eth = BigInteger.ZERO;
    public BigInteger normFactor = BigInteger.ZERO;
    public boolean isAllZero() {
        return (
                ethUsdc.equals(BigInteger.ZERO) &&
                osqthEth.equals(BigInteger.ZERO) &&
                crabEth.equals(BigInteger.ZERO) &&
                crabV2Eth.equals(BigInteger.ZERO) &&
                normFactor.equals(BigInteger.ZERO)
        );
    }
}
