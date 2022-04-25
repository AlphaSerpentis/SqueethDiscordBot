package space.alphaserpentis.squeethdiscordbot.data;

public class SqueethData {

    private String date;
    private long volume_osqth, volume_usd;
    private double underlying_price, current_mark, current_index, index, mark, current_implied_funding_value, daily_funding_value, current_implied_volatility_value, daily_implied_volatility_value, oSQTH_price, delta, gamma, vega, theta, normalization_factor;

    public SqueethData() {

    }

    public SqueethData(
            String _date,
            long _volumeoSQTH,
            long _volumeUSD,
            double _underlyingPrice,
            double _currentMark,
            double _currentIndex,
            double _index,
            double _mark,
            double _currentImpliedFundingValue,
            double _dailyFundingValue,
            double _currentImpliedVolatility,
            double _dailyImpliedVolatility,
            double _oSQTHPrice,
            double _delta,
            double _gamma,
            double _vega,
            double _theta,
            double _normalization_factor
    ) {
        date = _date;
        volume_osqth = _volumeoSQTH;
        volume_usd = _volumeUSD;
        underlying_price = _underlyingPrice;
        current_mark = _currentMark;
        current_index = _currentIndex;
        index = _index;
        mark = _mark;
        current_implied_funding_value = _currentImpliedFundingValue;
        daily_funding_value = _dailyFundingValue;
        current_implied_volatility_value = _currentImpliedVolatility;
        daily_implied_volatility_value = _dailyImpliedVolatility;
        oSQTH_price = _oSQTHPrice;
        delta = _delta;
        gamma = _gamma;
        vega = _vega;
        theta = _theta;
        normalization_factor = _normalization_factor;
    }

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
        return new Double[]{delta, gamma, vega, theta, current_implied_volatility_value};
    }
}
