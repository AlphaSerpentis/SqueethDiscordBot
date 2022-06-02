package space.alphaserpentis.squeethdiscordbot.data;

import java.math.BigInteger;
import java.text.DecimalFormat;

public class SimpleTokenTransferResponse {
    public int blockNum;
    public String from;
    public double value;

    public SimpleTokenTransferResponse(int blockNum, String from, double value) {
        this.blockNum = blockNum;
        this.from = from;
        this.value = value;
    }

    public int getBlockNum() {
        return blockNum;
    }
    public BigInteger getBigIntegerValue() {
        DecimalFormat df = new DecimalFormat("#");
        return new BigInteger(String.valueOf(df.format(value * Math.pow(10,18))));
    }
}
