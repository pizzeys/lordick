package xxx.moparisthebest.irclib;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import xxx.moparisthebest.irclib.messages.Ping;
import xxx.moparisthebest.irclib.properties.UserProperties;
import xxx.moparisthebest.util.ClassEnumerator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IrcHandler extends SimpleChannelInboundHandler<String> {

    // http://mybuddymichael.com/writings/a-regular-expression-for-irc-messages.html
    public static Pattern IRC_PATTERN = Pattern.compile("^(?:[:](\\S+) )?(\\S+)(?: (?!:)(.+?))?(?: [:](.+))?$");

    private List<IrcMessageHandler> messageHandlers = new CopyOnWriteArrayList<IrcMessageHandler>();

    public IrcHandler() {
        for (Class c : ClassEnumerator.getClassesForPackage(Ping.class.getPackage())) {
            try {
                IrcMessageHandler message = (IrcMessageHandler) c.newInstance();
                messageHandlers.add(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
        Matcher m = IRC_PATTERN.matcher(message);
        if (!m.matches()) {
            // todo: print error or something
            return;
        }
        IrcMessage msg = new IrcMessage(message, m.group(1), m.group(2), m.group(3), m.group(4), ctx.channel());
        for (IrcMessageHandler ircMessage : messageHandlers) {
            if (ircMessage.shouldHandle(msg)) {
                ircMessage.handle(msg);
            }
        }
        IrcClient.getIrcClient(ctx.channel()).OnIrcMessage(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        IrcClient.getIrcClient(ctx.channel()).OnException(ctx.channel(), cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        UserProperties userprops = IrcClient.getUserProperties(ctx.channel());
        ctx.write("NICK " + userprops.getNickname());
        ctx.writeAndFlush("USER " + userprops.getIdent() + " 0 * :" + userprops.getRealname());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close(); // hope this fires the closeFuture in IrcClient.connect
    }
}
