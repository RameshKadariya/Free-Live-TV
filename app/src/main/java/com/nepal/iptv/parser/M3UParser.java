package com.nepal.iptv.parser;

import com.nepal.iptv.model.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3UParser {
    
    public static List<Channel> parse(String m3uContent) {
        List<Channel> channels = new ArrayList<>();
        
        if (m3uContent == null || m3uContent.isEmpty()) {
            return channels;
        }
        
        String[] lines = m3uContent.split("\n");
        Channel currentChannel = null;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.startsWith("#EXTINF:")) {
                // Parse channel info
                String name = extractChannelName(line);
                String logo = extractAttribute(line, "tvg-logo");
                String group = extractAttribute(line, "group-title");
                String language = extractAttribute(line, "tvg-language");
                String country = extractAttribute(line, "tvg-country");
                
                currentChannel = new Channel(name, "");
                currentChannel.setLogo(logo);
                currentChannel.setGroup(group);
                currentChannel.setLanguage(language);
                currentChannel.setCountry(country);
                
            } else if (!line.isEmpty() && !line.startsWith("#") && currentChannel != null) {
                // This is the URL line
                currentChannel.setUrl(line);
                channels.add(currentChannel);
                currentChannel = null;
            }
        }
        
        return channels;
    }
    
    private static String extractChannelName(String line) {
        int lastComma = line.lastIndexOf(',');
        if (lastComma != -1 && lastComma < line.length() - 1) {
            return line.substring(lastComma + 1).trim();
        }
        return "Unknown Channel";
    }
    
    private static String extractAttribute(String line, String attribute) {
        Pattern pattern = Pattern.compile(attribute + "=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}
