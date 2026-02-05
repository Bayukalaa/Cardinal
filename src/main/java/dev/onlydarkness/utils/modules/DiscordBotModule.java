package dev.onlydarkness.utils.modules;


import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.SlashCommandManager;
import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;
import dev.onlydarkness.utils.slashCommands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DiscordBotModule implements IModule {
    private JDA jda;
    private String channelID;
    private ModuleManager moduleManager;
    private String guildID;

    private SlashCommandManager slashCommandManager;


    private PrintStream originalOut;
    private PrintStream originalErr;
    private final StringBuffer logBuffer = new StringBuffer();
    private ScheduledExecutorService scheduler;
    private static final Pattern ANSI_COLOR_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

    @Override
    public String getName() {
        return "Discord Bot Module";
    }

    @Override
    public void onEnable(ModuleContext context) {
        if (!context.getConfigBoolean("discord-enabled", false)) return;

        String token = context.getConfigString("discord-token", "TOKEN_YAZ");
        this.channelID = context.getConfigString("discord-channel-id", "KANAL_ID_YAZ");
        this.guildID = context.getConfigString("discord-guild-id", "SUNUCU_ID_YAZ");

        if (token.length() < 10) {
            System.out.println("[DiscordBot] Invalid Token.");
            return;
        }

        if (context instanceof ModuleManager) {
            this.moduleManager = (ModuleManager) context;
        }


        this.slashCommandManager = new SlashCommandManager();


        this.slashCommandManager.register(new PingCommand());
        this.slashCommandManager.register(new ExecCommand(this.moduleManager, this.channelID));
        this.slashCommandManager.register(new RegisterCommand(this.moduleManager));
        this.slashCommandManager.register(new ProfileCommand(this.moduleManager));
        this.slashCommandManager.register(new ProductsAddCommand(this.moduleManager));
        this.slashCommandManager.register(new ProductsRemoveCommand(this.moduleManager));
        this.slashCommandManager.register(new ProductsListCommand(this.moduleManager));
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new BotListener()) // Listener aşağıda
                    .build();

            jda.awaitReady();

            registerCommandsToDiscord();

            System.out.println("[DiscordBot] Connected as " + jda.getSelfUser().getName());

            hookConsoleOutput();
            startLogFlusher();

            sendToDiscord(":white_check_mark: **Cardinal System Online!**");

        } catch (Exception e) {
            System.err.println("[DiscordBot] Error: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {

        if (originalOut != null) System.setOut(originalOut);
        if (originalErr != null) System.setErr(originalErr);


        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        if (jda != null) {

            flushLogsToDiscordSync();


            try {
                TextChannel channel = jda.getTextChannelById(channelID);
                if (channel != null) {

                    channel.sendMessage(":octagonal_sign: **System Reloading...**").complete();
                }
            } catch (Exception ignored) {

            }


            jda.shutdownNow();
            jda = null;
        }
    }


    private void registerCommandsToDiscord() {

        var commandData = slashCommandManager.getCommandDataList();

        if (guildID.equals("SUNUCU_ID_YAZ")) {
            System.out.println("[DiscordBot] Updating Global Commands...");
            jda.updateCommands().addCommands(commandData).queue();
        } else {
            Guild guild = jda.getGuildById(guildID);
            if (guild != null) {
                guild.updateCommands().addCommands(commandData).queue();
                System.out.println("[DiscordBot] Updated Guild Commands for: " + guild.getName());
            }
        }
    }


    private class BotListener extends ListenerAdapter {
        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

            slashCommandManager.handle(event);
        }
    }


    private void hookConsoleOutput() {
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new InterceptorStream(originalOut));
        System.setErr(new InterceptorStream(originalErr));
    }

    private void startLogFlusher() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::flushLogsToDiscordSync, 3, 3, TimeUnit.SECONDS);
    }

    private void flushLogsToDiscordSync() {
        if (jda == null || logBuffer.length() == 0) return;

        synchronized (logBuffer) {
            String content = logBuffer.toString();
            logBuffer.setLength(0);

            if (!content.isBlank()) {
                if (content.length() > 1900) content = content.substring(0, 1900) + "\n... (Truncated)";

                try {
                    TextChannel channel = jda.getTextChannelById(channelID);
                    if (channel != null) {

                        channel.sendMessage("```ansi\n" + content + "\n```").complete();
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void sendToDiscord(String message) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(channelID);
        if (channel != null) channel.sendMessage(message).queue();
    }

    private class InterceptorStream extends PrintStream {
        public InterceptorStream(OutputStream out) { super(out, true); }
        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            String line = new String(buf, off, len);
            String cleanLine = ANSI_COLOR_PATTERN.matcher(line).replaceAll("");
            synchronized (logBuffer) { logBuffer.append(cleanLine); }
        }
    }
}