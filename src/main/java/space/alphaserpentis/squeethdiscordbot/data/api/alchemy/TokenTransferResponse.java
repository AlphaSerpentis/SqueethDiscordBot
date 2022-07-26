// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api.alchemy;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TokenTransferResponse {
    public Result result = new Result();

    @Nonnull
    public List<Result.Transfer> getData() {
        return result.transfers;
    }

    public static class Result {
        public List<Transfer> transfers = new ArrayList<>();
        public String pageKey;

        public static class Transfer {
            public String blockNum;
            public String from;
            public double value;

            public int getBlockNum() {
                return Integer.parseInt(blockNum.substring(2), 16);
            }
        }
    }
}
