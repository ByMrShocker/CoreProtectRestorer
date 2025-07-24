package bymrshocker.coreProtectRestorer.data;

import bymrshocker.coreProtectRestorer.CoreProtectRestorer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Random;
import org.joml.Vector3f;

public class DebugBlock {

    BlockDisplay display;
    TextDisplay text;

    public DebugBlock(Location location, int index) {
        Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
            Location loc = location.clone();
            loc.setYaw(0f);
            loc.setPitch(0f);

            display = location.getWorld().spawn(loc, BlockDisplay.class);

            display.setBlock(getRandomMat().createBlockData());

            display.setBrightness(new Display.Brightness(15, 15));

            //display.setTransformation(new Transformation(
            //        new Vector3f(0.5f, 0.5f, 0.5f),
            //        new Quaternionf(),
            //        new Vector3f(1f, 1f, 1f),
            //        new Quaternionf()
            //));


            text = location.getWorld().spawn(loc.clone().add(new Vector(0, index * 0.2, 0)), TextDisplay.class);

            text.setAlignment(TextDisplay.TextAlignment.CENTER);
            text.setBillboard(Display.Billboard.CENTER);

            text.text(Component.text("Starting..."));

            //text.setTransformation(new Transformation(
            //        new Vector3f(0.5f, 0.5f, 0.5f),
            //        new Quaternionf(),
            //        new Vector3f(1f, 1f, 1f),
            //        new Quaternionf()
            //));


            text.setPersistent(false);
            display.setPersistent(false);

        });
    }

    private static Material getRandomMat() {
        Random random = new Random();
        int i = random.nextInt(5);

        switch (i) {
            default -> {return Material.WHITE_STAINED_GLASS;}
            case 1 -> {return Material.BLACK_STAINED_GLASS;}
            case 2 -> {return Material.BROWN_STAINED_GLASS;}
            case 3 -> {return Material.RED_STAINED_GLASS;}
            case 4 -> {return Material.ORANGE_STAINED_GLASS;}
            case 5 -> {return Material.YELLOW_STAINED_GLASS;}
            case 6 -> {return Material.LIME_STAINED_GLASS;}
            case 7 -> {return Material.BLUE_STAINED_GLASS;}
            case 8 -> {return Material.PINK_STAINED_GLASS;}
            case 9 -> {return Material.PURPLE_STAINED_GLASS;}
        }
    }

    public void update(Location location) {
        if (display != null) {
            display.teleportAsync(location.getBlock().getLocation());
        }

        //if (text != null) {
        //    text.teleportAsync(location);
        //}
    }

    public void updateText(long unix) {
        if (text != null) {
            text.text(Component.text(unix));
        }
    }

    public void updateText(String val) {
        if (text != null) {
            text.text(Component.text(val));
        }
    }

    public void destroy() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(CoreProtectRestorer.getInstance(), bukkitTask -> {
               destroy();
            });
        }


        if (display == null) return;
        if (display.isDead()) return;

        display.remove();
        text.remove();
    }

    public TextDisplay getText() {
        return text;
    }
}
