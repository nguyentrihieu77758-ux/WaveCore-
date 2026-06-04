package thunder.hack.events.impl;

import meteordevelopment.orbit.ICancellable;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.Event;

public class EventFireworkMotion extends Event implements ICancellable {
    private boolean cancelled;
    private Vec3d vector;

    public EventFireworkMotion(Vec3d vector) {
        this.vector = vector;
    }

    public Vec3d getVector() { return vector; }
    public void setVector(Vec3d vector) { this.vector = vector; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
