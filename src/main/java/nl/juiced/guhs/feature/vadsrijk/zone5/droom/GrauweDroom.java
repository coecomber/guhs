package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5Baas;

/**
 * <b>De Grauwe Droom van Moeder Wolkwal</b> (T-DROOM; the part-2 deelbaas, lv 60, Water, ontvadst): the bad dream the spy gave the
 * whale by singing her lullaby backwards. A big grey whale of mist that swims slow rounds over the dream cloud and cries softly. A
 * VICTIM: you cheer it up, it turns pink and the dream becomes a sweet one (never killed, no gore).
 * <ul>
 *   <li><b>Grauwe golf</b> (red strip through someone, 1.5 s): 25 % + Plakkerig. <b>Slaapwolk</b> (red circle under everyone, 1.4 s):
 *       22 %. <b>Staartzwiep</b>: a light hit on whoever swims too close.</li>
 *   <li><b>Grauwe ranken</b> (every ~25 s): 3 grey tendrils of the Keizer's grip grow out of the cloud (4 in phase 3). Each grips the
 *       nearest fighter (Plakkerig + 4 % every 2 s) and makes the dream 15 % tougher. Hit them to cut them.</li>
 *   <li><b>Fase 2 (65 %), De droom draait om:</b> the cloud turns night-grey (inverted); for 20 s only the four drifting PINK dream
 *       clouds are safe, the grey mist takes 7 % every 2 s elsewhere. Comes back every ~60 s.</li>
 *   <li><b>Het achterstevoren slaapliedje</b> (from fase 2, every ~45 s; the signature): the whale hums the lullaby backwards; the
 *       four note stones on the rim light up in the RIGHT order (each its own colour and pitch). Then right-click them in that order
 *       (14-16 s, anyone may click; a wrong note just starts over). Right = "getroost": stunned 8 s and +50 % damage for 10 s. Too
 *       late = a nightmare: 30 % on everyone and it heals 4 %. 4 notes, 5 in fase 3.</li>
 *   <li><b>Fase 3 (30 %), Samen neuriën:</b> the dream holds at 30 % until the hum meter is full: stand in the YELLOW circle in the
 *       middle together (every fighter inside fills it; solo ~15 s, four players ~4 s) while dodging. Full = Moeder Wolkwal hums
 *       along: the dream turns lilac and takes +25 % damage for the rest of the fight.</li>
 *   <li>Enrage 7 min (kern default). The end: pink, hearts, the lullaby on a flute, it drifts up and fades.</li>
 * </ul>
 * A loose boss ({@link #losseStraal}), spawned and placed by {@link Z5Droom}; not saved (a restart ends the dream).
 */
public class GrauweDroom extends Z5Baas {
    private static final EntityDataAccessor<Integer> KLEUR = SynchedEntityData.defineId(GrauweDroom.class, EntityDataSerializers.INT);
    public static final int GRAUW = 0, LILA = 1, ROZE_DROOM = 2;
    public static final int NOTEN = 4, NOOT_AFSTAND = 13, ZWEM_STRAAL = 9, NEURIE_NODIG = 300, NEURIE_STRAAL = 4, OMKEER_DUUR = 400;
    /** The four notes: colour + pitch (the lullaby goes up and down between these). */
    public static final int[] NOOT_KLEUR = {0xFF8AD0, 0xFFD62A, 0x8AD0FF, 0xC090FF};
    public static final float[] NOOT_TOON = {0.7f, 0.84f, 1.0f, 1.19f};
    /** The real lullaby (played on a flute at the end): indices into the notes. */
    static final int[] MELODIE = {2, 1, 0, 1, 2, 2, 2, 1, 1, 1, 2, 3, 3, 2, 1, 0, 1, 2, 2, 1, 1, 2, 1, 0};

