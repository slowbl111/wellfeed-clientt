package dev.wellfeed.util;

/** Where the camera really is this frame; written by the mixins, read by the ESP. */
public final class CamState {
    public static volatile boolean valid;
    public static double x, y, z;
    public static float yaw, pitch;
    /** Vertical field of view in degrees, as the world was last rendered. 0 = unknown. */
    public static float fov;

    private CamState() {}
}
