// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api.alchemy;

import io.reactivex.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("CanBeFinal")
public class TokenTransferResponse {
    public Result result = new Result();

    @NonNull
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
            public RawContract rawContract;

            public static class RawContract {
                public String value;
                public String decimal;
            }

            public int getBlockNum() {
                return Integer.parseInt(blockNum.substring(2), 16);
            }
        }
    }
}
