package xxx.moparisthebest.irclib.messages.handlers;

import xxx.moparisthebest.irclib.messages.IrcMessage;
import xxx.moparisthebest.irclib.messages.IrcMessageHandler;

public class Ping implements IrcMessageHandler {
    @Override
    public boolean shouldHandle(IrcMessage message) {
        return message.getCommand().equalsIgnoreCase("PING");
    }

    @Override
    public void handle(IrcMessage message) {
        message.getServer().getChannel().writeAndFlush("PONG :" + message.getMessage());
    }
}
