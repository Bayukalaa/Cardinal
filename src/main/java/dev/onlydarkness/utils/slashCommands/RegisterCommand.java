package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class RegisterCommand implements ISlashCommand {

    private final ModuleManager moduleManager;

    public RegisterCommand(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public String getDescription() {
        return "Create a new account.";
    }

    @Override
    public List<OptionData> getOptions() {

        return List.of(
                new OptionData(OptionType.STRING, "username", "Your desired username", true),
                new OptionData(OptionType.STRING, "email", "Your email address", true),
                new OptionData(OptionType.STRING, "phone", "Your phone number", true),
                new OptionData(OptionType.STRING, "password", "Your secure password", true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        DatabaseManager db = moduleManager.getService(DatabaseManager.class);
        if (db == null) {
            event.reply("Database service is currently unavailable.").setEphemeral(true).queue();
            return;
        }


        String username = event.getOption("username").getAsString();
        String email = event.getOption("email").getAsString();
        String discord = event.getUser().getName();
        String phone = event.getOption("phone").getAsString();
        String password = event.getOption("password").getAsString();


        Map<String, Object> existingUser = db.getUser(username);
        if (existingUser != null) {
            event.reply(":warning: Username **" + username + "** is already taken!").setEphemeral(true).queue();
            return;
        }



        try {
            db.createUser(username, discord, email, phone, password);
        } catch (Exception e) {
            event.reply(":x: An error occurred during database registration.").setEphemeral(true).queue();
            e.printStackTrace();
            return;
        }

        Guild guild = event.getGuild();
        if (guild != null) {

            long roleId = 1466723379485605929L;
            Role role = guild.getRoleById(roleId);
            Member member = event.getMember();

            if (role != null && member != null) {

                guild.addRoleToMember(member, role).queue(
                        success -> System.out.println("[Register] Role added to " + username),
                        error -> System.err.println("[Register] Failed to add role: " + error.getMessage())
                );
            } else {
                System.err.println("[Register] Role or Member not found! (Check Role ID)");
            }
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Registration Successful");
        embed.setDescription("Account created successfully!");
        embed.addField("Username", username, true);
        embed.addField("Email", email, true);
        embed.addField("Phone", phone, true);


        embed.setColor(Color.GREEN);
        embed.setFooter("Cardinal System Database");


        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}