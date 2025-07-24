package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class cpr_ implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return new ArrayList<>();
    }

    @Override
    public String getName() { return "aitest"; }

    @Override
    public String getDescription() { return "example"; }

    @Override
    public String getSyntax() { return "/ssv example"; }

    @Override
    public String getPermission() {
        return "sre.admin";
    }

    @Override
    public void execute(Player player, String[] args) {


    }


}
