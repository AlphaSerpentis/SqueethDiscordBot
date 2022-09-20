// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.api.alchemy;

@SuppressWarnings("CanBeFinal")
public class AlchemyRequest {
    public String jsonrpc = "2.0";
    public int id = 1;
    public String method;
    public AlchemyParams[] params = new AlchemyParams[]{new AlchemyParams()};
}
