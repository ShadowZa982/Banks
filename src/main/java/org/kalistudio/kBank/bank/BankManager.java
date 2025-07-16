package org.kalistudio.kBank.bank;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.utils.ChatUtil;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Date;

public class BankManager {

    private final KBank plugin;
    private Connection connection;

    private final Map<UUID, BankAccount> accounts = new HashMap<>();


    public BankManager(KBank plugin) {
        this.plugin = plugin;
        connect();
        createTable();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/data.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS bank_data (" +
                "uuid TEXT PRIMARY KEY," +
                "money DOUBLE DEFAULT 0" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load player record or create if not exist
    private void ensureAccount(UUID uuid) {
        ensureConnected(); // Thêm dòng này
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO bank_data (uuid) VALUES (?)")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void ensureConnected() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // ===================== MONEY ========================

    public double getMoney(UUID uuid) {
        ensureConnected(); // Thêm dòng này
        ensureAccount(uuid);
        try (PreparedStatement stmt = connection.prepareStatement("SELECT money FROM bank_data WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.getDouble("money");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void depositMoney(Player player, double amount) {
        ensureConnected(); // Thêm dòng này
        UUID uuid = player.getUniqueId();
        ensureAccount(uuid);

        // Trừ tiền qua Vault
        plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)
                .getProvider().withdrawPlayer(player, amount);

        // Cộng vào ngân hàng
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE bank_data SET money = money + ? WHERE uuid=?")) {
            stmt.setDouble(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void withdrawMoney(Player player, double amount) {
        ensureConnected(); // Thêm dòng này
        UUID uuid = player.getUniqueId();
        ensureAccount(uuid);

        if (getMoney(uuid) < amount) return;

        // Trừ khỏi ngân hàng
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE bank_data SET money = money - ? WHERE uuid=?")) {
            stmt.setDouble(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cộng cho player
        plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)
                .getProvider().depositPlayer(player, amount);
    }

    // ================== TIỆN ÍCH =========================

    public Connection getConnection() {
        ensureConnected();
        return this.connection;
    }

    public Map<UUID, BankAccount> getAccounts() {
        return accounts;
    }

    public boolean hasAccount(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    public void createAccount(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasAccount(uuid)) return;

        String playerName = player.getName();
        String accountNumber = generateRandomAccountNumber();
        Date createdAt = new Date();

        BankAccount account = new BankAccount(playerName, accountNumber, createdAt, 0.0);
        accounts.put(uuid, account);

        saveAccountToFile(uuid, account); // Lưu lại khi tạo
    }

    public void depositMoney(UUID uuid, double amount) {
        ensureConnected();
        ensureAccount(uuid);

        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE bank_data SET money = money + ? WHERE uuid=?")) {
            stmt.setDouble(1, amount);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateRandomAccountNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    public BankAccount getAccount(UUID uuid) {
        return accounts.get(uuid);
    }

    public void saveAccountToFile(UUID uuid, BankAccount account) {
        File file = new File(plugin.getDataFolder(), "accounts.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = uuid.toString();
        config.set(path + ".owner", account.ownerName);
        config.set(path + ".accountNumber", account.accountNumber);
        config.set(path + ".createdAt", account.createdAt.getTime()); // Lưu dạng timestamp
        config.set(path + ".balance", account.balance);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAccounts() {
        accounts.clear(); // Làm sạch dữ liệu cũ

        File file = new File(plugin.getDataFolder(), "accounts.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String uuidStr : config.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String owner = config.getString(uuidStr + ".owner");
            String accountNumber = config.getString(uuidStr + ".accountNumber");
            long createdMillis = config.getLong(uuidStr + ".createdAt");
            double balance = config.getDouble(uuidStr + ".balance");

            BankAccount account = new BankAccount(owner, accountNumber, new Date(createdMillis), balance);
            accounts.put(uuid, account);
        }

        Bukkit.getLogger().info("[kBank] Đã tải " + accounts.size() + " tài khoản ngân hàng.");
    }

    public void saveAllAccounts() {
        File file = new File(plugin.getDataFolder(), "accounts.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, BankAccount> entry : accounts.entrySet()) {
            UUID uuid = entry.getKey();
            BankAccount acc = entry.getValue();

            String path = uuid.toString();
            config.set(path + ".owner", acc.ownerName);
            config.set(path + ".accountNumber", acc.accountNumber);
            config.set(path + ".createdAt", acc.createdAt.getTime());
            config.set(path + ".balance", acc.balance);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}