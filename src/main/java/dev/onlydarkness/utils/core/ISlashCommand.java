package dev.onlydarkness.utils.core;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public interface ISlashCommand {

    String getName();

    String getDescription();
    List<OptionData> getOptions();

    void execute(SlashCommandInteractionEvent event);
}
