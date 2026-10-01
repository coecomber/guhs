package nl.juiced.guhs.feature.band;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * A guh's dagboekje (2.10): statistics ({@link DagboekStat}), "eerste keren" (first tamed, first ride, first
 * Guhmension...: lang {@code gui.guhs.dagboek.eerste.<id>} + {@code .tekst}) and wist-je-datjes, little sentences the
 * guh "writes" after important moments (newest first, 40 kept; lang keys {@code gui.guhs.wistjedat.<pkg>.*}). Shown on
 * its page in the Guhdex tab "Mijn guhs". Only for band guhs; no-ops for other mobs.
 */
public final class Dagboek {
    private Dagboek() {
    }

    /** Adds n to a statistic (n &gt; 0). */
    public static void tel(Mob guh, DagboekStat stat, long n) {
        BandData.Rec r = Band.rec(guh);
        if (r == null || n <= 0) {
            return;
        }
        r.stats.merge(stat, n, Long::sum);
        BandData.get(guh.level().getServer()).setDirty();
    }

    public static long stat(MinecraftServer s, UUID eigenaar, UUID bandId, DagboekStat stat) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r == null ? 0 : r.stat(stat);
    }

    /**
     * A first time: true when it is new (stored with the Minecraft day). The owner (when online) gets a little note in
     * the chat: "Nieuw in het dagboekje van ...".
     */
    public static boolean eersteKeer(Mob guh, @Nullable ServerPlayer speler, String id) {
        if (!Band.isBandGuh(guh) || guh.level().getServer() == null) {
            return false;
        }
        Band.bijwerken(guh);
        return eersteKeer(guh.level().getServer(), Band.eigenaar(guh), Band.id(guh), id);
    }

    /** {@link #eersteKeer(Mob, ServerPlayer, String)} for a guh that isn't loaded (e.g. at the owner's login). */
    public static boolean eersteKeer(MinecraftServer s, UUID eigenaar, UUID bandId, String id) {
        BandData data = BandData.get(s);
        BandData.Rec r = data.rec(eigenaar, bandId);
        if (r.heeftEerste(id)) {
            return false;
        }
        r.eerste.add(new BandData.Eerste(id, Band.dag(s)));
        data.setDirty();
        ServerPlayer owner = s.getPlayerList().getPlayer(eigenaar);
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("gui.guhs.dagboek.nieuw", r.weergave(),
                    Component.translatable("gui.guhs.dagboek.eerste." + id).withStyle(ChatFormatting.BOLD)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    /** Did this guh already have this first time? */
    public static boolean heeftEersteKeer(MinecraftServer s, UUID eigenaar, UUID bandId, String id) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r != null && r.heeftEerste(id);
    }

    /** The guh "writes" a sentence (a translatable key + args), newest first, 40 kept. 1.2.0: an arg is a Component
     *  (a name every reader sees in their own language) or anything else (a player name, a number: a literal). */
    public static void wistJeDat(Mob guh, String langKey, Object... args) {
        if (!Band.isBandGuh(guh) || guh.level().getServer() == null) {
            return;
        }
        Band.bijwerken(guh);
        wistJeDat(guh.level().getServer(), Band.eigenaar(guh), Band.id(guh), langKey, args);
    }

    /** {@link #wistJeDat(Mob, String, Object...)} for a guh that isn't loaded. */
    public static void wistJeDat(MinecraftServer s, UUID eigenaar, UUID bandId, String langKey, Object... args) {
        BandData data = BandData.get(s);
        BandData.Rec r = data.rec(eigenaar, bandId);
        List<Component> tekst = new java.util.ArrayList<>();
        for (Object a : args) {
            tekst.add(a instanceof Component c ? c.copy() : Component.literal(String.valueOf(a)));
        }
        r.wist.add(0, new BandData.WistJeDat(langKey, List.copyOf(tekst), Band.dag(s)));
        while (r.wist.size() > BandData.WIST_MAX) {
            r.wist.remove(r.wist.size() - 1);
        }
        data.setDirty();
    }
}
