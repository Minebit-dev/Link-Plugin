package dev.minebit.link.message;

import dev.minebit.link.model.Role;
import dev.minebit.link.model.Snapshot;
import java.util.ArrayList;
import java.util.List;
import net.bitbylogic.utils.message.messages.MessageGroup;
import net.bitbylogic.utils.message.messages.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class RoleMessages extends MessageGroup {

    private MessageKey header;
    private MessageKey role;
    private MessageKey hover;
    private MessageKey footer;
    private MessageKey none;
    private MessageKey newRole;

    public RoleMessages() {
        super("Roles");
    }

    @Override
    public void register() {
        header = register("Header", "<gradient:#ffffff:#22d3ee><bold><smallcaps>hiring</smallcaps></bold></gradient> <#7a7f8a>•</#7a7f8a> <#f2f3f5><network></#f2f3f5> <#7a7f8a>•</#7a7f8a> <#34d399><count> <smallcaps>open</smallcaps></#34d399>");
        role = register("Role", " <#22d3ee>▸</#22d3ee> <#f2f3f5><title></#f2f3f5> <#7a7f8a>•</#7a7f8a> <#34d399><pay></#34d399>");
        hover = register("Hover", "<#22d3ee><title></#22d3ee><newline><#9aa0aa><category> • <pay></#9aa0aa><newline><#34d399><smallcaps>click to open on minebit</smallcaps></#34d399>");
        footer = register("Footer", " <click:open_url:'<url>'><#22d3ee><smallcaps>view all roles on minebit</smallcaps> ➜</#22d3ee></click>");
        none = register("None", "<#9aa0aa><smallcaps>no open roles right now.</smallcaps></#9aa0aa>");
        newRole = register("New-Role", "<newline><#22d3ee>▌</#22d3ee> <gradient:#ffffff:#22d3ee><bold><smallcaps>now hiring</smallcaps></bold></gradient><newline><#22d3ee>▌</#22d3ee> <#f2f3f5><title></#f2f3f5> <#7a7f8a>•</#7a7f8a> <#34d399><pay></#34d399><newline><#22d3ee>▌</#22d3ee> <#34d399>[ <smallcaps>click to view</smallcaps> ]</#34d399><newline>");
    }

    public Component newRole(Role entry) {
        TagResolver.Single[] placeholders = {
                Placeholder.unparsed("title", entry.title()),
                Placeholder.unparsed("pay", entry.pay()),
                Placeholder.unparsed("category", entry.category())
        };

        return newRole.get(placeholders).clickEvent(ClickEvent.openUrl(entry.url()));
    }

    public List<Component> roleList(Snapshot snapshot, int maxRoles) {
        List<Component> lines = new ArrayList<>();

        if (snapshot.roles().isEmpty()) {
            lines.add(none.get());
            return lines;
        }

        lines.add(header.get(
                Placeholder.unparsed("network", snapshot.networkName()),
                Placeholder.unparsed("count", String.valueOf(snapshot.roles().size()))));

        int shown = Math.min(maxRoles, snapshot.roles().size());

        for (int i = 0; i < shown; i++) {
            lines.add(roleLine(snapshot.roles().get(i)));
        }

        lines.add(footer.get(
                Placeholder.parsed("url", snapshot.networkUrl()),
                Placeholder.unparsed("network", snapshot.networkName())));
        return lines;
    }

    private Component roleLine(Role entry) {
        TagResolver.Single[] placeholders = {
                Placeholder.unparsed("title", entry.title()),
                Placeholder.unparsed("pay", entry.pay()),
                Placeholder.unparsed("category", entry.category())
        };

        Component line = role.get(placeholders).clickEvent(ClickEvent.openUrl(entry.url()));

        if (hover.getPlain().isBlank()) {
            return line;
        }

        return line.hoverEvent(HoverEvent.showText(hover.get(placeholders)));
    }

}
