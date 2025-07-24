package bymrshocker.coreProtectRestorer.commands;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import bymrshocker.coreProtectRestorer.data.CoBlock;
import bymrshocker.coreProtectRestorer.data.DebugBlock;
import bymrshocker.coreProtectRestorer.data.RestoreProcess;
import bymrshocker.shockerfunctionlibrary.commands.BaseCommandArg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class cpr_restoreMulti implements BaseCommandArg {


    @Override
    public List<String> getTabList(String key) {
        return List.of("");
    }

    @Override
    public String getName() { return "restoreMultiThread"; }

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
        if (args.length < 2) {
            player.sendMessage(Component.text("Invalid syntax. Usage: /cpr restore [radius] [forceBroken] [numThreads]"));
            return;
        }

        boolean forceBroken = false;

        if (args.length > 2) {
            try {
                forceBroken = Boolean.parseBoolean(args[2]);
            } catch (Exception e) {

            }
        }

        int threads = 4;

        if (args.length > 3) {
            try {
                threads = Integer.parseInt(args[3]);
            } catch (Exception e) {

            }
        }

        int radius = Integer.parseInt(args[1]);

        Location loc = player.getLocation().toBlockLocation();
        player.sendMessage(Component.text("Loading data for " + Math.pow((2 * radius + 1), 3) + " blocks. It may take a while..."));

        boolean finalForceBroken = forceBroken;
        int finalThreads = threads;
        Bukkit.getScheduler().runTaskAsynchronously(CoreProtectRestorer.getInstance(), bukkitTask -> {
            processCubeAsync(loc, radius, player, finalForceBroken, finalThreads);
        });
    }

    public void processCubeAsync(Location center, int radius, Player player, boolean forceBroken, int threads) {
        World world = center.getWorld();
        int centerY = center.getBlockY();

        int minY = Math.max(world.getMinHeight(), centerY - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + radius);
        int totalLayers = maxY - minY + 1;

        // Рассчитываем диапазоны Y для каждого потока
        int layersPerThread = totalLayers / threads;
        int remainder = totalLayers % threads;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        int currentY = minY;
        for (int i = 0; i < threads; i++) {
            int threadMinY = currentY;
            int threadMaxY = currentY + layersPerThread - 1 + (i < remainder ? 1 : 0);

            RestoreProcess process = new RestoreProcess(center, radius, threadMinY, threadMaxY, forceBroken, CoreProtectRestorer.getInstance().getRestoreProcesses().size(), player);
            process.createDebugBlock();
            process.connect(CoreProtectRestorer.getInstance().getDatabaseManager());
            CoreProtectRestorer.getInstance().getRestoreProcesses().add(process);

            futures.add(CompletableFuture.runAsync(() -> {
                process.processYLayer(center, radius, threadMinY, threadMaxY);
            }));

            currentY = threadMaxY + 1;
        }

        createProgessBar(player);


        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Ожидание завершения
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
                player.sendMessage(Component.text("Обнаружена ошибка обработки потока"));
            }
        }
        executor.shutdown();
    }

    private void createProgessBar(Player player) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(CoreProtectRestorer.getInstance(), bukkitTask -> {
            if (!player.isOnline() || CoreProtectRestorer.getInstance().getRestoreProcesses().isEmpty()) {
                bukkitTask.cancel();
                return;
            }

            List<RestoreProcess> processes = new ArrayList<>(CoreProtectRestorer.getInstance().getRestoreProcesses());

            double totalProgress = 0;
            for (RestoreProcess process : processes) {
                totalProgress += process.getProgress();
            }

            player.sendActionBar(Component.text("Restore progress: " + "%.2f".formatted((totalProgress / processes.size()) * 100)));

            if (totalProgress / processes.size() == 1) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                bukkitTask.cancel();
            }

        }, 10, 10);
    }

}
