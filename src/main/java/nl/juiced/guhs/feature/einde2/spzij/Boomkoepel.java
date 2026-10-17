package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 "Sponsguh", slice sp_zij: Sandy-guh's tree dome ({@value SpZijLijnen#SANDY}, four steps).
 * <ol start="0">
 *   <li>visit Sandy-guh through the airlock: walk up to her (or click her): scene {@code sandy} (the bubble helmet, her
 *       pink sky, karate?);</li>
 *   <li>karate on the mat ({@code karate_mat}): she winds up ("HAI...!", a ring of sparks where the shove will land), you
 *       step aside or jump; 10 dodges ({@link Karate}). A shove that lands only pushes you softly and costs nothing; leaving
 *       the mat pauses the game, the count stays;</li>
 *   <li>three rocket parts lie round town ({@code raket_onderdeel_1..3}, things of ONE player): walk over them;</li>
 *   <li>back to the rocket in the dome ({@code raket}): scene {@code sandy_raket}; reward the luchthelm.</li>
 * </ol>
 * Visibility (CONTRACT_E2_C 4.3, owner of {@code bikinibodem/sandy}): the shared resident is there for players past the
 * karate (steps 2.. and done); a player before that (steps 0-1, the visit and the karate) has Sandy-guh of their own
 * ({@link SpZijEigen}), on her spot or on the mat, so a resident and a story copy never stand together.
 */
public final class Boomkoepel {
    static final String T = SpZij.P + "sandy.";
    static final String KOPIE = "sandy";
    /** The three rocket parts: marker, the item it looks like. */
    static final List<String> ONDERDELEN = List.of("minecraft:firework_rocket", "minecraft:hopper", "minecraft:lightning_rod");
    static final double PAK = 1.7;

    /** The karate games that run now (per player, not saved: the count of dodges is, in the line). */
    private static final Map<UUID, Karate> KARATE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BELLEN = new ConcurrentHashMap<>();

    private Boomkoepel() {
    }

    static Verhaallijn ln() {
        return SpZijLijnen.SANDY_LIJN;
    }

    // =====================================================================================================================
    // the karate game (pure: the game test drives it with made-up positions)
    // =====================================================================================================================

    /**
     * One karate game: rest, wind-up, the shove; a dodge counts when the player stands at least {@link #OPZIJ} blocks from
     * where they stood when she wound up, or is in the air (a jump). A shove that lands counts nothing, but takes nothing
     * away either. {@link #NODIG} dodges win.
     */
    static final class Karate {
        static final int NODIG = 10, RUST = 30, AANLOOP = 26;
        static final double OPZIJ = 1.0;

        enum Uit { NIETS, AANLOOP, ONTWEKEN, GERAAKT, KLAAR }

        int ontweken;
        boolean aanloop;
        long sinds;
        @Nullable
        Vec3 vanaf;

        Karate(int ontweken, long nu) {
            this.ontweken = ontweken;
            this.sinds = nu;
        }

        Uit tik(long nu, Vec3 pos, boolean opGrond) {
            if (ontweken >= NODIG) {
                return Uit.KLAAR;
            }
            if (!aanloop) {
                if (nu - sinds < RUST) {
                    return Uit.NIETS;
                }
                aanloop = true;
                sinds = nu;
                vanaf = pos;
                return Uit.AANLOOP;
            }
            if (nu - sinds < AANLOOP) {
                return Uit.NIETS;
            }
            aanloop = false;
            sinds = nu;
            double dx = pos.x - vanaf.x, dz = pos.z - vanaf.z;
            if (dx * dx + dz * dz >= OPZIJ * OPZIJ || !opGrond) {
                ontweken++;
                return ontweken >= NODIG ? Uit.KLAAR : Uit.ONTWEKEN;
            }
            return Uit.GERAAKT;
        }
    }

    // =====================================================================================================================
    // every 5 ticks while the player is in town
    // =====================================================================================================================

