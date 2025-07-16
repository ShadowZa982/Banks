package org.kalistudio.kBank.loan;

import org.kalistudio.kBank.KBank;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final File dbFile;
    private final String url;

    public DatabaseManager(KBank plugin) {
        this.dbFile = new File(plugin.getDataFolder(), "saving.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        plugin.getLogger().info("SQLite path: " + url);
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
