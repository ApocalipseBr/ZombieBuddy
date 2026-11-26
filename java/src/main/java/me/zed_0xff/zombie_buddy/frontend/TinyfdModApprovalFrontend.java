package me.zed_0xff.zombie_buddy.frontend;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.zed_0xff.zombie_buddy.JarBatchApprovalProtocol;
import me.zed_0xff.zombie_buddy.Logger;
import me.zed_0xff.zombie_buddy.Reflect;
import me.zed_0xff.zombie_buddy.Utils;
import me.zed_0xff.zombie_buddy.ZombieBuddy;
import zombie.core.Core;
import zombie.core.GameVersion;

/**
 * LWJGL {@code TinyFileDialogs} (works when the game JVM is {@code java.awt.headless=true}).
 * No multi-mod window: {@link #approvePendingMods} prompts one entry at a time.
 */
public final class TinyfdModApprovalFrontend implements ModApprovalFrontend {

    private static final String DIALOG_TITLE = ZombieBuddy.DISPLAY_NAME + " | Aprovação de mods Java";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public List<JarBatchApprovalProtocol.Entry> approvePendingMods(List<JarBatchApprovalProtocol.Entry> pending) {
        if (pending.isEmpty()) {
            return pending;
        }
        for (JarBatchApprovalProtocol.Entry e : pending) {
            Boolean allow = promptForEntry(e);
            if (allow != null) {
                e.decision = allow;
            }
        }
        return pending;
    }

    private static Boolean promptForEntry(JarBatchApprovalProtocol.Entry e) {
        if (e.zbs.invalid()) {
            String note = !Utils.isBlank(e.zbs.notice())
                ? e.zbs.notice()
                : "A assinatura ZBS é inválida.";
            tinyfdYesNo(
                "O mod Java foi bloqueado porque a assinatura ZBS é inválida.\n\n"
                    + note
                    + "\n\nEste mod será bloqueado."
            );
            return false;
        }

        String modified = formatDate(e.date);
        String zbsLine = "";
        if (e.zbs.valid() || e.zbs.invalid() || e.zbs.unsigned()) {
            String sid = e.zbs.authorSteamId() != null ? e.zbs.authorSteamId().toString() : "";
            zbsLine = "ZBS: " + zbsStatus(e)
                + (!sid.isEmpty() ? " (Steam: " + sid + ")" : "")
                + "\n\n";
        }
        Boolean allow = tinyfdYesNo(
            "Permitir o carregamento do mod Java?\n\n"
                + zbsLine
                + "Mod: " + e.modId + "\n\n"
                + "JAR: " + e.jarAbsolutePath + "\n\n"
                + "Modificado em " + modified + "\n\n"
                + "SHA-256: " + e.sha256 + "\n\n"
                + "Permita apenas se confiar na origem deste mod."
        );
        if (allow == null) {
            return false;
        }
        return allow;
    }

    private static String zbsStatus(JarBatchApprovalProtocol.Entry e) {
        if (e.zbs.valid()) {
            return "válida";
        }
        if (e.zbs.invalid()) {
            return "inválida";
        }
        return "sem assinatura";
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return "<desconhecido>";
        }
        return new SimpleDateFormat(DATE_FORMAT, Locale.ROOT).format(date);
    }

    /**
     * Returns TRUE (Yes), FALSE (No), or null if the dialog could not be shown.
     */
    private static Boolean tinyfdYesNo(String msg) {
        Reflect r = Reflect.on("org.lwjgl.util.tinyfd.TinyFileDialogs");
        if (!r.isPresent()) {
            if (Core.getInstance().getGameVersion().isGreaterThan(GameVersion.parse("42.14"))) {
                Logger.error("tinyfdYesNo(): game version > 42.14 but TinyFileDialogs missing; returning null");
                return null;
            }
            Logger.info("tinyfdYesNo(): pre-42.15 and TinyFileDialogs missing; defaulting YES");
            return Boolean.TRUE;
        }
        try {
            return (boolean) Reflect
                .fastcall(() -> Reflect.on("org.lwjgl.util.tinyfd.TinyFileDialogs").getMethodHandle(
                            boolean.class,
                            new Class<?>[] {
                                CharSequence.class,
                                CharSequence.class,
                                CharSequence.class,
                                CharSequence.class,
                                boolean.class
                            },
                            "tinyfd_messageBox"))
                .invokeExact("tinyfd_messageBox", DIALOG_TITLE, msg, "yesno", "warning", false);
        } catch (Throwable t) {
            Logger.error("Could not show dialog: " + t);
            return null;
        }
    }
}
