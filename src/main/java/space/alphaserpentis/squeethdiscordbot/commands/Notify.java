package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.NotifyHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.TrackedData;

import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class Notify extends ButtonCommand<MessageEmbed> implements ModalCommand {

    public static class UserSession {
        public Button firstClick, secondClick, thirdClick;
        public Button[] conditionButtons;
        public TrackedData<?> trackedData;
        public Condition<?> condition;
        public boolean makeRecurring;
        public Number value;

        public void setFirstClick(Button firstClick) {
            if(this.firstClick != null) {

            } else {
                this.firstClick = firstClick;
            }
        }

        public void setSecondClick(Button secondClick) {
            if(this.secondClick != null) {

            } else {
                this.secondClick = secondClick;
            }
        }

        public void setThirdClick(Button thirdClick) {
            if(this.thirdClick != null) {

            } else {
                this.thirdClick = thirdClick;
            }
        }

        public void setCustomButtons(Button[] conditionButtons) {
            this.conditionButtons = conditionButtons;
        }
    }

    private static final String EMBED_TITLE = "Notify System";
    private static final HashMap<Long, UserSession> userSessionHashMap = new HashMap<>();

    public Notify() {
        super(
                new BotCommandOptions(
                        "notify",
                        "Receive notifications for various events to your liking!",
                        true,
                        true,
                        TypeOfEphemeral.DEFAULT
                )
        );

        buttonHashMap.put("Add", Button.primary("notify_add", "Add"));
        buttonHashMap.put("Remove", Button.primary("notify_remove", "Remove"));
        buttonHashMap.put("Edit", Button.primary("notify_edit", "Edit"));
        buttonHashMap.put("List", Button.primary("notify_list", "List"));

        for(TrackedData<?> data: NotifyHandler.trackedData) {
            String convertedDataName = "notify_" + data.getName().toLowerCase().replace(" ", "_");
            buttonHashMap.put(data.getName().replace(" ", ""), Button.primary(convertedDataName, data.getName()));
        }

        buttonHashMap.put("GreaterThan", Button.primary("notify_greater_than", "Greater Than (>)"));
        buttonHashMap.put("LessThan", Button.primary("notify_less_than", "Less Than (<)"));
        buttonHashMap.put("EqualTo", Button.primary("notify_equal_to", "Equal To (==)"));
        buttonHashMap.put("NotEqualTo", Button.primary("notify_not_equal_to", "Not Equal To (!=)"));

        buttonHashMap.put("Confirm", Button.primary("notify_confirm", "Confirm"));
        buttonHashMap.put("Cancel", Button.danger("notify_cancel", "Cancel"));
        buttonHashMap.put("Yes", Button.primary("notify_yes", "Yes"));
        buttonHashMap.put("No", Button.primary("notify_no", "No"));
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending;
        List<ItemComponent> buttons = new ArrayList<>();
        UserSession session = userSessionHashMap.get(event.getUser().getIdLong());
        long userId = event.getUser().getIdLong();

        if(session == null) {
            session = new UserSession();
            userSessionHashMap.put(userId, session);
        }

        configureDefaultEmbedBuilder(eb);

        switch(event.getButton().getId()) {
            case "notify_add" -> {
                pending = event.deferEdit().complete();
                session.setFirstClick(event.getButton());
                addButtonAction(event, eb, buttons);
            }
            case "notify_remove" -> {
                pending = event.deferEdit().complete();
                session.setFirstClick(event.getButton());
                removeButtonAction(event, eb, buttons);
            }
            case "notify_edit" -> {
                pending = event.deferEdit().complete();
                session.setFirstClick(event.getButton());
                editButtonAction(event, eb, buttons);
            }
            case "notify_list" -> {
                pending = event.deferEdit().complete();
                session.setFirstClick(event.getButton());
                listButtonAction(event, eb);
            }
            case "notify_greater_than", "notify_equal_to", "notify_less_than", "notify_not_equal_to" -> {
                session.setThirdClick(event.getButton());
                event.replyModal(promptModalAfterConditionSelection(getConditionFromButton(event.getButton()))).queue();
                return;
            }
            case "notify_yes" -> {
                pending = event.deferEdit().complete();
                session.makeRecurring = true;
                afterYesOrNoButtonAction(event, eb, buttons);
            }
            case "notify_no" -> {
                pending = event.deferEdit().complete();
                session.makeRecurring = false;
                afterYesOrNoButtonAction(event, eb, buttons);
            }
            case "notify_confirm" -> {
                pending = event.deferEdit().complete();
                confirmButtonAction(event, eb);
            }
            case "notify_cancel" -> {
                pending = event.deferEdit().complete();
                cancelButtonAction(eb);
            }
            default -> {
                pending = event.deferEdit().complete();
                for(TrackedData<?> data: NotifyHandler.trackedData) {
                    if(event.getButton().getId().equals("notify_" + data.getName().toLowerCase().replace(" ", "_"))) {
                        session.setSecondClick(event.getButton());
                        session.trackedData = data;

                        if(session.firstClick.getLabel().equals("Remove")) {
                            eb.setDescription("Are you sure you want to remove notification for " + data.getName() + "?");
                            buttons.add(getButton("Confirm"));
                            buttons.add(getButton("Cancel"));
                            break;
                        } else if(session.firstClick.getLabel().equals("Edit")) {
                            // Get a list of the conditions for this data from this user
                            ArrayList<Condition<?>> conditions = NotifyHandler.getConditionsForDataFromUser(userId, data);

                            for(int i = 0; i < conditions.size(); i++) {
                                Condition<?> condition = conditions.get(i);
                                Button button = Button.primary("notify_edit_" + i + "_" + userId, condition.getShortenedFormattedCondition(data));
                                buttons.add(button);

                                if(session.conditionButtons == null) {
                                    session.conditionButtons = new Button[conditions.size()];
                                }

                                session.conditionButtons[i] = button;
                            }

                            eb.setDescription("Select a condition to edit.");

                            break;
                        }

                        eb.setDescription("Select a comparison operator for " + data.getName() + ".");

                        buttons.add(getButton("GreaterThan"));
                        buttons.add(getButton("LessThan"));
                        buttons.add(getButton("EqualTo"));
                        buttons.add(getButton("NotEqualTo"));

                        break;
                    }
                }

                if(session.conditionButtons != null) {
                    // Iterate through all the buttons and find which one was clicked
                    for(Button button: session.conditionButtons) {
                        if(event.getButton().getId().equals(button.getId())) {
                            // Get the index of the button that was clicked
                            int index = Integer.parseInt(button.getId().split("_")[2]);

                            // Get the condition that was clicked
                            Condition<?> condition = NotifyHandler.getConditionsForDataFromUser(userId, session.trackedData).get(index);

                            // Set the condition in the session
                            session.condition = condition;

                            if(session.firstClick.getLabel().equals("Remove")) {
                                eb.setDescription("Are you sure you want to remove notification for " + session.trackedData.getName() + " " + condition.toString() + "?");
                                buttons.add(getButton("Confirm"));
                                buttons.add(getButton("Cancel"));
                                break;
                            } else if(session.firstClick.getLabel().equals("Edit")) {
                                eb.setDescription("Select a comparison operator for " + session.trackedData.getName() + ".");
                                buttons.add(getButton("GreaterThan"));
                                buttons.add(getButton("LessThan"));
                                buttons.add(getButton("EqualTo"));
                                buttons.add(getButton("NotEqualTo"));
                                break;
                            }

                            break;
                        }
                    }
                }
            }
        }

        if(!buttons.isEmpty())
            pending.editOriginalEmbeds(eb.build()).setActionRow(buttons).queue();
        else
            pending.editOriginalComponents().setEmbeds(eb.build()).queue();
    }

    @Override
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
        return Arrays.asList(
                new ItemComponent[]{
                        getButton("Add"),
                        getButton("Remove"),
                        getButton("Edit"),
                        getButton("List")
                }
        );
    }

    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setDescription("Receive notifications for various events to your liking! Click on a button to get started.");

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runModalInteraction(@NonNull ModalInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = event.deferEdit().complete();
        long userId = event.getUser().getIdLong();
        List<ItemComponent> buttons = new ArrayList<>();
        UserSession session = userSessionHashMap.get(userId);

        // Convert the input to a Number
        Number input;
        try {
            input = NumberFormat.getInstance().parse(event.getValues().get(0).getAsString());
            session.value = input;
        } catch (ParseException e) {
            return;
        }

        configureDefaultEmbedBuilder(eb);
        buttons.add(getButton("Yes"));
        buttons.add(getButton("No"));

        eb.setDescription("Do you want to make this notification recurring?");

        pending.editOriginalEmbeds(eb.build()).setActionRow(buttons).queue();
    }

    private void addButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb, @NonNull List<ItemComponent> buttons) {
        // Not subscribed list
        List<TrackedData<?>> notSubscribed = NotifyHandler.trackedData.stream().filter(
                data -> !NotifyHandler.getSubscribedData(event.getUser().getIdLong()).contains(data)
        ).toList();

        if(notSubscribed.isEmpty()) {
            eb.setDescription("You are subscribed to all data. Run the command again with </notify:" + commandId + "> to unsubscribe from data.");
            return;
        } else {
            for(TrackedData<?> data: notSubscribed) {
                buttons.add(getButton(data.getName().replace(" ", "")));
            }
        }

        eb.setDescription("Which data would you like to subscribe to?");

        // TODO: Add support for multiple conditions in one tracked data
    }

    private void removeButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb, @NonNull List<ItemComponent> buttons) {
        List<TrackedData<?>> subscribed = NotifyHandler.trackedData.stream().filter(
                data -> NotifyHandler.getSubscribedData(event.getUser().getIdLong()).contains(data)
        ).toList();

        if(subscribed.isEmpty()) {
            eb.setDescription("You are not subscribed to any data. Run the command again with </notify:" + commandId + "> to subscribe to data.");
            return;
        }

        for(TrackedData<?> data: subscribed) {
            buttons.add(getButton(data.getName().replace(" ", "")));
        }

        eb.setDescription("Which data would you like to unsubscribe from?");
    }

    private void editButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb, @NonNull List<ItemComponent> buttons) {
        List<TrackedData<?>> subscribed = NotifyHandler.trackedData.stream().filter(
                data -> NotifyHandler.getSubscribedData(event.getUser().getIdLong()).contains(data)
        ).toList();

        eb.setDescription("Which data would you like to edit?");

        for(TrackedData<?> data: subscribed) {
            buttons.add(getButton(data.getName().replace(" ", "")));
        }

        if(subscribed.isEmpty()) {
            eb.setDescription("You are not subscribed to any data. Run the command again with </notify:" + commandId + "> to subscribe to data.");
        }
    }

    private void listButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) {
        eb.setDescription("List of the available data you can subscribe to and the current status of your subscription. (Subscribed/Not Subscribed)");

        for(TrackedData<?> data: NotifyHandler.trackedData) {
            if(NotifyHandler.getSubscribedData(event.getUser().getIdLong()).contains(data)) {
                List<Condition<?>> conditions = NotifyHandler.getConditionsForDataFromUser(event.getUser().getIdLong(), data);
                StringBuilder fieldValueText = new StringBuilder();

                for(Condition<?> condition: conditions) {
                    fieldValueText.append("├─ ").append(condition.getFormattedCondition(data)).append("\n");
                }

                int lastIndexOf = fieldValueText.lastIndexOf("├");
                if(lastIndexOf != -1) {
                    fieldValueText.replace(lastIndexOf, lastIndexOf + 1, "└");
                }

                eb.addField(data.getName() + ": Subscribed",
                        fieldValueText.toString(),
                        true
                );
            } else {
                eb.addField(data.getName(), "Not Subscribed", true);
            }
        }
    }

    private void afterYesOrNoButtonAction(ButtonInteractionEvent event, EmbedBuilder eb, List<ItemComponent> buttons) {
        UserSession session = userSessionHashMap.get(event.getUser().getIdLong());
        eb.setDescription(
                "A notification will trigger if " +
                        session.secondClick.getLabel() + " is **" + getConditionFromButton(session.thirdClick).name +
                        "** " + session.value + session.trackedData.getUnitSymbol() +  " and " +
                        (session.makeRecurring ? "is" : "isn't") + " recurring. Confirm or cancel."
        );

        buttons.add(getButton("Confirm"));
        buttons.add(getButton("Cancel"));
    }

    private void confirmButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) {
        UserSession session = userSessionHashMap.get(event.getUser().getIdLong());
        Condition<?> condition = new Condition<>(
                getConditionFromButton(session.thirdClick),
                session.value,
                session.makeRecurring
        );

        if(session.firstClick.getLabel().equals("Add")) {
            eb.setDescription(
                    "Notification successfully created! You can run the command again with </notify:" + commandId + "> to create another notification or view your current subscribed notifications."
            );

            NotifyHandler.addUserWithCondition(
                    event.getUser().getIdLong(),
                    getDataFromButton(session.secondClick),
                    condition
            );
        } else if(session.firstClick.getLabel().equals("Remove")) {
            eb.setDescription(
                    "Notification successfully removed! You can run the command again with </notify:" + commandId + "> to remove, edit, create, or view your current subscribed notifications."
            );

            NotifyHandler.removeUserWithCondition(
                    event.getUser().getIdLong(),
                    getDataFromButton(session.secondClick),
                    condition
            );
        } else if(session.firstClick.getLabel().equals("Edit")) {
            eb.setDescription(
                    "Notification successfully edited! You can run the command again with </notify:" + commandId + "> to remove, edit, create, or view your current subscribed notifications."
            );

            NotifyHandler.editUserCondition(
                    event.getUser().getIdLong(),
                    session.trackedData,
                    session.condition,
                    condition
            );
        }
    }

    private void cancelButtonAction(@NonNull EmbedBuilder eb) {
        eb.setDescription("Notification creation cancelled. You can run the command again with </notify:" + commandId + "> to create a notification.");
    }

    private Modal promptModalAfterConditionSelection(@NonNull Condition.ComparisonOperator condition) {
        TextInput amount = TextInput.create(
                "notify_amount",
                "Enter the amount you want to be notified at.",
                TextInputStyle.SHORT
        ).build();

        return Modal.create(
                "notify_amount_modal",
                "Enter the amount you want to be notified at"
        ).addActionRows(
                ActionRow.of(amount)
        ).build();
    }

    private void configureDefaultEmbedBuilder(@NonNull EmbedBuilder eb) {
        eb.setTitle(EMBED_TITLE);
        eb.setColor(Color.WHITE);
    }

    private TrackedData<?> getDataFromButton(@NonNull Button button) {
        return NotifyHandler.trackedData.stream().filter(
                data -> data.getName().replace(" ", "").equalsIgnoreCase(button.getLabel().replace(" ", ""))
        ).findFirst().orElseThrow();
    }

    private Condition.ComparisonOperator getConditionFromButton(@NonNull Button button) {
        switch(button.getId()) {
            case "notify_greater_than" -> {
                return Condition.ComparisonOperator.GREATER_THAN;
            }
            case "notify_less_than" -> {
                return Condition.ComparisonOperator.LESS_THAN;
            }
            case "notify_equal_to" -> {
                return Condition.ComparisonOperator.EQUALS;
            }
            case "notify_not_equal_to" -> {
                return Condition.ComparisonOperator.NOT_EQUALS;
            }
            default -> throw new RuntimeException("Invalid button id: " + button.getId());
        }
    }
}
