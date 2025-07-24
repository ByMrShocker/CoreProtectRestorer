package bymrshocker.coreProtectRestorer.data;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import bymrshocker.shockerfunctionlibrary.math.KismetMathLibrary;
import bymrshocker.shockerfunctionlibrary.system.database.DBResultSet;
import bymrshocker.shockerfunctionlibrary.system.database.DatabaseManager;
import net.coreprotect.CoreProtect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RollbackProcess {

    public static int BATCH_SIZE = 10000;

    private final Location center;
    private final Player player;
    private final int radius;
    private final boolean forceBroken;
    private final List<Long> batch;
    private World world;

    private final int index;
    private long current;
    private int currentIndex;

    DebugBlock debugBlock;
    HashMap<Vector, DBResultSet> threadUpdates;

    public RollbackProcess(Location center, Player player, int radius, boolean forceBroken, List<Long> batch, int index, World world) {
        this.center = center;
        this.player = player;
        this.radius = radius;
        this.forceBroken = forceBroken;
        this.batch = batch;
        this.debugBlock = null;
        this.index = index;
        threadUpdates = new HashMap<>();
        this.world = world;
        current = 0;
        currentIndex = -1;
    }

    private void createDebug(int index) {
        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
            debugBlock = new DebugBlock(center, index);
        });
    }

    public void run() {
        createDebug(index);

        EDatabaseManager database = new EDatabaseManager(CoreProtect.getInstance().getDataFolder(), "", "database");

        while (world == null) {
            try {
                wait(5);
                world = CoreProtectRestorer.getInstance().getDatabaseManager().getRollbackWorld();

            } catch (InterruptedException e) {

            }
        }


        for (Long l : batch) {
            if (!CoreProtectRestorer.getInstance().getDatabaseManager().isRollbackActive()) {
                database.disconnect();
                destroy();
                return;
            }

            current = l;
            currentIndex++;



            DBResultSet rs = database.getStatementFromDB(EDatabaseManager.CO_BLOCK, "time", l.intValue());
            if (rs == null) continue;
            if (rs.get("wid") == null) continue;

            int worldID = rs.getInt("wid");
            String worldName = database.getStringFromDB(EDatabaseManager.CO_WORLD, worldID, "world");

            if (!world.getName().equals(worldName)) continue;

            int x = rs.getInt("x");
            int y = rs.getInt("y");
            int z = rs.getInt("z");

            if (debugBlock != null) {
                debugBlock.getText().text(Component.text(currentIndex + ". x: " + x + ", y: " + y + ", z: " + z));
            }

            Location location = new Location(world, x, y, z);
            if (!KismetMathLibrary.isLocationInBox(
                 location,
                 new Location(world, center.getBlockX() - radius, center.getBlockY() - radius, center.getBlockZ() - radius),
                 new Location(world, center.getBlockX() + radius, center.getBlockY() + radius, center.getBlockZ() + radius)
            )) continue;


            if (debugBlock != null) {
                debugBlock.update(location);
            }


            if (forceBroken) {
                if (rs.getInt("action") != 1) continue;
            }


            //player.sendMessage(Component.text("#" + index + ": found change at location: " + x + ", " + y + ", " + z + ". Material: " + rs.getInt("type") + ". Unix: " + l));


            threadUpdates.put(location.toVector(), rs);

        }

        player.sendMessage(Component.text("#" + index + ": Complete indexing. Resulting.."));


        HashMap<Vector, RollbackResult> coBlocks = new HashMap<>();
        threadUpdates.forEach((vector, rs) -> {
            CoBlock block = new CoBlock();

            block.x = rs.getInt("x");
            block.y = rs.getInt("y");
            block.z = rs.getInt("z");

            byte[] bBlockData = (byte[]) rs.get("blockdata");


            String sMaterial = database.getStringFromDB(EDatabaseManager.CO_MATERIAL_MAP, rs.getInt("type"), "material");
            block.material = Material.matchMaterial(sMaterial);

            if (bBlockData != null && bBlockData.length > 0) {
                block.blockData = database.deserializeBlockData(bBlockData, sMaterial);
            } else {
                block.blockData = null;
            }

            coBlocks.put(vector, new RollbackResult(block, rs.getLong("time"), vector));
        });

        player.sendMessage(Component.text("#" + index + ": Finished joB!"));

        database.disconnect();

        CoreProtectRestorer.getInstance().getDatabaseManager().onRollbackProcessEnd(coBlocks, player);

        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
            destroy();
        });

    }

    public void destroy() {
        debugBlock.destroy();
    }

    public double getProgress() {
        return Math.clamp(KismetMathLibrary.inverseLerp(0, batch.size(), currentIndex), 0, 1);
    }

    public DebugBlock getDebugBlock() {
        return debugBlock;
    }

}
