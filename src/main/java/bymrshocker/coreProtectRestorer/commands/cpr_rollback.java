package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.data.CoBlock;
import bymrshocker.coreProtectRestorer.data.DebugBlock;
import bymrshocker.coreProtectRestorer.data.RollbackProcess;
import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class cpr_rollback implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return List.of("");
    }

    @Override
    public String getName() { return "rollback"; }

    @Override
    public String getDescription() { return "example"; }

    @Override
    public String getSyntax() { return "/ssv example"; }

    @Override
    public String getPermission() {
        return "cpr.admin";
    }

    @Override
    public void execute(Player player, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(CoreProtectRestorer.getInstance(), bukkitTask -> {
            if (args.length < 4) {
                player.sendMessage(Component.text("Invalid syntax. Usage: /cpr rollback [radius] [startTime] [endTime] [forceBroken] [batchSize]"));
                return;
            }

            boolean forceBroken = false;

            if (args.length > 4) {
                try {
                    forceBroken = Boolean.parseBoolean(args[4]);
                } catch (Exception e) {

                }
            }

            int batchSize = RollbackProcess.BATCH_SIZE;

            if (args.length > 5) {
                try {
                    batchSize = Integer.parseInt(args[5]);
                    System.out.println("Batch override detected. New batch size: " + batchSize);
                } catch (Exception e) {

                }
            }

            int radius = Integer.parseInt(args[1]);
            long unix = Long.parseLong(args[2]);
            long endUnix = Long.parseLong(args[3]);

            Location loc = player.getLocation().toBlockLocation();


            player.sendMessage(Component.text("Loading data for " + Math.pow((2 * radius + 1), 3) + " blocks. It may take a while..."));

            List<CoBlock> blocks = new ArrayList<>();

            System.out.println("Running rollback with batchSize: " + batchSize);

            CoreProtectRestorer.getInstance().getDatabaseManager().runRollback(loc, radius, forceBroken, unix, player, batchSize, endUnix);
        });


    }


}
