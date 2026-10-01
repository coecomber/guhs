package nl.juiced.guhs.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.net.RequestTranslationTableMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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

    /** English twins of our chapter pictures (textures/ftbquests/<chapter>/en/<name>.png), or null when there is none. */
    private static final Map<Identifier, java.util.Optional<Icon<?>>> ENGELS = new ConcurrentHashMap<>();

    /** A chapter picture as the quest book shows it (mixin client.FtbQuestsImageMixin): the English twin of our pictures
     *  with text in them when the Guhs texts are English. */
    public static Icon<?> image(Icon<?> icon) {
        if (!(icon instanceof ImageIcon img) || !img.texture.getNamespace().equals(nl.juiced.guhs.Guhs.MODID)
                || !img.texture.getPath().startsWith("textures/ftbquests/") || GuhsTaal.dutch()) {
            return icon;
        }
        return ENGELS.computeIfAbsent(img.texture, id -> {
            String path = id.getPath();
            Identifier en = id.withPath(path.substring(0, path.lastIndexOf('/')) + "/en" + path.substring(path.lastIndexOf('/')));
            return Minecraft.getInstance().getResourceManager().getResource(en).isPresent()
                    ? java.util.Optional.<Icon<?>>of(Icon.getIcon(en)) : java.util.Optional.empty();
        }).orElse(icon);
    }

    /** After a resource reload (a resource pack may add or change pictures). */
    public static void forgetImages() {
        ENGELS.clear();
    }

    public static void onSwitch() {
        if (!ClientQuestFile.exists()) {
            return;
        }
        request();
        ClientQuestFile.getInstance().clearCachedData();
        ClientQuestFile.getInstance().refreshGui();
    }

    /** Joined a world/server: the quest file only arrives after the login, so the table is asked for once it is there
     *  ({@link #tick()}). */
    private static volatile boolean wachtOpBoek;

    public static void onLogin() {
        wachtOpBoek = true;
    }

    /** Client tick (GuhsTaal, only with FTB Quests): ask for our table as soon as the quest file has arrived. */
    public static void tick() {
        if (wachtOpBoek && Minecraft.getInstance().getConnection() != null && ClientQuestFile.exists()) {
            wachtOpBoek = false;
            request();
        }
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
