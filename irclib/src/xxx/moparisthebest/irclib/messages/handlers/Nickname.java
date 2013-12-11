package xxx.moparisthebest.irclib.messages.handlers;

import xxx.moparisthebest.irclib.messages.IrcMessage;
import xxx.moparisthebest.irclib.messages.IrcMessageHandler;
import xxx.moparisthebest.irclib.properties.UserProperties;

import java.util.Random;

@SuppressWarnings("unused")
public class Nickname implements IrcMessageHandler {
    @Override
    public boolean shouldHandle(IrcMessage message) {
        return message.getCommand().equalsIgnoreCase("NICK") || message.getCommand().equalsIgnoreCase("433") || message.getCommand().equalsIgnoreCase("396");
    }

    @Override
    public void handle(IrcMessage message) {
        UserProperties up = message.getServer().getUserProperties();
        String nick = message.getHostmask().getNick();
        if (message.getCommand().equalsIgnoreCase("NICK")) { // if we change (or someone else) changes our nickname, keep track
            if (nick.equals(up.getNickname())) {
                up.setNickname(nick);
            }
        } else if (message.getCommand().equalsIgnoreCase("433")) { // if our nick is in use
            nick = up.getAltnick() + new Random().nextInt(1000);
            message.getServer().getChannel().writeAndFlush("NICK " + nick);
            up.setNickname(nick);
        } else { // update host
            up.setHost(message.getTarget());
        }
    }
}
