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
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.IPaperTrade;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradeAccount;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.games.PaperTradingHandler;

import io.reactivex.annotations.NonNull;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PaperTrade extends ButtonCommand<MessageEmbed> implements ModalCommand {

    enum ButtonStates {
        SHOW_BUY_SELL,
        SHOW_ASSETS,
        SHOW_CONFIRM_AND_CANCEL,
        SHOW_HISTORY
    }

    protected static class PaperTradeSession {
        final ButtonStates originalState;
        long messageId;
        ButtonStates buttonState;
        ArrayList<Object> sessionData;

        protected PaperTradeSession(ButtonStates originalState) {
            this.originalState = this.buttonState = originalState;
        }
    }

//    protected record SessionData<T>(@NonNull ArrayList<T> data) {
//        public void addData(@NonNull T newData) {
//            data.add(newData);
//        }
//    }

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
//                    put("USDC", Button.primary("paper_usdc", "USDC"));
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

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        sessions.remove(event.getUser().getIdLong());

        switch(event.getSubcommandName()) {
            case "about" -> aboutPage(eb);
            case "open" -> openAccountPage(userId, eb);
            case "view" -> viewAccountPage(event, eb);
            case "trade" -> tradePositionsPage(event, eb);
            case "history" -> historyPage(event, eb);
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
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
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = null;
        Collection<ItemComponent> buttons = new ArrayList<>();
        long userId = event.getUser().getIdLong();
        PaperTradeSession session = sessions.get(userId);

        if(session == null) {
            invalidSessionFoundResponse(eb);
            event.deferEdit().complete().editOriginalComponents().setEmbeds(eb.build()).complete();
            return;
        }

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
                    session.sessionData = new ArrayList<>(){{
                            add(true);
                    }};
                }
                case "paper_sell" -> {
                    pending = event.deferEdit().complete();
                    sellButtonAction(event, eb);
                    buttons.add(getButton("ETH"));
                    buttons.add(getButton("LONG_OSQTH"));
                    buttons.add(getButton("CRAB"));
                    session.sessionData = new ArrayList<>(){{
                        add(false);
                    }};
                }
                case "paper_eth", "paper_long_osqth", "paper_crab" -> {
                    ArrayList<Object> sessionData = session.sessionData;

                    if(sessionData.size() < 2)
                        sessionData.add(event.getButton().getLabel());
                    else
                        sessionData.set(1, event.getButton().getLabel());
                    promptModalForAmount(sessionData, event.getInteraction(), event.getGuild().getIdLong(), userId);
                    return;
                }
                case "paper_confirm_position" -> {
                    pending = event.deferEdit().complete();
                    confirmPositionButtonAction(event, eb);
                }
                case "paper_previous_history" -> {
                    if(!((boolean) session.sessionData.get(2))) {
                        session.sessionData.set(2, true);
                        pending = event.deferEdit().complete();
                        Button prev, page, next;

                        session.sessionData.set(1, (int) session.sessionData.get(1) - 1);
                        prev = getButton("Previous_History").asEnabled();
                        page = getButton("Page_History").withLabel(session.sessionData.get(1) + "/" + session.sessionData.get(0));
                        next = getButton("Next_History").asEnabled();

                        if((int) session.sessionData.get(1) == 1)
                            prev = prev.asDisabled();

                        historyPage(
                                (int) session.sessionData.get(1),
                                PaperTradingHandler.getAccount(
                                        event.getGuild().getIdLong(),
                                        event.getUser().getIdLong()
                                ),
                                eb
                        );

                        buttons.add(prev);
                        buttons.add(page);
                        buttons.add(next);
                        session.sessionData.set(2, false);
                    }
                }
                case "paper_next_history" -> {
                    if(!((boolean) session.sessionData.get(2))) {
                        session.sessionData.set(2, true);
                        pending = event.deferEdit().complete();
                        Button prev, page, next;

                        session.sessionData.set(1, (int) session.sessionData.get(1) + 1);
                        prev = getButton("Previous_History").asEnabled();
                        page = getButton("Page_History").withLabel(session.sessionData.get(1) + "/" + session.sessionData.get(0));
                        next = getButton("Next_History").asEnabled();

                        if ((int) session.sessionData.get(1) == (int) session.sessionData.get(0))
                            next = next.asDisabled();

                        historyPage(
                                (int) session.sessionData.get(1),
                                PaperTradingHandler.getAccount(
                                        event.getGuild().getIdLong(),
                                        event.getUser().getIdLong()
                                ),
                                eb
                        );

                        buttons.add(prev);
                        buttons.add(page);
                        buttons.add(next);
                        session.sessionData.set(2, false);
                    }
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
    public void runModalInteraction(@NonNull ModalInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = event.deferEdit().complete();
        long userId = event.getUser().getIdLong();

        try {
            Collection<ItemComponent> buttons = new ArrayList<>();
            PaperTradeSession session = sessions.get(userId);

            session.sessionData.add(event.getValues().get(0).getAsString());

            switch(session.originalState) {
                case SHOW_BUY_SELL -> {
                    try {
                        afterTradeAmountInputPage(session.sessionData, eb);
                        buttons.add(getButton("Confirm_Position"));
                        buttons.add(getButton("Cancel"));
                    } catch(RuntimeException ignored) {
                        invalidInputResponse(eb);
                        sessions.remove(userId);
                    }
                }
            }

            if(buttons.isEmpty())
                pending.editOriginalComponents().setEmbeds(eb.build()).complete();
            else
                pending.editOriginalComponents().setActionRow(buttons).setEmbeds(eb.build()).complete();
        } catch (Exception e) {
            pending.editOriginalComponents().setEmbeds(BotCommand.handleError(e)).complete();
            sessions.remove(userId);
        }
    }

    @NonNull
    @Override
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
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
            Button next = getButton("Next_History");

            if((int) session.sessionData.get(1) == (int) session.sessionData.get(0))
                next = next.asDisabled();

            return Arrays.asList(
                    new ItemComponent[]{
                        getButton("Previous_History"),
                        getButton("Page_History").withLabel("1/" + (int) session.sessionData.get(0)),
                        next
                    }
            );
        }

        return List.of();
    }

    // BUTTON INTERACTIONS
    private void confirmOpenAccountButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) throws Exception {
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

    private void confirmPositionButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());
        ArrayList<Object> sessionData = sessions.get(event.getUser().getIdLong()).sessionData;

        eb = account.trade(
                (boolean) sessionData.get(0) ? IPaperTrade.Action.BUY : IPaperTrade.Action.SELL,
                buttonLabelToAsset((String) sessionData.get(1)),
                Double.parseDouble((String) sessionData.get(2)),
                eb
        );

        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.CYAN);

        sessions.remove(event.getUser().getIdLong());
    }

    private void cancelButtonAction(@NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("Action cancelled");
        eb.setColor(Color.CYAN);
    }

    private void buyButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) {
        eb.setColor(Color.GREEN);
        afterBuyOrSellPage(true, eb);
        sessions.get(event.getUser().getIdLong()).buttonState = ButtonStates.SHOW_ASSETS;
    }

    private void sellButtonAction(@NonNull ButtonInteractionEvent event, @NonNull EmbedBuilder eb) {
        eb.setColor(Color.RED);
        afterBuyOrSellPage(false, eb);
        sessions.get(event.getUser().getIdLong()).buttonState = ButtonStates.SHOW_ASSETS;
    }

    // PAGES
    private static void aboutPage(@NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("This is a trading game that trades with equivalent assets of ETH, oSQTH, Crab v2, and USDC! " + defaultDisclaimer);
        eb.addField("How do I play?", "Simply run `/paper account open` to open a new account", false);
        eb.addField("How do I trade?", "Simply run `/paper trade` and on moves to do", false);
    }

    private static void openAccountPage(long userId, @NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setColor(Color.RED);
        eb.setDescription("Are you sure you want to open/reset your account?\n\nIf you're new, press 'Confirm'");
        sessions.put(userId, new PaperTradeSession(ButtonStates.SHOW_CONFIRM_AND_CANCEL));
        sessions.get(userId).buttonState = ButtonStates.SHOW_CONFIRM_AND_CANCEL;
    }

    private static void viewAccountPage(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());
        NumberFormat instance = NumberFormat.getInstance();

        if(account == null) {
            noAccountFoundResponse(eb);
        } else {
            double pnl = account.portfolioValueInUsd() - 10000;

            eb.setTitle(defaultTitle);
            eb.setFooter(defaultDisclaimer);
            eb.setColor(Color.CYAN);
            if(pnl == 0) {
                eb.setThumbnail("https://media.tenor.com/WeKxpDGmElMAAAAd/luluwpp.gif");
            } else if(pnl > 0) {
                eb.setThumbnail("https://media.tenor.com/b7jgsT3ctlwAAAAC/when-the-money-fast-money.gif");
            } else {
                eb.setThumbnail("https://media.tenor.com/ShzdJcrguswAAAAC/burn-elmo.gif");
            }
            eb.setDescription("View your balances and PNL");
            eb.addField(
                    "Portfolio Value",
                    "$" + instance.format(account.portfolioValueInUsd()),
                    false
            );
            eb.addField(
                    "PNL",
                    "$" + instance.format(pnl),
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
                    "oSQTH Holdings",
                    instance.format(account.balance.getOrDefault(IPaperTrade.Asset.OSQTH, 0d))
                            + " oSQTH ($" + instance.format(account.balanceInUsd(IPaperTrade.Asset.OSQTH)) + ")",
                    false
            );
            eb.addField(
                    "Crab Holdings",
                    instance.format(account.balance.getOrDefault(IPaperTrade.Asset.CRAB, 0d))
                            + " Crab ($" + instance.format(account.balanceInUsd(IPaperTrade.Asset.CRAB)) + ")",
                    false
            );
        }
    }

    private static void tradePositionsPage(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder eb) {
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

    private static void afterBuyOrSellPage(boolean isBuying, @NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setDescription("Choose the asset you want to " + (isBuying ? "buy": "sell"));
    }

    private static void afterTradeAmountInputPage(@NonNull ArrayList<Object> sessionData, @NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        try {
            PriceData priceData = PositionsDataHandler.getPriceData(
                    new PriceData.Prices[]{
                            PaperTradeAccount.assetToPrices(
                                    buttonLabelToAsset((String) sessionData.get(1))
                            ),
                            PriceData.Prices.ETHUSD
                    });

            eb.setDescription(
                    "Confirm you want to " + ((boolean) sessionData.get(0) ? "buy " : "sell ")
                            + sessionData.get(2) + " " + sessionData.get(1) + " for $"
                            + NumberFormat.getInstance().format(
                                    Double.parseDouble((String) sessionData.get(2)) * PaperTradeAccount.assetPriceInUsd(
                                            buttonLabelToAsset(
                                                    (String) sessionData.get(1)
                                            ), priceData
                                    )
                    ) + "?"
            );
        } catch (ExecutionException | InterruptedException | IOException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
        eb.addField("Quantity", sessionData.get(2) + " " + sessionData.get(1), false);
    }

    private static void historyPage(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder eb) {
        PaperTradeAccount account = PaperTradingHandler.getAccount(event.getGuild().getIdLong(), event.getUser().getIdLong());
        ArrayList<PaperTradeAccount.FinalizedTrade> trades;

        if(account == null) {
            noAccountFoundResponse(eb);
        } else {
            trades = account.history;

            for(int i = 0; i < 15 && i < trades.size(); i++) {
                eb.addField("Trade #" + (i + 1), String.valueOf(trades.get(trades.size() - (1 + i))),false);
            }

            eb.setTitle(defaultTitle);
            eb.setFooter(defaultDisclaimer);
            eb.setColor(Color.CYAN);

            PaperTradeSession newSession = new PaperTradeSession(ButtonStates.SHOW_HISTORY);
            newSession.sessionData = new ArrayList<>() {{
                add((int) Math.ceil(trades.size()/15d));
                add(1);
                add(false);
            }};
            sessions.put(event.getUser().getIdLong(), newSession);
        }
    }

    private static void historyPage(int page, @NonNull PaperTradeAccount account, @NonNull EmbedBuilder eb) {
        for(int i = (15 * (page - 1)); i < 15 + (15 * (page - 1)) && i < account.history.size(); i++) {
            eb.addField("Trade #" + (i + 1), String.valueOf(account.history.get(account.history.size() - (1 + i))),false);
        }

        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.CYAN);
    }

    // Responses

    private static void noAccountFoundResponse(@NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        eb.setDescription("You haven't registered an account yet! Run `/paper account open` to open a new account!");
    }

    private static void invalidSessionFoundResponse(@NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        eb.setDescription("Invalid session. Did you run a new command?");
    }

    private static void invalidInputResponse(@NonNull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setFooter(defaultDisclaimer);
        eb.setColor(Color.RED);
        eb.setDescription("**Error**: Input must be a numerical value!");
    }

    // Misc
    private static void promptModalForAmount(@NonNull ArrayList<Object> sessionData, @NonNull ComponentInteraction interaction, long serverId, long userId) {
        NumberFormat instance = NumberFormat.getInstance();
        TextInput amount = TextInput.create("paper_amount_ti", "Amount to " + ((boolean) sessionData.get(0) ? "buy" : "sell"), TextInputStyle.SHORT).build();
        Modal modal = Modal.create(
                        "paper_asset_modal", "How much? (Maximum is " + instance.format(
                                calculateMaxAmountToBuyOrSell(
                                        (boolean) sessionData.get(0),
                                        buttonLabelToAsset((String) sessionData.get(1)),
                                        Objects.requireNonNull(
                                                PaperTradingHandler.getAccount(serverId, userId)
                                        )
                                )) + ")"
                )
                .addActionRows(ActionRow.of(amount))
                .build();

        interaction.replyModal(modal).complete();
    }

    private static IPaperTrade.Asset buttonLabelToAsset(@NonNull String label) {
        switch(label) {
            case "USDC" -> {
                return IPaperTrade.Asset.USDC;
            }
            case "Crab" -> {
                return IPaperTrade.Asset.CRAB;
            }
            case "oSQTH" -> {
                return IPaperTrade.Asset.OSQTH;
            }
            case "ETH" -> {
                return IPaperTrade.Asset.ETH;
            }
            default -> throw new UnsupportedOperationException("Invalid label");
        }
    }

    private static double calculateMaxAmountToBuyOrSell(boolean isBuying, @NonNull IPaperTrade.Asset asset, @NonNull PaperTradeAccount account) {
        if(isBuying) {
            try {
                PriceData priceData = PositionsDataHandler.getPriceData(
                        new PriceData.Prices[]{
                                PaperTradeAccount.assetToPrices(
                                        asset
                                ),
                                PriceData.Prices.ETHUSD
                        });

                return account.balance.get(IPaperTrade.Asset.USDC)/PaperTradeAccount.assetPriceInUsd(asset, priceData);
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return account.balance.get(asset);
        }
    }

    private static boolean isSessionForHistory(@NonNull PaperTradeSession session) {
        return session.originalState.equals(ButtonStates.SHOW_HISTORY);
    }
}
