package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data;

import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;

public class VaultCollateralization extends TrackedData<Double> {

    public VaultCollateralization() {
        super("Vault Collateralization", "VaultCollateralization", "%");
    }

    @Override
    public void update() {

    }

    @Override
    public boolean checkCondition(@NonNull Condition<Double> condition) {
        return false;
    }
}
