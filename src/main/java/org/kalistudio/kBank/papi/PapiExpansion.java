package org.kalistudio.kBank.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.loan.LoanData;
import org.kalistudio.kBank.utils.ChatUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class PapiExpansion extends PlaceholderExpansion {

    private final KBank plugin;

    public PapiExpansion(KBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "KaliStudio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (!player.hasPlayedBefore() && !player.isOnline()) return "";

        UUID uuid = player.getUniqueId();

        switch (identifier.toLowerCase()) {
            case "money" -> {
                double money = plugin.getBankManager().getMoney(uuid);
                return ChatUtil.formatVND(money);
            }

            case "loan" -> {
                LoanData loan = plugin.getLoanManager().getLoan(uuid);
                return (loan != null) ? ChatUtil.formatVND(loan.totalToPay) : "0₫";
            }

            case "loan_timeleft" -> {
                LoanData loan = plugin.getLoanManager().getLoan(uuid);
                if (loan == null) return "Không có";

                long seconds = loan.timeLeft;
                long days = seconds / 86400;
                long hours = (seconds % 86400) / 3600;
                long minutes = (seconds % 3600) / 60;

                StringBuilder sb = new StringBuilder();
                if (days > 0) sb.append(days).append(" ngày ");
                if (hours > 0) sb.append(hours).append(" giờ ");
                if (minutes > 0) sb.append(minutes).append(" phút");
                if (sb.length() == 0) sb.append("Dưới 1 phút");

                return sb.toString().trim();
            }

            case "loan_due" -> {
                LoanData loan = plugin.getLoanManager().getLoan(uuid);
                if (loan == null) return "Không có";

                long currentMillis = System.currentTimeMillis();
                long dueMillis = currentMillis + (loan.timeLeft * 1000L);

                Date date = new Date(dueMillis);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                return sdf.format(date);
            }

            default -> {
                return null;
            }
        }
    }

}