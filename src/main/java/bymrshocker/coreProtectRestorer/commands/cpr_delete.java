package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import bymrshocker.shockerfunctionlibrary.system.database.DBResultSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class cpr_delete implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return new ArrayList<>();
    }

    @Override
    public String getName() { return "delete"; }

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
        if (args.length < 2) {
            player.sendMessage(Component.text("ERR. usage: /spr delete [UnixTime]"));
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i < 1) continue;
            builder.append(args[i]);
            if (args.length - 1 != i) builder.append(" ");
        }

        String sql = "DELETE FROM " + EDatabaseManager.CO_BLOCK + " WHERE " + builder.toString(); //time > 1000
        System.out.println("Executing deletion sql: " + sql);

        Bukkit.getScheduler().runTaskAsynchronously(CoreProtectRestorer.getInstance(), bukkitTask -> {
            try (PreparedStatement pstmt = CoreProtectRestorer.getInstance().getDatabaseManager().getConnection().prepareStatement(sql)) {
                int affected = pstmt.executeUpdate();

                player.sendMessage(Component.text("Deleted " + affected + " rows!"));

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });


    }


}
