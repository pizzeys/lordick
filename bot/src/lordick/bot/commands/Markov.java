package lordick.bot.commands;

import lordick.bot.BotCommand;
import xxx.moparisthebest.irclib.IrcClient;
import xxx.moparisthebest.irclib.messages.IrcMessage;

import java.sql.*;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Markov extends BotCommand {

    private Connection connection;
    private Random randy = new Random();
    private int replyrate = 2;
    private int replynick = 100;

    public Markov() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:markov.db");
            //connection.setAutoCommit(false);
            connection.createStatement().executeUpdate("create table if not exists markov (seed_a TEXT, seed_b TEXT, seed_c TEXT, unique(seed_a, seed_b, seed_c) on conflict ignore)");
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

    private static Pattern command = Pattern.compile("chat\\s(\\S+):?\\s*(\\S+)?(?: (\\S+))?", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean shouldHandleCommand(IrcClient client, IrcMessage message) {
        return command.matcher(message.getMessage()).find();
    }

    private int s2i(String s, int min, int max) {
        try {
            int i = Integer.parseInt(s);
            if (i < min) {
                return min;
            } else if (i > max) {
                return max;
            } else {
                return i;
            }
        } catch (Exception ignored) {

        }
        return min;
    }

    @Override
    public void handleCommand(IrcClient client, IrcMessage message) {
        Matcher m = command.matcher(message.getMessage());
        if (m.find()) {
            String cmd = m.group(1);
            if (cmd.equalsIgnoreCase("about")) {
                if (m.group(2) == null || m.group(2).length() == 0) {
                    message.sendChat("Need context");
                } else if (randy.nextFloat() * 100 <= replynick) {
                    String markov = markov_find(m.group(2), m.group(3));
                    if (markov == null) {
                        message.sendChat("I can't :(");
                    } else {
                        message.sendChat(markov);
                    }
                }
            } else if (message.getHostmask().getNick().equalsIgnoreCase("exemplar")) {
                if (cmd.equalsIgnoreCase("replyrate")) {
                    if (m.group(2) == null) {
                        message.sendChatf("Reply rate is: %d%%", replyrate);
                    } else {
                        replyrate = s2i(m.group(2), 0, 100);
                        message.sendChatf("Reply rate set to: %d%%", replyrate);
                    }
                } else if (cmd.equalsIgnoreCase("replynick")) {
                    if (m.group(2) == null) {
                        message.sendChatf("Reply nick is: %d%%", replynick);
                    } else {
                        replynick = s2i(m.group(2), 0, 100);
                        message.sendChatf("Reply nick set to: %d%%", replynick);
                    }
                } else {
                    message.sendChatf("Unknown command: %d%%", cmd);
                }
            } else {
                message.sendChatf("Unknown command: %d%%", cmd);
            }
        }
    }

    @Override
    public String getHelp() {
        return "Usage: chat [about|replynick|replyrate] <context>";
    }

    @Override
    public String getCommand() {
        return "chat";
    }

    @Override
    public void unhandledMessage(IrcClient client, IrcMessage message) {
        if (message.isDestChannel()) {
            markov_learn(message.getMessage());
            if (randy.nextFloat() * 100 <= replyrate) {
                String markov = markov_generate();
                if (markov != null) {
                    message.sendChat(markov);
                }
            }
        }
    }

    private void markov_learn(String input) {
        String seed1, seed2;
        seed1 = seed2 = "\n";
        String[] words = input.split(" ");
        for (String seed3 : words) {
            try {
                PreparedStatement ps = connection.prepareStatement("insert into markov (seed_a, seed_b, seed_c) values (?, ?, ?)");
                ps.setString(1, seed1);
                ps.setString(2, seed2);
                ps.setString(3, seed3);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            seed1 = seed2;
            seed2 = seed3;
        }
    }

    private String markov_find(String seed1, String seed2) {
        try {
            PreparedStatement ps;
            ps = connection.prepareStatement("select seed_a, seed_b from markov where seed_a = ? or seed_a = ? or seed_b = ? or seed_b = ? order by random() limit 1");
            ps.setString(1, seed1);
            ps.setString(2, (seed2 == null ? seed1 : seed2));
            ps.setString(3, seed1);
            ps.setString(4, (seed2 == null ? seed1 : seed2));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String found1 = rs.getString(1);
                String found2 = rs.getString(2);
                return markov_generate(found1, found2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String markov_generate() {
        try {
            PreparedStatement ps = connection.prepareStatement("select seed_a, seed_b from markov order by random() limit 1");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String found1 = rs.getString(1);
                String found2 = rs.getString(2);
                String markov = markov_generate(found1, found2);
                if (markov != null) {
                    if (!found2.equalsIgnoreCase("\n")) {
                        markov = found2 + " " + markov;
                    }
                    if (!found1.equalsIgnoreCase("\n")) {
                        markov = found1 + " " + markov;
                    }
                    return markov;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String markov_generate(String seed1, String seed2) {
        //System.out.printf("Start seeds: %s - %s\n", seed1.replace("\n", "\\n"), seed2.replace("\n", "\\n"));
        int wordcount = randy.nextInt(20) + 10;
        StringBuilder result = new StringBuilder();
        if (!seed1.equalsIgnoreCase("\r")) {
            result.append(seed1);
            result.append(' ');
        }
        if (!seed2.equalsIgnoreCase("\r")) {
            result.append(seed2);
        }
        for (int i = 0; i < wordcount / 2; i++) {
            String seed0 = markov_previousseed(seed1, seed2);
            if (seed0 == null || seed0.equalsIgnoreCase("\n")) {
                break;
            }
            if (result.length() > 0) {
                result.insert(0, ' ');
            }
            result.insert(0, seed0);
            seed2 = seed1;
            seed1 = seed0;
        }
        for (int i = 0; i < wordcount / 2; i++) {
            String seed3 = markov_nextseed(seed1, seed2);
            if (seed3 == null || seed3.equalsIgnoreCase("\n")) {
                break;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(seed3);
            seed1 = seed2;
            seed2 = seed3;
        }
        if (result.length() > 0) {
            return result.toString().trim();
        } else {
            return null;
        }
    }

    private String markov_nextseed(String seed1, String seed2) {
        try {
            PreparedStatement ps = connection.prepareStatement("select seed_c from markov where seed_a = ? and seed_b = ? order by random() limit 1");
            ps.setString(1, seed1);
            ps.setString(2, seed2);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String markov_previousseed(String seed2, String seed3) {
        try {
            PreparedStatement ps = connection.prepareStatement("select seed_a from markov where seed_b = ? and seed_c = ? order by random() limit 1");
            ps.setString(1, seed2);
            ps.setString(2, seed3);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
