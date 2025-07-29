The plugin was created to restore the world after server griefing. 
Since CoreProtect only records actions that were intercepted via EventHandler, actions performed using WorldEdit or cheats cannot be restored via standart CoreProtect commands. 
This plugin scans the database and searches for all records related to the necessary blocks in order to restore them.


ShockerFunctionLibrary is required! If you have no access, well...


The plugin can restore actions performed using Axiom Mod, as it is recorded in the database.
However, actions performed using WorldEdit cannot be restored.


To speed up the process, the plugin creates the number of threads selected by the user (4 is recommended) and each thread creates an individual connection to the database. 
Multithreading is implemented using “CompletableFuture” rather than Bukkit.Scheduler due to the latter's limitations. 
The plugin is not immune to thread leaks, so it is recommended to restart the server after completing the restoration of your world.


The plugin has many commands, INCLUDING FOR DELETING DATA IN THE DATABASE! Be careful and do not give access to untrusted persons.


The restore and rollback commands are legacy, as I have tried a huge number of options for implementing this idea.
The best results were achieved with /cpr restoreMulti [radius] [forceBroken] [numThreads].
forceBroken - whether to restore even blocks that had triggered a BlockBreak event.

