package dev.onlydarkness.utils.slashCommands;

import dev.onlydarkness.utils.DatabaseManager;
import dev.onlydarkness.utils.ModuleManager;
import dev.onlydarkness.utils.core.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ProductsRemoveCommand implements ISlashCommand {
    private final ModuleManager manager;
    private final String OWNER_ID = "449536056019124234";

    public ProductsRemoveCommand(ModuleManager manager) {
        this.manager = manager;
    }

    @Override
    public String getName() { return "products-remove"; }

    @Override
    public String getDescription() { return "Remove a product (Owner Only)."; }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.INTEGER, "id", "Product ID", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        if (!event.getUser().getId().equals(OWNER_ID)) {
            event.reply(":x: Bu komutu kullanmak için yetkiniz yok!").setEphemeral(true).queue();
            return;
        }

        DatabaseManager db = manager.getService(DatabaseManager.class);
        if (db == null) return;

        int id = event.getOption("id").getAsInt();
        db.removeProduct(id);

        event.reply("🗑️ ID: " + id + " olan ürün silindi.").setEphemeral(true).queue();
    }
}