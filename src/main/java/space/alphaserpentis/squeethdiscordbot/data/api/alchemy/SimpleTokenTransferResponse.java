// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api.alchemy;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.text.DecimalFormat;

public class SimpleTokenTransferResponse {
    public String token;
    public int blockNum;
    public String from;
    public double value;

    public SimpleTokenTransferResponse(@Nonnull String token, int blockNum, @Nonnull String from, double value) {
        this.token = token;
        this.blockNum = blockNum;
        this.from = from;
        this.value = value;
    }

    public int getBlockNum() {
        return blockNum;
    }

    @Nonnull
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

    @Override
    public boolean equals(Object o) {
        if(o instanceof SimpleTokenTransferResponse) {
            return (
                    blockNum == ((SimpleTokenTransferResponse) o).blockNum
                    && token.equalsIgnoreCase(((SimpleTokenTransferResponse) o).token)
                    && from.equalsIgnoreCase(((SimpleTokenTransferResponse) o).from)
                    && value == ((SimpleTokenTransferResponse) o).value
            );
        } else {
            return false;
        }
    }
}
