// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api.alchemy;

import java.util.ArrayList;
import java.util.List;

public class TokenTransferResponse {
    public Result result = new Result();

    public List<Result.Transfer> getData() {
        return result.transfers;
    }

    public class Result {   
        public List<Transfer> transfers = new ArrayList<>();
        public String pageKey;

        public class Transfer {
            public String blockNum;
            public String from;
            public double value;

            public int getBlockNum() {
                return Integer.parseInt(blockNum.substring(2), 16);
            }
        }
    }
}
