package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data;

import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class TrackedData<T extends Number> {

    protected String name;
    protected T currentData;
    protected HashMap<Long, ArrayList<Condition<T>>> conditions = new HashMap<>();
    protected ArrayList<Long> userIdsSubscribed = new ArrayList<>();

    public void addUser(long userId) {
        if(!userIdsSubscribed.contains(userId))
            userIdsSubscribed.add(userId);
    }
    public void removeUser(long userId) {
        userIdsSubscribed.remove(userId);
    }
    public void addCondition(long userId, Condition<T> condition) {
        if(!conditions.containsKey(userId))
            conditions.put(userId, new ArrayList<>());
        conditions.get(userId).add(condition);
    }
    public void removeCondition(long userId, Condition<T> condition) {
        conditions.get(userId).remove(condition);
    }
    @NonNull
    public ArrayList<Long> usersEligibleForNotification() {
        ArrayList<Long> eligibleUsers = new ArrayList<>();
        for(Long userId : userIdsSubscribed) {
            for(Condition<T> condition : conditions.get(userId)) {
                if(checkCondition(condition) && !condition.isConditionActive) {
                    eligibleUsers.add(userId);
                    condition.isConditionActive = true;
                    if(!condition.recurring)
                        removeCondition(userId, condition);
                    break;
                } else if(!checkCondition(condition) && condition.isConditionActive) {
                    condition.isConditionActive = false;
                    break;
                }
            }
        }
        return eligibleUsers;
    }
    public String getName() {
        return name;
    }
    public T getCurrentData() {
        return currentData;
    }
    abstract public void update();
    abstract public boolean checkCondition(@NonNull Condition<T> condition);

}
