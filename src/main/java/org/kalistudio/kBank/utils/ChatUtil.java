package org.kalistudio.kBank.utils;

import org.bukkit.ChatColor;
import org.kalistudio.kBank.KBank;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ChatUtil {

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static List<String> colorList(String... lines) {
        return Arrays.stream(lines)
                .map(ChatUtil::color)
                .collect(Collectors.toList());
    }

    public static List<String> colorList(List<String> lines) {
        return lines.stream()
                .map(ChatUtil::color)
                .collect(Collectors.toList());
    }

    public static String lang(String key, String... replacements) {
        String msg = KBank.getInstance().getLang().getString(key, "&c[!] Thiếu key: " + key);
        if (msg == null) return "";
        msg = ChatUtil.color(msg);

        // Xử lý thay thế dạng %key%
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return getPrefix() + msg;
    }

    public static String formatVND(double amount) {
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        return format.format(amount) + "₫"; // Hoặc + " VNĐ" nếu bạn thích
    }

    public static String formatDate(long timeLeftInSeconds) {
        long dueMillis = System.currentTimeMillis() + timeLeftInSeconds * 1000L;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new Date(dueMillis));
    }

    public static String formatDates(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(new Date(millis));
    }

    public static String getPrefix() {
        return ChatUtil.color(KBank.getInstance().getLang().getString("prefix", ""));
    }

    public static String stripColor(String input) {
        return ChatColor.stripColor(input);
    }

    public static boolean matchTitle(String actual, String keyFromLang) {
        return stripColor(actual).equalsIgnoreCase(stripColor(keyFromLang));
    }
}
