package dev.onlydarkness.utils;

import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommandManager {
    private final Map<String, ISlashCommand> commands = new HashMap<>();

    public void register(ISlashCommand command) {
        commands.put(command.getName(), command);
    }

    public void handle(SlashCommandInteractionEvent event){
        String commandName = event.getName();

        if (commands.containsKey(commandName)){
            commands.get(commandName).execute(event);
        } else {
            event.reply("Slash command not found!").queue();
        }
    }

    public List<SlashCommandData> getCommandDataList() {
        List<SlashCommandData> dataList = new ArrayList<>();

        for (ISlashCommand cmd : commands.values()) {
            SlashCommandData data = Commands.slash(cmd.getName(), cmd.getDescription());

            if (cmd.getOptions() != null && !cmd.getOptions().isEmpty()) {
                data.addOptions(cmd.getOptions());
            }

            dataList.add(data);
        }
        return dataList;
    }
}
