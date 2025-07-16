package org.kalistudio.kBank.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BankTabCompleter implements TabCompleter {

    private static final List<String> ROOT_SUBS = Arrays.asList("check", "create", "saving");

    private static final List<String> SAVING_SUBS = Arrays.asList("withdraw", "earlywithdraw", "list", "menu");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            return partial(args[0], ROOT_SUBS);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("saving")) {
            return partial(args[1], SAVING_SUBS);
        }

        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}