package space.alphaserpentis.squeethdiscordbot.data;

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
}
