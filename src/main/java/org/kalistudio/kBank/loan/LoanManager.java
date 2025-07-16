package org.kalistudio.kBank.loan;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.utils.ChatUtil;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoanManager {

    private final KBank plugin;
    private final HashMap<UUID, LoanData> loanMap = new HashMap<>();

    public LoanManager(KBank plugin) {
        this.plugin = plugin;
        createTable();
        startLoanTimer();
    }

    private void createTable() {
        Connection conn = plugin.getBankManager().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS loans (" +
                    "uuid TEXT PRIMARY KEY," +
                    "borrowed DOUBLE," +
                    "totalToPay DOUBLE," +
                    "timeLeft BIGINT" +
                    ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void borrow(Player player, double amount) {
        UUID uuid = player.getUniqueId();

        if (loanMap.containsKey(uuid)) {
            player.sendMessage("§cBạn đã có khoản vay!");
            return;
        }

        double interestRate = plugin.getConfig().getDouble("interest-rate", 10);
        long loanHours = plugin.getConfig().getLong("loan-time", 24);
        long seconds = loanHours * 3600;

        double totalToPay = amount * (1 + (interestRate / 100));
        LoanData loan = new LoanData(amount, totalToPay, seconds);
        loanMap.put(uuid, loan);

        try (Connection conn = plugin.getBankManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO loans VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, loan.borrowed);
            stmt.setDouble(3, loan.totalToPay);
            stmt.setLong(4, loan.timeLeft);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Gửi tiền vào ví qua Vault
        plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)
                .getProvider().depositPlayer(player, amount);

        player.sendMessage(ChatUtil.lang("gui.loan-success",
                "amount", String.valueOf(amount),
                "total", String.format("%.2f", totalToPay)));
    }

    public void loadLoan(UUID uuid) {
        try (Connection conn = plugin.getBankManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM loans WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                loanMap.put(uuid, new LoanData(
                        rs.getDouble("borrowed"),
                        rs.getDouble("totalToPay"),
                        rs.getLong("timeLeft")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeLoan(UUID uuid) {
        loanMap.remove(uuid);
        try (Connection conn = plugin.getBankManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM loans WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasLoan(UUID uuid) {
        return loanMap.containsKey(uuid);
    }

    public LoanData getLoan(UUID uuid) {
        return loanMap.get(uuid);
    }

    // =============== Theo dõi thời gian online ==============

    private void startLoanTimer() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (!hasLoan(uuid)) continue;

                LoanData loan = getLoan(uuid);
                loan.timeLeft -= 60; // trừ 60 giây mỗi phút
                if (loan.timeLeft <= 0) {
                    double balance = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)
                            .getProvider().getBalance(player);

                    if (balance >= loan.totalToPay) {
                        plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)
                                .getProvider().withdrawPlayer(player, loan.totalToPay);
                        player.sendMessage("§a✔ Khoản vay đã được thanh toán tự động: §e$" + loan.totalToPay);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    } else {
                        double missing = loan.totalToPay - balance;
                        player.sendMessage(ChatUtil.lang("gui.loan-failed",
                                "missing", String.format("%.2f", missing)));
                        // Có thể áp dụng hình phạt khác nếu cần
                    }
                    removeLoan(uuid);
                } else {
                    // Cập nhật vào database
                    try (Connection conn = plugin.getBankManager().getConnection();
                         PreparedStatement stmt = conn.prepareStatement("UPDATE loans SET timeLeft=? WHERE uuid=?")) {
                        stmt.setLong(1, loan.timeLeft);
                        stmt.setString(2, uuid.toString());
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 20 * 60L, 20 * 60L); // Mỗi phút
    }

    public void loadAllLoans() {
        try (Connection conn = plugin.getBankManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM loans");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                loanMap.put(uuid, new LoanData(
                        rs.getDouble("borrowed"),
                        rs.getDouble("totalToPay"),
                        rs.getLong("timeLeft")
                ));
            }

            Bukkit.getLogger().info("[kBank] Đã load " + loanMap.size() + " khoản vay từ database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, LoanData> getAllLoans() {
        return new HashMap<>(loanMap); // Trả bản sao để tránh sửa trực tiếp
    }

}