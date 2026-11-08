package io.github.iruurpg.treetool;

/**
 * The block a player looks at, however far within the reach: a walk along the ray through the blocks (the preview's, which only that player sees, and the
 * world's). Arithmetic only, for the tests.
 */
final class Sight {

    private Sight() {
    }

    /** Whether a block is solid for the ray (in the world's coordinates). */
    interface Solid {
        boolean at(int x, int y, int z);
    }

    /** A block the ray hit, the empty block just before it (where something put against it goes), and whether it is the preview's. */
    record Hit(int x, int y, int z, int beforeX, int beforeY, int beforeZ, boolean preview) {
    }

    /**
     * The first block of {@code preview} or {@code world} on the ray from {@code (ox, oy, oz)} along {@code (dx, dy, dz)} within {@code reach} blocks (the
     * block the eye is in is not one), or null; a voxel walk (Amanatides and Woo), so no block on the way is missed.
     */
    static Hit ray(double ox, double oy, double oz, double dx, double dy, double dz, double reach, Solid preview, Solid world) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-9) return null;
        dx /= length;
        dy /= length;
        dz /= length;
        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;
        double tMaxX = boundary(ox, x, dx);
        double tMaxY = boundary(oy, y, dy);
        double tMaxZ = boundary(oz, z, dz);
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
        double tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);
        while (true) {
            int px = x;
            int py = y;
            int pz = z;
            double t;
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                t = tMaxZ;
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            if (t > reach) return null;
            if (preview != null && preview.at(x, y, z)) return new Hit(x, y, z, px, py, pz, true);
            if (world != null && world.at(x, y, z)) return new Hit(x, y, z, px, py, pz, false);
        }
    }

    /** How far along the ray the first boundary of the block is on one axis. */
    private static double boundary(double origin, int block, double direction) {
        if (direction > 0) return (block + 1 - origin) / direction;
        if (direction < 0) return (origin - block) / -direction;
        return Double.POSITIVE_INFINITY;
    }
}