    static void tick(ServerPlayer p, ServerLevel level, long nu) {
        int stap = ln().stap(p);
        Vec3 sandyPlek = SpZij.voeten(level, "sandy");
        if (sandyPlek == null) {
            return;
        }
        // Sandy-guh of your own while you have not done the karate yet
        boolean dichtbij = p.position().distanceTo(sandyPlek) < 48;
        if (stap <= SpZijLijnen.S_KARATE && dichtbij) {
            SponsguhWezen s = SpZijEigen.wezen(level, p, KOPIE, Cast.SANDY_GUH, sandyPlek, 90f);
            if (s != null && !KARATE.containsKey(p.getUUID()) && s.position().distanceToSqr(sandyPlek) > 0.5) {
                s.teleportTo(sandyPlek.x, sandyPlek.y, sandyPlek.z);
            }
        } else if (SpZijEigen.heeft(level, p, KOPIE) && !Praat.bezig(p)) {
            SpZijEigen.weg(level, p, KOPIE);           // (never while she still talks to you: the talk would close)
        }
        if (nu % 100 < 5 && dichtbij) {
            markeerOudeBewoners(level, p);
        }
        if (Cutscenes.bezig(p) || Praat.bezig(p)) {
            return;
        }
        boolean inKoepel = inZone(level, "boomkoepel", p.position());
        if (inKoepel && stap >= SpZijLijnen.S_KARATE && stap < SpZijLijnen.S_KLAAR) {
            bellen(p, level, nu);
        }
        switch (stap) {
            case SpZijLijnen.S_BEZOEK -> {
                if (SpZij.open(p) && SpZij.bij(p.position(), sandyPlek, 3.5)) {
                    bezoek(p, level);
                }
            }
            case SpZijLijnen.S_ONDERDELEN -> onderdelen(p, level);
            case SpZijLijnen.S_RAKET -> {
                Vec3 raket = SpZij.voeten(level, "raket");
                if (inKoepel && SpZij.bij(p.position(), raket, 4.5)) {
                    raket(p, level);
                }
            }
            default -> {
            }
        }
        if (stap != SpZijLijnen.S_ONDERDELEN) {
            SpZijEigen.wegAlles(level, p, "onderdeel_");
        }
    }

