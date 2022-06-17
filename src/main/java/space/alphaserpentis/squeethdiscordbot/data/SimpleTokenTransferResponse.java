// SPDX-License-Identifier: GPL-2.0-only

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
        double strippedDouble = Double.parseDouble(String.format("%.8f", value));

        return new BigInteger(df.format(strippedDouble * Math.pow(10,18)));
    }

    @Override
    public String toString() {
        return "SimpleTokenTransferResponse{" +
                "blockNum=" + blockNum +
                ", from='" + from + '\'' +
                ", value=" + value +
                ", bigIntegerValue=" + getBigIntegerValue() +
                '}';
    }
}
