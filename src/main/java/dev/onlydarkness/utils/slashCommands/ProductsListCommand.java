package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class ProductsListCommand implements ISlashCommand {
    private final ModuleManager manager;
    public ProductsListCommand(ModuleManager manager) { this.manager = manager; }

    @Override
    public String getName() { return "products-list"; }
    @Override
    public String getDescription() { return "Sends all products as embeds to a specific channel."; }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.CHANNEL, "channel", "The channel to send products to", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        DatabaseManager db = manager.getService(DatabaseManager.class);

        // Potansiyel Sorun 1: Kanal çekme hatası (null kontrolü)
        var channelOption = event.getOption("channel");
        if (channelOption == null) return;

        TextChannel targetChannel = (TextChannel) channelOption.getAsChannel();
        List<Map<String, Object>> products = db.getAllProducts();

        if (products.isEmpty()) {
            event.reply("❌ Veritabanında sergilenecek ürün bulunamadı.").setEphemeral(true).queue();
            return;
        }

        event.reply("📦 Ürünler " + targetChannel.getAsMention() + " kanalına diziliyor...").setEphemeral(true).queue();

        for (Map<String, Object> product : products) {
            EmbedBuilder embed = new EmbedBuilder();


            String title = (String) product.getOrDefault("title", "No Title");
            String name = (String) product.getOrDefault("name", "Unknown Product");
            String desc = (String) product.getOrDefault("description", "");
            String ptext = (String) product.getOrDefault("ptext", ""); // SQL'de ptext yapmıştık
            String price = (String) product.getOrDefault("price", "TBA");
            String img = (String) product.getOrDefault("image_url", "");
            String link = (String) product.getOrDefault("buy_link", "#");


            embed.setAuthor(name, null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());


            if (img != null && !img.isEmpty()) embed.setThumbnail(img);

             embed.setColor(Color.decode("#2ecc71"));
            embed.setTitle("🛡️ " + title);


            String formattedDesc = String.format(
                    "*%s*\n\n" +
                            "ℹ️ **Detay:** %s\n\n" +
                            "💰 **Fiyat:** `%s`\n\n" +
                            "🔗 **Satın Al:** [Buraya Tıkla](%s)",
                    desc, ptext, price, link
            );
            embed.setDescription(formattedDesc);


            if (img != null && !img.isEmpty()) embed.setImage(img);


            embed.setFooter("OnlyDarknesss ™ 2026 • Ürün ID: " + product.get("id"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());
            embed.setTimestamp(java.time.Instant.now());

            targetChannel.sendMessageEmbeds(embed.build()).queue();
        }
    }
}