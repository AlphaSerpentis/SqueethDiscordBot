package space.alphaserpentis.squeethdiscordbot.data;

public class ServerData {

    private boolean onlyEphemeral = true;

    public void setOnlyEphemeral(boolean value) {
        onlyEphemeral = value;
    }

    public boolean isOnlyEphemeral() {
        return onlyEphemeral;
    }
}
