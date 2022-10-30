// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api;

public class SqueethData {

    public long date;
    public Data data;

    public static class Data {
        private double underlying_price;
        private double current_index;
        private double current_mark;
        private double index;
        private double mark;
        private double current_implied_funding_value;
        private double daily_funding_value;
        private double current_implied_volatility_value;
        private double daily_implied_volatility_value;
        private double normalization_factor;
        private double delta;
        private double gamma;
        private double vega;
        private double theta;
        private long volume_osqth;
        private long volume_usd;
        private double liquidity;
        private double token0Price;
        private double volumeToken0;
        private double volumeToken1;
        private double volumeUSD;
        private double feesUSD;
        private double totalValueLockedToken0;
        private double totalValueLockedToken1;
        private double totalValueLockedETH;
        private double totalValueLockedUSD;
        private double txCount;
        private double oSQTH_price;
        private String date;

        public String getDate() {
            return date;
        }

        public long getVolumeoSQTH() {
            return volume_osqth;
        }

        public long getVolumeUSD() {
            return volume_usd;
        }

        public double getUnderlyingPrice() {
            return underlying_price;
        }

        public double getCurrentIndex() {
            return current_index;
        }

        public double getCurrentMark() {
            return current_mark;
        }

        public double getIndex() {
            return index;
        }

        public double getMark() {
            return mark;
        }

        public double getCurrentImpliedFundingValue() {
            return current_implied_funding_value;
        }

        public double getDailyFundingValue() {
            return daily_funding_value;
        }

        public double getCurrentImpliedVolatility() {
            return current_implied_volatility_value;
        }

        public double getDailyImpliedVolatility() {
            return daily_implied_volatility_value;
        }

        public double getoSQTHPrice() {
            return oSQTH_price;
        }

        public double getDelta() {
            return delta;
        }

        public double getGamma() {
            return gamma;
        }

        public double getVega() {
            return vega;
        }

        public double getTheta() {
            return theta;
        }

        public double getNormalizationFactor() {
            return normalization_factor;
        }

        public Double[] getGreeks() {
            return new Double[]{delta, gamma, vega, -theta, current_implied_volatility_value};
        }
    }
}
