package nl.juiced.guhs.feature.einde2.h2water;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestspel.GeestToegang;
import nl.juiced.guhs.feature.einde2.geestwezens.KohEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 h2_water: the story's way into the Geestenwereld (CONTRACT_E2_B 7.3, DESIGN 5 "De Geestenwereld").
 * <ol>
 *   <li>{@link #vertrek}: the meditation by the pond of the oasis: the scene {@code geest_reis} (you and Aang-guh go "super
 *       saiyan", land in the wrong dimension first: the surprise party), which every player within 5 blocks watches too
 *       (they travel along as guests, CONTRACT_E2_B 2.4); then the flag {@value E2Verhaal#V_GEEST} and
 *       {@code GeestToegang.stuur(p, "aankomst_speler")} once the guests are done watching.</li>
 *   <li>Koh ({@link E2H2Water#KOH}): walking into his chamber (zone {@code koh_kamer}) plays {@code koh}, then the test is
 *       play: for {@link #PROEF_TICKS} ticks no move, jump or click while he changes faces. Fail: your face goes blank for a
 *       while (a client layer, also seen by the players around you) and you are back at the arrival; never any harm.</li>
 *   <li>The Hemelvijver ({@link E2H2Water#HEMELVIJVER}): near it the scene {@code la_woede} (a vision of the angry ocean
 *       spirit), then back to the body ({@code Geestenwereld.terug}).</li>
 * </ol>
 * Everything is triggered by where the player is (CONTRACT_E2_B 4.4), never by a click.
 */
public final class Geestreis {
    /** How long the test of Koh lasts once it began (ticks), the grace before it begins, and how often he changes face. */
    public static final int PROEF_TICKS = 240, VOORAF = 40, WISSEL = 40;
    /** How long a face stays blank after a failed test (ticks). */
    public static final int BLANCO_TICKS = 200;
    /** How far you may sway while you "stand still" (blocks). */
    public static final double STIL = 0.45;
    /** The faces Koh shows in turn while he talks. */
    static final int[] GEZICHTEN = {KohEntity.GUH, KohEntity.MIKA, KohEntity.LACH, KohEntity.VERDRIET, KohEntity.GUH, KohEntity.MIKA};
    /** The guests who came along wait at most this long for the scene of the others to end (ticks). */
    static final int WACHT_MAX = 20 * 40;

    /** A test of Koh that runs for a player: since when (game time; it really begins at {@code begin}) and where they stand. */
    static final class Proef {
        final long begin;
        Vec3 plek;
        int gezicht;

        Proef(long begin, Vec3 plek) {
            this.begin = begin;
            this.plek = plek;
        }
    }

    private static final Map<UUID, Proef> PROEVEN = new ConcurrentHashMap<>();
    /** Players whose trip waits for its guests: until when (game time). */
    private static final Map<UUID, Long> VERTREK = new ConcurrentHashMap<>();

    private Geestreis() {
    }

    // =====================================================================================================================
    // the trip
    // =====================================================================================================================

    /** The meditation is done: the scene for the player and every player within 5 blocks, then into the Geestenwereld. */
    static boolean vertrek(ServerPlayer p) {
        if (!E2H2Water.aanZet(p, E2H2Water.MEDITEER)) {
            return false;
        }
        boolean gestart = E2H2Water.speel(p, E2H2WaterScenes.GEEST_REIS, "yue_maan", Geestreis::naReis);
        if (!gestart) {
            if (E2H2Water.oord(p.level().getServer()) == null) {
                naReis(p);
                return true;
            }
            return false;
        }
        for (ServerPlayer gast : gastenBij(p)) {
            E2H2Water.speel(gast, E2H2WaterScenes.GEEST_REIS, "yue_maan", null);   // (a guest only watches: nothing of their own changes)
        }
        return true;
    }

    /** The players within 5 blocks of the guide who could travel along (not the guide, not watching anything). */
    static List<ServerPlayer> gastenBij(ServerPlayer gids) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer q : gids.level().getEntitiesOfClass(ServerPlayer.class, gids.getBoundingBox().inflate(5))) {
            if (q != gids && !q.isSpectator() && q.distanceToSqr(gids) <= 25 && !Cutscenes.bezig(q)) {
                uit.add(q);
            }
        }
        return uit;
    }

    static void naReis(ServerPlayer p) {
        Verhaallijn l = E2H2Water.lijn();
        if (!l.verder(p, E2H2Water.MEDITEER)) {
            return;
        }
        l.vlag(p, E2Verhaal.V_GEEST, true);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        VERTREK.put(p.getUUID(), p.level().getGameTime() + WACHT_MAX);
        stuurAlsKlaar(p);
    }

    /** Sends the guide in once no guest within 8 blocks still watches the scene (or the wait is over). */
    private static void stuurAlsKlaar(ServerPlayer p) {
        Long tot = VERTREK.get(p.getUUID());
        if (tot == null) {
            return;
        }
        boolean wacht = false;
        for (ServerPlayer q : p.level().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(8))) {
            if (q != p && Cutscenes.bezig(q)) {
                wacht = true;
                break;
            }
        }
        if (wacht && p.level().getGameTime() < tot) {
            return;
        }
        VERTREK.remove(p.getUUID());
        if (!GeestToegang.stuur(p, Geestenwereld.AANKOMST)) {
            E2H2Water.verteller(p, "quest.guhs.einde2_h2_water.geest.geen_wereld");
            return;
        }
        E2H2Water.zeg(p, GuhNpcEntity.Kind.AANG_GUH, "quest.guhs.einde2_h2_water.geest.aang");
        E2H2Water.hint(p, "quest.guhs.einde2_h2_water.hint.koh");
    }

    /** At Koh's or the Hemelvijver's step, but the soul went back to its body: meditating again takes you straight in. */
    static void terugIn(ServerPlayer p) {
        if (GeestToegang.stuur(p, Geestenwereld.AANKOMST)) {
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.geest.weer");
        }
    }

    // =====================================================================================================================
    // inside the Geestenwereld
    // =====================================================================================================================

    /** The world position of a marker of the Geestenwereld (null: none). */
    @Nullable
    static BlockPos plek(String id) {
        return Geestenwereld.wereld(id);
    }

    /** Is the player inside this zone marker of the Geestenwereld (grown by rand)? */
    static boolean inZone(ServerPlayer p, String zone, double rand) {
        Plekken.Plek z = Plekken.plek(Geestenwereld.NAAM, zone);
        if (z == null) {
            return false;
        }
        BlockPos a = Geestenwereld.OORSPRONG.offset(z.pos()), b = Geestenwereld.OORSPRONG.offset(z.tot() == null ? z.pos() : z.tot());
        AABB doos = new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1).inflate(rand);
        return doos.contains(p.position());
    }

    /** Once a second for a player in the Geestenwereld whose chapter is not done. */
    static void seconde(ServerPlayer p, int stap) {
        stuurAlsKlaar(p);
        if (stap == E2H2Water.KOH) {
            if (PROEVEN.containsKey(p.getUUID()) || Cutscenes.bezig(p)) {
                return;
            }
            if (inZone(p, "koh_kamer", 0)) {
                BlockPos koh = plek("koh");
                if (!Cutscenes.gezien(p, E2H2WaterScenes.KOH.id()) && koh != null) {
                    Cutscenes.speel(p, E2H2WaterScenes.KOH, koh, Rotation.NONE, speler -> start(speler, 20));
                } else {
                    E2H2Water.zegAls(p, "quest.guhs.einde2_h2_water.naam.koh", "quest.guhs.einde2_h2_water.koh.weer");
                    start(p, VOORAF);
                }
            } else {
                wijsNaar(p, "koh");
            }
        } else if (stap == E2H2Water.HEMELVIJVER) {
            BlockPos vijver = plek("plek_hemelvijver");
            if (vijver == null) {
                return;
            }
            boolean bij = Vec3.atBottomCenterOf(vijver).distanceToSqr(p.position()) < 14 * 14 || inZone(p, "vijver", 4)
                    || (plek("tui_la_speler") != null && Vec3.atBottomCenterOf(plek("tui_la_speler")).distanceToSqr(p.position()) < 10 * 10);
            if (bij) {
                Cutscenes.speel(p, E2H2WaterScenes.LA_WOEDE, vijver, Rotation.NONE, Geestreis::naLaWoede);
            } else {
                wijsNaar(p, "plek_hemelvijver");
            }
        }
    }

    /** A soft light over where the player has to go in the Geestenwereld (only for them, only when it is near). */
    static void wijsNaar(ServerPlayer p, String id) {
        BlockPos b = plek(id);
        if (b != null && b.distToCenterSqr(p.position()) < 64 * 64) {
            Schijn.deeltjes(p, ParticleTypes.END_ROD, Vec3.atBottomCenterOf(b).add(0, 3, 0), 3, 0.3, 0.01);
        }
    }

    /** After La's wrath: back to the body, on to the moon fish. */
    static void naLaWoede(ServerPlayer p) {
        if (!E2H2Water.lijn().verder(p, E2H2Water.HEMELVIJVER)) {
            return;
        }
        E2H2Water.zeg(p, GuhNpcEntity.Kind.AANG_GUH, "quest.guhs.einde2_h2_water.la.aang");
        Geestenwereld.terug(p);
        E2H2Water.hint(p, "quest.guhs.einde2_h2_water.hint.vis_halen");
    }

    // =====================================================================================================================
    // Koh's test
    // =====================================================================================================================

    /** The test begins after {@code vooraf} ticks: stand still, show nothing. */
    static void start(ServerPlayer p, int vooraf) {
        if (!E2H2Water.aanZet(p, E2H2Water.KOH)) {
            return;
        }
        PROEVEN.put(p.getUUID(), new Proef(p.level().getGameTime() + vooraf, p.position()));
        titel(p, "quest.guhs.einde2_h2_water.koh.titel", "quest.guhs.einde2_h2_water.koh.ondertitel");
        E2Audio.stinger(p, "geest.koh_stilte");
    }

    /** Is a test of Koh running for this player? */
    static boolean bezig(ServerPlayer p) {
        return PROEVEN.containsKey(p.getUUID());
    }

    /** Every tick (the feature's tick listener): the test, and a trip that waits for its guests. */
    static void tick(ServerPlayer p) {
        Proef proef = PROEVEN.get(p.getUUID());
        if (proef == null) {
            return;
        }
        if (!E2H2Water.aanZet(p, E2H2Water.KOH) || (!Geestenwereld.in(p) && E2H2Water.TEST == null)) {
            PROEVEN.remove(p.getUUID());
            return;
        }
        long nu = p.level().getGameTime();
        if (nu < proef.begin) {
            proef.plek = p.position();   // (settle in before it begins)
            return;
        }
        Vec3 d = p.position().subtract(proef.plek);
        if (d.horizontalDistance() > STIL || Math.abs(d.y) > 0.6) {
            faal(p, "beweeg");
            return;
        }
        long t = nu - proef.begin;
        if (t % WISSEL == 0) {
            int i = Math.min(GEZICHTEN.length - 1, (int) (t / WISSEL));
            proef.gezicht = GEZICHTEN[i];
            KohEntity koh = koh(p);
            if (koh != null) {
                koh.wissel(proef.gezicht);
            }
            E2Audio.stinger(p, "geest.koh_kraak");
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.koh.gezicht." + i);
        }
        if (t >= PROEF_TICKS) {
            geslaagd(p);
        }
    }

    /** A jump or a click: a feeling shown. */
    static void emotie(ServerPlayer p, String wat) {
        Proef proef = PROEVEN.get(p.getUUID());
        if (proef != null && p.level().getGameTime() >= proef.begin - VOORAF / 2) {
            faal(p, wat);
        }
    }

    /** Koh, if he is near (the resident of his cave). */
    @Nullable
    static KohEntity koh(ServerPlayer p) {
        List<KohEntity> k = p.level().getEntitiesOfClass(KohEntity.class, p.getBoundingBox().inflate(32));
        return k.isEmpty() ? null : k.get(0);
    }

    static void geslaagd(ServerPlayer p) {
        PROEVEN.remove(p.getUUID());
        KohEntity koh = koh(p);
        if (koh != null) {
            koh.wissel(KohEntity.MASKER);
        }
        String naam = "quest.guhs.einde2_h2_water.naam.koh";
        E2H2Water.zegAls(p, naam, "quest.guhs.einde2_h2_water.koh.goed.1");
        E2H2Water.zegAls(p, naam, "quest.guhs.einde2_h2_water.koh.goed.2");
        E2H2Water.zegAls(p, naam, "quest.guhs.einde2_h2_water.koh.goed.3");
        if (E2H2Water.lijn().verder(p, E2H2Water.KOH)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.7f);
            E2H2Water.zeg(p, GuhNpcEntity.Kind.AANG_GUH, "quest.guhs.einde2_h2_water.koh.aang");
            E2H2Water.hint(p, "quest.guhs.einde2_h2_water.hint.hemelvijver");
        }
    }

    /** Failed: Koh takes your face for a while (a blank face, seen by you and the players around), back to the arrival. */
    static void faal(ServerPlayer p, String wat) {
        if (PROEVEN.remove(p.getUUID()) == null) {
            return;
        }
        KohEntity koh = koh(p);
        if (koh != null) {
            koh.zetAnimatie("steel");
            koh.wissel(KohEntity.BLANCO);
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.PLAYERS, 1f, 0.6f);
        E2H2Water.zegAls(p, "quest.guhs.einde2_h2_water.naam.koh", "quest.guhs.einde2_h2_water.koh.fout." + wat);
        E2H2WaterPayloads.blanco(p, BLANCO_TICKS);
        p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h2_water.koh.fout").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (Geestenwereld.in(p)) {
            Vec3 a = Geestenwereld.aankomst(p.level(), p.getX(), p.getZ());
            Duwtje.terug(p, p.level().dimension(), a, 180f);
        }
    }

    private static void titel(ServerPlayer p, String titel, String onder) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(onder).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(titel).withStyle(ChatFormatting.DARK_AQUA)));
    }

    /** (logout, wipe) forget what runs for this player. */
    static void vergeet(ServerPlayer p) {
        PROEVEN.remove(p.getUUID());
        VERTREK.remove(p.getUUID());
    }

    /** (tests) the level a test of Koh runs in may be any level. */
    static void zetProef(ServerPlayer p, long begin) {
        PROEVEN.put(p.getUUID(), new Proef(begin, p.position()));
    }

    /** (unused level helper kept for the commands) */
    @Nullable
    static ServerLevel wereld(ServerPlayer p) {
        return Geestenwereld.level(p.level().getServer());
    }
}
