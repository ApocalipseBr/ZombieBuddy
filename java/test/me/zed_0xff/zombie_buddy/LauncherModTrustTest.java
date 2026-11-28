package me.zed_0xff.zombie_buddy;

import me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

        var profile = new LauncherModTrust(
                "Viewpoint:3809306528:" + hash + ",OtherMod:3810302175:" + hash, List.of("Viewpoint"));
        assertTrue(profile.manages("Viewpoint", managedFolder.resolve("42/mod.info"), cache));
        assertTrue(profile.approves("Viewpoint", managedJar, hash, workshopId, cache));
        assertFalse(profile.manages("Viewpoint", cache.resolve("mods/Viewpoint/42/mod.info"), cache));
        assertFalse(profile.manages("OtherMod"));

        Files.writeString(managedJar, "updated jar");
        assertFalse(trust.approves("Viewpoint", managedJar, Utils.sha256Hex(managedJar), workshopId, cache));
    }

    @Test
    void treeFlickerVersionExceptionRequiresTheApprovedStagedCopy(@TempDir Path cache) throws IOException {
        String modId = "ViewpointDisableIsometricUpkeep";
        Path modRoot = LauncherModTemplate.stagedMods(cache).resolve(modId);
        Path modInfo = Files.createDirectories(modRoot.resolve("42")).resolve("mod.info");
        Files.writeString(modRoot.resolve("workshop.txt"), "id=3842290180\n");
        Path jar = Files.writeString(modInfo.resolveSibling("patch.jar"), "approved jar");
        Path playerJar = Files.copy(jar, cache.resolve("patch.jar"));
        var trust = new LauncherModTrust(modId + ":3842290180:" + Utils.sha256Hex(jar), List.of(modId));

        assertTrue(trust.allowsTreeFlickerLegacyVersion(modInfo, jar, cache));
        assertFalse(trust.allowsTreeFlickerLegacyVersion(cache.resolve("mods/mod.info"), jar, cache));
        assertFalse(trust.allowsTreeFlickerLegacyVersion(modInfo, playerJar, cache));
        Files.writeString(modRoot.resolve("workshop.txt"), "id=3809306528\n");
        assertFalse(trust.allowsTreeFlickerLegacyVersion(modInfo, jar, cache));
        Files.writeString(modRoot.resolve("workshop.txt"), "id=3842290180\n");
        Files.writeString(jar, "modified jar");
        assertFalse(trust.allowsTreeFlickerLegacyVersion(modInfo, jar, cache));
    }
}
