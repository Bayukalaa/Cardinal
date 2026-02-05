package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;

public class ExecCommand implements ISlashCommand {

    private final ModuleManager moduleManager;
    private final String allowedChannelId;

    public ExecCommand(ModuleManager moduleManager, String allowedChannelId) {
        this.moduleManager = moduleManager;
        this.allowedChannelId = allowedChannelId;
    }

    @Override
    public String getName() {
        return "exec";
    }

    @Override
    public String getDescription() {
        return "Executes a system console command.";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(
                new OptionData(OptionType.STRING, "command", "The command to run (e.g. stop, help)", true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        if (!event.getChannel().getId().equals(allowedChannelId)) {
            event.reply(":x: You cannot use system commands in this channel.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        OptionMapping cmdOption = event.getOption("command");
        if (cmdOption == null) return;

        String commandToRun = cmdOption.getAsString();


        event.reply(":gear: Executing: `" + commandToRun + "`").queue();


        if (moduleManager != null) {
            System.out.println("[DiscordBot] Remote Command: " + commandToRun);
            moduleManager.dispatchCommand(commandToRun);
        }
    }
}