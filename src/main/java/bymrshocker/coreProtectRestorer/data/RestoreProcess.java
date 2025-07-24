package bymrshocker.coreProtectRestorer.data;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import bymrshocker.shockerfunctionlibrary.math.KismetMathLibrary;
import bymrshocker.shockerfunctionlibrary.system.database.DBResultSet;
import net.coreprotect.CoreProtect;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestoreProcess {
    Location center;
    int radius;
    int threadMinY;
    int threadMaxY;
    boolean forceBroken;

    EDatabaseManager localDB;
    EDatabaseManager globalDB;

    int threadIndex;

    DebugBlock debugBlock;
    Player player;

    int currentBlock = 0;
    int totalBlocks = 0;

    public boolean isCanceled;

    public RestoreProcess(Location center, int radius, int threadMinY, int threadMaxY, boolean forceBroken, int threadIndex, Player player) {
        this.center = center;
        this.radius = radius;
        this.threadMinY = threadMinY;
        this.threadMaxY = threadMaxY;
        this.forceBroken = forceBroken;
        this.threadIndex = threadIndex;
        this.player = player;
    }

    public void createDebugBlock() {
        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
            debugBlock = new DebugBlock(center, threadIndex);
        });
    }

    public void connect(EDatabaseManager globalDB) {
        this.globalDB = globalDB;
        localDB = new EDatabaseManager(CoreProtect.getInstance().getDataFolder(), "", "database");
    }

    private static List<Material> airMats = List.of(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR);

    public void processYLayer(Location center, int radius, int minY, int maxY) {

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        World world = center.getWorld();

        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;

        totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        currentBlock = 0;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {

                    currentBlock++;

                    Location location = new Location(world, x, y, z);
                    if (!airMats.contains(location.getBlock().getType())) continue;

                    if (isCanceled) {
                        if (debugBlock != null) debugBlock.destroy();
                        return;
                    }

                    if (debugBlock != null) {
                        debugBlock.update(location);
                        //debugBlock.text.teleportAsync(new Location(world, x, y, z).add(new Vector(0.5, 0.5, 0.5)));
                        debugBlock.updateText("#" + threadIndex);
                    }

                    CoBlock coBlock = localDB.getBlockData(location, forceBroken, 0, debugBlock, null);

                    if (coBlock == null) continue;

                    restore(coBlock, world, player);

                }
            }
        }

        if (debugBlock != null) debugBlock.destroy();
        localDB.disconnect();

        CoreProtectRestorer.getInstance().getRestoreProcesses().remove(this);

    }

    private Map<Vector, List<DBResultSet>> cacheJob(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, World world) {

        Map<Vector, List<DBResultSet>> resultSetList = new HashMap<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location location = new Location(world, x, y, z);

                    resultSetList.put(location.toVector(), localDB.findIdsByBlock(location, 0, null));
                }
            }
        }

        return resultSetList;
    }



    private void restore(CoBlock block, World world, Player player) {
        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
            world.spawnParticle(Particle.POOF, new Location(world, block.x, block.y, block.z), 0);
            block.set(world, player);
        });
    }

    public DebugBlock getDebugBlock() {
        return debugBlock;
    }

    public double getProgress() {
        return Math.clamp(KismetMathLibrary.inverseLerp(0, Math.max(totalBlocks, 1), currentBlock), 0, 1);
    }
}
