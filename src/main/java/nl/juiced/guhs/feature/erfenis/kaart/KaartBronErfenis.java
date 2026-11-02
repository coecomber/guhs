package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.ErfenisData;
import nl.juiced.guhs.feature.erfenis.GebouwType;

/**
 * The {@link KaartBron} on the core (feature/erfenis): the map's real bases are ErfenisData's bases.
 * <ul>
 *   <li>Players: every base with {@code !npc}; its id is the owner's uuid, its cell the map position.</li>
 *   <li>NPC villages: {@link KaartData} decides where they are and how they grow; every new or grown village is turned into a
 *   real NPC base with the SAME id in the SAME cell ({@link ErfenisData#npcBase}), which is cheap (data only). The blocks are
 *   stamped lazily: on the first visit, or at once when somebody stands there.</li>
 *   <li>Cells: the player spiral skips every cell that has a base (NPC villages included), and new NPC villages skip every
 *   cell with a base ({@link #bezetteCellen}), so the two never collide.</li>
 * </ul>
 */
public final class KaartBronErfenis implements KaartBron {
    @Override
    public List<KaartDorp> spelerDorpen(MinecraftServer server, long nu) {
        List<KaartDorp> l = new ArrayList<>();
        for (Base b : ErfenisData.get(server).alleBases()) {
            if (!b.npc) {
                l.add(new KaartDorp(b.id, b.naam, b.naam, false, b.cx, b.cz, b.stadhuisLevel(), b.knusheid(), NpcNamen.spelerKleur(b.id), false));
            }
        }
        return l;
    }

    @Override
    public Set<Long> bezetteCellen(MinecraftServer server) {
        return ErfenisData.get(server).bezetteCellen();
    }

    @Override
    public UUID eigenDorp(ServerPlayer p) {
        return Erfenis.eigenBase(p).id;
    }

    @Override
    public boolean bezoek(ServerPlayer p, UUID dorp, @Nullable NpcDorp npc, boolean bovenaanzicht) {
        MinecraftServer server = p.level().getServer();
        ErfenisData data = ErfenisData.get(server);
        Base b = data.van(dorp);
        if (b == null && npc != null) {
            npcVeranderd(server, npc);
            b = data.van(dorp);
        }
        if (b == null) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.weg"));
            return false;
        }
        if (!Erfenis.gaNaar(p, b)) {
            p.sendSystemMessage(Component.translatable("erfenis.cmd.geen_dim"));
            return false;
        }
        if (bovenaanzicht) {
            Erfenis.openOverzichtStraks(p);   // (after the teleport has landed on the client)
        }
        return true;
    }

    @Override
    public void npcVeranderd(MinecraftServer server, NpcDorp dorp) {
        ErfenisData data = ErfenisData.get(server);
        Base b = data.npcBase(dorp.id, dorp.naam, dorp.cx, dorp.cz, plan(dorp), System.currentTimeMillis());
        if (b == null) {
            LogUtils.getLogger().warn("Erfenis kaart: NPC village {} cannot get cell {},{} (taken by another base)", dorp.naam, dorp.cx, dorp.cz);
            return;
        }
        data.veranderd(server, b);
    }

    /** Every NPC village of the map that has no base yet gets one (a world whose map was made before the merge). */
    public static void zorgNpcBases(MinecraftServer server) {
        if (!(KaartFeature.BRON instanceof KaartBronErfenis bron)) {
            return;
        }
        ErfenisData data = ErfenisData.get(server);
        for (NpcDorp d : new ArrayList<>(KaartData.get(server).npcs())) {
            if (data.van(d.id) == null) {
                bron.npcVeranderd(server, d);
            }
        }
    }

    /** The village's plan in the core's terms (the ids are the same, see {@link KaartGebouwType}). */
    static List<Base.Plan> plan(NpcDorp d) {
        List<Base.Plan> l = new ArrayList<>();
        for (KaartGebouw g : d.plan) {
            GebouwType t = GebouwType.van(g.type().id);
            if (t != null) {
                l.add(new Base.Plan(t, g.level(), g.x(), g.z(), g.rot()));
            }
        }
        return l;
    }
}
