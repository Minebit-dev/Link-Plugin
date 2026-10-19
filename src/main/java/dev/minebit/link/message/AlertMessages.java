package dev.minebit.link.message;

import dev.minebit.link.model.Alert;
import net.bitbylogic.utils.message.messages.MessageGroup;
import net.bitbylogic.utils.message.messages.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class AlertMessages extends MessageGroup {

    private MessageKey application;

    public AlertMessages() {
        super("Alerts");
    }

    @Override
    public void register() {
        application = register("Application", "<newline><#22d3ee>▌</#22d3ee> <gradient:#ffffff:#22d3ee><bold><smallcaps>new application</smallcaps></bold></gradient><newline><#22d3ee>▌</#22d3ee> <#f2f3f5><applicant></#f2f3f5> <#7a7f8a><smallcaps>applied to</smallcaps></#7a7f8a> <#22d3ee><role></#22d3ee><newline><#22d3ee>▌</#22d3ee> <#34d399>[ <smallcaps>click to review</smallcaps> ]</#34d399><newline>");
    }

    public Component application(Alert alert) {
        return application.get(
                        Placeholder.unparsed("applicant", alert.applicant()),
                        Placeholder.unparsed("role", alert.role()))
                .clickEvent(ClickEvent.openUrl(alert.url()));
    }

}
