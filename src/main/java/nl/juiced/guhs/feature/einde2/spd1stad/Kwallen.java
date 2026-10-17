package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spcast.SpCast;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.KruimelkwalEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh", slice sp_d1_stad: part 1 step {@value D1Stad#KWALLEN}, jellyfishing with Patguh (CONTRACT_E2_C 7.4, DESIGN
 * 5b deel 1.2, wink 7).
 * <ol>
 *   <li><b>To Patguh</b> (sleutel {@code 5}): he naps in front of his kruimelrots on the Schelpstraat (the resident). Come within
 *       {@value #BIJ_ROTS} blocks, or click him: the scene {@code patguh} (why: the kwallenjam for De Krokante Knabbel), you get
 *       his net ({@link E2SpD1StadFeature#KWALLENNET}, kept), the flag {@value D1Stad#V_NET}.</li>
 *   <li><b>Catch eight</b> (sleutel {@code 5_b}): Patguh (your own copy, {@code Zicht.alleenVoor}; the resident stays at his
 *       rock) waits at the edge of {@code kwallenveld_1}. Step into the field: the first time wink 7 ({@code knipoog_rookguh}),
 *       then a title with the one-line rule. Kruimelkwallen float in the field: your own ({@value #EIGEN_MIN} or more, topped up)
 *       and the wild ones of everybody. A tap with the net (left or right click) catches one: yours pops into your jar and a new
 *       one floats in later; a wild one counts once per try and bobs on for the others. The counter "Kwallen n/8" stays over
 *       the hotbar while you are in the field; it is saved ({@value D1Stad#T_KWALLEN}), so leaving and coming back never loses a
 *       jelly. No timer, nothing to lose.</li>
 *   <li><b>Done</b> at {@value #NODIG}: a title and a sound, the scene {@code kwallen_klaar}, the rewards (three kwallenjam and
 *       Patguh's bloemetjesbroek, once), the line moves on.</li>
 * </ol>
 */
public final class Kwallen {
    public static final int NODIG = 8;
    /** How close (blocks) to Patguh at his rock starts the scene; how many of your own jellies float in the field. */
    public static final double BIJ_ROTS = 6.5;
    public static final int EIGEN_MIN = 6;
    /** The field of the game (the one next to Patguh's spot), and how far outside it still counts as "in". */
    public static final String VELD = "kwallenveld_1";
    public static final double RAND = 3;
    /** Ticks before a caught jelly of your own is followed by a new one. */
    public static final int TERUG = 120;

    /** Per player, not saved (the count is in the line). */
    static final class Spel {
        boolean aan;
        @Nullable
        UUID patguh;
        final List<UUID> eigen = new ArrayList<>();
        final Set<UUID> wild = ConcurrentHashMap.newKeySet();
        long volgende;
        boolean klaar;
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private Kwallen() {
    }

    static Spel spel(ServerPlayer p) {
        return SPELLEN.computeIfAbsent(p.getUUID(), u -> new Spel());
    }

    static int gevangen(ServerPlayer p) {
        return D1Stad.lijn().teller(p, D1Stad.T_KWALLEN);
    }

    // =====================================================================================================================
    // the step
    // =====================================================================================================================

    /** Every 10 ticks for a player at step 5 (E2SpD1StadFeature). */
    static void tick(ServerPlayer p, long nu) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        if (g == null || p.level() != g || Cutscenes.bezig(p)) {
            return;
        }
        if (!D1Stad.tweedeHelft(p, D1Stad.KWALLEN)) {
            Vec3 rots = D1Stad.midden(g, "patguh");
            if (rots != null && rots.distanceToSqr(p.position()) < BIJ_ROTS * BIJ_ROTS && p.isAlive()) {
                begin(p);
            }
            return;
        }
        Spel s = spel(p);
        Vec3 plek = D1Stad.midden(g, "kwallen_patguh");
        // your Patguh at the edge of the field while you are in this part of town
        if (plek != null && plek.distanceToSqr(p.position()) < 80 * 80) {
            if (D1Stad.levend(g, s.patguh) == null) {
                SponsguhWezen pat = D1Stad.wezen(g, E2SponsguhFeature.PATGUH.get(), plek, 45f, p);
                if (pat != null) {
                    SpCast.zetLook(pat, SpCast.NETJE, true);
                    s.patguh = pat.getUUID();
                }
            }
        } else {
            ruimOp(p, g, true);
            return;
        }
        if (gevangen(p) >= NODIG) {
            klaar(p);
            return;
        }
        boolean in = D1Stad.inZone(g, VELD, p.position(), RAND);
        if (in && !s.aan) {
            if (!Cutscenes.gezien(p, D1StadScenes.KNIPOOG_ROOKGUH.id())) {
                D1Stad.speel(p, D1StadScenes.KNIPOOG_ROOKGUH, "knipoog_rookguh_patguh", q -> start(q));
            } else {
                start(p);
            }
            return;
        }
        if (!in && s.aan && !D1Stad.inZone(g, VELD, p.position(), RAND + 10)) {
            pauze(p, g);
            return;
        }
        if (s.aan) {
            if (nu >= s.volgende) {
                s.volgende = nu + 100;
                vulAan(p, g, s);
            }
            if (nu % 20 < 10) {
                D1Stad.balk(p, "kwallen.teller", gevangen(p), NODIG);
            }
        }
    }

    /** The scene {@code patguh}, then the net and the second half of the step. */
    static void begin(ServerPlayer p) {
        if (!D1Stad.aanZet(p, D1Stad.KWALLEN) || D1Stad.tweedeHelft(p, D1Stad.KWALLEN) || Cutscenes.bezig(p)) {
            return;
        }
        D1Stad.speel(p, D1StadScenes.PATGUH, "patguh", q -> {
            if (!D1Stad.aanZet(q, D1Stad.KWALLEN)) {
                return;
            }
            D1Stad.lijn().vlag(q, D1Stad.V_NET, true);
            geefNet(q);
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(q);
            D1Stad.hint(q, D1Stad.KWALLEN, D1StadScenes.KIJK_KWALLEN, "kwallen_start", "hint.kwallen", 20);
        });
    }

    /** Patguh's net, unless the player already carries one. */
    static void geefNet(ServerPlayer p) {
        if (!p.getInventory().contains(s -> s.is(E2SpD1StadFeature.KWALLENNET.get()))) {
            Minigames.give(p, new ItemStack(E2SpD1StadFeature.KWALLENNET.get()));
        }
    }

    /** In the field: the title, the rule, the tune, the jellies. */
    static void start(ServerPlayer p) {
        if (!D1Stad.aanZet(p, D1Stad.KWALLEN) || !D1Stad.tweedeHelft(p, D1Stad.KWALLEN)) {
            return;
        }
        Spel s = spel(p);
        s.aan = true;
        s.klaar = false;
        s.wild.clear();
        s.volgende = 0;
        geefNet(p);
        D1Stad.titel(p, "kwallen.titel", "kwallen.regel", NODIG);
        SpAudio.stinger(p, "deuntje.kwallen");
        D1Stad.geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 1.2f);
    }

    /** Out of the field: the jellies of this player go, the count stays. */
    static void pauze(ServerPlayer p, ServerLevel g) {
        Spel s = spel(p);
        s.aan = false;
        for (UUID id : s.eigen) {
            D1Stad.weg(g, id);
        }
        s.eigen.clear();
        D1Stad.balk(p, "kwallen.pauze", gevangen(p), NODIG);
    }

    /** Your own jellies in the field: at least {@value #EIGEN_MIN}. */
    static void vulAan(ServerPlayer p, ServerLevel g, Spel s) {
        s.eigen.removeIf(id -> D1Stad.levend(g, id) == null);
        Vec3 midden = midden(g);
        if (midden == null) {
            return;
        }
        for (int i = s.eigen.size(); i < EIGEN_MIN; i++) {
            KruimelkwalEntity kwal = E2ZweversFeature.KRUIMELKWAL.get().create(g, EntitySpawnReason.TRIGGERED);
            if (kwal == null) {
                return;
            }
            double hoek = p.getRandom().nextDouble() * Math.PI * 2, af = 3 + p.getRandom().nextDouble() * 7;
            Vec3 plek = midden.add(Math.cos(hoek) * af, 1.6 + p.getRandom().nextDouble() * 1.6, Math.sin(hoek) * af);
            kwal.snapTo(plek.x, plek.y, plek.z, p.getRandom().nextFloat() * 360f, 0f);
            kwal.setThuis(plek, 4);
            D1Stad.eigen(kwal, p);
            g.addFreshEntity(kwal);
            g.sendParticles(p, ParticleTypes.GLOW, true, false, plek.x, plek.y + 0.4, plek.z, 6, 0.3, 0.3, 0.3, 0.01);
            s.eigen.add(kwal.getUUID());
        }
    }

    /** The middle of the field at walking height (null: not known). */
    @Nullable
    static Vec3 midden(@Nullable ServerLevel g) {
        var z = nl.juiced.guhs.feature.einde2.Plekken.plek(D1Stad.STRUCTUUR, VELD);
        BlockPos o = g == null || !nl.juiced.guhs.feature.einde2.Plekken.bekend(g, D1Stad.STRUCTUUR) ? null
                : nl.juiced.guhs.feature.einde2.Plekken.oorsprong(g, D1Stad.STRUCTUUR);
        if (z == null || z.tot() == null || o == null) {
            return null;
        }
        BlockPos a = o.offset(z.pos()), b = o.offset(z.tot());
        BlockPos m = new BlockPos((a.getX() + b.getX()) / 2, Math.min(a.getY(), b.getY()), (a.getZ() + b.getZ()) / 2);
        BlockPos top = g.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, m);
        return Vec3.atBottomCenterOf(top.getY() >= m.getY() - 2 && top.getY() <= m.getY() + 12 ? top : m);
    }

    // =====================================================================================================================
    // catching
    // =====================================================================================================================

    /**
     * A tap with the net on a kruimelkwal (attack or use). True = the story took it (the event is cancelled): the jelly was
     * counted, or it was a jelly of this player's game.
     */
    static boolean tik(ServerPlayer p, Entity doel) {
        if (!(doel instanceof KruimelkwalEntity kwal) || !p.getMainHandItem().is(E2SpD1StadFeature.KWALLENNET.get())
                || !D1Stad.aanZet(p, D1Stad.KWALLEN) || !D1Stad.tweedeHelft(p, D1Stad.KWALLEN) || Cutscenes.bezig(p)) {
            return false;
        }
        Spel s = spel(p);
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        boolean inVeld = D1Stad.PROEF || D1Stad.inZone(g, VELD, kwal.position(), RAND + 2);
        if (!inVeld) {
            D1Stad.balk(p, "kwallen.niet_hier");
            return false;
        }
        if (!s.aan) {
            start(p);
        }
        boolean vanMij = p.getUUID().equals(Zicht.eigenaar(kwal));
        if (!vanMij && !s.wild.add(kwal.getUUID())) {
            D1Stad.balk(p, "kwallen.al");
            return true;
        }
        if (vanMij) {
            s.eigen.remove(kwal.getUUID());
            kwal.discard();
            s.volgende = Math.min(s.volgende, p.level().getServer().getTickCount() + TERUG);
        }
        ((ServerLevel) p.level()).sendParticles(p, ParticleTypes.BUBBLE_POP, true, false, kwal.getX(), kwal.getY() + 0.4, kwal.getZ(), 14, 0.3, 0.3, 0.3, 0.05);
        ((ServerLevel) p.level()).sendParticles(p, ParticleTypes.GLOW, true, false, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        D1Stad.geluid(p, SoundEvents.BOTTLE_FILL, 0.8f, 1.4f + 0.05f * gevangen(p));
        return tel(p);
    }

    /** One jelly in the jar; at {@value #NODIG} the end of the step. Always true. */
    static boolean tel(ServerPlayer p) {
        int n = Math.min(NODIG, gevangen(p) + 1);
        D1Stad.lijn().teller(p, D1Stad.T_KWALLEN, n);
        D1Stad.balk(p, "kwallen.teller", n, NODIG);
        if (n >= NODIG) {
            klaar(p);
        }
        return true;
    }

    /** The jar is full: title, sound, scene kwallen_klaar, rewards, step 6. */
    static void klaar(ServerPlayer p) {
        Spel s = spel(p);
        if (s.klaar) {
            return;
        }
        s.klaar = true;
        s.aan = false;
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        for (UUID id : s.eigen) {
            D1Stad.weg(g, id);
        }
        s.eigen.clear();
        D1Stad.titel(p, "kwallen.vol", "kwallen.vol_onder");
        SpAudio.stinger(p, "stinger.opgelost");
        D1Stad.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
        if (D1Stad.PROEF) {
            afronden(p);                                     // (a test: no scene)
        } else {
            nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 30,
                    q -> D1Stad.speel(q, D1StadScenes.KWALLEN_KLAAR, "kwallen_start", Kwallen::afronden));
        }
    }

    /** After the scene: the rewards (once), the flags of the step gone, on to the exam. */
    static void afronden(ServerPlayer p) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        ruimOp(p, g, true);
        if (!D1Stad.aanZet(p, D1Stad.KWALLEN)) {
            return;
        }
        if (D1Stad.lijn().eenmalig(p, "sp_d1_stad_jam")) {
            Minigames.give(p, new ItemStack(E2SponsguhFeature.KWALLENJAM.get(), 3));
            Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.SPONSGUH_BLOEMETJESBROEK)));
            D1Stad.balk(p, "kwallen.beloning");
        }
        D1Stad.lijn().vlag(p, D1Stad.V_NET, false);
        D1Stad.lijn().teller(p, D1Stad.T_KWALLEN, 0);
        D1Stad.lijn().verder(p, D1Stad.KWALLEN);
    }

    /** The actors of this player go (the field's Patguh too when {@code alles}). */
    static void ruimOp(ServerPlayer p, @Nullable ServerLevel g, boolean alles) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        for (UUID id : s.eigen) {
            D1Stad.weg(g, id);
        }
        s.eigen.clear();
        s.aan = false;
        if (alles) {
            D1Stad.weg(g, s.patguh);
            s.patguh = null;
            SPELLEN.remove(p.getUUID());
        }
    }

    static void vergeet(UUID u) {
        SPELLEN.remove(u);
    }
}
