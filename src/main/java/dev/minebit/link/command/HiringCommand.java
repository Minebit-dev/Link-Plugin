package dev.minebit.link.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.minebit.link.MinebitLink;
import dev.minebit.link.config.Settings;
import dev.minebit.link.message.CommandMessages;
import dev.minebit.link.message.RoleMessages;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.utils.message.messages.MessageKey;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
@CommandAlias("hiring|jobs|apply|minebit")
public class HiringCommand extends BaseCommand {

    private final MinebitLink plugin;
    private final RoleMessages roleMessages;
    private final CommandMessages commandMessages;

    @Default
    @CommandPermission("minebit.link.use")
    @Description("Shows the open roles on Minebit")
    public void onRoles(CommandSender sender) {
        Settings settings = plugin.getSettings();

        if (!settings.configured()) {
            commandMessages.getNotConfigured().send(sender);
            return;
        }

        roleMessages.roleList(plugin.getRoleManager().getSnapshot(), settings.maxRoles()).forEach(sender::sendMessage);
    }

    @Subcommand("reload")
    @CommandPermission("minebit.link.admin")
    @Description("Reloads the config and messages")
    public void onReload(CommandSender sender) {
        plugin.reload();
        commandMessages.getReloaded().send(sender);
    }

    @Subcommand("status")
    @CommandPermission("minebit.link.admin")
    @Description("Shows whether the server is linked")
    public void onStatus(CommandSender sender) {
        Settings settings = plugin.getSettings();

        commandMessages.getStatusLines().send(sender,
                Placeholder.unparsed("network", settings.slug()),
                Placeholder.unparsed("roles", String.valueOf(plugin.getRoleManager().getSnapshot().roles().size())),
                Placeholder.parsed("link", linkState(settings).getPlain()),
                Placeholder.parsed("alerts", toggle(settings.hasKey() && settings.staffAlerts()).getPlain()),
                Placeholder.parsed("discord", toggle(settings.hasWebhook()).getPlain()));
    }

    private MessageKey linkState(Settings settings) {
        if (!settings.hasKey()) {
            return commandMessages.getStatusNoKey();
        }

        if (!plugin.getLinkManager().isLinked()) {
            return commandMessages.getStatusNotLinked();
        }

        return commandMessages.getStatusLinked();
    }

    private MessageKey toggle(boolean enabled) {
        if (enabled) {
            return commandMessages.getStatusOn();
        }

        return commandMessages.getStatusOff();
    }

}
