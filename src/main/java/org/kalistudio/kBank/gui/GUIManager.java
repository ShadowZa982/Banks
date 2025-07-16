package org.kalistudio.kBank.gui;

import net.wesjd.anvilgui.AnvilGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.bank.BankAccount;
import org.kalistudio.kBank.loan.LoanData;
import org.kalistudio.kBank.loan.SavingData;
import org.kalistudio.kBank.utils.ChatUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GUIManager {

    private final KBank plugin;

    public GUIManager(KBank plugin) {
        this.plugin = plugin;
    }

    public void openBankMenu(Player player) {
        if (!plugin.getBankManager().hasAccount(player.getUniqueId())) {
            player.sendMessage(ChatUtil.color("&cBạn chưa có tài khoản ngân hàng. Dùng &e/bank create &cđể tạo."));
            return;
        }

        Inventory inv = Bukkit.createInventory(new BankMenuHolder(), 3 * 9,
                ChatUtil.color(plugin.getLang().getString("menu.bank.title")));

        inv.setItem(10, createItem("menu.bank.deposit"));
        inv.setItem(12, createItem("menu.bank.withdraw"));
        inv.setItem(14, createItem("menu.bank.loan"));
        inv.setItem(16, createItem("menu.bank.saving"));

        double bankMoney = plugin.getBankManager().getMoney(player.getUniqueId());
        LoanData loan = plugin.getLoanManager().getLoan(player.getUniqueId());

        BankAccount acc = plugin.getBankManager().getAccount(player.getUniqueId());
        String accountNum = acc.accountNumber;
        String createdDate = new SimpleDateFormat("dd/MM/yyyy").format(acc.createdAt);

        String formattedBank = ChatUtil.formatVND(bankMoney);
        String formattedLoan = (loan != null) ? ChatUtil.formatVND(loan.totalToPay) : "Không có";
        String formattedDue = (loan != null) ? ChatUtil.formatDate(loan.timeLeft) : "Không có";

        SavingData saving = plugin.getSavingManager().getSaving(player.getUniqueId());
        String formattedSaving = (saving != null)
                ? ChatUtil.formatVND(saving.getAmount())
                : "Không có";

        String savingDue = (saving != null)
                ? new SimpleDateFormat("dd/MM/yyyy").format(saving.getMatureAt())
                : "Không có";

        inv.setItem(22, createDynamicItem("menu.bank.info",
                "bank_money", formattedBank,
                "loan", formattedLoan,
                "due", formattedDue,
                "account", accountNum,
                "created", createdDate,
                "player", player.getName(),
                "saving", formattedSaving,
                "saving_due", savingDue
        ));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        player.openInventory(inv);
    }

    public void openSavingMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatUtil.color("&dChọn kỳ hạn gửi thêm"));

        inv.setItem(2, createTimeOption("3 tháng", 3));
        inv.setItem(4, createTimeOption("6 tháng", 6));
        inv.setItem(6, createTimeOption("12 tháng", 12));

        player.openInventory(inv);
    }

    private ItemStack createTimeOption(String name, int months) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtil.color("&e" + name));
        meta.setLore(List.of(ChatUtil.color("&7Click để gửi tiết kiệm " + months + " tháng")));
        meta.setCustomModelData(months);
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack createItem(String path) {
        String name = plugin.getLang().getString(path + ".name", "Tên mặc định");
        List<String> lore = plugin.getLang().getStringList(path + ".lore");
        int model = plugin.getLang().getInt(path + ".model", 0);
        String matStr = plugin.getLang().getString(path + ".material", "STONE");
        Material material = Material.getMaterial(matStr.toUpperCase());

        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.color(name));
            if (!lore.isEmpty()) meta.setLore(ChatUtil.colorList(lore));
            if (model > 0) meta.setCustomModelData(model);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDynamicItem(String path, String... replacements) {
        String name = plugin.getLang().getString(path + ".name", "Thông tin");
        List<String> lore = plugin.getLang().getStringList(path + ".lore");
        int model = plugin.getLang().getInt(path + ".model", 0);
        String materialName = plugin.getLang().getString(path + ".material", "BOOK");

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.BOOK; // fallback nếu sai tên

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.color(name));

            List<String> replacedLore = lore.stream()
                    .map(line -> {
                        for (int i = 0; i < replacements.length - 1; i += 2) {
                            String key = replacements[i];
                            String value = replacements[i + 1];
                            line = line.replace("{" + key + "}", value)
                                    .replace("%" + key + "%", value); // hỗ trợ cả hai
                        }
                        return ChatUtil.color(line);
                    })
                    .collect(Collectors.toList());
            meta.setLore(replacedLore);

            if (model > 0) meta.setCustomModelData(model);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openAnvilInput(Player player, String title, InputCallback callback) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(title)
                .text("Nhập số...")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String text = stateSnapshot.getText();
                    try {
                        double value = Double.parseDouble(text);
                        callback.onInput(value);
                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                    } catch (NumberFormatException e) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Chỉ nhập số!"));
                    }
                })
                .open(player);
    }

    public interface InputCallback {
        void onInput(double value);
    }
}