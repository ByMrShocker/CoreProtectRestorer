package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class cpr_test implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return new ArrayList<>();
    }

    @Override
    public String getName() { return "test"; }

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
        System.out.println(player.getLocation().clone().add(new Vector(0, -1, 0)).getBlock().getBlockData().getAsString());
    }


}
