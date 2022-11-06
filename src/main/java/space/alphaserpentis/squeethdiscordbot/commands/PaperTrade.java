package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.IPaperTrade;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradeAccount;
import space.alphaserpentis.squeethdiscordbot.handler.games.PaperTradingHandler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class PaperTrade extends ButtonCommand<MessageEmbed> implements ModalCommand {

    enum ButtonStates {
        SHOW_BUY_SELL,
        SHOW_ASSETS,
        SHOW_CONFIRM_AND_CANCEL,
        SHOW_HISTORY
    }

    protected static class PaperTradeSession {
        final ButtonStates originalState;
        ButtonStates buttonState;
        SessionData<Object> sessionData;

        protected PaperTradeSession(ButtonStates originalState) {
            this.originalState = originalState;
        }
    }

    protected record SessionData<T>(@Nonnull ArrayList<T> data) {
        public void addData(@Nonnull T newData) {
            data.add(newData);
        }
    }

    public PaperTrade() {
        super(new BotCommandOptions(
            "paper",
            "Paper trading game",
            true,
            true,
            TypeOfEphemeral.DEFAULT // might change later
        ));

        buttonHashMap.putAll(
                new HashMap<>() {{
                    put("Confirm_Open_Account", Button.danger("paper_confirm_open_account", "Confirm"));
                    put("Confirm_Position", Button.primary("paper_confirm_position", "Confirm"));
                    put("Cancel", Button.primary("paper_cancel", "Cancel"));
                    put("Buy", Button.primary("paper_buy", "Buy"));
                    put("Sell", Button.primary("paper_sell", "Sell"));
                    put("USDC", Button.primary("paper_usdc", "USDC"));
                    put("ETH", Button.primary("paper_eth", "ETH"));
                    put("LONG_OSQTH", Button.primary("paper_long_osqth", "oSQTH"));
                    put("CRAB", Button.primary("paper_crab", "Crab"));
                    put("Previous_History", Button.primary("paper_previous_history", "Previous").asDisabled());
                    put("Page_History", Button.secondary("paper_page_history", "1/?").asDisabled());
                    put("Next_History", Button.primary("paper_next_history", "Next"));
                }}
        );
    }

    private static final String defaultTitle = "Paper Trading Game!";
    private static final String defaultDisclaimer = "Trades made are NOT real and have no real-life value whether physically or digitally.";
    private static final HashMap<Long, PaperTradeSession> sessions = new HashMap<>();

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        switch(event.getSubcommandName()) {
            case "about" -> aboutPage(eb);
            case "open" -> openAccountPage(userId, eb);
            case "view" -> viewAccountPage(event, eb);
            case "trade" -> tradePositionsPage(event, eb);
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData about = new SubcommandData("about", "What is Paper Trading?");
        SubcommandData trade = new SubcommandData("trade", "Trade your positions");
        SubcommandGroupData account = new SubcommandGroupData("account", "Account-related commands")
                .addSubcommands(
                        new SubcommandData("open", "Open (or reset) a paper trading account"),
                        new SubcommandData("view", "View PNL and current positions"),
                        new SubcommandData("history", "View historical trades")
                );
        Command cmd = jda.upsertCommand(name, description)
                .addSubcommandGroups(account)
                .addSubcommands(about, trade)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = null;
        Collection<ItemComponent> buttons = new ArrayList<>();
        long userId = event.getUser().getIdLong();
        try {
            switch(event.getButton().getId()) {
                case "paper_confirm_open_account" -> {
                    pending = event.deferEdit().complete();
                    confirmOpenAccountButtonAction(event, eb);
                }
                case "paper_cancel" -> {
                    pending = event.deferEdit().complete();
                    cancelButtonAction(eb);
                    sessions.remove(event.getUser().getIdLong());
                }
                case "paper_buy" -> {
                    pending = event.deferEdit().complete();
                    buyButtonAction(event, eb);
                    buttons.add(getButton("ETH"));
                    buttons.add(getButton("LONG_OSQTH"));
                    buttons.add(getButton("CRAB"));
                    sessions.get(userId).sessionData = new SessionData<>(new ArrayList<>(){{
                        add(true);
                    }});
                }
                case "paper_sell" -> {
                    pending = event.deferEdit().complete();
                    sellButtonAction(event, eb);
                    buttons.add(getButton("ETH"));
                    buttons.add(getButton("LONG_OSQTH"));
                    buttons.add(getButton("CRAB"));
                    sessions.get(userId).sessionData = new SessionData<>(new ArrayList<>(){{
                        add(false);
                    }});
                }
                case "paper_usdc", "paper_eth", "paper_long_osqth", "paper_crab" -> {
                    sessions.get(userId).sessionData.data.add(event.getButton().getLabel());
                    TextInput amount = TextInput.create("paper_amount_ti", "Amount to " + ((boolean) sessions.get(userId).sessionData.data.get(0) ? "buy" : "sell"), TextInputStyle.SHORT).build();
                    Modal modal = Modal.create("paper_asset_modal", "How much?")
                            .addActionRows(ActionRow.of(amount))
                            .build();

                    event.replyModal(modal).complete();
                    return;
                }
                case "paper_confirm_position" -> {
                    pending = event.deferEdit().complete();
                    confirmPositionButtonAction(event, eb);
                }
            }

            if(pending != null) {
                if(buttons.isEmpty())
                    pending.editOriginalComponents().setEmbeds(eb.build()).complete();
                else
                    pending.editOriginalComponents().setActionRow(buttons).setEmbeds(eb.build()).complete();
            }
        } catch(Exception e) {
            sessions.remove(userId);
            if(pending != null)
                pending.editOriginalComponents().setEmbeds(handleError(e)).complete();
            else
                event.deferEdit().setEmbeds(handleError(e)).complete();
        }
    }

    @Override
    public void runModalInteraction(@Nonnull ModalInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = event.deferEdit().complete();
        Collection<ItemComponent> buttons = new ArrayList<>();
        long userId = event.getUser().getIdLong();
        PaperTradeSession session = sessions.get(userId);

        session.sessionData.addData(event.getValues().get(0).getAsString());

        switch(session.originalState) {
            case SHOW_BUY_SELL -> {
                afterTradeAmountInputPage(event.getGuild().getIdLong(), userId, session.sessionData, eb);
                buttons.add(getButton("Confirm_Position"));
                buttons.add(getButton("Cancel"));
            }
        }

        if(buttons.isEmpty())
            pending.editOriginalComponents().setEmbeds(eb.build()).complete();
        else
            pending.editOriginalComponents().setActionRow(buttons).setEmbeds(eb.build()).complete();
    }

    @Nonnull
    @Override
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        PaperTradeSession session = sessions.get(event.getUser().getIdLong());
        if(session == null)
            return List.of();
        ButtonStates buttonState = session.buttonState;

        if(buttonState == ButtonStates.SHOW_CONFIRM_AND_CANCEL) {
            return Arrays.asList(
                    new ItemComponent[]{
                            getButton("Confirm_Open_Account"), getButton("Cancel")
                    }
            );
        } else if(buttonState == ButtonStates.SHOW_BUY_SELL) {
            return Arrays.asList(
                    new ItemComponent[]{
                        getButton("Buy"),
                        getButton("Sell")
                    }
            );
        } else if(buttonState == ButtonStates.SHOW_ASSETS) {
            return Arrays.asList(
                    new ItemComponent[]{
//                        getButton("USDC"),
                        getButton("ETH"),
                        getButton("LONG_OSQTH"),
                        getButton("CRAB")
                    }
            );
        } else if(buttonState == ButtonStates.SHOW_HISTORY) {
            return Arrays.asList(
                    new ItemComponent[]{
                        getButton("Previous_History"),
                        getButton("Page_History"),
                        getButton("Next_History")
                    }
            );
        }

        return List.of();
    }

    // BUTTON INTERACTIONS
    private void confirmOpenAccountButtonAction(@Nonnull ButtonInteractionEvent event, @Nonnull EmbedBuilder eb) throws Exception {
        eb.setTitle(defaultTitle);
        eb.setDescription("You've opened a new account! You will start with $10,000.00 USDC. Good luck and have fun!");
        eb.setColor(Color.CYAN);
        eb.setFooter(defaultDisclaimer);

        Exception response = PaperTradingHandler.openNewAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());

        if(response != null) {
            throw response;
        }
        sessions.remove(event.getUser().getIdLong());
    }

    private void confirmPositionButtonAction(@Nonnull ButtonInteractionEvent event, @Nonnull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());
        SessionData<Object> sessionData = sessions.get(event.getUser().getIdLong()).sessionData;

        eb = account.trade(
                (boolean) sessionData.data.get(0) ? IPaperTrade.Action.BUY : IPaperTrade.Action.SELL,
                buttonLabelToAsset((String) sessionData.data.get(1)),
                Double.parseDouble((String) sessionData.data.get(2)),
                eb
        );

        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.CYAN);

        sessions.remove(event.getUser().getIdLong());
    }

    private void cancelButtonAction(@Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("Action cancelled");
        eb.setColor(Color.CYAN);
    }

    private void buyButtonAction(@Nonnull ButtonInteractionEvent event, @Nonnull EmbedBuilder eb) {
        eb.setColor(Color.GREEN);
        afterBuyOrSellPage(true, eb);
        sessions.get(event.getUser().getIdLong()).buttonState = ButtonStates.SHOW_ASSETS;
    }

    private void sellButtonAction(@Nonnull ButtonInteractionEvent event, @Nonnull EmbedBuilder eb) {
        eb.setColor(Color.RED);
        afterBuyOrSellPage(false, eb);
        sessions.get(event.getUser().getIdLong()).buttonState = ButtonStates.SHOW_ASSETS;
    }

    // PAGES
    private static void aboutPage(@Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("This is a trading game that trades with equivalent assets of ETH, oSQTH, Crab v2, and USDC! " + defaultDisclaimer);
        eb.addField("How do I play?", "Simply run `/paper account open` to open a new account", false);
        eb.addField("How do I trade?", "Simply run `/paper trade` and on moves to do", false);
    }

    private static void openAccountPage(long userId, @Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setColor(Color.RED);
        eb.setDescription("Are you sure you want to open/reset your account?\n\nIf you're new, press 'Confirm'");
        sessions.put(userId, new PaperTradeSession(ButtonStates.SHOW_CONFIRM_AND_CANCEL));
        sessions.get(userId).buttonState = ButtonStates.SHOW_CONFIRM_AND_CANCEL;
    }

    private static void viewAccountPage(@Nonnull SlashCommandInteractionEvent event, @Nonnull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());
        NumberFormat instance = NumberFormat.getInstance();

        if(account == null) {
            noAccountFoundResponse(eb);
        } else {
            eb.setTitle(defaultTitle);
            eb.setFooter(defaultDisclaimer);
            eb.setColor(Color.CYAN);
            eb.setThumbnail("https://media.tenor.com/b7jgsT3ctlwAAAAC/when-the-money-fast-money.gif");
            eb.setDescription("View your balances and PNL");
            eb.addField(
                    "Portfolio Value",
                    "$" + instance.format(account.portfolioValueInUsd()),
                    false
            );
            eb.addField(
                    "PNL",
                    "$" + instance.format(account.portfolioValueInUsd() - 10000),
                    false
            );
            eb.addField(
                    "USDC Holdings",
                    "$" + instance.format(account.balanceInUsd(IPaperTrade.Asset.USDC)),
                    false
            );
            eb.addField(
                    "ETH Holdings",
                    instance.format(account.balance.getOrDefault(IPaperTrade.Asset.ETH,0d))
                            + " Ξ ($" + instance.format(account.balanceInUsd(IPaperTrade.Asset.ETH)) + ")",
                    false
            );
            eb.addField(
                    "Crab Holdings",
                    instance.format(account.balance.getOrDefault(IPaperTrade.Asset.CRAB, 0d))
                            + " Crab ($" + instance.format(account.balanceInUsd(IPaperTrade.Asset.CRAB)) + ")",
                    false
            );
            eb.addField(
                    "oSQTH Holdings",
                    instance.format(account.balance.getOrDefault(IPaperTrade.Asset.LONG_OSQTH, 0d))
                            + " oSQTH ($" + instance.format(account.balanceInUsd(IPaperTrade.Asset.LONG_OSQTH)) + ")",
                    false
            );
        }
    }

    private static void tradePositionsPage(@Nonnull SlashCommandInteractionEvent event, @Nonnull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());

        if(account == null) {
            noAccountFoundResponse(eb);
        } else {
            eb.setTitle(defaultTitle);
            eb.setFooter(defaultDisclaimer);
            eb.setColor(Color.CYAN);
            eb.setDescription("Manage your positions here");
            sessions.put(event.getUser().getIdLong(), new PaperTradeSession(ButtonStates.SHOW_BUY_SELL));
            sessions.get(event.getUser().getIdLong()).buttonState = ButtonStates.SHOW_BUY_SELL;
        }
    }

    private static void afterBuyOrSellPage(boolean isBuying, @Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setDescription("Choose the asset you want to " + (isBuying ? "buy": "sell"));
    }

    private static void afterTradeAmountInputPage(long serverId, long userId, @Nonnull SessionData<Object> sessionData, @Nonnull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(serverId, userId);

        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        eb.setDescription(
                "Confirm you want to " + ((boolean) sessionData.data.get(0) ? "buy " : "sell ")
                        + sessionData.data.get(2) + " " + sessionData.data.get(1) + " for $"
                        + NumberFormat.getInstance().format(
                                Double.parseDouble(
                                        (String) sessionData.data.get(2)) * account.assetPriceInUsd(
                                                buttonLabelToAsset((String) sessionData.data.get(1)
                                                )
                                )
                ) + "?"
        );
        eb.addField("Quantity", sessionData.data.get(2) + " " + sessionData.data.get(1), false);

    }

    private static void noAccountFoundResponse(@Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        eb.setDescription("You haven't registered an account yet! Run `/paper account open` to open a new account!");
    }

    // Misc
    private static IPaperTrade.Asset buttonLabelToAsset(@Nonnull String label) {
        switch(label) {
            case "USDC" -> {
                return IPaperTrade.Asset.USDC;
            }
            case "Crab" -> {
                return IPaperTrade.Asset.CRAB;
            }
            case "oSQTH" -> {
                return IPaperTrade.Asset.LONG_OSQTH;
            }
            case "ETH" -> {
                return IPaperTrade.Asset.ETH;
            }
            default -> throw new UnsupportedOperationException("Invalid label");
        }
    }
}
