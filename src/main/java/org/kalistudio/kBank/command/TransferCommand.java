package org.kalistudio.kBank.command;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.bank.BankAccount;
import org.kalistudio.kBank.utils.ChatUtil;

import java.util.Map;
import java.util.UUID;

public class TransferCommand implements CommandExecutor {

    private final KBank plugin;

    public TransferCommand() {
        this.plugin = KBank.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color("&cChỉ người chơi mới dùng được lệnh này."));
            return true;
        }

        if (!plugin.getBankManager().hasAccount(player.getUniqueId())) {
            player.sendMessage(ChatUtil.color("&cBạn chưa có tài khoản ngân hàng."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatUtil.color("&cCách dùng: /ck <tên người chơi | số tài khoản> <số tiền>"));
            return true;
        }

        String target = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtil.color("&cSố tiền không hợp lệ."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatUtil.color("&cSố tiền phải lớn hơn 0."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        UUID senderUUID = player.getUniqueId();
        BankAccount senderAcc = plugin.getBankManager().getAccount(senderUUID);

        // Tìm người nhận qua account number hoặc player name
        BankAccount recipientAcc = getAccountByNumber(target);
        if (recipientAcc == null) {
            Player targetPlayer = Bukkit.getPlayerExact(target);
            if (targetPlayer != null) {
                recipientAcc = plugin.getBankManager().getAccount(targetPlayer.getUniqueId());
            }
        }

        if (recipientAcc == null) {
            player.sendMessage(ChatUtil.color("&cKhông tìm thấy người nhận. Kiểm tra lại tên hoặc số tài khoản."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (recipientAcc.accountNumber.equals(senderAcc.accountNumber)) {
            player.sendMessage(ChatUtil.color("&cBạn không thể chuyển tiền cho chính mình."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        double senderMoney = plugin.getBankManager().getMoney(senderUUID);
        if (senderMoney < amount)
        {
            player.sendMessage(ChatUtil.color("&cBạn không đủ tiền trong tài khoản."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        plugin.getBankManager().withdrawMoney(player, amount); // player là người gửi
        Player recipientPlayer = Bukkit.getPlayerExact(recipientAcc.ownerName);
        if (recipientPlayer != null && recipientPlayer.isOnline()) {
            plugin.getBankManager().depositMoney(recipientPlayer, amount);
        } else {
            // Nếu offline, cộng trực tiếp vào database bằng UUID
            UUID recipientUUID = null;
            for (Map.Entry<UUID, BankAccount> entry : plugin.getBankManager().getAccounts().entrySet()) {
                if (entry.getValue().accountNumber.equals(recipientAcc.accountNumber)) {
                    recipientUUID = entry.getKey();
                    break;
                }
            }

            if (recipientUUID != null) {
                plugin.getBankManager().depositMoney(recipientUUID, amount);
            }
        }


        player.sendMessage(ChatUtil.color("&a✔ Đã chuyển &e" + ChatUtil.formatVND(amount)
                + " &ađến tài khoản &e" + recipientAcc.accountNumber + " &7(" + recipientAcc.ownerName + ")"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        plugin.getBankManager().saveAccountToFile(senderUUID, senderAcc);
        plugin.getBankManager().saveAccountToFile(getUUIDByAccount(recipientAcc), recipientAcc);


        return true;
    }

    private BankAccount getAccountByNumber(String accountNumber) {
        for (Map.Entry<UUID, BankAccount> entry : plugin.getBankManager().getAccounts().entrySet()) {
            if (entry.getValue().accountNumber.equals(accountNumber)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private UUID getUUIDByAccount(BankAccount account) {
        for (Map.Entry<UUID, BankAccount> entry : plugin.getBankManager().getAccounts().entrySet()) {
            if (entry.getValue().accountNumber.equals(account.accountNumber)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
