package dev.minebit.link.message;

import java.util.List;
import lombok.Getter;
import net.bitbylogic.utils.message.messages.MessageGroup;
import net.bitbylogic.utils.message.messages.MessageKey;

@Getter
public class CommandMessages extends MessageGroup {

    private MessageKey notConfigured;
    private MessageKey reloaded;
    private MessageKey statusLines;
    private MessageKey statusOn;
    private MessageKey statusOff;
    private MessageKey statusSet;
    private MessageKey statusNotSet;
    private MessageKey statusLinked;
    private MessageKey statusNotLinked;
    private MessageKey statusNoKey;

    public CommandMessages() {
        super("Commands");
    }

    @Override
    public void register() {
        notConfigured = register("Not-Configured", "<#ff6b6b><smallcaps>set</smallcaps> <white>Network-Slug</white> <smallcaps>in the minebit link config first.</smallcaps></#ff6b6b>");
        reloaded = register("Reloaded", "<#34d399><smallcaps>minebit link reloaded.</smallcaps></#34d399>");
        statusLines = register("Status-Lines", List.of(
                "<gradient:#ffffff:#22d3ee><bold><smallcaps>minebit link</smallcaps></bold></gradient>",
                " <#22d3ee>▸</#22d3ee> <#7a7f8a><smallcaps>network</smallcaps></#7a7f8a> <#f2f3f5><network></#f2f3f5> <#7a7f8a>•</#7a7f8a> <#7a7f8a><smallcaps>roles loaded</smallcaps></#7a7f8a> <#34d399><roles></#34d399>",
                " <#22d3ee>▸</#22d3ee> <#7a7f8a><smallcaps>server link</smallcaps></#7a7f8a> <link>",
                " <#22d3ee>▸</#22d3ee> <#7a7f8a><smallcaps>staff alerts</smallcaps></#7a7f8a> <alerts> <#7a7f8a>•</#7a7f8a> <#7a7f8a><smallcaps>discord</smallcaps></#7a7f8a> <discord>"));
        statusOn = register("Status-On", "<#34d399><smallcaps>on</smallcaps></#34d399>");
        statusOff = register("Status-Off", "<#7a7f8a><smallcaps>off</smallcaps></#7a7f8a>");
        statusSet = register("Status-Set", "<#34d399><smallcaps>set</smallcaps></#34d399>");
        statusNotSet = register("Status-Not-Set", "<#7a7f8a><smallcaps>not set</smallcaps></#7a7f8a>");
        statusLinked = register("Status-Linked", "<#34d399><smallcaps>linked</smallcaps></#34d399>");
        statusNotLinked = register("Status-Not-Linked", "<#ff6b6b><smallcaps>not linked yet, or the key was refused</smallcaps></#ff6b6b>");
        statusNoKey = register("Status-No-Key", "<#ff6b6b><smallcaps>no</smallcaps> Link-Key <smallcaps>set</smallcaps></#ff6b6b>");
    }

}
