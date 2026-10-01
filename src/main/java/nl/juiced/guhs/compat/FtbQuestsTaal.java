package nl.juiced.guhs.compat;

import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.net.RequestTranslationTableMessage;
import net.minecraft.client.Minecraft;
import nl.juiced.guhs.client.GuhsTaal;

/**
 * 1.2.0: the Guhs NL/EN switch for FTB Quests (client; only called when FTB Quests is loaded). Our chapters have
 * lang/nl_nl and lang/en_us tables (FtbQuestsChapter). FTB Quests shows the table of {@code ClientQuestFile.getLocale()},
 * normally the Minecraft language; with NL or EN chosen, {@link #locale(String)} changes that (mixin
 * client.FtbQuestsLocaleMixin). The server only sends the tables of en_us, its fallback and the player's Minecraft
 * language, so the other one is asked for (the answer clears FTB Quests' caches and refreshes its screen).
 */
public final class FtbQuestsTaal {
    /** What FTB Quests' getLocale() returns with the Guhs switch (`original`: its own answer). */
    public static String locale(String original) {
        return GuhsTaal.choice().ftbLocale(original);
    }

    public static void onSwitch() {
        if (!ClientQuestFile.exists()) {
            return;
        }
        request();
        ClientQuestFile.getInstance().clearCachedData();
        ClientQuestFile.getInstance().refreshGui();
    }

    public static void onLogin() {
        Minecraft.getInstance().execute(FtbQuestsTaal::request);
    }

    private static void request() {
        if (Minecraft.getInstance().getConnection() == null || !ClientQuestFile.exists()) {
            return;
        }
        String locale = ClientQuestFile.getInstance().getLocale();
        if (!locale.equals(Minecraft.getInstance().options.languageCode) && !locale.equals("en_us")) {
            Play2ServerNetworking.send(new RequestTranslationTableMessage(locale));
        }
    }

    private FtbQuestsTaal() {
    }
}
