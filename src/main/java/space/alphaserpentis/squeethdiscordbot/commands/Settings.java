package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;

import java.util.List;

public class Settings extends BotCommand {

    public Settings() {
        name = "settings";
        description = "Configure the server settings";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        List<OptionMapping> optionMappingList = event.getOptions();

        if(optionMappingList.size() != 2)
            return defaultResponse();

        eb.setTitle("Server Settings");

        if(event.getGuild() == null) {
            eb.setDescription("Nice try.");
            return eb.build();
        }

        if(verifyServerPerms(event.getMember())) {
            switch (optionMappingList.get(0).getAsString().toLowerCase()) {
                case "ephemeral" -> setChangeOnlyEphemeral(event.getGuild().getIdLong(), optionMappingList.get(1).getAsString(), eb);
            }
        } else {
            eb.setDescription("You do not have `Manage Server` permissions");
        }

        // You remembered to add the case to the list of settings in defaultResponse, right?

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "name", "Name of the command ")
                .addOption(OptionType.STRING, "value", "Value of the setting to configure")
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "name", "Name of the command ")
                .addOption(OptionType.STRING, "value", "Value of the setting to configure")
                .complete();

        System.out.println("[Settings] Updating command");
    }

    private MessageEmbed defaultResponse() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Server Settings");
        eb.setDescription("Configure how the bot works in your server!\n" +
                "\n" +
                "To use this command, type in the name of the setting under the `name` field when using the command. Next, input the value that the setting requires you to input under the `value` field when using the command." +
                "\n\n" +
                "**Settings List Below:**");

        eb.addField("ephemeral", "`true` or `false`\n" +
                "\n" +
                "Sets the server to use ephemeral messages or not. If enabled, messages can only be seen by the user who ran the command, otherwise it is publicly visible to everyone.", false);

        return eb.build();
    }

    private void setChangeOnlyEphemeral(Long id, String input, EmbedBuilder eb) {
        ServerDataHandler.serverDataHashMap.get(id).setOnlyEphemeral(Boolean.parseBoolean(input));

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Setting your server to **" + (!Boolean.parseBoolean(input) ? "publicly " : "privately ") + "**display commands being ran");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean verifyServerPerms(Member member) {
        return member.hasPermission(
                Permission.MANAGE_SERVER
        );
    }
}
