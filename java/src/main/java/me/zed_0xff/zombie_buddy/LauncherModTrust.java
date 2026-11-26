package me.zed_0xff.zombie_buddy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;

/** Session-only approval for the Java mod verified by the Apocalipse BR launcher. */
final class LauncherModTrust {
    private static final long WORKSHOP_ID = 3809306528L;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String HASH_PROPERTY = "apocbr.launcher.viewpoint.java.sha256";
    static final LauncherModTrust SESSION = new LauncherModTrust(
            LauncherModTemplate.isViewpoint() ? System.getProperty(HASH_PROPERTY) : null);

    private final boolean enabled;
    private final Set<String> approvedHashes;

    LauncherModTrust(String hashes) {
        enabled = hashes != null;
        approvedHashes = !enabled ? Set.of() : Arrays.stream(hashes.split(","))
                .filter(SHA256.asMatchPredicate())
                .collect(Collectors.toUnmodifiableSet());
    }

    static void initialize() {
        if (SESSION.enabled) {
            Logger.info("Aprovação de mods Java do Viewpoint delegada ao launcher.");
        }
    }

    boolean manages(String modId) {
        return enabled && "Viewpoint".equals(modId);
    }

    boolean approves(String modId, Path jar, String hash, WorkshopItemID workshopId, Path cache) {
        if (!manages(modId) || jar == null || hash == null || !approvedHashes.contains(hash)
                || workshopId == null || workshopId.value() != WORKSHOP_ID || cache == null) {
            return false;
        }

        Path gameCache = cache.toAbsolutePath().normalize();
        Path ownedMod = LauncherModTemplate.stagedMods(gameCache).resolve(modId);
        try {
            Path realMod = ownedMod.toRealPath();
            Path realJar = jar.toRealPath();
            return realMod.equals(gameCache.toRealPath().resolve(gameCache.relativize(ownedMod)))
                    && realJar.startsWith(realMod) && Files.isRegularFile(realJar);
        } catch (IOException ex) {
            return false;
        }
    }
}
