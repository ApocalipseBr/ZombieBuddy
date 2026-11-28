package me.zed_0xff.zombie_buddy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import zombie.gameStates.ChooseGameInfo;
import zombie.modding.ActiveMods;
import zombie.network.GameClient;
import zombie.network.GameServer;

import static me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;

public final class LauncherModTemplate {
    private static final List<String> MOD_IDS = List.of("ZombieBuddy", "Viewpoint", "PZVoxelStudioViewpoint");
    private static final String MODS_PROPERTY = "apocbr.launcher.mods";
    private static final String APOCBR_PACK = "APOCALISEBR_PACK";
    private static final String VIEWPOINT = "viewpoint";

    private LauncherModTemplate() {}

    static boolean isViewpoint() {
        return VIEWPOINT.equals(System.getProperty("apocbr.launcher.template"));
    }

    static boolean isLauncherSession() {
        return System.getProperty(MODS_PROPERTY) != null || isViewpoint();
    }

    static List<String> configuredModIds() {
        String configured = System.getProperty(MODS_PROPERTY);
        if (configured == null) {
            return isViewpoint() ? MOD_IDS : List.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::strip).filter(id -> !id.isEmpty()).distinct().toList();
    }

    public static List<String> forSession(List<String> mods) {
        List<String> required = configuredModIds();
        if (GameServer.server || required.isEmpty()) {
            return mods;
        }
        boolean localList = mods == ActiveMods.getById("default").getMods()
                || mods == ActiveMods.getById("currentGame").getMods();
        if (!localList && (System.getProperty(MODS_PROPERTY) == null || !GameClient.client)) {
            return mods;
        }

        var available = new ArrayList<String>();
        for (String modId : required) {
            // The ApocBR pack also ran on multiplayer clients before this moved to ZombieBuddy.
            if (!localList && !APOCBR_PACK.equals(modId)) continue;
            if (ChooseGameInfo.getAvailableModDetails(modId) == null) {
                if (APOCBR_PACK.equals(modId)) {
                    Logger.warn("Launcher mod " + modId + " is not installed or compatible with this game build.");
                    continue;
                }
                throw new IllegalStateException("Launcher mod " + modId
                        + " is unavailable for this Project Zomboid build.");
            }
            available.add(modId);
        }
        if (available.isEmpty()) return mods;
        return localList ? mergeMods(available, mods) : mergeMods(mods, available);
    }

    static List<String> mergeViewpointMods(List<String> mods) {
        return mergeMods(MOD_IDS, mods);
    }

    private static List<String> mergeMods(List<String> required, List<String> mods) {
        var sessionMods = new LinkedHashSet<>(required);
        sessionMods.addAll(mods);
        return new ArrayList<>(sessionMods);
    }

    static WorkshopItemID workshopItemId(Path modRoot) {
        if (modRoot == null || modRoot.getParent() == null) {
            return null;
        }
        Path cache = Utils.getCachePath();
        if (cache == null) {
            return null;
        }
        Path templateMods = stagedMods(cache);
        return Utils.isSameFile(modRoot.getParent(), templateMods)
            ? JavaModInfo.workshopItemIdFromWorkshopTxtIn(modRoot)
            : null;
    }

    static Path stagedMods(Path cache) {
        return cache.resolve("Workshop/Apocalipse Launcher - Viewpoint/Contents/mods");
    }
}
