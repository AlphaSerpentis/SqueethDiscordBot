package space.alphaserpentis.squeethdiscordbot.data.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class LatestCrabAuctionResponse {
    public static class Auction {
        public static class Bid {
            public static class Order {
                BigInteger quantity = BigInteger.ZERO;
                BigInteger price = BigInteger.ZERO;
                long bidId = 0;
                String trader = "";
                BigInteger nonce = BigInteger.ZERO;
                boolean isBuying = false;
                BigInteger expiry = BigInteger.ZERO;
            }

            public String s = "";
            public Order order = null;
            public BigInteger v = BigInteger.ZERO;
            public String signature = "";
            public String bidder = "";
            public String r = "";

            @Override
            public String toString() {
                String shortenedBidder = bidder.substring(6, bidder.length() - 4);
                double convertedQuantity = order.quantity.doubleValue() / Math.pow(10,18);
                double convertedPrice = order.price.doubleValue() / Math.pow(10,18);

                return shortenedBidder + (order.isBuying ? " is buying " : " is selling ") + convertedQuantity + " oSQTH for" + (convertedPrice * convertedQuantity) + " ETH";
            }
        }

        public BigInteger minSize = BigInteger.ZERO;
        public boolean isSelling = false;
        public HashMap<String, Bid> bids = null;
        public BigInteger auctionEnd = BigInteger.ZERO;
        public long currentAuctionId = 0;
        public ArrayList<Bid> winningBids = null;
        public BigInteger price = BigInteger.ZERO;
        public long nextAuctionId = 0;
        public BigInteger oSqthAmount = BigInteger.ZERO;
    }

    public Auction auction = null;
    public boolean isLive = false;
    public String status = "";
    public String message = "";
}
