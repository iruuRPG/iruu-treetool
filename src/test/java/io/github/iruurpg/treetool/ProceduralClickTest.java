package io.github.iruurpg.treetool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A left click with the tool opens the menu and leaves the place as it was; a right click chooses the place. */
class ProceduralClickTest {

    @Test
    void leftClickKeepsThePlace() {
        assertTrue(ProceduralModule.menuOnly(true, true));
    }

    @Test
    void rightClickChoosesThePlace() {
        assertFalse(ProceduralModule.menuOnly(false, true));
        assertFalse(ProceduralModule.menuOnly(false, false));
    }

    @Test
    void leftClickChoosesThePlaceWhenThereIsNone() {
        assertFalse(ProceduralModule.menuOnly(true, false));
    }
}