    /** Every tick of a player at the karate step: the game runs while they stand on the mat. */
    static void karateTik(ServerPlayer p, ServerLevel level, long nu) {
        if (ln().stap(p) != SpZijLijnen.S_KARATE || !SpZij.open(p)) {
            KARATE.remove(p.getUUID());
            return;
        }
        boolean opMat = inZone(level, "karate_mat", p.position()) && p.isAlive() && !Cutscenes.bezig(p) && !Praat.bezig(p);
        Karate k = KARATE.get(p.getUUID());
        SponsguhWezen sandy = SpZijEigen.get(level, p, KOPIE) instanceof SponsguhWezen w ? w : null;
        if (!opMat) {
            if (k != null) {
                KARATE.remove(p.getUUID());
                SpZij.balk(p, T + "karate.pauze", k.ontweken, Karate.NODIG);
            }
            return;
        }
        if (k == null) {
            k = new Karate(ln().teller(p, "karate"), nu);
            KARATE.put(p.getUUID(), k);
            SpZij.titel(p, T + "karate.titel", T + "karate.regel");
            SpZij.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1.2f);
            Vec3 matRand = matRand(level);
            if (sandy != null && matRand != null) {
                sandy.teleportTo(matRand.x, matRand.y, matRand.z);
            }
        }
        if (sandy != null) {
            kijkNaar(sandy, p.position());
        }
        Karate.Uit uit = k.tik(nu, p.position(), p.onGround());
        switch (uit) {
            case AANLOOP -> {
                SpZij.balk(p, T + "karate.hai", k.ontweken, Karate.NODIG);
                SpZij.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 0.8f);
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    level.sendParticles(p, ParticleTypes.CRIT, true, true, k.vanaf.x + Math.cos(a) * 0.8, k.vanaf.y + 0.1,
                            k.vanaf.z + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
                }
                if (sandy != null) {
                    sandy.zetAnimatie("praat");
                }
            }
            case ONTWEKEN, KLAAR -> {
                ln().teller(p, "karate", k.ontweken);
                if (sandy != null) {
                    sandy.zetAnimatie("karate");
                }
                SpZij.geluid(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
                SpZij.geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.7f, 0.8f + k.ontweken * 0.06f);
                SpZij.balk(p, T + "karate.ontweken", k.ontweken, Karate.NODIG);
                if (uit == Karate.Uit.KLAAR) {
                    karateKlaar(p, level, sandy);
                }
            }
            case GERAAKT -> {
                if (sandy != null) {
                    sandy.zetAnimatie("karate");
                    Vec3 weg = p.position().subtract(sandy.position()).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : weg.normalize();
                    p.setDeltaMovement(weg.x * 0.35, 0.22, weg.z * 0.35);
                    p.hurtMarked = true;
                }
                SpZij.geluid(p, SoundEvents.WOOL_HIT, 1f, 0.7f);
                SpZij.balk(p, T + "karate.geraakt", k.ontweken, Karate.NODIG);
            }
            default -> {
            }
        }
    }

    private static void karateKlaar(ServerPlayer p, ServerLevel level, @Nullable SponsguhWezen sandy) {
        KARATE.remove(p.getUUID());
        SpZij.titel(p, T + "karate.klaar", T + "karate.klaar_onder", Karate.NODIG, Karate.NODIG);
        SpZij.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.8f, 1.2f);
        if (sandy != null) {
            sandy.zetAnimatie("juich");
            Praat.scene(p, "einde2_sp_zij_sandy_karate", List.of(new Praat.Regel(sandy, "", T + "na_karate.1"),
                    new Praat.Regel(sandy, "", T + "na_karate.2"), new Praat.Regel(sandy, "", T + "na_karate.3")));
        } else {
            SpZij.stem(p, naam(), T + "na_karate.2");
            SpZij.stem(p, naam(), T + "na_karate.3");
        }
        ln().verder(p, SpZijLijnen.S_KARATE);
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** Step 0: the visit (scene sandy). */
    static void bezoek(ServerPlayer p, ServerLevel level) {
        BlockPos anker = SpZij.plek(level, "sandy");
        if (anker == null || !Cutscenes.speel(p, SpZijScenes.SANDY, anker, Rotation.NONE, s -> ln().verder(s, SpZijLijnen.S_BEZOEK))) {
            if (anker != null && !Cutscenes.bezig(p)) {
                ln().verder(p, SpZijLijnen.S_BEZOEK);
            }
        }
    }

    /** Step 2: the parts of ONE player round town; walking over one picks it up. */
    static void onderdelen(ServerPlayer p, ServerLevel level) {
        for (int i = 1; i <= ONDERDELEN.size(); i++) {
            String naam = "onderdeel_" + i;
            Vec3 plek = SpZij.voeten(level, "raket_onderdeel_" + i);
            if (plek == null || ln().vlag(p, naam)) {
                SpZijEigen.weg(level, p, naam);
                continue;
            }
            double d = p.position().distanceTo(plek);
            if (d > 64) {
                SpZijEigen.weg(level, p, naam);
                continue;
            }
            SpZijEigen.ding(level, p, naam, ONDERDELEN.get(i - 1), plek.add(0, 0.55, 0), 1.1f);
            level.sendParticles(p, ParticleTypes.END_ROD, true, true, plek.x, plek.y + 0.9, plek.z, 2, 0.25, 0.3, 0.25, 0.01);
            if (SpZij.bij(p.position(), plek, PAK)) {
                pak(p, level, i);
            }
        }
    }

    /** One part picked up (true: it was new). The third moves the line on. */
    static boolean pak(ServerPlayer p, @Nullable ServerLevel level, int i) {
        String naam = "onderdeel_" + i;
        if (ln().stap(p) != SpZijLijnen.S_ONDERDELEN || ln().vlag(p, naam)) {
            return false;
        }
        ln().vlag(p, naam, true);
        if (level != null) {
            SpZijEigen.weg(level, p, naam);
        }
        int n = gevonden(p);
        SpZij.geluid(p, SoundEvents.ITEM_PICKUP, 1f, 0.9f);
        SpZij.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 1.0f + n * 0.2f);
        SpZij.titel(p, T + "onderdeel.titel", T + "onderdeel." + i, n, ONDERDELEN.size());
        if (n >= ONDERDELEN.size()) {
            SpZij.merk(p, T + "onderdeel.alle");
            ln().verder(p, SpZijLijnen.S_ONDERDELEN);
        }
        return true;
    }

    static int gevonden(ServerPlayer p) {
        int n = 0;
        for (int i = 1; i <= ONDERDELEN.size(); i++) {
            n += ln().vlag(p, "onderdeel_" + i) ? 1 : 0;
        }
        return n;
    }

    /** Step 3: the rocket (scene sandy_raket), then the luchthelm. */
    static void raket(ServerPlayer p, ServerLevel level) {
        BlockPos anker = SpZij.plek(level, "raket");
        if (anker == null) {
            return;
        }
        if (!Cutscenes.speel(p, SpZijScenes.RAKET, anker, Rotation.NONE, Boomkoepel::klaar) && !Cutscenes.bezig(p)) {
            klaar(p);
        }
    }

    static void klaar(ServerPlayer p) {
        if (!ln().verder(p, SpZijLijnen.S_RAKET)) {
            return;
        }
        if (SpZij.eenmalig(ln(), p, "luchthelm", new ItemStack(ModItems.clothingItem(GuhClothes.SPONSGUH_LUCHTHELM)))) {
            SpZij.titel(p, T + "klaar.titel", T + "klaar.onder");
            SpZij.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }
    }

    // =====================================================================================================================
    // clicks, residents
    // =====================================================================================================================

    /** {@code SponsguhWezen.BIJ_KLIK}: Sandy-guh (the resident of the dome or a player's own copy). */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        if (w.cast() != Cast.SANDY_GUH || !isOnze(w, p, "sandy")) {
            return false;
        }
        w.getLookControl().setLookAt(p);
        if (!SpZij.open(p)) {
            w.zetAnimatie("praat");
            GuhQuests.say(p, w, T + "later");
            return true;
        }
        int stap = ln().stap(p);
        ServerLevel level = p.level();
        switch (stap) {
            case SpZijLijnen.S_BEZOEK -> bezoek(p, level);
            case SpZijLijnen.S_KARATE -> {
                w.zetAnimatie("karate");
                GuhQuests.say(p, w, T + "klik.karate", ln().teller(p, "karate"), Karate.NODIG);
            }
            case SpZijLijnen.S_ONDERDELEN -> {
                w.zetAnimatie("praat");
                GuhQuests.say(p, w, T + "klik.onderdelen", gevonden(p), ONDERDELEN.size());
            }
            case SpZijLijnen.S_RAKET -> {
                w.zetAnimatie("juich");
                GuhQuests.say(p, w, T + "klik.raket");
            }
            default -> {
                w.zetAnimatie(p.getRandom().nextBoolean() ? "lach" : "karate");
                GuhQuests.say(p, w, T + "na." + (1 + Math.floorMod(p.level().getGameTime() / 40, 3)));
            }
        }
        return true;
    }

    /**
     * Is this cast member "ours" for this player: the resident of {@code bikinibodem/<plek>} or a copy this slice made for
     * them? (Another slice's copy of the same character is not ours.)
     */
    static boolean isOnze(Entity e, ServerPlayer p, String plek) {
        if (e.entityTags().contains(SpZij.EIGEN)) {
            UUID eigenaar = Zicht.eigenaar(e);
            return eigenaar == null || eigenaar.equals(p.getUUID());
        }
        return e.getPersistentData().getStringOr(Bewoners.TAG, "").startsWith("einde2_" + SpZij.BB + "_" + plek + "@");
    }

    /** {@code Bewoners.BIJ_MAKEN}: the resident Sandy-guh is there for players past the karate (before it they have their own). */
    static void markeer(Entity e, String plek) {
        if ((SpZij.BB + "/sandy").equals(plek)) {
            Zicht.alleenBij(e, SpZijLijnen.SANDY, SpZijLijnen.S_ONDERDELEN, 99);
        }
    }

    /** Residents made before this slice existed get their mark too (now and then, when a player is near). */
    private static void markeerOudeBewoners(ServerLevel level, ServerPlayer p) {
        String voor = "einde2_" + SpZij.BB + "_sandy@";
        for (Entity e : level.getEntities((Entity) null, p.getBoundingBox().inflate(64),
                e -> !e.entityTags().contains(Zicht.MERK) && e.getPersistentData().getStringOr(Bewoners.TAG, "").startsWith(voor))) {
            markeer(e, SpZij.BB + "/sandy");
        }
    }

    // =====================================================================================================================
    // small things
    // =====================================================================================================================

    static String naam() {
        return "entity.guhs.sandy_guh";
    }

    /** Inside a zone marker of the town (its box, 2 blocks of air above it counted in)? */
    static boolean inZone(ServerLevel level, String zone, Vec3 pos) {
        Plekken.Plek z = Plekken.plek(SpZij.BB, zone);
        BlockPos o = SpZij.oorsprong(level);
        if (z == null || z.tot() == null || o == null) {
            return false;
        }
        AABB doos = new AABB(Vec3.atLowerCornerOf(o.offset(z.pos())), Vec3.atLowerCornerOf(o.offset(z.tot())).add(1, 3, 1));
        return doos.contains(pos);
    }

    /** The middle of a zone marker at its floor (null: unknown). */
    @Nullable
    static Vec3 zoneMidden(ServerLevel level, String zone) {
        Plekken.Plek z = Plekken.plek(SpZij.BB, zone);
        BlockPos o = SpZij.oorsprong(level);
        if (z == null || z.tot() == null || o == null) {
            return null;
        }
        BlockPos a = o.offset(z.pos()), b = o.offset(z.tot());
        return new Vec3((a.getX() + b.getX() + 1) / 2.0, a.getY(), (a.getZ() + b.getZ() + 1) / 2.0);
    }

    /** Where Sandy-guh stands for the karate: on the north edge of the mat. */
    @Nullable
    static Vec3 matRand(ServerLevel level) {
        Plekken.Plek z = Plekken.plek(SpZij.BB, "karate_mat");
        BlockPos o = SpZij.oorsprong(level);
        if (z == null || z.tot() == null || o == null) {
            return null;
        }
        BlockPos a = o.offset(z.pos()), b = o.offset(z.tot());
        return new Vec3((a.getX() + b.getX() + 1) / 2.0, a.getY(), a.getZ() + 0.6);
    }

    static void kijkNaar(Entity e, Vec3 doel) {
        Vec3 d = doel.subtract(e.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        e.setYRot(yaw);
        e.setYHeadRot(yaw);
        if (e instanceof SponsguhWezen w) {
            w.setYBodyRot(yaw);
        }
    }

    /** The joke of the bubble helmet: inside the dome a few bubbles pop round your head now and then. */
    private static void bellen(ServerPlayer p, ServerLevel level, long nu) {
        Long laatst = BELLEN.get(p.getUUID());
        if (laatst != null && nu - laatst < 40) {
            return;
        }
        BELLEN.put(p.getUUID(), nu);
        level.sendParticles(p, ParticleTypes.BUBBLE_POP, true, false, p.getX(), p.getEyeY() + 0.2, p.getZ(), 4, 0.3, 0.25, 0.3, 0.0);
        if (laatst == null) {
            SpZij.balk(p, T + "helm_op");
            SpZij.geluid(p, E2SponsguhFeature.BELLEN.get(), 0.6f, 1.4f);
        }
    }

    static void vergeet(UUID id) {
        KARATE.remove(id);
        BELLEN.remove(id);
    }

    /** (tests) the karate game of this player now (null: none runs). */
    @Nullable
    static Karate karate(UUID id) {
        return KARATE.get(id);
    }
}
