package nl.juiced.guhs.feature.dirigent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.feature.zingendeweide.ZingendeWeideFeature;

/**
 * 1.8.0 (uitbreiding U3): the Dirigent in de Koorkom. With a {@link Dirigeerstokje} you stand at a Koorkom's podium and
 * conduct the Blubs through the lullaby's tune.
 * <ul>
 *   <li><b>Gathering</b>: up to five Blubs nearby (wild ones, and your own tame ones) hop to an arc in front of you (at dusk the
 *       choir is there already; in the daytime they come hopping). Each Blub sings one or more notes (do-re-mi-fa-sol), shown by
 *       a coloured dust over its head.</li>
 *   <li><b>Conducting</b>: {@value #MAAT} beats. The Blub whose note comes next lifts its ears and sparkles; on the beat you look
 *       at it and click with the stokje. Right on the beat: perfect (2 points), a little off: good (1), wrong Blub or way off:
 *       nothing (the Blub startles). A beat nobody gave goes plof.</li>
 *   <li><b>The end</b>: stars by points (max {@value #MAX}): ★ 8, ★★ 18, ★★★ 26. With a star the whole choir sings the
 *       lullaby ({@code blubguh.liedje}), the better, the louder and the more notes and hearts.</li>
 *   <li><b>Rewards</b>: droomzand per star every time; once per Minecraft day (with ★★ or more) the big prize: a piece of
 *       Blub clothing ({@code guhs:blubkleding_*}, when those exist) or else a maanbloemetje and extra droomzand; with ★★★ rarely
 *       the golden dirigeerstokje (a shiny one, conducts just the same). Each player's best score is kept.</li>
 * </ul>
 * While a concert runs at a podium, its own dusk song waits ({@link #bezig}).
 */
public final class Concert {
    /** The tune: notes 0-4 (do-sol), one per beat (the lullaby's soft up and down). */
    static final int[] WIJS = {0, 2, 4, 2, 0, 2, 4, 4, 3, 2, 1, 2, 0, 1, 2, 0};
    public static final int MAAT = WIJS.length, MAX = MAAT * 2;
    /** Ticks per beat, how long the Blubs may take to get there, the count-in, how far a click may be off the beat. */
    static final int TEL = 14, VERZAMEL_MAX = 8 * 20, AFTEL = 3, RAAK_GOED = 6, RAAK_PERFECT = 2;
    /** How far Blubs are gathered from, how far the stokje finds a podium, the arc's radius. */
    static final double VAN_VER = 40, EIGEN_VER = 16, BOOG = 4.0;
    /** Points for one, two and three stars. */
    static final int STER1 = 8, STER2 = 18, STER3 = 26;
    /** Semitones above do (a major scale) and each note's dust colour (roze, boter, aqua, lavendel, hemelsblauw). */
    static final int[] HALVE_TONEN = {0, 2, 4, 5, 7};
    static final int[] KLEUR = {0xF4A6C8, 0xFFE38A, 0x8FE3E8, 0xC7A6F0, 0x8FB8F4};
    public static final String RECORD = "guhs_dirigent_record", DAG = "guhs_dirigent_dag";
    /** The chance of the golden dirigeerstokje with ★★★ (only once: who has one, doesn't get another). */
    static final float GOUD_KANS = 0.1f;
    public static final String GOUD_OOIT = "guhs_dirigent_goud";
    /** The Blub clothing pieces the big prize may give (items guhs:blubkleding_<id>, from the blubkleding branch). */
    static final String[] BLUBKLEDING = {"slaapmutsje", "sjaaltje", "sterrenkroontje", "bubbelhelm", "pyjamaatje", "slaapmaskertje",
            "bloemenkransje", "brilletje"};
    static final String T = "gui.guhs.dirigent.";

    public enum Fase { VERZAMELEN, AFTELLEN, SPELEN, KLAAR }

    /** The running concerts, one per podium. */
    private static final Map<BlockPos, Concert> LOPEND = new HashMap<>();

    final ServerLevel level;
    final BlockPos podium;
    final UUID dirigent;
    final List<BlubGuhEntity> blubs;
    final Vec3[] plekken;
    Fase fase = Fase.VERZAMELEN;
    int tick;
    /** The tick (of this concert) the first beat falls on. */
    int start;
    final boolean[] beoordeeld = new boolean[MAAT];
    int punten, combo, besteCombo, perfect;
    int slot;

