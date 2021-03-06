package lordick.bot.commands;

import lordick.Lordick;
import lordick.bot.CommandListener;
import xxx.moparisthebest.irclib.messages.IrcMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class Weather implements CommandListener {

    private static final String WEATHER_URL = "http://mobile.wunderground.com/cgi-bin/findweather/getForecast?brand=mobile&query=";
    private static Pattern table = Pattern.compile("<table border=\"1\" width=\"100%\">(.+?)</table>");

    @Override
    public String getHelp() {
        return "Usage: weather [location]";
    }

    @Override
    public String getCommands() {
        return "weather";
    }

    @Override
    public void handleCommand(Lordick client, String command, IrcMessage message) {
        String location;
        if (!message.hasMessage()) {
            location = client.getKeyValue(message.getServer(), "weather." + message.getHostmask().getNick());
            if (location == null) {
                message.sendChatf("%s: No previous location stored", message.getHostmask().getNick());
                return;
            }
        } else {
            location = message.getMessage();
        }
        location = location.replaceAll("\\s+", "");
        String data = readurl(WEATHER_URL + location); // todo: spin this off as a job/future
        if (data != null) {
            Matcher m = table.matcher(data.replaceAll("\t+", ""));
            if (m.find()) {
                client.setKeyValue(message.getServer(), "weather." + message.getHostmask().getNick(), location);
                String weather = m.group(1).replaceAll("</?b>", "\02").replaceAll("</tr>", ",").replaceAll("<.+?>", " ").replaceAll("&.+?;", " ").replaceAll("\\s+", " ").replaceAll("( ,)+", ",").replaceAll(", UV .*", "").trim();
                String[] moredata = weather.split("\02,", 2);
                message.sendChatf("%s: %s", message.getHostmask().getNick(), moredata[0]);
                message.sendChat(moredata[1].trim());
            } else {
                message.sendChatf("%s: Invalid location: %s", message.getHostmask().getNick(), location);
            }
        } else {
            message.sendChatf("%s: Unable to read weather data", message.getHostmask().getNick());
        }
    }

    private String readurl(String url) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(' ');
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
