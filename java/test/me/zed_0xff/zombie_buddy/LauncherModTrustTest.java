package me.zed_0xff.zombie_buddy;

import me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherModTrustTest {
    @Test
    void appliesOnlyToTheStagedViewpointJarWithTheApprovedDigest(@TempDir Path cache) throws IOException {
        Path managedFolder = Files.createDirectories(LauncherModTemplate.stagedMods(cache).resolve("Viewpoint"));
        Path managedJar = Files.writeString(managedFolder.resolve("Viewpoint.jar"), "approved jar");
        Path subscribedJar = Files.copy(managedJar, cache.resolve("Viewpoint.jar"));
        var workshopId = new WorkshopItemID(3809306528L);
        String hash = Utils.sha256Hex(managedJar);
        var trust = new LauncherModTrust(hash);

        assertTrue(trust.manages("Viewpoint"));
        assertTrue(trust.approves("Viewpoint", managedJar, hash, workshopId, cache));
        assertFalse(trust.manages("OtherMod"));
        assertFalse(trust.approves("OtherMod", managedJar, hash, workshopId, cache));
        assertFalse(trust.approves("Viewpoint", subscribedJar, hash, workshopId, cache));
        assertFalse(trust.approves("Viewpoint", managedJar, hash, new WorkshopItemID(3810302175L), cache));
        assertFalse(new LauncherModTrust(null).manages("Viewpoint"));

        Files.writeString(managedJar, "updated jar");
        assertFalse(trust.approves("Viewpoint", managedJar, Utils.sha256Hex(managedJar), workshopId, cache));
    }
}
