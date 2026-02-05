package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProfileCommand implements ISlashCommand {

    private final ModuleManager manager;

    public ProfileCommand(ModuleManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public String getDescription() {
        return "Displays user profile information.";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.singletonList(
                new OptionData(OptionType.STRING, "username", "The username to view (Optional)", false)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        DatabaseManager db = manager.getService(DatabaseManager.class);
        if (db == null) {
            event.reply("Database service is unavailable.").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("username");
        Map<String, Object> userData;

        if (userOption != null) {
            String targetUsername = userOption.getAsString();
            userData = db.getUser(targetUsername);

            if (userData == null) {
                event.reply(":x: User **" + targetUsername + "** not found in database.").setEphemeral(true).queue();
                return;
            }
        } else {
            String discordName = event.getUser().getName();
            userData = db.getUserByDiscord(discordName);

            if (userData == null) {
                event.reply(":warning: You are not registered! Use `/register` first.").setEphemeral(true).queue();
                return;
            }
        }

        String username = (String) userData.get("username");
        String email = (String) userData.get("email");
        String phone = (String) userData.get("phone");
        String role = (String) userData.get("role");
        int coins = (int) userData.get("coins");

        String maskedPhone = "Unknown";
        if (phone != null && phone.length() > 3) {
            maskedPhone = phone.substring(0, 3) + "*******";
        } else if (phone != null) {
            maskedPhone = "***";
        }

        EmbedBuilder embed = new EmbedBuilder();

        embed.setAuthor("User Profile: " + username, null, event.getUser().getEffectiveAvatarUrl());

        embed.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        embed.setColor(Color.decode("#00b0f4"));


        embed.addField("🛡️ Rank / Role", "`" + role.toUpperCase() + "`", false);
        embed.addField("💰 Wallet Balance", "**" + coins + "** 🪙", false);


        embed.addBlankField(true);


        String accountDetails = String.format(
                "\n📧 **Email:** %s\n\n" +
                        "📱 **Phone:** %s\n\n" +
                        "🔗 **Linked Discord:** @%s",
                email, maskedPhone, userData.get("discord")
        );


        embed.addField("📝 Account Details", accountDetails, false);

        embed.setFooter("Cardinal Security System • ID: " + userData.get("id"));
        embed.setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }
}