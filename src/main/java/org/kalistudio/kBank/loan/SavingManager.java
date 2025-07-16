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

public class SavingManager {
    private final KBank plugin;
    private final Map<UUID, Boolean> selecting = new HashMap<>();

    public SavingManager(KBank plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    public void setSelectingPeriod(UUID uuid, boolean value) {
        selecting.put(uuid, value);
    }

    public boolean isSelecting(UUID uuid) {
        return selecting.getOrDefault(uuid, false);
    }

    public void saveDeposit(UUID uuid, double amount, int months) {
        long createdAt = System.currentTimeMillis();
        long matureAt = createdAt + months * 30L * 24 * 60 * 60 * 1000; // 30 ngày mỗi tháng
        double rate = plugin.getConfig().getDouble("saving.rate", 0.01); // lãi suất mặc định 1%/tháng

        try (Connection connection = plugin.getDatabase().getConnection()) {
            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT amount, months, created_at FROM savings WHERE uuid = ?"
            );
            checkStmt.setString(1, uuid.toString());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Nếu đã có khoản tiết kiệm → cộng thêm vào
                double oldAmount = rs.getDouble("amount");
                int oldMonths = rs.getInt("months");
                long oldCreatedAt = rs.getLong("created_at");

                double newAmount = oldAmount + amount;

                // Reset kỳ hạn mới
                long newCreatedAt = System.currentTimeMillis();
                long newMatureAt = newCreatedAt + months * 30L * 24 * 60 * 60 * 1000;

                PreparedStatement update = connection.prepareStatement(
                        "UPDATE savings SET amount = ?, months = ?, created_at = ?, mature_at = ?, rate = ? WHERE uuid = ?"
                );
                update.setDouble(1, newAmount);
                update.setInt(2, months);
                update.setLong(3, newCreatedAt);
                update.setLong(4, newMatureAt);
                update.setDouble(5, rate);
                update.setString(6, uuid.toString());
                update.executeUpdate();

            } else {
                // Nếu chưa có → tạo mới
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO savings (uuid, amount, months, created_at, mature_at, rate) VALUES (?, ?, ?, ?, ?, ?)"
                );
                insert.setString(1, uuid.toString());
                insert.setDouble(2, amount);
                insert.setInt(3, months);
                insert.setLong(4, createdAt);
                insert.setLong(5, matureAt);
                insert.setDouble(6, rate);
                insert.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        try (Connection connection = plugin.getDatabase().getConnection();

             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS savings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT," +
                    "amount REAL," +
                    "months INTEGER," +
                    "created_at LONG," +
                    "rate REAL," +
                    "mature_at LONG)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SavingData getSaving(UUID uuid) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM savings WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new SavingData(
                        uuid,
                        rs.getDouble("amount"),
                        rs.getInt("months"),
                        rs.getLong("created_at"),
                        rs.getLong("mature_at"),
                        rs.getDouble("rate")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void withdrawSaving(UUID playerId) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM savings WHERE uuid = ?");
            ps.setString(1, playerId.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null)
                    player.sendMessage(ChatUtil.color("&cBạn không có khoản tiết kiệm nào."));
                return;
            }

            double amount = rs.getDouble("amount");
            int months = rs.getInt("months");
            long startTime = rs.getLong("created_at");
            double rate = plugin.getConfig().getDouble("interest-rates." + months);


            long now = System.currentTimeMillis();
            long endTime = startTime + (long) months * 30 * 24 * 60 * 60 * 1000;

            Player player = Bukkit.getPlayer(playerId);
            if (now < endTime) {
                long daysLeft = (endTime - now) / (1000 * 60 * 60 * 24);
                player.sendMessage(ChatUtil.color("&eBạn chưa đến hạn rút. Còn khoảng &c" + daysLeft + " &engày nữa."));
                return;
            }

            double interest = amount * rate * months;
            double total = amount + interest;

            // Xoá khoản tiết kiệm
            PreparedStatement del = conn.prepareStatement("DELETE FROM savings WHERE uuid = ?");
            del.setString(1, playerId.toString());
            del.executeUpdate();

            plugin.getBankManager().depositMoney(player, total);

            player.sendMessage(ChatUtil.color("&a✔ Bạn đã rút tiết kiệm thành công!"));
            player.sendMessage(ChatUtil.color("&7• Gốc: &f" + ChatUtil.formatVND(amount)));
            player.sendMessage(ChatUtil.color("&7• Lãi: &a" + ChatUtil.formatVND(interest)));
            player.sendMessage(ChatUtil.color("&7• Tổng cộng nhận: &e" + ChatUtil.formatVND(total)));

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendSavingInfo(Player player) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM savings WHERE uuid = ?");
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                player.sendMessage(ChatUtil.color("&7Hiện bạn không có khoản tiết kiệm nào."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            double amount = rs.getDouble("amount");
            int months = rs.getInt("months");
            long createdAt = rs.getLong("created_at");
            long matureAt = rs.getLong("mature_at");
            double rate = plugin.getConfig().getDouble("interest-rates." + months);
            double interest = amount * rate * months;
            double total = amount + interest;

            player.sendMessage(ChatUtil.color("&a📘 Thông tin tiết kiệm:"));
            player.sendMessage(ChatUtil.color("&7• Gửi: &e" + ChatUtil.formatVND(amount)));
            player.sendMessage(ChatUtil.color("&7• Kỳ hạn: &6" + months + " tháng"));
            player.sendMessage(ChatUtil.color("&7• Ngày gửi: &b" + ChatUtil.formatDates(createdAt)));
            player.sendMessage(ChatUtil.color("&7• Đến hạn rút: &b" + ChatUtil.formatDates(matureAt)));
            player.sendMessage(ChatUtil.color("&7• Dự kiến nhận: &a" + ChatUtil.formatVND(total)));

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1.2f);
        } catch (SQLException e) {
            e.printStackTrace();
            player.sendMessage(ChatUtil.color("&cĐã xảy ra lỗi khi truy xuất dữ liệu tiết kiệm."));
        }
    }

    public void forceWithdraw(UUID playerId) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM savings WHERE uuid = ?");
            ps.setString(1, playerId.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null)
                    player.sendMessage(ChatUtil.color("&cBạn không có khoản tiết kiệm nào."));
                return;
            }

            double amount = rs.getDouble("amount");

            // Xoá khoản tiết kiệm
            PreparedStatement del = conn.prepareStatement("DELETE FROM savings WHERE uuid = ?");
            del.setString(1, playerId.toString());
            del.executeUpdate();

            Player player = Bukkit.getPlayer(playerId);
            plugin.getBankManager().depositMoney(player, amount);

            player.sendMessage(ChatUtil.color("&e⚠ Bạn đã rút tiết kiệm sớm và chỉ nhận lại số tiền gốc: &f" +
                    ChatUtil.formatVND(amount)));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1.2f);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}