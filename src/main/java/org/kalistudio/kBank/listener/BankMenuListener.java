package org.kalistudio.kBank.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.gui.BankMenuHolder;
import org.kalistudio.kBank.utils.ChatUtil;

import java.util.Collections;

public class BankMenuListener implements Listener {

    private final KBank plugin;

    public BankMenuListener(KBank plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getView().getTitle().equals(ChatUtil.color("&dChọn thời hạn tiết kiệm"))) {
            e.setCancelled(true);

            if (!(e.getWhoClicked() instanceof Player player)) return;

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) return;

            int months = meta.getCustomModelData();

            player.closeInventory();
            plugin.getGuiManager().openAnvilInput(player,
                    "Nhập số tiền gửi (" + months + " tháng)", amount -> {

                        double bankBalance = plugin.getBankManager().getMoney(player.getUniqueId());
                        double vaultBalance = plugin.getEconomy().getBalance(player);
                        double totalBalance = bankBalance + vaultBalance;

                        if (amount > totalBalance || amount <= 0) {
                            player.sendMessage(ChatUtil.color("&cBạn không đủ tiền để tiết kiệm số tiền này."));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                            return;
                        }

                        // Trừ tiền từ Bank và Vault nếu cần
                        if (bankBalance >= amount) {
                            plugin.getBankManager().withdrawMoney(player, amount);
                        } else {
                            double fromVault = amount - bankBalance;
                            plugin.getBankManager().withdrawMoney(player, bankBalance);
                            plugin.getEconomy().withdrawPlayer(player, fromVault);
                        }

                        plugin.getSavingManager().saveDeposit(player.getUniqueId(), amount, months);

                        player.sendMessage(ChatUtil.color("&a✔ Gửi tiết kiệm thành công &e" +
                                ChatUtil.formatVND(amount) + " &avới kỳ hạn &e" + months + " tháng."));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    });

        }

        if (e.getView().getTitle().equals(ChatUtil.color("&dChọn kỳ hạn gửi thêm"))) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player player)) return;

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (!meta.hasCustomModelData()) return;

            int months = meta.getCustomModelData();
            player.closeInventory();

            plugin.getGuiManager().openAnvilInput(player, "Nhập số tiền gửi (" + months + " tháng)", amount -> {
                double bankBalance = plugin.getBankManager().getMoney(player.getUniqueId());
                double vaultBalance = plugin.getEconomy().getBalance(player);
                double totalBalance = bankBalance + vaultBalance;

                if (amount > totalBalance || amount <= 0) {
                    player.sendMessage(ChatUtil.color("&cBạn không đủ tiền để gửi tiết kiệm."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                // Trừ tiền từ Bank và Vault nếu cần
                if (bankBalance >= amount) {
                    plugin.getBankManager().withdrawMoney(player, amount);
                } else {
                    double fromVault = amount - bankBalance;
                    plugin.getBankManager().withdrawMoney(player, bankBalance);
                    plugin.getEconomy().withdrawPlayer(player, fromVault);
                }

                plugin.getSavingManager().saveDeposit(player.getUniqueId(), amount, months);

                player.sendMessage(ChatUtil.color("&a✔ Đã gửi tiết kiệm &e" +
                        ChatUtil.formatVND(amount) + " &avới kỳ hạn &e" + months + " tháng."));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            });
        }

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof BankMenuHolder)) return;{

        e.setCancelled(true);

            int slot = e.getRawSlot();

            switch (slot) {
                case 10 -> { // Gửi tiền
                    plugin.getGuiManager().openAnvilInput(player, "Gửi tiền", amount -> {
                        double balance = plugin.getEconomy().getBalance(player);
                        if (amount > balance) {
                            player.sendMessage(ChatUtil.color("&cKhông đủ tiền để gửi!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                            return;
                        }
                        plugin.getBankManager().depositMoney(player, amount);
                        player.sendMessage(ChatUtil.lang("gui.deposit-money", "amount", String.valueOf(amount)));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        plugin.getGuiManager().openBankMenu(player);
                    });
                }

                case 12 -> { // Rút tiền
                    plugin.getGuiManager().openAnvilInput(player, "Rút tiền", amount -> {
                        double bank = plugin.getBankManager().getMoney(player.getUniqueId());
                        if (amount > bank) {
                            player.sendMessage(ChatUtil.color("&cBạn không đủ tiền trong ngân hàng!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                            return;
                        }
                        plugin.getBankManager().withdrawMoney(player, amount);
                        player.sendMessage(ChatUtil.color("&a✔ Đã rút &e$" + amount + "&a từ ngân hàng."));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
                        plugin.getGuiManager().openBankMenu(player);
                    });
                }

                case 14 -> { // Vay tiền
                    if (plugin.getLoanManager().hasLoan(player.getUniqueId())) {
                        player.sendMessage(ChatUtil.color("&cBạn đã có khoản vay đang hoạt động!"));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                        return;
                    }

                    plugin.getGuiManager().openAnvilInput(player, "Vay tiền", amount -> {
                        double max = plugin.getConfig().getDouble("max-loan", 10000);
                        if (amount > max) {
                            player.sendMessage(ChatUtil.color("&cSố tiền vượt quá giới hạn vay tối đa (&e" + max + "&c)!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                            return;
                        }
                        plugin.getLoanManager().borrow(player, amount);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        plugin.getGuiManager().openBankMenu(player);
                    });
                }

                case 16 -> { // Gửi tiết kiệm
                    Inventory periodMenu = Bukkit.createInventory(player, 9, ChatUtil.color("&dChọn thời hạn tiết kiệm"));

                    periodMenu.setItem(2, createTimeOption("3 tháng", 3));
                    periodMenu.setItem(4, createTimeOption("6 tháng", 6));
                    periodMenu.setItem(6, createTimeOption("12 tháng", 12));

                    player.openInventory(periodMenu);
                    plugin.getSavingManager().setSelectingPeriod(player.getUniqueId(), true);
                }
            }
        }
    }

    private ItemStack createTimeOption(String label, int months) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtil.color("&6" + label));
        meta.setLore(Collections.singletonList(ChatUtil.color("&7Click để chọn")));
        meta.setCustomModelData(months);
        item.setItemMeta(meta);
        return item;
    }
}