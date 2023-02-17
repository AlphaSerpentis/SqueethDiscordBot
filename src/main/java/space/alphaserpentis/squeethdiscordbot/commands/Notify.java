package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;

public class Notify extends BotCommand<MessageEmbed> {

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
    }
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, SlashCommandInteractionEvent event) {
        return null;
    }

    @Override
    public void updateCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }
}
