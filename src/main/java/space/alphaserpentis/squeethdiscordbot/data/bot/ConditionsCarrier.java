package space.alphaserpentis.squeethdiscordbot.data.bot;

import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public record ConditionsCarrier(
        HashMap<Long, ArrayList<Condition<?>>> conditions
) {
    public ConditionsCarrier(
            HashMap<Long, ArrayList<Condition<?>>> conditions
    ) {
        this.conditions = Objects.requireNonNullElseGet(conditions, HashMap::new);
    }

    public void addCondition(long userId, Condition<?> condition) {
        if(!conditions.containsKey(userId))
            conditions.put(userId, new ArrayList<>());
        conditions.get(userId).add(condition);
    }

    public void removeCondition(long userId, Condition<?> condition) {
        conditions.get(userId).remove(condition);
    }

    public void editCondition(long userId, Condition<?> condition, Condition<?> newCondition) {
        conditions.get(userId).set(conditions.get(userId).indexOf(condition), newCondition);
    }

    public ArrayList<Condition<?>> getConditions(long userId) {
        return conditions.getOrDefault(userId, new ArrayList<>());
    }

    public ArrayList<Long> getUsersSubscribed() {
        return new ArrayList<>(conditions.keySet());
    }
}
