package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-LEVEN-RITME: the town at night, around the players.
 * <ul>
 *   <li><b>Sleepers</b>: guhs asleep in the beds of the houses (up to {@link #SLAPERS_PER_SPELER} per player, beds within
 *   {@link #STRAAL} blocks that no shopkeeper claimed), in pyjamas and nightcaps now and then; "Zzz" above them. Clicking one:
 *   a soft snore, it sleeps on. Gone at dawn.</li>
 *   <li><b>Owls</b>: the existing guh-uiltjes (feature vogels; awake and calling at night) perch on the roofs of the town,
 *   {@link #UILEN_PER_STAD} per town with a player in it. Gone at dawn.</li>
 * </ul>
 * Fireflies are client particles ({@code client.NachtBeeld}). Everything is code-spawned and never kept ({@link Ritme#TAG}).
 */
public final class Nachtleven {
    static final int SLAPERS_PER_SPELER = 6, STRAAL = 40, UILEN_PER_STAD = 3;
    /** The Slaapgrot snore (sounds.json "onderwol.snurkje", registered by the onderwol feature). */
    static final net.minecraft.sounds.SoundEvent SNURK = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(nl.juiced.guhs.Guhs.id("onderwol.snurkje"));
    private static final GuhVariant[] KLEUREN = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};
    /** bed head -> sleeper. */
    private static final Map<BlockPos, UUID> SLAPERS = new HashMap<>();
    /** town id -> owls. */
    private static final Map<String, List<UUID>> UILEN = new HashMap<>();

    private Nachtleven() {
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 21) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        boolean nacht = Rooster.nacht(sl);
        opruimen(sl, nacht);
        if (!nacht) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator()) {
                continue;
            }
            Steden.Stad stad = Steden.op(p.getX(), p.getY(), p.getZ());
            if (stad == null) {
                continue;
            }
            slapers(sl, p);
            uilen(sl, stad, p);
        }
    }

    private static void opruimen(ServerLevel sl, boolean nacht) {
        Iterator<Map.Entry<BlockPos, UUID>> it = SLAPERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, UUID> e = it.next();
            Entity g = sl.getEntity(e.getValue());
            if (g == null || !nacht || !Ritme.spelerBij(sl, e.getKey(), STRAAL + 24)) {
                if (g != null) {
                    g.discard();
                }
                Rooster.SLAPEND.remove(e.getValue());
                Bedden.vrij(wie(e.getKey()));
                it.remove();
            }
        }
        for (Map.Entry<String, List<UUID>> e : UILEN.entrySet()) {
            Steden.Stad s = Steden.van(e.getKey());
            boolean houden = nacht && s != null && spelerIn(sl, s);
            if (!houden) {
                Ritme.weg(sl, e.getValue());
            } else {
                e.getValue().removeIf(u -> sl.getEntity(u) == null);
            }
        }
    }

    private static boolean spelerIn(ServerLevel sl, Steden.Stad s) {
        for (ServerPlayer p : sl.players()) {
            if (s.bevat(p.getX(), p.getY(), p.getZ())) {
                return true;
            }
        }
        return false;
    }

    private static String wie(BlockPos bed) {
        return "slaper@" + bed.asLong();
    }

    private static void slapers(ServerLevel sl, ServerPlayer p) {
        int bij = 0;
        for (BlockPos b : SLAPERS.keySet()) {
            if (b.distToCenterSqr(p.getX(), b.getY() + 0.5, p.getZ()) < STRAAL * STRAAL) {
                bij++;
            }
        }
        if (bij >= SLAPERS_PER_SPELER) {
            return;
        }
        RandomSource r = sl.getRandom();
        for (Bedden.Bed bed : Bedden.rond(sl, p.blockPosition(), STRAAL, 16)) {
            if (bij >= SLAPERS_PER_SPELER) {
                break;
            }
            if (SLAPERS.containsKey(bed.hoofd()) || Bedden.bezet(bed.hoofd()) || r.nextInt(3) == 0) {
                continue;   // (a third of the beds stay empty: not every house is home)
            }
            if (p.distanceToSqr(bed.x(), bed.y(), bed.z()) < 36) {
                continue;   // (never pop up right under your nose)
            }
            RijkGuh g = slaper(sl, bed, r);
            if (g != null && Bedden.claim(sl, bed.hoofd(), 0, wie(bed.hoofd())) != null) {
                SLAPERS.put(bed.hoofd(), g.getUUID());
                bij++;
            } else if (g != null) {
                g.discard();
            }
        }
    }

    /** One sleeping guh on this bed (a RijkGuh without an NPC id: no quests, never kept, never hurt). */
    static RijkGuh slaper(ServerLevel sl, Bedden.Bed bed, RandomSource r) {
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.setVariant(KLEUREN[r.nextInt(KLEUREN.length)]);
        if (r.nextInt(3) > 0) {
            g.wear(GuhClothes.SLAAPMUTSJE);
        }
        if (r.nextInt(3) == 0) {
            g.wear(GuhClothes.PYJAMA_PAKJE);
        }
        g.setCustomName(Component.literal("z Z z").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        g.snapTo(bed.x(), bed.y(), bed.z(), bed.yaw(), 0);
        g.setYBodyRot(bed.yaw());
        g.setYHeadRot(bed.yaw());
        g.addTag(Ritme.TAG);
        g.addTag(Ritme.TAG + "_slaper");
        sl.addFreshEntity(g);
        Rooster.SLAPEND.add(g.getUUID());
        return g;
    }

    /** Keeps the sleepers curled up (every second; RijkGuh.STIL stops their waving). */
    static void houSlapend(MinecraftServer server) {
        if (server.getTickCount() % 20 != 3 || SLAPERS.isEmpty()) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (Map.Entry<BlockPos, UUID> e : SLAPERS.entrySet()) {
            if (sl.getEntity(e.getValue()) instanceof RijkGuh g) {
                if (g.emotes.current() != Emote.SLAPEN && GuhEmotes.canStart(g)) {
                    g.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
                }
            }
        }
    }

    /** A click on a sleeper: a soft snore, it sleeps on. True = it was one of ours. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!(e instanceof RijkGuh g) || !e.entityTags().contains(Ritme.TAG + "_slaper")) {
            return false;
        }
        p.sendOverlayMessage(Component.translatable(Ritme.P + "slaper.klik." + (1 + p.getRandom().nextInt(3))).withStyle(ChatFormatting.GRAY));
        g.playSound(SNURK, 0.4f, 1.0f);
        return true;
    }

    private static void uilen(ServerLevel sl, Steden.Stad stad, ServerPlayer p) {
        List<UUID> l = UILEN.computeIfAbsent(stad.id(), k -> new ArrayList<>());
        if (l.size() >= UILEN_PER_STAD) {
            return;
        }
        RandomSource r = sl.getRandom();
        int x = p.getBlockX() + r.nextInt(49) - 24, z = p.getBlockZ() + r.nextInt(49) - 24;
        if (!sl.hasChunk(x >> 4, z >> 4) || !stad.bevatXZ(x, z)) {
            return;
        }
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (!stad.bevat(x, y, z) || Math.abs(y - p.getY()) > 30 || p.distanceToSqr(x, y, z) < 100) {
            return;
        }
        var type = nl.juiced.guhs.feature.vogels.VogelsFeature.GUH_UILTJE.get();
        var uil = type.create(sl, EntitySpawnReason.EVENT);
        if (uil == null) {
            return;
        }
        uil.snapTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360f, 0);
        uil.zetThuis(new BlockPos(x, y, z));
        uil.addTag(Ritme.TAG);
        sl.addFreshEntity(uil);
        l.add(uil.getUUID());
    }

    static int aantalSlapers() {
        return SLAPERS.size();
    }

    static int aantalUilen() {
        int n = 0;
        for (List<UUID> l : UILEN.values()) {
            n += l.size();
        }
        return n;
    }

    static void wis() {
        SLAPERS.clear();
        UILEN.clear();
    }
}
