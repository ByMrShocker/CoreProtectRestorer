package bymrshocker.coreProtectRestorer.database;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.data.CoBlock;
import bymrshocker.coreProtectRestorer.data.DebugBlock;
import bymrshocker.coreProtectRestorer.data.RollbackProcess;
import bymrshocker.coreProtectRestorer.data.RollbackResult;
import bymrshocker.shockerfunctionlibrary.system.database.DBResultSet;
import bymrshocker.shockerfunctionlibrary.system.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EDatabaseManager extends DatabaseManager {

    public static String CO_BLOCK = "co_block",
            CO_BLOCKDATA_MAP = "co_blockdata_map",
            CO_MATERIAL_MAP = "co_material_map",
            CO_WORLD = "co_world";

    public EDatabaseManager(File path, String subfolder, String name) {
        super(path, subfolder, name);
        futures = new ArrayList<>();
        processes = new ArrayList<>();
    }

    private boolean rollbackActive = false;
    private HashMap<Vector, List<RollbackResult>> blocks;
    private int processesComplete;
    private List<RollbackProcess> processes;
    private World rollbackWorld;
    List<CompletableFuture<Void>> futures;

    private int restoreIndex = -1;

    @Override
    public void initializeDatabase(Connection connection) {
        System.out.println("DATABASE NOT FOUND!");
    }


    public void runRollback(Location location, int radius, boolean forceBroken, long unixTime, Player player, int batchSize, long endUnix) {
        if (rollbackActive) {
            player.sendMessage(Component.text("Failed to start. Rollback already active!"));
            return;
        }

        killFutures();

        blocks = new HashMap<>();
        processesComplete = 0;
        rollbackWorld = location.getWorld();

        List<Long> batch = new ArrayList<>(batchSize);
        List<Long> times = getLongColumns(CO_BLOCK, "time");


        player.sendMessage("Total recordings: " + times.size());


        int threadIndex = 0;
        for (long value : times) {
            if (value < unixTime || value > endUnix) continue;

            batch.add(value);

            if (batch.size() >= batchSize) {
                processes.add(new RollbackProcess(location, player, radius, forceBroken, new ArrayList<>(batch), threadIndex, getRollbackWorld()));
                player.sendMessage(Component.text("Created new RollbackProcess. Batch: " + batch.getFirst() + "-" + batch.getLast()));
                batch.clear();
                threadIndex++;
            }
        }


        if (!batch.isEmpty()) {
            processes.add(new RollbackProcess(location, player, radius, forceBroken, new ArrayList<>(batch), threadIndex, getRollbackWorld()));
            player.sendMessage(Component.text("Created new RollbackProcess. Batch: " + batch.getFirst() + "-" + batch.getLast()));
        }

        rollbackActive = true;
        restoreIndex = -1;

        player.sendMessage(Component.text("Created " + processes.size() + " working threads."));

        createDisplayTimer(player);

        for (RollbackProcess process : processes) {
            futures.add(CompletableFuture.runAsync(process::run));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();


    }

    private void createDisplayTimer(Player player) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(CoreProtectRestorer.getInstance(), bukkitTask -> {
            if (!isRollbackActive()) {
                bukkitTask.cancel();
                return;
            }

            if (restoreIndex == -1) {
                double vals = 0;
                for (RollbackProcess process : processes) {
                    vals += process.getProgress();
                }

                player.sendActionBar(Component.text("lookup: %.2f".formatted((vals / processes.size()) * 100f) + "%"));
            } else {
                if (blocks.isEmpty()) return;

                for (RollbackProcess process : processes) {
                    if (process.getDebugBlock() != null) {
                        process.getDebugBlock().getText().text(Component.text("lookup: %.2f".formatted((restoreIndex / blocks.size()) * 100f) + "%"));
                    }
                }
                player.sendActionBar(Component.text("rollback: %.2f".formatted((restoreIndex / blocks.size()) * 100f) + "%"));

            }



        }, 5, 5);
    }

    public void onRollbackProcessEnd(HashMap<Vector, RollbackResult> blocks, Player player) {

        blocks.forEach((vector, rollbackResult) -> {
            List<RollbackResult> res = this.blocks.getOrDefault(vector, new ArrayList<>());

            res.add(rollbackResult);

            this.blocks.put(vector, res);
        });

        processesComplete++;

        player.sendMessage(Component.text("Lookup Thread finished: " + processesComplete));



        if (processesComplete >= processes.size()) {

            restoreIndex = 0;

            player.sendMessage(Component.text("Starting rollback"));

            List<Vector> keys = blocks.keySet().stream().toList();

            Bukkit.getScheduler().runTaskTimer(CoreProtectRestorer.getInstance(), bukkitTask -> {

                for (int i = 0; i < 20; i++) {

                    if (restoreIndex >= keys.size()) {
                        rollbackActive = false;
                        restoreIndex = -1;
                        player.sendMessage(Component.text("Rollback COMPLETE!"));
                        bukkitTask.cancel();
                        return;
                    }


                    long bestTime = -1;
                    RollbackResult bestResult = null;
                    for (RollbackResult rr : this.blocks.get(keys.get(restoreIndex))) {
                        if (rr.getTime() <= bestTime) continue;
                        bestResult = rr;
                        bestTime = rr.getTime();
                    }

                    if (bestTime == -1) return;

                    bestResult.getCoBlock().set(getRollbackWorld(), player);

                    restoreIndex++;
                }

            }, 10, 10);
        }
    }

    public List<Long> getLongColumns(String table, String column) {
        List<Long> values = new ArrayList<>();
        String sql = "SELECT " + column + " FROM " + table;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                values.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return values;
    }

    public void killFutures() {
        rollbackActive = false;
        processes.forEach(RollbackProcess::destroy);
        processes.clear();
    }

    private static List<String> ignoreMats = List.of("minecraft:dirt", "minecraft:copper_ore", "minecraft:iron_ore");

    public CoBlock getBlockData(Location location, boolean forceBroken, long unixTime, DebugBlock debugBlock, List<DBResultSet> cache) {

        long bestTime = 0;
        int bestID = -1;

        long lastBreakTime = -1;


        List<DBResultSet> resultSetList;
        if (cache == null) {
            resultSetList = findIdsByBlock(location, unixTime, debugBlock);
        } else {
            resultSetList = cache;
        }

        if (resultSetList.isEmpty()) return null;

        //System.out.println("Found " + resultSetList.size() + " changes of a block at " + location.toVector());

        DBResultSet bestRS = null;

        for (DBResultSet rs : resultSetList) {
            if (ignoreMats.contains(getStringFromDB(CO_MATERIAL_MAP, rs.getInt("type"), "material"))) {
                continue;
            }

            if (rs.getInt("action") == 1) {
                bestTime = rs.getLong("time");
                bestID = rs.getInt("time");
                bestRS = rs;
            } else if (!forceBroken) {
                bestRS = null;
                lastBreakTime = rs.getLong("time");

                bestID = -1;
            } else {
                bestRS = rs;
            }
        }

        if (bestRS == null) return null;

        DBResultSet rs = bestRS;

        CoBlock block = new CoBlock();
        block.x = location.getBlockX();
        block.y = location.getBlockY();
        block.z = location.getBlockZ();
        byte[] bBlockData = (byte[]) rs.get("blockdata");


        String sMaterial = getStringFromDB(CO_MATERIAL_MAP, rs.getInt("type"), "material");
        block.material = Material.matchMaterial(sMaterial);

        if (bBlockData != null && bBlockData.length > 0) {
            block.blockData = deserializeBlockData(bBlockData, sMaterial);
        } else {
            //System.out.println("BlockData is null! Using material mode");
            block.blockData = null;
        }


        if (block.equals(new CoBlock(location.getBlock()))) return null;
        if (block.material == Material.WATER) return null;
        if (block.material == Material.LAVA) return null;


        //System.out.println("Found best DB time: " + bestTime);
        //System.out.println("Found best DB id: " + bestID);
        //System.out.println("DB TypeId = " + rs.getInt("type"));
        //System.out.println("DB Type Mat = " + getStringFromDB(CO_MATERIAL_MAP, rs.getInt("type"), "material"));

        return block;
    }




    public List<DBResultSet> findIdsByBlock(Location location, long unixTime, DebugBlock debugBlock) {
        long currentTime = Instant.now().getEpochSecond();


        List<DBResultSet> result = new ArrayList<>();

        int worldID = findIdByColumnValue(CO_WORLD, "world", location.getWorld().getName());

        List<String> all = findIdsByColumnValues(CO_BLOCK, "time", Map.of(
                "x", String.valueOf(location.getBlockX()),
                "y", String.valueOf(location.getBlockY()),
                "z", String.valueOf(location.getBlockZ()),
                "wid", String.valueOf(worldID)

                ));

        for (String s : all) {
            if (debugBlock != null) debugBlock.updateText(s);

            List<DBResultSet> f = getStatementsFromDB(CO_BLOCK, "time", s);

            for (DBResultSet rs : f) {
                if (rs == null) continue;
                if (rs.get("wid") == null) continue;

                if (rs.getInt("wid") != worldID) continue;
                if (rs.getInt("x") != location.getBlockX()) continue;
                if (rs.getInt("y") != location.getBlockY()) continue;
                if (rs.getInt("z") != location.getBlockZ()) continue;

                result.add(rs);

            }
        }



        return result;
    }

    private List<Integer> findIdsByBlock(Location location) {
        List<Integer> idsX = findIdsByColumnValue(CO_BLOCK, "x", "time", String.valueOf((int) location.getX()));
        List<Integer> idsY = findIdsByColumnValue(CO_BLOCK, "y", "time", String.valueOf((int) location.getY()));
        List<Integer> idsZ = findIdsByColumnValue(CO_BLOCK, "z", "time", String.valueOf((int) location.getZ()));

        List<Integer> all = new ArrayList<>();
        all.addAll(idsX);
        all.addAll(idsY);
        all.addAll(idsZ);

        List<Integer> res = new ArrayList<>();

        for (Integer i : all) {
            if (!idsX.contains(i)) continue;
            if (!idsY.contains(i)) continue;
            if (!idsZ.contains(i)) continue;

            res.add(i);
        }

        return res;
    }


    public BlockData deserializeBlockData(byte[] vals, String material) {

        List<Integer> keys = Arrays.stream(deserializeBlobToIntegers(vals)).toList();


        StringBuilder builder = new StringBuilder(material + "[");

        for (int i : keys) {
            builder.append(getStringFromDB(CO_BLOCKDATA_MAP, i, "data"));

            if (keys.getLast().equals(i)) {
                builder.append("]");
            } else {
                builder.append(",");
            }
        }

        System.out.println("Created blockData: " + builder.toString());
        return Bukkit.createBlockData(builder.toString());
    }

    public static Integer[] deserializeBlobToIntegers(byte[] blobBytes) {
        if (blobBytes == null || blobBytes.length == 0) {
            return new Integer[0];
        }

        // Конвертируем байты в строку (UTF-8)
        String rawString = new String(blobBytes, StandardCharsets.UTF_8);

        // Удаляем квадратные скобки по краям
        String cleanedString = rawString.replaceAll("^\\[|\\]$", "");

        // Разделяем строку по запятым
        String[] numberStrings = cleanedString.split(",");

        List<Integer> resultList = new ArrayList<>();
        for (String numStr : numberStrings) {
            try {
                // Парсим каждое число
                int number = Integer.parseInt(numStr.trim());
                resultList.add(number);
            } catch (NumberFormatException e) {
                // Обработка ошибок (например, логирование)
                System.err.println("Ошибка парсинга числа: " + numStr);
            }
        }

        return resultList.toArray(new Integer[0]);
    }

    public World getRollbackWorld() {
        return rollbackWorld;
    }

    public boolean isRollbackActive() {
        return rollbackActive;
    }
}
