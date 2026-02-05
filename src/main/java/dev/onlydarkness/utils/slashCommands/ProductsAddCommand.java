package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ProductsAddCommand implements ISlashCommand {
    private final ModuleManager manager;
    private final String OWNER_ID = "449536056019124234";

    public ProductsAddCommand(ModuleManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() { return "products-add"; }

    @Override
    public String getDescription() { return "Add a new product (Owner Only)."; }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "title", "Product title", true),
                new OptionData(OptionType.STRING, "name", "Product name", true),
                new OptionData(OptionType.STRING, "description", "Product details", true),
                new OptionData(OptionType.STRING, "text", "Product text", true),
                new OptionData(OptionType.STRING, "price", "Price", true),
                new OptionData(OptionType.STRING, "image", "Image URL", true),
                new OptionData(OptionType.STRING, "link", "Purchase link", true)
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        if (!event.getUser().getId().equals(OWNER_ID)) {
            event.reply(":x: Bu komutu kullanmak için yetkiniz yok!").setEphemeral(true).queue();
            return;
        }

        DatabaseManager db = manager.getService(DatabaseManager.class);
        if (db == null) return;

        db.addProduct(
                event.getOption("title").getAsString(),
                event.getOption("name").getAsString(),
                event.getOption("description").getAsString(),
                event.getOption("text").getAsString(),
                event.getOption("price").getAsString(),
                event.getOption("image").getAsString(),
                event.getOption("link").getAsString()
        );

        event.reply("✅ Ürün başarıyla eklendi.").setEphemeral(true).queue();
    }
}