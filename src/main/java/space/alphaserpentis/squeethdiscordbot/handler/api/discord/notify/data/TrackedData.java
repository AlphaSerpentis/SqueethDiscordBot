package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data;

import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;

import java.text.NumberFormat;

public abstract class TrackedData<T extends Number> {

    protected String name;
    protected String shortName;
    protected String unitSymbol;
    protected NumberFormat numberFormat = NumberFormat.getInstance();
    protected boolean isUnitSymbolPrefix = false;
    protected T currentData;

    public TrackedData(String name, String shortName, String unitSymbol) {
        this.name = name;
        this.shortName = shortName;
        this.unitSymbol = unitSymbol;
    }

//    protected HashMap<Long, ArrayList<Condition<T>>> conditions = null;

//    @SuppressWarnings("unchecked")
//    public void setConditions(HashMap<Long, ArrayList<Condition<?>>> conditions) {
////        if(this.conditions == null && conditions != null) {
////            for (Long userId : conditions.keySet()) {
////                for (Condition<?> condition : conditions.get(userId)) {
////                    this.conditions.put(userId, new ArrayList<>());
////                    this.conditions.get(userId).add((Condition<T>) condition);
////                }
////            }
////        } else if(conditions == null)
////            this.conditions = new HashMap<>();
//    }
//    @SuppressWarnings("unchecked")
//    public void addCondition(long userId, Condition<?> condition) {
//        if(!conditions.containsKey(userId))
//            conditions.put(userId, new ArrayList<>());
//        conditions.get(userId).add((Condition<T>) condition);
//    }
//    public void removeCondition(long userId, Condition<T> condition) {
//        conditions.get(userId).remove(condition);
//    }
//    public ArrayList<Long> getUsersSubscribed() {
//        return new ArrayList<>(conditions.keySet());
//    }
    public String getName() {
        return name;
    }
    public String getShortName() {
        return shortName;
    }
    public String getUnitSymbol() {
        return unitSymbol;
    }
    public T getCurrentData() {
        return currentData;
    }
    public String getFormattedData() {
        return isUnitSymbolPrefix ? unitSymbol + numberFormat.format(currentData) : numberFormat.format(currentData) + unitSymbol;
    }
    public String getFormattedData(T data) {
        return isUnitSymbolPrefix ? unitSymbol + numberFormat.format(data) : numberFormat.format(data) + unitSymbol;
    }
    abstract public void update();
    abstract public boolean checkCondition(@NonNull Condition<T> condition);
}
