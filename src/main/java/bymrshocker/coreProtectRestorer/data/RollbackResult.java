package bymrshocker.coreProtectRestorer.data;

import org.bukkit.util.Vector;

public class RollbackResult {

    private final CoBlock coBlock;
    private final long time;
    private final Vector location;

    public RollbackResult(CoBlock coBlock, long time, Vector location) {
        this.coBlock = coBlock;
        this.time = time;
        this.location = location;
    }

    public Vector getLocation() {
        return location;
    }

    public CoBlock getCoBlock() {
        return coBlock;
    }

    public long getTime() {
        return time;
    }
}
