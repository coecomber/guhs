package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;

/**
 * The stand-alone {@link KaartBron} (until the core is merged): every player who opens the map gets a cell on the core's
 * spiral around (0, 0) (kept in {@link KaartData}), shown as a fresh level-1 base. A visit teleports to that plot's arrival
 * spot only when a dimension {@code guhs:erfenis} exists (nothing is stamped there: that is the core's job), else the player
 * gets a friendly "not yet" message.
 * <p>
 * MERGE: replaced by a bron on ErfenisData/Erfenis (see {@link KaartBron}); this class can then go.
 */
public final class KaartBronStandaard implements KaartBron {
    /** The core's dimension and plot geometry (copied from feature/erfenis/Plot: CEL 256, MAAT 64, Y 64). */
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("erfenis"));
    static final int CEL = 256, MAAT = 64, Y = 64;
    /** Knusheid of the core's fresh base: stadhuis 1 (10) + molen 1 (2) + 4 walls (4) + a boompje (3). */
    static final int START_PUNTEN = 19;

    @Override
    public List<KaartDorp> spelerDorpen(MinecraftServer server, long nu) {
        List<KaartDorp> l = new ArrayList<>();
        for (Map.Entry<UUID, KaartData.SpelerCel> e : KaartData.get(server).spelers.entrySet()) {
            KaartData.SpelerCel c = e.getValue();
            l.add(new KaartDorp(e.getKey(), c.naam(), c.naam(), false,
                    c.cx(), c.cz(), 1, START_PUNTEN, spelerKleur(e.getKey()), false));
        }
        return l;
    }

    @Override
    public UUID eigenDorp(ServerPlayer p) {
        KaartData data = KaartData.get(p.level().getServer());
        String naam = p.getGameProfile().name();
        KaartData.SpelerCel c = data.spelers.get(p.getUUID());
        if (c == null) {
            Set<Long> npc = data.npcCellen();
            while (true) {
                int[] s = spiraal(data.volgendeSpiraal++);
                boolean bezet = npc.contains(KaartData.sleutel(s[0], s[1]));
                for (KaartData.SpelerCel o : data.spelers.values()) {
                    bezet |= o.cx() == s[0] && o.cz() == s[1];
                }
                if (!bezet) {
                    c = new KaartData.SpelerCel(naam, s[0], s[1]);
                    break;
                }
            }
            data.spelers.put(p.getUUID(), c);
            data.setDirty();
        } else if (!c.naam().equals(naam)) {
            data.spelers.put(p.getUUID(), new KaartData.SpelerCel(naam, c.cx(), c.cz()));
            data.setDirty();
        }
        return p.getUUID();
    }

    @Override
    public boolean bezoek(ServerPlayer p, UUID dorp, @Nullable NpcDorp npc, boolean bovenaanzicht) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = server.getLevel(DIM);
        int[] cel = null;
        if (npc != null) {
            cel = new int[] {npc.cx, npc.cz};
        } else {
            KaartData.SpelerCel c = KaartData.get(server).spelers.get(dorp);
            if (c != null) {
                cel = new int[] {c.cx(), c.cz()};
            }
        }
        if (cel == null) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.weg"));
            return false;
        }
        if (level == null) {
            // TODO(merge): the core's dimension and Erfenis.gaNaar take over here (KaartBronErfenis).
            p.sendSystemMessage(Component.translatable("erfenis.kaart.geen_dim"));
            return false;
        }
        int rand = (CEL - MAAT) / 2;
        Vec3 aankomst = new Vec3(cel[0] * CEL + rand + MAAT / 2 + 0.5, Y + 1, cel[1] * CEL + rand + MAAT + 1.5);
        p.teleport(new TeleportTransition(level, aankomst, Vec3.ZERO, 180f, 20f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable(bovenaanzicht ? "erfenis.kaart.bovenaanzicht_later" : "erfenis.kaart.bezocht"));
        return true;
    }

    /** A soft colour of its own for every player (from the palette, by uuid). */
    static int spelerKleur(UUID id) {
        return NpcNamen.PALET[Math.floorMod(id.hashCode(), NpcNamen.PALET.length)];
    }

    /** The n-th cell of the core's square spiral around (0, 0) (a copy of Plot.spiraal). */
    static int[] spiraal(int n) {
        int x = 0, z = 0, stap = 1, i = 0, r = 0;
        int[][] richting = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        while (i < n) {
            for (int herhaal = 0; herhaal < 2 && i < n; herhaal++) {
                for (int s = 0; s < stap && i < n; s++) {
                    x += richting[r][0];
                    z += richting[r][1];
                    i++;
                }
                r = (r + 1) % 4;
            }
            stap++;
        }
        return new int[] {x, z};
    }
}
