package bymrshocker.coreProtectRestorer;

import bymrshocker.coreProtectRestorer.commands.*;
import bymrshocker.coreProtectRestorer.data.RestoreProcess;
import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import bymrshocker.shockerfunctionlibrary.commands.CommandsHandler;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CoreProtectRestorer extends JavaPlugin {

    private static CoreProtectRestorer instance;
    private CommandsHandler commandsHandler;
    private EDatabaseManager databaseManager;

    private List<RestoreProcess> restoreProcesses;

    //TODO добавить список блоков, которые были восстановлены в текущей сессии

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        createCommands();


        databaseManager = new EDatabaseManager(CoreProtect.getInstance().getDataFolder(), "", "database");


        restoreProcesses = new ArrayList<>();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        databaseManager.killFutures();

        for (RestoreProcess restoreProcess : restoreProcesses) {
            restoreProcess.isCanceled = true;
            if (restoreProcess.getDebugBlock() == null) continue;
            restoreProcess.getDebugBlock().destroy();
        }
    }

    public static CoreProtectRestorer getInstance() {
        return instance;
    }

    private void createCommands() {
        commandsHandler = new CommandsHandler(new ArrayList<>(List.of(
                new cpr_restore(),
                new cpr_test(),
                new cpr_rollback(),
                new cpr_delete(),
                new cpr_restoreMulti()
        )));
        Objects.requireNonNull(getCommand("cpr")).setExecutor(commandsHandler);
    }

    public EDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public List<RestoreProcess> getRestoreProcesses() {
        return restoreProcesses;
    }
}
