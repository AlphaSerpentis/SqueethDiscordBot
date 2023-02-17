package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify;

public class Condition<T extends Number> {
    public enum TypeOfCondition {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        NOT_EQUALS
    }

    public final TypeOfCondition typeOfCondition;
    public final T condition;
    public final boolean recurring;
    public boolean isConditionActive = false;

    public Condition(TypeOfCondition typeOfCondition, T condition, boolean recurring) {
        this.typeOfCondition = typeOfCondition;
        this.condition = condition;
        this.recurring = recurring;
    }
}
