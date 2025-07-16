package org.kalistudio.kBank.command;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.bank.BankAccount;
import org.kalistudio.kBank.loan.LoanData;
import org.kalistudio.kBank.utils.ChatUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BankCommand implements CommandExecutor {

    private final KBank plugin;

    public BankCommand() {
        this.plugin = KBank.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.color("&cChỉ người chơi mới dùng được lệnh này."));
            return true;
        }

        // /bank check → kiểm tra trạng thái khoản vay
        if (args.length >= 1 && args[0].equalsIgnoreCase("check")) {
            LoanData loan = plugin.getLoanManager().getLoan(player.getUniqueId());
            if (loan == null) {
                player.sendMessage(ChatUtil.color("&7Hiện tại bạn không có khoản vay nào."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            String borrowed = ChatUtil.formatVND(loan.borrowed);
            String total = ChatUtil.formatVND(loan.totalToPay);

            String dueDate;
            if (loan.timeLeft <= 0) {
                dueDate = "Hết hạn";
            } else {
                long dueMillis = System.currentTimeMillis() + loan.timeLeft * 1000L;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                dueDate = sdf.format(new Date(dueMillis));
            }

            player.sendMessage(ChatUtil.color("&a===== Trạng thái khoản vay ====="));
            player.sendMessage(ChatUtil.color("&7• Đã vay: &a" + borrowed));
            player.sendMessage(ChatUtil.color("&7• Cần trả: &c" + total));
            player.sendMessage(ChatUtil.color("&7• Hạn thanh toán: &b" + dueDate));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("create")) {
            if (plugin.getBankManager().hasAccount(player.getUniqueId())) {
                player.sendMessage(ChatUtil.color("&cBạn đã có tài khoản ngân hàng rồi!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            plugin.getBankManager().createAccount(player);
            BankAccount acc = plugin.getBankManager().getAccount(player.getUniqueId());

            player.sendMessage(ChatUtil.color("&a✔ Tạo tài khoản thành công!"));
            player.sendMessage(ChatUtil.color("&7Tên chủ tài khoản: &e" + acc.ownerName));
            player.sendMessage(ChatUtil.color("&7Số tài khoản: &e" + acc.accountNumber));
            player.sendMessage(ChatUtil.color("&7Ngày tạo: &e" +
                    new SimpleDateFormat("dd/MM/yyyy").format(acc.createdAt)));

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("saving") && args[1].equalsIgnoreCase("withdraw")) {
            plugin.getSavingManager().withdrawSaving(player.getUniqueId());
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("saving") && args[1].equalsIgnoreCase("list")) {
            plugin.getSavingManager().sendSavingInfo(player);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("saving") && args[1].equalsIgnoreCase("earlywithdraw")) {
            plugin.getSavingManager().forceWithdraw(player.getUniqueId());
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("saving") && args[1].equalsIgnoreCase("menu")) {
            plugin.getGuiManager().openSavingMenu(player);
            return true;
        }

        plugin.getGuiManager().openBankMenu(player);
        return true;
    }
}
