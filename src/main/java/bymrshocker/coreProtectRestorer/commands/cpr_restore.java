package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.data.CoBlock;
import bymrshocker.coreProtectRestorer.data.DebugBlock;
import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class cpr_restore implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return List.of("");
    }

    @Override
    public String getName() { return "restore"; }

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
            if (args.length < 3) {
                player.sendMessage(Component.text("Invalid syntax. Usage: /cpr restore [radius] [UnixTime] [forceBroken]"));
                return;
            }

            boolean forceBroken = false;

            if (args.length > 3) {
                try {
                    forceBroken = Boolean.parseBoolean(args[3]);
                } catch (Exception e) {

                }
            }

            int radius = Integer.parseInt(args[1]);
            long unix = Long.parseLong(args[2]);
            int jobs = 0;

            Location loc = player.getLocation().toBlockLocation();


            player.sendMessage(Component.text("Loading data for " + Math.pow((2 * radius + 1), 3) + " blocks. It may take a while..."));


            World world = player.getWorld();

            int centerX = loc.getBlockX();
            int centerY = loc.getBlockY();
            int centerZ = loc.getBlockZ();

            int minX = centerX - radius;
            int maxX = centerX + radius;
            int minY = Math.max(world.getMinHeight(), centerY - radius);
            int maxY = Math.min(world.getMaxHeight() - 1, centerY + radius);
            int minZ = centerZ - radius;
            int maxZ = centerZ + radius;


            DebugBlock debugBlock = new DebugBlock(player.getLocation(), 0);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Location location = new Location(world, x, y, z);

                        debugBlock.update(location);
                        CoBlock block = CoreProtectRestorer.getInstance().getDatabaseManager().getBlockData(location, forceBroken, unix, debugBlock, null);
                        if (block == null) continue;

                        //System.out.println("Detected at offset: " + vector);
                        //System.out.println("BlockLocation: " + loc.clone().add(vector).toVector());
                        //System.out.println("Origin: " + loc.toVector());

                        restore(block, loc.getWorld(), player);

                        jobs++;
                    }
                }
            }

            debugBlock.destroy();

            //player.sendMessage("Starting restore jobs: " + jobs);
            //Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask1 -> {
            //    for (CoBlock block : blocks) {
            //        block.set(player.getWorld(), player);
            //    }/
            //    player.sendMessage("Blocks restored: " + blocks.size());
            //});
        });


    }

    private void restore(CoBlock block, World world, Player player) {
        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
           block.set(world, player);
        });
    }

}
