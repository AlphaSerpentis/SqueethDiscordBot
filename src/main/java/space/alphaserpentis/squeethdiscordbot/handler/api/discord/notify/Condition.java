package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify;

import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.TrackedData;

public class Condition<T extends Number> {

    public enum ComparisonOperator {
        GREATER_THAN ("Greater Than", ">"),
        LESS_THAN ("Less Than", "<"),
        EQUALS ("Equals", "="),
        NOT_EQUALS ("Not Equals", "!=");

        public final String name;
        public final String symbol;

        ComparisonOperator(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }
    }

    public final ComparisonOperator comparisonOperator;
    public final T value;
    public final boolean recurring;
    public boolean isConditionActive = false;

    public Condition(ComparisonOperator comparisonOperator, T value, boolean recurring) {
        this.comparisonOperator = comparisonOperator;
        this.value = value;
        this.recurring = recurring;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Condition<?> condition) {
            return condition.comparisonOperator == comparisonOperator && condition.value.equals(value) && condition.recurring == recurring;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public String getFormattedCondition(TrackedData data) {
        return data.getName() + " is " + comparisonOperator.name + " " + data.getFormattedData(value) + ".";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public String getShortenedFormattedCondition(TrackedData data) {
        return data.getName() + " " + comparisonOperator.symbol + " " + data.getFormattedData(value);
    }
}
