package org.kalistudio.kBank.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.kalistudio.kBank.KBank;
import org.kalistudio.kBank.loan.LoanData;
import org.kalistudio.kBank.utils.ChatUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class AdminBankCommand implements CommandExecutor {

    private final KBank plugin;

    public AdminBankCommand() {
        this.plugin = KBank.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("kbank.admin")) {
            sender.sendMessage(ChatUtil.color("&cBạn không có quyền dùng lệnh này."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtil.color("&cSử dụng: /kbank reload | loans"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                // Lưu dữ liệu hiện tại
                plugin.getBankManager().saveAllAccounts();

                // Reload config và lang
                plugin.reloadConfig();
                plugin.reloadLang();

                // Tải lại dữ liệu tài khoản
                plugin.getBankManager().loadAccounts();

                sender.sendMessage(ChatUtil.lang("reload-success"));

                // Thêm âm thanh xác nhận nếu là người chơi
                if (sender instanceof org.bukkit.entity.Player player) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                }
            }


            case "loans" -> {
                Map<UUID, LoanData> loans = plugin.getLoanManager().getAllLoans();

                if (loans.isEmpty()) {
                    sender.sendMessage(ChatUtil.color("&7Không có khoản vay nào đang hoạt động."));
                    if (sender instanceof org.bukkit.entity.Player player) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.2f);
                    }
                    return true;
                }

                sender.sendMessage(ChatUtil.color("&a===== Danh sách khoản vay ====="));
                for (Map.Entry<UUID, LoanData> entry : loans.entrySet()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                    LoanData loan = entry.getValue();

                    String dueDate;
                    if (loan.timeLeft <= 0) {
                        dueDate = "Hết hạn";
                    } else {
                        long millis = System.currentTimeMillis() + loan.timeLeft * 1000L;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                        dueDate = sdf.format(new Date(millis));
                    }

                    String borrowed = ChatUtil.formatVND(loan.borrowed);
                    String totalToPay = ChatUtil.formatVND(loan.totalToPay);

                    sender.sendMessage(ChatUtil.color(String.format(
                            "&e%s&7: Đã vay &a%s&7, cần trả &c%s&7, hạn đến &b%s",
                            offlinePlayer.getName(),
                            borrowed,
                            totalToPay,
                            dueDate
                    )));
                    if (sender instanceof org.bukkit.entity.Player player) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    }
                }
            }

            default -> sender.sendMessage(ChatUtil.color("&cLệnh không hợp lệ. Dùng: /kbank reload | loans"));
        }

        return true;
    }
}