package me.zed_0xff.zombie_buddy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;

/** Session-only approval for the Java mod verified by the Apocalipse BR launcher. */
final class LauncherModTrust {
    private static final long VIEWPOINT_WORKSHOP_ID = 3809306528L;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern APPROVAL = Pattern.compile("([^,:]+):([0-9]{1,18}):([0-9a-f]{64})");
    private static final String HASH_PROPERTY = "apocbr.launcher.viewpoint.java.sha256";
    private static final String APPROVAL_PROPERTY = "apocbr.launcher.java.approvals";
    static final LauncherModTrust SESSION = forSession();

    private final Map<String, Set<Approval>> approvals;

    private record Approval(long workshopId, String hash) {
        static Map<String, Set<Approval>> parse(String configured) {
            return Arrays.stream(configured.split(","))
                    .map(APPROVAL::matcher).filter(Matcher::matches)
                    .collect(Collectors.groupingBy(m -> m.group(1),
                            Collectors.mapping(m -> new Approval(Long.parseLong(m.group(2)), m.group(3)),
                                    Collectors.toUnmodifiableSet())));
        }
    }

    private static LauncherModTrust forSession() {
        String configured = System.getProperty(APPROVAL_PROPERTY);
        if (LauncherModTemplate.isLauncherSession() && configured != null) {
            return new LauncherModTrust(Approval.parse(configured));
        }
        return new LauncherModTrust(LauncherModTemplate.isViewpoint() ? System.getProperty(HASH_PROPERTY) : null);
    }

    LauncherModTrust(String hashes) {
        approvals = hashes == null ? Map.of() : Map.of("Viewpoint", Arrays.stream(hashes.split(","))
                .filter(SHA256.asMatchPredicate())
                .map(hash -> new Approval(VIEWPOINT_WORKSHOP_ID, hash))
                .collect(Collectors.toUnmodifiableSet()));
    }

    private LauncherModTrust(Map<String, Set<Approval>> approvals) {
        this.approvals = approvals;
    }

    static void initialize() {
        if (!SESSION.approvals.isEmpty()) {
            Logger.info("Launcher supplied session approvals for Java mods.");
        }
    }

    boolean manages(String modId) {
        return approvals.containsKey(modId);
    }

    boolean approves(String modId, Path jar, String hash, WorkshopItemID workshopId, Path cache) {
        if (jar == null || hash == null || workshopId == null || cache == null
                || !approvals.getOrDefault(modId, Set.of()).contains(new Approval(workshopId.value(), hash))) {
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
