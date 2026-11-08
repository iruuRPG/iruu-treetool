package io.github.iruurpg.treetool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The block in sight, far off too: the preview's block before the world's. */
class SightTest {

    @Test
    void theRayFindsTheFirstBlockFarOffAndThePreviewBeforeTheWorld() {
        // the ground at y 0 everywhere; a block of the preview at (40, 1, 0)
        Sight.Solid ground = (x, y, z) -> y <= 0;
        Sight.Solid shown = (x, y, z) -> x == 40 && y == 1 && z == 0;
        // looking along +x, a little down, from 2 over the ground: the preview's block first (it stands on the ground in the way)
        Sight.Hit hit = Sight.ray(0.5, 2.5, 0.5, 1.0, -0.035, 0.0, 96.0, shown, ground);
        assertNotNull(hit);
        assertTrue(hit.preview());
        assertEquals(40, hit.x());
        assertEquals(39, hit.beforeX());
        // without it, the ground far off; the block before it is the air over it
        Sight.Hit far = Sight.ray(0.5, 2.5, 0.5, 1.0, -0.035, 0.0, 96.0, null, ground);
        assertNotNull(far);
        assertEquals(0, far.y());
        assertEquals(1, far.beforeY());
        assertEquals(43, far.x());
        // out of reach, and nothing at all
        assertNull(Sight.ray(0.5, 2.5, 0.5, 1.0, -0.035, 0.0, 30.0, null, ground));
        assertNull(Sight.ray(0.5, 2.5, 0.5, 0.0, 1.0, 0.0, 96.0, null, ground));
    }
}
