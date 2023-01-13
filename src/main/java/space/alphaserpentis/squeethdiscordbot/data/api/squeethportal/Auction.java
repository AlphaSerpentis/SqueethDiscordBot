package space.alphaserpentis.squeethdiscordbot.data.api.squeethportal;

import io.reactivex.annotations.NonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Auction {
    public static class Bid {
        @SuppressWarnings("CanBeFinal")
        public static class Order implements Comparable<Order> {
            public BigInteger quantity = BigInteger.ZERO;
            public BigInteger price = BigInteger.ZERO;
            public long bidId = 0;
            public String trader = "";
            public BigInteger nonce = BigInteger.ZERO;
            public boolean isBuying = false;
            public BigInteger expiry = BigInteger.ZERO;

            @Override
            public String toString() {
                double convertedQuantity = Double.parseDouble(String.format("%.5f", quantity.doubleValue() / Math.pow(10,18)));
                double convertedPrice = Double.parseDouble(String.format("%.5f", price.doubleValue() / Math.pow(10,18)));

                return (isBuying ? " is buying " : " is selling ") + convertedQuantity + " oSQTH for " + String.format("%.5f", convertedPrice * convertedQuantity) + " ETH (" + convertedPrice + " ETH)";
            }

            @Override
            public int compareTo(@NonNull Order o) {
                if(isBuying) {
                    if(o.price.compareTo(price) > 0) {
                        return 1;
                    } else if(o.price.compareTo(price) < 0) {
                        return -1;
                    }
                } else {
                    if(o.price.compareTo(price) < 0) {
                        return 1;
                    } else if(o.price.compareTo(price) > 0) {
                        return -1;
                    }
                }

                // prices are equal, check for quantity
                if(o.quantity.compareTo(quantity) < 0) {
                    return -1;
                } else if (o.quantity.compareTo(quantity) > 0) {
                    return 1;
                }

                return 0;
            }
        }

        public String s = "";
        public Order order = null;
        public BigInteger v = BigInteger.ZERO;
        public String signature = "";
        public String bidder = "";
        public String r = "";

        @Override
        public String toString() {
            return order.toString();
        }
    }

    public long currentAuctionId = 0;
    public double minSize = 0;
    public boolean isSelling = false;
    public HashMap<String, Bid> bids = new HashMap<>();
    public BigInteger auctionEnd = BigInteger.ZERO;
    public ArrayList<String> winningBids = new ArrayList<>();
    public BigInteger price = BigInteger.ZERO;
    public long nextAuctionId = 0;
    public BigInteger oSqthAmount = BigInteger.ZERO;

    public static ArrayList<Bid> sortedBids(@NonNull Auction auction) {
        ArrayList<Bid> bids = new ArrayList<>(auction.bids.values());
        ArrayList<Bid> bidsNotIncluded;

        bids.removeIf(
                bid -> {
                    if(auction.isSelling != bid.order.isBuying) {
                        return true;
                    }

                    return bid.order.quantity.doubleValue() / Math.pow(10,18) < auction.minSize;
                }
        );

        bidsNotIncluded = (ArrayList<Bid>) bids.stream().filter(b -> {
            if (!auction.winningBids.isEmpty()) {
                for (String winningBidKey : auction.winningBids) {
                    if (auction.bids.get(winningBidKey).equals(b)) {
                        return false;
                    }
                }
            }
            return true;
        }).collect(Collectors.toList());

        bids.removeAll(bidsNotIncluded);

        bids.sort(
                Comparator.comparing(o -> o.order)
        );

        bidsNotIncluded.sort(
                Comparator.comparing(o -> o.order)
        );

        bids.addAll(bidsNotIncluded);

        return bids;
    }
}
