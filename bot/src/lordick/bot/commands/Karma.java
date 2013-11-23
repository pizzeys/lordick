package lordick.bot.commands;

import lordick.bot.BotCommand;
import xxx.moparisthebest.irclib.IrcClient;
import xxx.moparisthebest.irclib.messages.IrcMessage;
import xxx.moparisthebest.util.StringUtil;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Karma extends BotCommand {

    private Connection connection;

    public Karma() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:karma.db");
            //connection.setAutoCommit(false);
            connection.createStatement().executeUpdate("create table if not exists karma (name TEXT UNIQUE, score INTEGER)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (connection != null) {
                connection.commit();
                connection.close();
            }
        } finally {
            super.finalize();
        }
    }

    private static String[] commandList = {"karma", "rep"};
    private static Pattern command = Pattern.compile("(?:" + StringUtil.join("|", commandList) + ")(?: for)?:?(?: (\\S+))", Pattern.CASE_INSENSITIVE);
    private static Pattern karma = Pattern.compile("(?:(\\S+)\\s?\\+\\++)");
    private static String help = command.pattern() + " - shows karma score for name, or name++ increases karma score for name";

    @Override
    public boolean shouldHandleCommand(IrcClient client, IrcMessage message) {
        return command.matcher(message.getMessage()).find();
    }

    @Override
    public void handleCommand(IrcClient client, IrcMessage message) {
        Matcher m = command.matcher(message.getMessage());
        if (m.find()) {
            String nick = m.group(1).toLowerCase();
            try {
                PreparedStatement ps = connection.prepareStatement("select score from karma where name == ?");
                ps.setString(1, nick);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int score = rs.getInt(1);
                    message.sendChatf("karma for %s: %d", nick, score);
                } else {
                    message.sendChatf("no karma for %s", nick);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getHelp() {
        return help;
    }

    @Override
    public String[] getCommandList() {
        return commandList;
    }

    @Override
    public boolean shouldHandleMessage(IrcClient client, IrcMessage message) {
        return message.isDestChannel() && message.getMessage().contains("++");
    }

    @Override
    public void handleMessage(IrcClient client, IrcMessage message) {
        Matcher m = karma.matcher(message.getMessage());
        while (m.find()) {
            try {
                String nick = m.group(1).toLowerCase();
                PreparedStatement ps = connection.prepareStatement("insert or replace into karma (name, score) values (?, ifnull((select score + 1 from karma where name = ?), 1))");
                ps.setString(1, nick);
                ps.setString(2, nick);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
