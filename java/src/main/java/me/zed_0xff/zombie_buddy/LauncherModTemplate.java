package me.zed_0xff.zombie_buddy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import zombie.gameStates.ChooseGameInfo;
import zombie.modding.ActiveMods;

import static me.zed_0xff.zombie_buddy.SteamWorkshop.WorkshopItemID;

public final class LauncherModTemplate {
    private static final List<String> MOD_IDS = List.of("ZombieBuddy", "Viewpoint", "PZVoxelStudioViewpoint");
    private static final String VIEWPOINT = "viewpoint";

    private LauncherModTemplate() {}

    static boolean isViewpoint() {
        return VIEWPOINT.equals(System.getProperty("apocbr.launcher.template"));
    }

    public static List<String> forSession(List<String> mods) {
        if (!isViewpoint() || (mods != ActiveMods.getById("default").getMods()
                && mods != ActiveMods.getById("currentGame").getMods())) {
            return mods;
        }
        for (String modId : MOD_IDS) {
            if (ChooseGameInfo.getAvailableModDetails(modId) == null) {
                throw new IllegalStateException("Viewpoint template mod " + modId
                        + " is unavailable for this Project Zomboid build.");
            }
        }
        return mergeViewpointMods(mods);
    }

    static List<String> mergeViewpointMods(List<String> mods) {
        var sessionMods = new ArrayList<>(MOD_IDS);
        for (String mod : mods) {
            if (!MOD_IDS.contains(mod)) {
                sessionMods.add(mod);
            }
        }
        return sessionMods;
    }

    static WorkshopItemID workshopItemId(Path modRoot) {
        if (modRoot == null || modRoot.getParent() == null) {
            return null;
        }
        Path cache = Utils.getCachePath();
        if (cache == null) {
            return null;
        }
        Path templateMods = cache.resolve("Workshop/Apocalipse Launcher - Viewpoint/Contents/mods");
        return Utils.isSameFile(modRoot.getParent(), templateMods)
            ? JavaModInfo.workshopItemIdFromWorkshopTxtIn(modRoot)
            : null;
    }
}
