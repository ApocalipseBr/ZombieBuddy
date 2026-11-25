package me.zed_0xff.zombie_buddy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LauncherModTemplateTest {
    @Test
    void givesTemplateModsTheirRequiredOrderAndPreservesUserMods() {
        var saved = List.of("UserModA", "Viewpoint", "UserModB", "ZombieBuddy");

        assertEquals(List.of("ZombieBuddy", "Viewpoint", "PZVoxelStudioViewpoint", "UserModA", "UserModB"),
                LauncherModTemplate.mergeViewpointMods(saved));
        assertEquals(List.of("UserModA", "Viewpoint", "UserModB", "ZombieBuddy"), saved);
    }
}
