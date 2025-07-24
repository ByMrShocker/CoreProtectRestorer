package bymrshocker.coreProtectRestorer.data;

import bymrshocker.coreProtectRestorer.database.EDatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.List;

public class CoBlock {
    public int x,y,z;
    public Material material;
    public BlockData blockData;

    public CoBlock() {

    }

    public CoBlock(Block block) {
        x = block.getX();
        y = block.getY();
        z = block.getZ();
        material = block.getType();
        blockData = block.getBlockData();
    }


    public boolean equals(CoBlock block) {
        if (this.x != block.x) return false;
        if (this.y != block.y) return false;
        if (this.z != block.z) return false;

        if (this.material != block.material) return false;

        if (this.blockData != null && block.blockData != null) {
            if (!this.blockData.equals(block.blockData)) return false;
        } else {
            if (this.blockData != block.blockData) return false;
        }



        return true;
    }

    public void set(World world, Player player) {



        Location location = new Location(world, x, y, z);

        if (this.equals(new CoBlock(location.getBlock()))) {
            player.sendMessage(Component.text("Block match at " + location.toVector() + ". Skipping."));
            System.out.println("Block match. Skipping.");
            return;
        }

        if (List.of(Material.WATER, Material.LAVA).contains(material)) {
            player.sendMessage(Component.text("Block is liquid at " + location.toVector() + ". Skipping."));
            System.out.println("Block is liquid. Skipping.");
            return;
        }


        boolean outOfRange = location.isChunkLoaded();

        if (outOfRange) {
            System.out.println("Chunk out of range. Loading...");
            location.getWorld().loadChunk(location.getWorld().getChunkAt(location));
        }

        System.out.println("Setting block: " + material + " at location: " + location);

        location.getBlock().setType(material);




        if (blockData != null) {
            location.getBlock().setBlockData(blockData, true);
        }

        player.sendMessage(Component.text("Block set at " + location.toVector() + ". Material: " + material));


        if (outOfRange) {
            System.out.println("Chunk was out of range. Unloading...");
            location.getWorld().unloadChunk(location.getWorld().getChunkAt(location));
        }
    }
}
