package space.alphaserpentis.squeethdiscordbot.data.ethereum;

public interface Addresses {
    String zeroAddress = "0x0000000000000000000000000000000000000000";
    String weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
    String usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48";

    interface Squeeth {
        String controller = "0x64187ae08781b09368e6253f9e94951243a493d5";
        String osqth = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b";
        String crabv1 = "0xf205ad80bb86ac92247638914265887a8baa437d";
        String crabv2 = "0x3b960e47784150f5a63777201ee2b15253d713e8";
    }

    interface Uniswap {
        String osqthEthPool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c";
        String ethUsdcPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8";
        String oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1";
    }

}