    private final List<UUID> noten = new ArrayList<>(), ranken = new ArrayList<>();
    private double hoek;
    private int golfCd = 120, wolkCd = 220, rankCd = 300, wiegCd = 140, omkeerCd = 900;
    // the backwards lullaby
    @Nullable
    private int[] reeks;
    private int reeksStap, toonT, invoerT = -1;
    // the inverted dream
    private int omkeerT;
    private boolean nacht;
    // humming together
    private int neurie;
    private boolean neurieKlaar;
    private int getroostT, melodieT;
    /** Gametests / dev. */
    public int wiegeliedGoed, wiegeliedMis, rankenGeknipt, omkeringen, nachtmerries;

    public GrauweDroom(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KLEUR, GRAUW);
    }

    /** 0 grey, 1 lilac (after the humming), 2 pink (cheered up): the client picks the texture. */
    public int kleur() {
        return entityData.get(KLEUR);
    }

    // ---- identity ----------------------------------------------------------------------------------------------------------------

    @Override
    public String baasId() {
        return "grauwe_droom";
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z5droom.baas.grauwe_droom";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"zing", "zwiep", "schrik", "blij"};
    }

    @Override
    protected double hp(int level) {
        return 17000 + 560 * level;
    }

    @Override
    protected double klap(int level) {
        return 24 + 2.6 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.65, 0.30};
    }

    @Override
    public int losseStraal() {
        return Z5Droom.STRAAL + 9;
    }

    /** The music hook (t-geluid): its own id; the client only plays it once a sound with that id exists. */
    @Override
    public Identifier muziek() {
        // T-INT: no own track rendered yet: t-geluid's grand Vahoegnir boss theme (the finale's dragon-sized feel). An own dreamy
        // lullaby-boss loop (guhs:vr.z5droom.muziek.grauwe_droom) can replace this once the sound exists.
        // U-AUDIO2: now its own dreamy lullaby-boss theme (+ a darker last-phase layer and a music-box fanfare)
        return Guhs.id(nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.baasId("grauwe_droom"));
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected double jaagSnelheid() {
        return 0;
    }

    /** It never walks to anyone (it swims its own rounds): the engine's chase never starts. */
    @Override
    protected double bereik() {
        return 64;
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (d.distanceTo(this) < getBbWidth() / 2 + 2.5) {
            speel("zwiep");
            raak(d, 0.8, "staartzwiep");
        }
    }

    // ---- damage rules ------------------------------------------------------------------------------------------------------------

    public int rankenLevend() {
        if (!(level() instanceof ServerLevel sl)) {
            return 0;
        }
        int n = 0;
        for (UUID u : ranken) {
            if (sl.getEntity(u) instanceof Z3Doel d && !d.isRemoved() && d.raakbaar()) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        double f = Math.max(0.55, 1 - 0.15 * rankenLevend());
        if (getroostT > 0) {
            f *= 1.5;
        }
        if (neurieKlaar) {
            f *= 1.25;
        }
        return f;
    }

    @Override
    protected double vadsVloer() {
        return fase() >= 3 && !neurieKlaar ? 0.30 : 0;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z5droom.neurie_eerst";
    }

    public boolean getroost() {
        return getroostT > 0;
    }

    public boolean neurieKlaar() {
        return neurieKlaar;
    }

    public int neurie() {
        return neurie;
    }

    public boolean nacht() {
        return nacht;
    }

    @Nullable
    public int[] reeks() {
        return reeks;
    }

    public boolean wachtOpNoten() {
        return reeks != null && invoerT > 0;
    }

    // ---- phases --------------------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z5droom.fase" + fase, fase == 3 ? GEEL : PAARS);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            praat("praat.fase2");
            omkeer(sl);
            wiegCd = 160;
        }
        if (fase == 3) {
            praat("praat.fase3");
            neurie = 0;
        }
    }

    /** A chat line by the dream (key under vadsrijk.z5droom.). */
    @Override
    public void praat(String key, Object... args) {
        spreek("vadsrijk.z5droom." + key, args);
    }

    // ---- the note stones -------------------------------------------------------------------------------------------------------------

    public Vec3 nootPlek(int i) {
        return ring(zMidden(), NOOT_AFSTAND, Math.PI / 4 + i * Math.PI / 2);
    }

    @Nullable
    public Z3Doel noot(int i) {
        return level() instanceof ServerLevel sl && i < noten.size() && sl.getEntity(noten.get(i)) instanceof Z3Doel d && !d.isRemoved() ? d : null;
    }

    private void notenKlaar(ServerLevel sl) {
        if (noten.size() == NOTEN && noot(0) != null && noot(NOTEN - 1) != null) {
            return;
        }
        for (UUID u : noten) {
            if (sl.getEntity(u) != null) {
                sl.getEntity(u).discard();
            }
        }
        noten.clear();
        for (int i = 0; i < NOTEN; i++) {
            Z3Doel d = Z5DroomRegistry.WIEGNOOT.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (d == null) {
                return;
            }
            Vec3 v = nootPlek(i);
            d.koppel(this, i, 100, v);
            d.zetRaakbaar(false);
            d.zetStand(i);
            d.kijk((float) Math.toDegrees(Math.atan2(-(zMidden().x - v.x), zMidden().z - v.z)));
            sl.addFreshEntity(d);
            noten.add(d.getUUID());
        }
    }

    private void nootKlinkt(ServerLevel sl, int i, boolean groot) {
        Z3Doel d = noot(i);
        Vec3 v = d != null ? d.position() : nootPlek(i);
        if (d != null) {
            d.speel("klop");
        }
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, groot ? 2.5f : 1.6f, NOOT_TOON[i]);
        sl.sendParticles(ParticleTypes.NOTE, v.x, v.y + 2.2, v.z, groot ? 6 : 3, 0.4, 0.3, 0.4, 1);
        sl.sendParticles(new DustParticleOptions(NOOT_KLEUR[i], 2.0f), v.x, v.y + 1, v.z, groot ? 40 : 16, 0.5, 0.9, 0.5, 0);
        if (groot) {
            Telegraaf.cirkel(sl, voetVan(v), 1.6, 14, NOOT_KLEUR[i]);
        }
    }

    /** Het achterstevoren slaapliedje (dev/tests too): the right order is shown on the stones, then the players click it. */
    public void wiegelied(ServerLevel sl) {
        notenKlaar(sl);
        int n = fase() >= 3 ? NOTEN + 1 : NOTEN;
        reeks = new int[n];
        int vorige = -1;
        for (int i = 0; i < n; i++) {
            int x;
            do {
                x = getRandom().nextInt(NOTEN);
            } while (x == vorige);
            reeks[i] = x;
            vorige = x;
        }
        reeksStap = 0;
        toonT = 0;
        invoerT = -1;
        speel("zing");
        praat("praat.achterstevoren");
        zeg("vadsrijk.z5droom.wiegelied.luister", ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** The show (one note every 16 ticks), then the input window. Runs every tick, also while it is stunned. */
    private void wiegeliedTik(ServerLevel sl) {
        if (reeks == null) {
            return;
        }
        if (invoerT < 0) {
            if (toonT % 16 == 0 && toonT / 16 < reeks.length) {
                nootKlinkt(sl, reeks[toonT / 16], true);
            }
            toonT++;
            if (toonT >= reeks.length * 16 + 8) {
                invoerT = 20 * (6 + 2 * reeks.length);
                zeg("vadsrijk.z5droom.wiegelied.nu", ChatFormatting.LIGHT_PURPLE, reeks.length);
            }
            return;
        }
        if (--invoerT <= 0) {
            nachtmerrie(sl);
        } else if (invoerT % 20 == 0 && invoerT <= 100) {
            zeg("vadsrijk.z5droom.wiegelied.tijd", ChatFormatting.GOLD, invoerT / 20, reeksStap, reeks.length);
        }
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || d.getType() != Z5DroomRegistry.WIEGNOOT.get() || d.nummer < 0 || d.nummer >= NOTEN) {
            return;
        }
        int i = d.nummer;
        if (!wachtOpNoten()) {
            nootKlinkt(sl, i, false);   // (just a note: harmless fun)
            return;
        }
        if (reeks[reeksStap] == i) {
            nootKlinkt(sl, i, true);
            reeksStap++;
            if (reeksStap >= reeks.length) {
                getroost(sl);
            } else {
                zeg("vadsrijk.z5droom.wiegelied.goed", ChatFormatting.LIGHT_PURPLE, reeksStap, reeks.length);
            }
        } else {
            reeksStap = 0;
            sl.playSound(null, d.blockPosition(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
            sl.sendParticles(ParticleTypes.SMOKE, d.getX(), d.getY() + 1.6, d.getZ(), 10, 0.3, 0.3, 0.3, 0.01);
            zeg("vadsrijk.z5droom.wiegelied.vals", ChatFormatting.GOLD);
        }
    }

    /** The lullaby sounds right again: stunned 8 s, +50 % damage for 10 s. */
    private void getroost(ServerLevel sl) {
        reeks = null;
        invoerT = -1;
        wiegeliedGoed++;
        getroostT = 200;
        Statussen.geef(this, Status.VERDOOFD, 160, 1, null);
        speel("schrik");
        kaart("vadsrijk.z5droom.getroost", ROZE);
        sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.5, getZ(), 12, 1.5, 0.6, 1.5, 0);
        sl.sendParticles(new DustParticleOptions(ROZE, 2.2f), getX(), getY() + 1.5, getZ(), 80, 2, 1, 2, 0);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, 1.2f);
    }

    /** Too late: the backwards song wins for now. 30 % on everyone, it heals 4 %. */
    private void nachtmerrie(ServerLevel sl) {
        reeks = null;
        invoerT = -1;
        wiegeliedMis++;
        nachtmerries++;
        kaart("vadsrijk.z5droom.nachtmerrie", PAARS);
        for (ServerPlayer p : strijders()) {
            mech(p, 0.30, "nachtmerrie");
            sl.sendParticles(new DustParticleOptions(0x6A6A78, 2.4f), p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.8, 0.6, 0);
        }
        heal(maxVads() * 0.04);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1f, 1.6f);
    }

    // ---- the grey tendrils ---------------------------------------------------------------------------------------------------------

    /** Grauwe ranken (dev/tests too): n tendrils on random spots of the cloud. */
    public void ranken(ServerLevel sl, int n) {
        ranken.removeIf(u -> !(sl.getEntity(u) instanceof Z3Doel d) || d.isRemoved());
        for (int i = 0; i < n; i++) {
            Z3Doel d = Z5DroomRegistry.GRAUWE_RANK.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (d == null) {
                return;
            }
            Vec3 v = voetVan(ring(zMidden(), 4 + getRandom().nextDouble() * (NOOT_AFSTAND - 6), getRandom().nextDouble() * Math.PI * 2));
            d.koppel(this, 100 + i, Math.max(300, maxVads() * 0.015), v);
            d.zetRaakbaar(true);
            sl.addFreshEntity(d);
            ranken.add(d.getUUID());
            sl.sendParticles(new DustParticleOptions(0x707080, 2.0f), v.x, v.y + 0.5, v.z, 30, 0.4, 0.6, 0.4, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.SCULK_VEIN_PLACE, SoundSource.HOSTILE, 2f, 0.6f);
        zeg("vadsrijk.z5droom.ranken", ChatFormatting.GRAY);
    }

    private void rankenTik(ServerLevel sl, List<ServerPlayer> l) {
        if (tickCount % 40 != 0 || l.isEmpty()) {
            return;
        }
        for (UUID u : ranken) {
            if (!(sl.getEntity(u) instanceof Z3Doel d) || d.isRemoved() || !d.raakbaar()) {
                continue;
            }
            ServerPlayer best = null;
            double bd = 7 * 7;
            for (ServerPlayer p : l) {
                double x = p.distanceToSqr(d);
                if (x < bd) {
                    bd = x;
                    best = p;
                }
            }
            if (best == null) {
                continue;
            }
            d.speel("grijp");
            mech(best, 0.04, "grauwe_rank");
            Statussen.geef(best, Status.PLAKKERIG, 40, 0.4, this);
            Vec3 a = d.position().add(0, 1.6, 0), b = best.position().add(0, 1, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 q = a.lerp(b, i / 8.0);
                sl.sendParticles(new DustParticleOptions(0x6A6A78, 1.3f), q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl) || d.getType() != Z5DroomRegistry.GRAUWE_RANK.get()) {
            return;
        }
        rankenGeknipt++;
        sl.sendParticles(new DustParticleOptions(ROZE, 1.8f), d.getX(), d.getY() + 1.2, d.getZ(), 40, 0.5, 0.9, 0.5, 0);
        sl.sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1.2, d.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        sl.playSound(null, d.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.HOSTILE, 1.5f, 1.2f);
        if (bron instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z5droom.geknipt").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        d.discard();
    }

    // ---- the inverted dream ------------------------------------------------------------------------------------------------------------

    /** De droom draait om (dev/tests too): the cloud goes night-grey, only the drifting pink clouds are safe. */
    public void omkeer(ServerLevel sl) {
        omkeerT = OMKEER_DUUR;
        omkeerCd = 1200;
        omkeringen++;
        if (!nacht) {
            nacht = true;
            Z5Droom.keerOm(sl, BlockPos.containing(zMidden()), true);
        }
        zeg("vadsrijk.z5droom.omkeer", ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2f, 0.6f);
    }

    /** The four pink dream clouds right now (they drift round). */
    public List<Vec3> wolkjes() {
        List<Vec3> l = new ArrayList<>();
        double a = (OMKEER_DUUR - omkeerT) * 0.008;
        for (int i = 0; i < 4; i++) {
            l.add(voetVan(ring(zMidden(), 8, a + i * Math.PI / 2)));
        }
        return l;
    }

    public boolean opWolkje(ServerPlayer p) {
        for (Vec3 w : wolkjes()) {
            if (p.position().distanceToSqr(w.x, p.getY(), w.z) <= 3.2 * 3.2) {
                return true;
            }
        }
        return false;
    }

    private void omkeerTik(ServerLevel sl, List<ServerPlayer> l) {
        if (omkeerT <= 0) {
            if (nacht) {
                nacht = false;
                Z5Droom.keerOm(sl, BlockPos.containing(zMidden()), false);
                zeg("vadsrijk.z5droom.omkeer_weg", ChatFormatting.LIGHT_PURPLE);
            }
            return;
        }
        omkeerT--;
        if (omkeerT % 10 == 0) {
            for (Vec3 w : wolkjes()) {
                Telegraaf.cirkel(sl, w, 3, 12, ROZE);
                sl.sendParticles(ParticleTypes.CLOUD, w.x, w.y + 0.4, w.z, 6, 1.4, 0.1, 1.4, 0.01);
                sl.sendParticles(new DustParticleOptions(0xFFB0DA, 1.6f), w.x, w.y + 0.3, w.z, 8, 1.4, 0.1, 1.4, 0);
            }
            Vec3 m = zMidden();
            sl.sendParticles(new DustParticleOptions(0x5A5A66, 2.0f), m.x, m.y + 0.3, m.z, 40, Z5Droom.STRAAL * 0.5, 0.2, Z5Droom.STRAAL * 0.5, 0);
        }
        if (omkeerT % 40 == 0) {
            for (ServerPlayer p : l) {
                if (!opWolkje(p)) {
                    mech(p, 0.07, "grauwe_mist");
                    sl.sendParticles(new DustParticleOptions(0x5A5A66, 1.6f), p.getX(), p.getY() + 0.8, p.getZ(), 12, 0.4, 0.5, 0.4, 0);
                }
            }
        }
    }

    // ---- humming together ------------------------------------------------------------------------------------------------------------

    private void neurieTik(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() < 3 || neurieKlaar) {
            return;
        }
        Vec3 m = zMidden();
        if (tickCount % 10 == 0) {
            Telegraaf.cirkel(sl, m, NEURIE_STRAAL, 12, GEEL);
        }
        int in = Math.min(4, inCirkel(m, NEURIE_STRAAL).size());
        if (in > 0) {
            neurie += in;
            if (tickCount % 6 == 0) {
                for (ServerPlayer p : inCirkel(m, NEURIE_STRAAL)) {
                    sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 1, 0.2, 0.1, 0.2, getRandom().nextDouble());
                }
                sl.playSound(null, m.x, m.y, m.z, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.5f, NOOT_TOON[(tickCount / 6) % NOTEN]);
            }
        }
        if (tickCount % 20 == 0 && !l.isEmpty()) {
            zeg("vadsrijk.z5droom.neurie.meter", ChatFormatting.YELLOW, Math.min(100, neurie * 100 / NEURIE_NODIG));
        }
        if (neurie >= NEURIE_NODIG) {
            neurieKlaar(sl);
        }
    }

    /** Moeder Wolkwal hums along (dev/tests too): lilac, no floor any more, +25 % damage. */
    public void neurieKlaar(ServerLevel sl) {
        neurieKlaar = true;
        neurie = NEURIE_NODIG;
        entityData.set(KLEUR, LILA);
        kaart("vadsrijk.z5droom.neurie.klaar", ROZE);
        praat("praat.neurie");
        speel("schrik");
        sl.sendParticles(new DustParticleOptions(0xD8A8FF, 2.4f), getX(), getY() + 1.5, getZ(), 120, 3, 1.2, 3, 0);
        sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 3, getZ(), 16, 2, 0.6, 2, 1);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.8f);
    }

    // ---- the fight ------------------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() >= 2 && wiegCd <= 0 && reeks == null) {
            wiegelied(sl);
            wiegCd = fase() >= 3 ? 700 : 900;
            rustT = 60;
            laatsteAanval = "wiegelied";
            return;
        }
        if (fase() >= 2 && omkeerCd <= 0 && omkeerT <= 0) {
            omkeer(sl);
            rustT = 40;
            laatsteAanval = "omkeer";
            return;
        }
        if (rankCd <= 0 && rankenLevend() < 3) {
            ranken(sl, adds(fase() >= 3 ? 4 : 3));
            rankCd = 500;
            rustT = 40;
            laatsteAanval = "ranken";
            return;
        }
        if (golfCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 c = voetVan(v), r = c.subtract(position()).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            Vec3 a = voetVan(c.subtract(r.scale(16))), b = voetVan(c.add(r.scale(16)));
            Telegraaf.lijn(sl, a, b, 4, tg(30), ROOD);
            speel("zwiep");
            later(tg(30), () -> {
                for (int i = 0; i <= 16; i++) {
                    Vec3 q = a.lerp(b, i / 16.0);
                    sl.sendParticles(new DustParticleOptions(0x7A7A88, 2.2f), q.x, q.y + 0.8, q.z, 6, 1.2, 0.6, 1.2, 0);
                }
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
                for (ServerPlayer p : strijders()) {
                    if (inStrook(p.position(), a, b, 2) && Math.abs(p.getY() - c.y) < 4) {
                        mech(p, 0.25, "grauwe_golf");
                        Statussen.geef(p, Status.PLAKKERIG, 60, 0.4, this);
                    }
                }
            });
            golfCd = fase() >= 2 ? 150 : 180;
            rustT = 35;
            laatsteAanval = "golf";
            return;
        }
        if (wolkCd <= 0) {
            speel("zing");
            for (ServerPlayer p : l) {
                inslag(voetVan(p), 2.5, 28, 0.22, "slaapwolk", null, ParticleTypes.CLOUD);
            }
            wolkCd = 220;
            rustT = 30;
            laatsteAanval = "slaapwolk";
            return;
        }
        rustT = 10;
    }

    /** Swims slow rounds over the cloud (frozen while stunned: it stops and listens). */
    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        notenKlaar(sl);
        golfCd--;
        wolkCd--;
        rankCd--;
        if (fase() >= 2) {
            wiegCd--;
            omkeerCd--;
        }
        hoek += fase() >= 3 ? 0.014 : 0.010;
        Vec3 m = zMidden();
        Vec3 doel = new Vec3(m.x + Math.cos(hoek) * ZWEM_STRAAL, m.y + 1.0 + Math.sin(tickCount * 0.05) * 0.4, m.z + Math.sin(hoek) * ZWEM_STRAAL);
        Vec3 v = position().lerp(doel, 0.12);
        setPos(v.x, v.y, v.z);
        setDeltaMovement(Vec3.ZERO);
        float yaw = (float) Math.toDegrees(Math.atan2(Math.sin(hoek), Math.cos(hoek)));   // facing along the circle
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        if (tickCount % 4 == 0) {
            sl.sendParticles(new DustParticleOptions(kleur() == LILA ? 0xC8A0F0 : 0x8A8A98, 1.6f), getX(), getY() + 0.6, getZ(), 3, getBbWidth() / 3, 0.4,
                    getBbWidth() / 3, 0);
        }
        if (tickCount % 80 == 0 && kleur() == GRAUW) {
            sl.playSound(null, blockPosition(), SoundEvents.ALLAY_HURT, SoundSource.HOSTILE, 1.2f, 0.4f);   // it cries softly
        }
    }

    /** The mechanics that run every tick, also while it is stunned (the input window, the inverted dream, humming, tendrils). */
    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (geplofd()) {
            plofTik(sl);
            return;
        }
        if (inIntro()) {
            return;
        }
        if (getroostT > 0) {
            getroostT--;
        }
        notenKlaar(sl);
        List<ServerPlayer> l = strijders();
        wiegeliedTik(sl);
        omkeerTik(sl, l);
        neurieTik(sl, l);
        rankenTik(sl, l);
    }

    // ---- the end: a sweet dream ---------------------------------------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            entityData.set(KLEUR, ROZE_DROOM);
            speel("blij");
            sl.sendParticles(new DustParticleOptions(ROZE, 2.6f), getX(), getY() + 1.5, getZ(), 160, 2.5, 1.4, 2.5, 0.02);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 3, getZ(), 14, 2, 0.6, 2, 0);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.5f, 1.0f);
            if (nacht) {
                nacht = false;
                Z5Droom.keerOm(sl, BlockPos.containing(zMidden()), false);
            }
        }
    }

    @Override
    protected int plofDuur() {
        return 20 * 12;
    }

    /** After the plof: the real lullaby on a flute, pink rain, it drifts up. */
    private void plofTik(ServerLevel sl) {
        setDeltaMovement(0, 0.02, 0);
        if (melodieT % 7 == 0 && melodieT / 7 < MELODIE.length) {
            int i = MELODIE[melodieT / 7];
            sl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 2.5f, NOOT_TOON[i]);
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + 3, getZ(), 1, 1.5, 0.4, 1.5, i / 4.0);
        }
        melodieT++;
        if (tickCount % 3 == 0) {
            sl.sendParticles(new DustParticleOptions(ROZE, 1.2f), getX(), getY() + 0.3, getZ(), 6, getBbWidth() / 3, 0.1, getBbWidth() / 3, 0);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd()) {
            praat("praat.blij");
            if (level() instanceof ServerLevel sl) {
                for (UUID u : ranken) {
                    if (sl.getEntity(u) != null) {
                        sl.getEntity(u).discard();
                    }
                }
            }
        }
    }

    /** Per helper (Z5Baas): the story counts, the replay flag, the wake-up timer. */
    @Override
    protected void naVerslagen(ServerPlayer p) {
        Z5Droom.opgevrolijkt(p);
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return DroomLoot.kist(p, mobLevel(), getRandom());
    }

    @Override
    protected void kistGeplaatst(ServerPlayer p, BlockPos pos) {
        super.kistGeplaatst(p, pos);
        Z5Droom.kist(p, pos);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : noten) {
                if (sl.getEntity(u) != null) {
                    sl.getEntity(u).discard();
                }
            }
            for (UUID u : ranken) {
                if (sl.getEntity(u) != null) {
                    sl.getEntity(u).discard();
                }
            }
            if (nacht) {
                nacht = false;
                Z5Droom.keerOm(sl, BlockPos.containing(zMidden()), false);
            }
        }
        super.remove(reason);
    }
}
