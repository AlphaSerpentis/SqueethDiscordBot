package space.alphaserpentis.squeethdiscordbot.data.api.squeethportal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class Auction {
    public static class Bid {
        public static class Order {
            public BigInteger quantity = BigInteger.ZERO;
            public BigInteger price = BigInteger.ZERO;
            public long bidId = 0;
            public String trader = "";
            public BigInteger nonce = BigInteger.ZERO;
            public boolean isBuying = false;
            public BigInteger expiry = BigInteger.ZERO;

            @Override
            public String toString() {
                double convertedQuantity = quantity.doubleValue() / Math.pow(10,18);
                double convertedPrice = price.doubleValue() / Math.pow(10,18);

                return (isBuying ? " is buying " : " is selling ") + convertedQuantity + " oSQTH for " + (convertedPrice * convertedQuantity) + " ETH";
            }
        }

        public String s = "";
        public Order order = null;
        public BigInteger v = BigInteger.ZERO;
        public String signature = "";
        public String bidder = "";
        public String r = "";

        public String shortenedBidder() {
            return bidder.substring(0, 6) + "..." + bidder.substring(bidder.length() - 4);
        }

        @Override
        public String toString() {
            return shortenedBidder() + order.toString();
        }
    }

    public long currentAuctionId = 0;
    public double minSize = 0;
    public boolean isSelling = false;
    public HashMap<String, Bid> bids = null;
    public BigInteger auctionEnd = BigInteger.ZERO;
    public ArrayList<String> winningBids = null;
    public BigInteger price = BigInteger.ZERO;
    public long nextAuctionId = 0;
    public BigInteger oSqthAmount = BigInteger.ZERO;
}