    private Concert(ServerLevel level, BlockPos podium, ServerPlayer dirigent, List<BlubGuhEntity> blubs, Vec3[] plekken) {
        this.level = level;
        this.podium = podium.immutable();
        this.dirigent = dirigent.getUUID();
        this.blubs = blubs;
        this.plekken = plekken;
    }

    // =====================================================================================================================
    // starting, finding
    // =====================================================================================================================

    /** Is a concert running at this podium? (Then its own dusk song waits.) */
    public static boolean bezig(ServerLevel level, BlockPos podium) {
        Concert c = LOPEND.get(podium);
        return c != null && c.level == level;
    }

    /** The concert this player conducts. */
    @Nullable
    public static Concert van(ServerPlayer p) {
        for (Concert c : LOPEND.values()) {
            if (c.dirigent.equals(p.getUUID()) && c.level == p.level()) {
                return c;
            }
        }
        return null;
    }

    /** The nearest Koorkom podium within reach of this player. */
    @Nullable
    static BlockPos podiumBij(ServerPlayer p) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos b : BlockPos.betweenClosed(p.blockPosition().offset(-6, -3, -6), p.blockPosition().offset(6, 3, 6))) {
            if (p.level().getBlockState(b).is(ZingendeWeideFeature.KOORKOM_PODIUM.get())) {
                double d = b.distSqr(p.blockPosition());
                if (d < bestD) {
                    bestD = d;
                    best = b.immutable();
                }
            }
        }
        return best;
    }

    /** Starts a concert at this podium (null with a message when it can't). */
    @Nullable
    public static Concert begin(ServerPlayer p, BlockPos podium) {
        ServerLevel level = p.level();
        if (bezig(level, podium)) {
            p.sendOverlayMessage(Component.translatable(T + "al_bezig").withStyle(ChatFormatting.GRAY));
            return null;
        }
        if (!BlubSnoes.heeft(p) && !BlubSnoes.inDroom(level)) {
            p.sendOverlayMessage(Component.translatable(T + "snoes_eerst").withStyle(ChatFormatting.LIGHT_PURPLE));
            return null;
        }
        List<BlubGuhEntity> blubs = kies(level, podium, p);
        if (blubs.isEmpty()) {
            p.sendOverlayMessage(Component.translatable(T + "geen_blubs").withStyle(ChatFormatting.GRAY));
            return null;
        }
        Concert c = new Concert(level, podium, p, blubs, boog(level, podium, p, blubs.size()));
        LOPEND.put(c.podium, c);
        for (BlubGuhEntity b : blubs) {
            b.setSlaapt(false);
            b.emotie(BlubGuhEntity.KRUL, 60);
        }
        p.sendSystemMessage(Component.translatable(T + "begin", blubs.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, podium, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        return c;
    }

    /** Up to five singers: wild Blubs near the podium first, then your own tame ones (not riding along on you). */
    static List<BlubGuhEntity> kies(ServerLevel level, BlockPos podium, ServerPlayer p) {
        List<BlubGuhEntity> uit = new ArrayList<>();
        Vec3 m = Vec3.atBottomCenterOf(podium);
        List<BlubGuhEntity> wild = level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(podium).inflate(VAN_VER),
                b -> b.isAlive() && !b.isTame() && !b.isPassenger() && !b.isNoAi() && !bezet(b) && b.entityTags().stream().noneMatch(t -> t.startsWith("guhs.")));
        wild.sort((a, b) -> Double.compare(a.distanceToSqr(m), b.distanceToSqr(m)));
        for (BlubGuhEntity b : wild) {
            if (uit.size() < 5) {
                uit.add(b);
            }
        }
        if (uit.size() < 5) {
            for (BlubGuhEntity b : level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(podium).inflate(EIGEN_VER),
                    b -> b.isAlive() && b.isTame() && b.isOwnedBy(p) && !b.isPassenger() && !b.isNoAi() && !bezet(b))) {
                if (uit.size() < 5) {
                    uit.add(b);
                }
            }
        }
        return uit;
    }

    private static boolean bezet(BlubGuhEntity b) {
        for (Concert c : LOPEND.values()) {
            if (c.blubs.contains(b)) {
                return true;
            }
        }
        return false;
    }

    /** Spots in an arc in front of the conductor (on the ground, around the podium's height). */
    static Vec3[] boog(ServerLevel level, BlockPos podium, ServerPlayer p, int n) {
        Vec3 m = Vec3.atBottomCenterOf(podium);
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1);
        if (kijk.lengthSqr() < 1e-4) {
            kijk = new Vec3(0, 0, 1);
        }
        double basis = Math.atan2(kijk.z, kijk.x);
        Vec3[] uit = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double hoek = basis + Math.toRadians(n == 1 ? 0 : -55 + 110.0 * i / (n - 1));
            double x = m.x + Math.cos(hoek) * BOOG, z = m.z + Math.sin(hoek) * BOOG;
            uit[i] = new Vec3(x, grond(level, BlockPos.containing(x, m.y, z)), z);
        }
        return uit;
    }

    private static double grond(ServerLevel level, BlockPos b) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos p = b.offset(0, dy, 0);
            if (level.getBlockState(p).isAir() && !level.getBlockState(p.below()).isAir()) {
                return p.getY();
            }
        }
        return b.getY();
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    static void tickAlles() {
        LOPEND.values().removeIf(c -> !c.tick());
    }

    /** One tick; false when the concert is over (or broken off). */
    boolean tick() {
        tick++;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(dirigent);
        blubs.removeIf(b -> !b.isAlive() || b.level() != level);
        if (p == null || p.level() != level || p.distanceToSqr(Vec3.atCenterOf(podium)) > 16 * 16 || blubs.isEmpty()) {
            if (p != null && fase != Fase.KLAAR) {
                p.sendOverlayMessage(Component.translatable(T + "gestopt").withStyle(ChatFormatting.GRAY));
            }
            return false;
        }
        switch (fase) {
            case VERZAMELEN -> verzamel(p);
            case AFTELLEN -> aftellen(p);
            case SPELEN -> speel(p);
            case KLAAR -> {
                if (--slot <= 0) {
                    return false;
                }
                if (slot % 10 == 0) {
                    zingDeeltjes(sterren(punten));
                }
            }
        }
        if (fase != Fase.KLAAR) {
            houdOpPlek(p);
        }
        return true;
    }

    /** The Blubs hop to their spots; when they stand there (or after a while), the count-in starts. */
    private void verzamel(ServerPlayer p) {
        boolean klaar = true;
        for (int i = 0; i < blubs.size(); i++) {
            if (blubs.get(i).distanceToSqr(plekken[i]) > 2.5) {
                klaar = false;
            }
        }
        if (tick % 20 == 0) {
            p.sendOverlayMessage(Component.translatable(T + "verzamelen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (klaar || tick >= VERZAMEL_MAX) {
            fase = Fase.AFTELLEN;
            start = tick + AFTEL * TEL;
        }
    }

    /** Three ticks of the stokje on the podium: tik, tik, tik... */
    private void aftellen(ServerPlayer p) {
        int rest = start - tick;
        if (rest > 0 && rest % TEL == 0) {
            level.playSound(null, podium, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.9f, 1.4f);
            p.sendOverlayMessage(Component.translatable(T + "aftellen", rest / TEL).withStyle(ChatFormatting.GOLD));
        }
        if (tick >= start - TEL) {
            fase = Fase.SPELEN;
        }
    }

    /** The beats: cue the next singer, and let beats nobody gave go plof. */
    private void speel(ServerPlayer p) {
        int na = tick - start;
        // the singer of the coming beat lifts its ears and sparkles from half a beat before
        int volgende = Math.floorDiv(na + TEL / 2, TEL);
        if (volgende >= 0 && volgende < MAAT && !beoordeeld[volgende]) {
            BlubGuhEntity b = zanger(volgende);
            if (tick % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, b.getX(), b.getY() + b.getBbHeight() + 0.5, b.getZ(), 1, 0.15, 0.05, 0.15, 0.01);
            }
            b.emotie(BlubGuhEntity.KRUL, 10);
            b.getLookControl().setLookAt(p, 30, 30);
        }
        if (na >= 0 && na % TEL == 0 && na / TEL < MAAT) {
            level.playSound(null, podium, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.25f, 1.8f);   // the soft beat
        }
        // a beat whose window has passed without a click: plof
        for (int i = 0; i < MAAT; i++) {
            if (!beoordeeld[i] && na > i * TEL + RAAK_GOED) {
                beoordeeld[i] = true;
                combo = 0;
                BlubGuhEntity b = zanger(i);
                level.playSound(null, b.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.NEUTRAL, 0.5f, 0.6f);
                level.sendParticles(ParticleTypes.SMOKE, b.getX(), b.getY() + b.getBbHeight(), b.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
                toon(p);
            }
        }
        if (na > (MAAT - 1) * TEL + RAAK_GOED) {
            einde(p);
        }
        if (tick % 10 == 0) {
            for (int i = 0; i < blubs.size(); i++) {           // each singer's colour: the notes it sings
                BlubGuhEntity b = blubs.get(i);
                level.sendParticles(new DustParticleOptions(KLEUR[noot(i)], 1.0f), b.getX(), b.getY() + b.getBbHeight() + 0.3, b.getZ(),
                        1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    /** The conductor clicks with the stokje: judged against the nearest beat and the Blub looked at. */
    public void tik(ServerPlayer p) {
        if (fase != Fase.SPELEN) {
            return;
        }
        int na = tick - start;
        int i = Math.round(na / (float) TEL);
        if (i < 0 || i >= MAAT || beoordeeld[i] || Math.abs(na - i * TEL) > RAAK_GOED) {
            // a click between the beats: a little "huh?"
            combo = 0;
            p.sendOverlayMessage(Component.translatable(T + "naast").withStyle(ChatFormatting.GRAY));
            return;
        }
        beoordeeld[i] = true;
        BlubGuhEntity gezien = gekeken(p);
        BlubGuhEntity moet = zanger(i);
        if (gezien != moet) {
            combo = 0;
            if (gezien != null) {
                gezien.emotie(BlubGuhEntity.PLAT, 30);
                BlubGeluid.SCHRIK.speel(gezien);
            }
            p.sendOverlayMessage(Component.translatable(T + "verkeerd").withStyle(ChatFormatting.GRAY));
            return;
        }
        int af = Math.abs(na - i * TEL);
        int pt = af <= RAAK_PERFECT ? 2 : 1;
        raak(i, pt);
        toon(p);
    }

    /** A beat given right: the note, its colour, the Blub's ears up. */
    void raak(int i, int pt) {
        punten += pt;
        combo++;
        besteCombo = Math.max(besteCombo, combo);
        if (pt == 2) {
            perfect++;
        }
        BlubGuhEntity b = zanger(i);
        float toon = 0.75f * (float) Math.pow(2, HALVE_TONEN[WIJS[i]] / 12.0);
        level.playSound(null, b.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.9f, toon);
        BlubGeluid.BLIJ.speel(b, 0.9f + WIJS[i] * 0.08f);
        level.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + b.getBbHeight() + 0.5, b.getZ(), 0, WIJS[i] / 24.0 * 4, 0, 0, 1);
        if (pt == 2) {
            level.sendParticles(ParticleTypes.END_ROD, b.getX(), b.getY() + b.getBbHeight() + 0.3, b.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        }
        b.emotie(BlubGuhEntity.RECHTOP, 20);
    }

    /** The overlay: where in the song, the combo and the stars so far. */
    private void toon(ServerPlayer p) {
        int gedaan = 0;
        for (boolean b : beoordeeld) {
            gedaan += b ? 1 : 0;
        }
        p.sendOverlayMessage(Component.translatable(T + "stand", gedaan, MAAT, combo, sterrenTekst(sterren(punten)))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** The end: stars, the whole choir's lullaby (when it went well), the rewards and the record. */
    private void einde(ServerPlayer p) {
        fase = Fase.KLAAR;
        int sterren = sterren(punten);
        slot = sterren > 0 ? 24 * 20 : 40;
        if (sterren > 0) {
            level.playSound(null, podium, BlubGeluid.LIEDJE.get(), SoundSource.NEUTRAL, 0.8f + 0.3f * sterren, 1.0f);
            for (BlubGuhEntity b : blubs) {
                b.squish(null);
            }
            GuhAdvancements.grant(p, "dirigent_concert");
            if (sterren == 3) {
                GuhAdvancements.grant(p, "dirigent_drie_sterren");
            }
        } else {
            for (BlubGuhEntity b : blubs) {
                b.emotie(BlubGuhEntity.PLAT, 60);
            }
        }
        p.sendSystemMessage(Component.translatable(T + "einde." + sterren, punten, MAX, perfect, besteCombo)
                .withStyle(sterren > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        int record = GuhQuests.saved(p).getIntOr(RECORD, 0);
        if (punten > record) {
            GuhQuests.saved(p).putInt(RECORD, punten);
            if (record > 0) {
                p.sendSystemMessage(Component.translatable(T + "record", punten, record).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        beloon(p, sterren, level.getRandom());
    }

    /** Droomzand per star every time; once a day (★★ or more) the big prize. */
    static void beloon(ServerPlayer p, int sterren, RandomSource r) {
        if (sterren <= 0) {
            return;
        }
        Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), sterren));
        long dag = Band.dag(p.level().getServer());
        if (sterren < 2 || GuhQuests.saved(p).getLongOr(DAG, Long.MIN_VALUE) == dag) {
            return;
        }
        GuhQuests.saved(p).putLong(DAG, dag);
        ItemStack prijs = blubKleding(r);
        if (prijs.isEmpty()) {
            Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), 3));
            prijs = new ItemStack(DroomblokkenFeature.MAANBLOEMETJE.get(), 2);
        }
        Minigames.give(p, prijs);
        p.sendSystemMessage(Component.translatable(T + "prijs", prijs.getHoverName()).withStyle(ChatFormatting.GOLD));
        if (sterren == 3 && !GuhQuests.saved(p).getBooleanOr(GOUD_OOIT, false) && r.nextFloat() < GOUD_KANS) {
            GuhQuests.saved(p).putBoolean(GOUD_OOIT, true);
            Minigames.give(p, new ItemStack(DirigentFeature.GOUDEN_STOKJE.get()));
            p.sendSystemMessage(Component.translatable(T + "goud").withStyle(ChatFormatting.GOLD));
        }
    }

    /** A random piece of Blub clothing, when those items exist (another branch); empty when none do. */
    static ItemStack blubKleding(RandomSource r) {
        List<Item> er = new ArrayList<>();
        for (String id : BLUBKLEDING) {
            BuiltInRegistries.ITEM.getOptional(Guhs.id("blubkleding_" + id)).ifPresent(er::add);
        }
        return er.isEmpty() ? ItemStack.EMPTY : new ItemStack(er.get(r.nextInt(er.size())));
    }

    /** The song after a good concert: notes and (with more stars) hearts over the singers. */
    private void zingDeeltjes(int sterren) {
        for (BlubGuhEntity b : blubs) {
            for (int k = 0; k < sterren; k++) {
                level.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + b.getBbHeight() + 0.4 + k * 0.2, b.getZ(), 0,
                        level.getRandom().nextInt(24) / 24.0, 0, 0, 1);
            }
            if (sterren == 3 && level.getRandom().nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.HEART, b.getX(), b.getY() + b.getBbHeight() + 0.2, b.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
    }

    /** Keep the singers at their spots, looking at the conductor. */
    private void houdOpPlek(ServerPlayer p) {
        for (int i = 0; i < blubs.size(); i++) {
            BlubGuhEntity b = blubs.get(i);
            Vec3 plek = plekken[i];
            if (b.distanceToSqr(plek) > 1.2) {
                if (tick % 10 == i) {
                    b.getNavigation().moveTo(plek.x, plek.y, plek.z, 1.1);
                }
            } else {
                b.getNavigation().stop();
                b.getLookControl().setLookAt(p, 20, 20);
            }
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** The note (0-4) a singer sings; with fewer than five singers some sing more than one note. */
    int noot(int blub) {
        return blub % 5;
    }

    /** The singer of beat i. */
    BlubGuhEntity zanger(int i) {
        return blubs.get(WIJS[i] % blubs.size());
    }

    /** The singer the conductor looks at (the one nearest to the line of sight, within a generous cone). */
    @Nullable
    BlubGuhEntity gekeken(ServerPlayer p) {
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        BlubGuhEntity best = null;
        double bestDot = 0.9;
        for (BlubGuhEntity b : blubs) {
            Vec3 naar = b.position().add(0, b.getBbHeight() / 2, 0).subtract(oog);
            double d = naar.length();
            if (d < 0.1 || d > 24) {
                continue;
            }
            double dot = naar.scale(1 / d).dot(kijk);
            if (dot > bestDot) {
                bestDot = dot;
                best = b;
            }
        }
        return best;
    }

    public static int sterren(int punten) {
        return punten >= STER3 ? 3 : punten >= STER2 ? 2 : punten >= STER1 ? 1 : 0;
    }

    static String sterrenTekst(int s) {
        return "★".repeat(s) + "☆".repeat(3 - s);
    }

    public Fase fase() {
        return fase;
    }

    public int punten() {
        return punten;
    }

    /** Tests: jump to the playing part straight away (the Blubs are already standing there). */
    void speelNu() {
        fase = Fase.SPELEN;
        start = tick + TEL;
    }

    /** Tests: the tick of beat i. */
    int beatTick(int i) {
        return start + i * TEL;
    }

    /** Tests: jumps the clock to this tick of the concert (no beats are judged in between). */
    void zetTick(int t) {
        tick = t;
    }

    /** Tests: ends now. */
    void eindeNu(ServerPlayer p) {
        einde(p);
    }

    static void wisAlles() {
        LOPEND.clear();
    }
}
