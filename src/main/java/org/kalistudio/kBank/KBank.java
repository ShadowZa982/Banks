package org.kalistudio.kBank;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.kalistudio.kBank.bank.BankManager;
import org.kalistudio.kBank.command.AdminBankCommand;
import org.kalistudio.kBank.command.BankCommand;
import org.kalistudio.kBank.command.BankTabCompleter;
import org.kalistudio.kBank.command.TransferCommand;
import org.kalistudio.kBank.gui.GUIManager;
import org.kalistudio.kBank.listener.BankMenuListener;
import org.kalistudio.kBank.loan.DatabaseManager;
import org.kalistudio.kBank.loan.LoanManager;
import org.kalistudio.kBank.loan.SavingManager;
import org.kalistudio.kBank.papi.PapiExpansion;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class KBank extends JavaPlugin {

    private static KBank instance;

    private BankManager bankManager;
    public BankManager getBankManager() { return bankManager; }

    private LoanManager loanManager;
    public LoanManager getLoanManager() { return loanManager; }

    private GUIManager guiManager;
    public GUIManager getGuiManager() { return guiManager; }

    private Economy economy;
    public Economy getEconomy() { return economy; }

    private SavingManager savingManager;
    private DatabaseManager database;


    private FileConfiguration lang;
    private File langFile;

    public static KBank getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        reloadLang();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault không được cài!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();

        // ✅ Đúng thứ tự khởi tạo
        database = new DatabaseManager(this);
        bankManager = new BankManager(this);
        loanManager = new LoanManager(this);
        savingManager = new SavingManager(this);

        guiManager = new GUIManager(this);
        new BankMenuListener(this);
        getCommand("bank").setExecutor(new BankCommand());
        getCommand("bank").setTabCompleter(new BankTabCompleter());
        getCommand("kbank").setExecutor(new AdminBankCommand());
        getCommand("ck").setExecutor(new TransferCommand());

        // ✅ Chờ 1 tick để PlaceholderAPI chắc chắn đã được load
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new PapiExpansion(this).register();
                getLogger().info("✅ Đăng ký thành công");
            } else {
                getLogger().warning("❌ PlaceholderAPI vẫn chưa được bật sau delay!");
            }
        }, 1L);

        loanManager.loadAllLoans();
        bankManager.loadAccounts();

        logBanner();
        notifyDiscord();
    }

    @Override
    public void onDisable() {
        getLogger().info("&ckBank đã được tắt!");
        bankManager.saveAllAccounts();
        if (bankManager != null) {
            try {
                bankManager.getConnection().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getLang() {
        if (lang == null) reloadLang();
        return lang;
    }

    public SavingManager getSavingManager() {
        return savingManager;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public void reloadLang() {
        if (langFile == null) {
            langFile = new File(getDataFolder(), "lang.yml");
        }

        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    private void logBanner() {
        logWithColor("&b===== &fKazami Studio &b=====");
        logWithColor("&7[&a✔&7] &aSystems have been started..");
        logWithColor("&7[&a✔&7] &fVersion: " + getDescription().getVersion());
        logWithColor("&7[&a✔&7] &fAuthor by &bKazami Studio");
        logWithColor("&7[&a✔&7] &9Discord: https://discord.gg/kQsg6JyT");
        logWithColor("&7[&a✔&7] " + getDescription().getName() + " started successfully!");
        logWithColor("&b===== &fKazami Studio &b=====");
    }

    private void logWithColor(String msg) {
        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public void notifyDiscord() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL("http://gold06.vpsbumboo.com:31205/notify");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                String motd = Bukkit.getMotd();
                String cleanMotd = stripColorCodes(motd);

                String pluginName = getDescription().getName(); // Lấy tên plugin từ plugin.yml
                String json = String.format(
                        "{\"server\":\"%s\",\"status\":\"Đang hoạt động\",\"plugin\":\"%s\",\"info\":\"Đã sử dụng plugin của Kazami Studio\"}",
                        cleanMotd, pluginName
                );

                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = con.getResponseCode();
                getLogger().info("Đã gửi thông báo tới Discord với mã HTTP: " + code);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static String stripColorCodes(String input) {
        // Loại bỏ mã hex trước (gồm chuỗi như §x§r§g§b§c§d§e)
        input = input.replaceAll("§x(§[0-9A-Fa-f]){6}", "");

        // Loại bỏ mã màu thường
        return input.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
