package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.boor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Hulpje;

/**
 * De Mika-boormeester, the secret boss of the Splinterkloof drill site (ronde 15), lv 28. A Mika in a drilling machine with
 * 3 drill heads ({@link Boorkop}: rood, geel, blauw).
 * <ul>
 *   <li>Asleep (dormant machine) until the npc slice's secret trigger calls {@link #ontgrendel(ServerPlayer)}; after that the player
 *       may restart it with a right-click ({@link #MAG_STARTEN}).</li>
 *   <li>His armour is thick (players do 25 % to the machine). Hits on a drill head heat it; at 100 % it overheats. In the RIGHT
 *       order (a new random order each cycle) every overheat costs him 10 % and after all 3: <b>kernsmelting</b> (stunned 8 s, double
 *       damage). In the WRONG order the head vents steam (a red ring, 45 % of your Vads, knocked away) and all heads cool down.</li>
 *   <li>The clue: every 15 s the control panel plays the order: the heads light up in their colour, one by one, each with its own
 *       tone (rood low, geel middle, blauw high).</li>
 *   <li>Attacks: Boorstoot (a red strip, then he rams along it; aimed at whoever keeps their distance), Steenregen (red circles),
 *       Mika-mijnwerkers (purple circles where they climb out, max 5).</li>
 *   <li>Defeated: he does not die. A smoke puff, and he races off on a little mine cart back to the Mika fort, with a cliffhanger.
 *       The machine is back (asleep) {@link Z2BazenPlekken#BOOR_TERUG_SECONDEN} later.</li>
 * </ul>
 */
public class Boormeester extends Z2Baas {
    public static final int SLAAPT = 0, VECHT = 1;
    private static final EntityDataAccessor<Integer> TOESTAND = SynchedEntityData.defineId(Boormeester.class, EntityDataSerializers.INT);
    public static final double PANTSER = 0.25, HITTE_PER = 0.04, STAP_SCHADE = 0.10;
    public static final int SMELT_TICKS = 160, PANEEL_PAUZE = 300;
    public static final float[] TOON = {0.55f, 1.0f, 1.7f};
    public static final int[] RGB = {0xFF3A30, 0xFFD62A, 0x3FB8FF};

    /** npc slice: who may start the machine with a right-click (default: whoever it was unlocked for once). */
    public static Predicate<ServerPlayer> MAG_STARTEN = p -> Spelers.data(p).extra("z2bazen").getBooleanOr("boormeester_ontgrendeld", false);

    @Nullable
    private static UUID huidig;
    private static long terugOp;

    public enum Actie {
        GEEN, STOOT, MIJN
    }

    private record Inslag(Vec3 plek, double straal, int[] t, int duur, double factor, String skill, boolean stoom) {
    }

    private final UUID[] koppen = new UUID[3];
    private int[] volgorde = {0, 1, 2};
    private int stap, smeltT, paneelT = 60, paneelStap = -1, paneelKlok;
    private Actie actie = Actie.GEEN;
    private int actieT, stootCd = 60, regenCd = 120, mijnCd = 200;
    private Vec3 stootVan = Vec3.ZERO, stootNaar = Vec3.ZERO;
    private final List<Vec3> mijnPlekken = new ArrayList<>();
    private final List<Inslag> inslagen = new ArrayList<>();
    /** Gametests: overheats in the right order, wrong ones (steam), meltdowns. */
    public int goedeHitte, stoom, smeltingen;

    public Boormeester(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TOESTAND, SLAAPT);
    }

    public int toestand() {
        return entityData.get(TOESTAND);
    }

    public boolean smelt() {
        return smeltT > 0;
    }

    public int[] volgorde() {
        return volgorde.clone();
    }

    public int stap() {
        return stap;
    }

    @Override
    public String baasId() {
        return "boormeester";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"stoot", "stoom", "smelt"};
    }

    @Override
    public int losseStraal() {
        return toestand() == VECHT ? Z2BazenPlekken.BOOR_STRAAL : 0;
    }

    @Override
    public double zStraal() {
        return Z2BazenPlekken.BOOR_STRAAL;
    }

    @Override
    protected double basisVads(int level) {
        return 5600 + 240 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 17 + 2.0 * level;
    }

    @Override
    public int xp() {
        return 4000 + 140 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.9;
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN;
    }

    @Override
    public boolean stil() {
        return super.stil() || toestand() != VECHT;
    }

    @Override
    protected boolean slaapt() {
        return toestand() != VECHT;
    }

    // ---- the secret start ---------------------------------------------------------------------------------------------

    /** The machine in this world (spawning it asleep at the drill site when needed and allowed). */
    @Nullable
    public static Boormeester zoek(ServerLevel sl, boolean maak) {
        if (huidig != null && sl.getEntity(huidig) instanceof Boormeester m && !m.isRemoved() && !m.geplofd()) {
            return m;
        }
        BlockPos c = Z2BazenPlekken.BOOR_CENTRUM;
        Boormeester gevonden = null;
        for (Boormeester m : sl.getEntitiesOfClass(Boormeester.class, new AABB(c).inflate(80, 40, 80), x -> !x.geplofd())) {
            if (gevonden == null) {
                gevonden = m;
            } else if (m.getTarget() == null) {
                m.discard();   // U-DUBBEL: an old twin
            }
        }
        if (gevonden != null) {
            huidig = gevonden.getUUID();
            return nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(gevonden, "baas:boormeester");
        }
        // U-DUBBEL: while its chunk's entities are still coming back from disk the saved machine is invisible: no twin then
        if (!maak || (sl.isLoaded(c) && !nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, c))) {
            return null;
        }
        sl.getChunk(c);
        Boormeester m = Z2BazenRegistry.BOORMEESTER.get().create(sl, EntitySpawnReason.SPAWNER);
        if (m == null) {
            return null;
        }
        m.stelIn(28, Element.VUUR, false, Set.of());
        m.zetOntvadst(false);
        m.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 180, 0);
        m.zetThuis(Vec3.atBottomCenterOf(c));
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(m, "baas:boormeester");   // U-DUBBEL
        sl.addFreshEntity(m);
        huidig = m.getUUID();
        return m;
    }

    /**
     * The npc slice's secret trigger: unlocks the machine for this player (remembered: later a right-click restarts it) and starts the
     * fight. False when it can't start right now (fled and not back yet, or already fighting someone else: then it just joins).
     */
    public static boolean ontgrendel(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(p)) {
            return false;
        }
        Spelers.data(p).extra("z2bazen").putBoolean("boormeester_ontgrendeld", true);
        if (sl.getGameTime() < terugOp && zoek(sl, false) == null) {
            terugOp = Math.min(terugOp, sl.getGameTime() + 600);   // 1.10.1: a new player waits 30 s, not 10 min
            return false;
        }
        Boormeester m = zoek(sl, true);
        if (m == null) {
            return false;
        }
        if (m.toestand() != VECHT) {
            m.start(sl, p);
        }
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (toestand() == VECHT || !(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || !(level() instanceof ServerLevel sl)) {
            return super.mobInteract(player, hand);
        }
        if (MAG_STARTEN.test(p)) {
            start(sl, p);
        } else {
            p.sendSystemMessage(Component.translatable("vadsrijk.z2bazen.bm.slot").withStyle(ChatFormatting.GRAY));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (toestand() != VECHT && source.getEntity() instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.z2bazen.bm.koud").withStyle(ChatFormatting.GRAY));
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    /** The machine roars awake: heads out, a fresh order, the intro for everyone at the site. */
    public void start(ServerLevel sl, @Nullable ServerPlayer wie) {
        if (toestand() == VECHT) {
            return;
        }
        entityData.set(TOESTAND, VECHT);
        zetThuis(Vec3.atBottomCenterOf(Z2BazenPlekken.BOOR_CENTRUM).distanceToSqr(position()) < 40 * 40
                ? Vec3.atBottomCenterOf(Z2BazenPlekken.BOOR_CENTRUM) : position());
        maakKoppen(sl);
        nieuweVolgorde();
        paneelT = 60;
        rustT = 20;
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 60, 1.5, 1, 1.5, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 2f, 0.5f);
        for (ServerPlayer p : sl.players()) {
            if (zDoetMee(p) || p == wie) {
                intro(p, Spelers.data(p).gezien.add("baas:z2_boormeester"));
                Tips.toon(p, "vadsrijk.z2bazen.tip.boormeester_paneel");
            }
        }
    }

    private void maakKoppen(ServerLevel sl) {
        for (int i = 0; i < 3; i++) {
            if (koppen[i] != null && sl.getEntity(koppen[i]) instanceof Boorkop k && !k.isRemoved()) {
                continue;
            }
            Boorkop k = Z2BazenRegistry.BOORKOP.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (k != null) {
                k.koppel(this, i);
                Vec3 v = kopPlek(i);
                k.snapTo(v.x, v.y, v.z, getYRot(), 0);
                sl.addFreshEntity(k);
                koppen[i] = k.getUUID();
            }
        }
    }

    @Nullable
    public Boorkop kop(int i) {
        return koppen[i] != null && level() instanceof ServerLevel sl && sl.getEntity(koppen[i]) instanceof Boorkop k && !k.isRemoved() ? k : null;
    }

    /** Where head i sits (0 left, 1 front, 2 right of the machine). */
    public Vec3 kopPlek(int i) {
        double a = Math.toRadians(getYRot());
        Vec3 f = new Vec3(-Math.sin(a), 0, Math.cos(a));
        Vec3 r = new Vec3(-f.z, 0, f.x);
        return switch (i) {
            case 0 -> position().add(f.scale(2.3)).subtract(r.scale(2.0)).add(0, 0.9, 0);
            case 1 -> position().add(f.scale(2.9)).add(0, 0.5, 0);
            default -> position().add(f.scale(2.3)).add(r.scale(2.0)).add(0, 0.9, 0);
        };
    }

    private void nieuweVolgorde() {
        List<Integer> l = new ArrayList<>(List.of(0, 1, 2));
        java.util.Collections.shuffle(l, new java.util.Random(getRandom().nextLong()));
        volgorde = new int[] {l.get(0), l.get(1), l.get(2)};
        stap = 0;
        for (int i = 0; i < 3; i++) {
            Boorkop k = kop(i);
            if (k != null) {
                k.zetHitte(0);
                k.zetOververhit(false);
            }
        }
    }

    /** Dev/gametest: force an order. */
    public void zetVolgorde(int a, int b, int c) {
        volgorde = new int[] {a, b, c};
        stap = 0;
    }

    // ---- heat -----------------------------------------------------------------------------------------------------------

    /** A hit on a drill head (from {@link Boorkop#krijgSchade}). */
    public void kopGeraakt(ServerLevel sl, Boorkop k, double n, @Nullable LivingEntity bron) {
        if (toestand() != VECHT || geplofd() || smelt()) {
            return;
        }
        if (bron instanceof ServerPlayer p) {
            if (!zDoetMee(p)) {
                return;
            }
            bijdragen().schade(p.getUUID(), n * 0.5);
        }
        if (k.oververhit()) {
            return;
        }
        k.zetHitte((float) (k.hitte() + n / Math.max(1, maxVads() * HITTE_PER) * 100));
        sl.sendParticles(ParticleTypes.SMOKE, k.getX(), k.getY() + 0.6, k.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
        if (k.hitte() >= 100) {
            oververhit(sl, k);
        }
    }

    public void oververhit(ServerLevel sl, Boorkop k) {
        if (k.kleur() == volgorde[stap]) {
            k.zetOververhit(true);
            k.zetHitte(100);
            stap++;
            goedeHitte++;
            sl.sendParticles(ParticleTypes.FLAME, k.getX(), k.getY() + 0.6, k.getZ(), 40, 0.4, 0.4, 0.4, 0.08);
            sl.sendParticles(ParticleTypes.LAVA, k.getX(), k.getY() + 0.6, k.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, k.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2f, 0.7f);
            krijgSchade(maxVads() * STAP_SCHADE, null);
            zeg("vadsrijk.z2bazen.bm.goed", ChatFormatting.GOLD, Component.translatable("vadsrijk.z2bazen.bm.kop." + Boorkop.KLEUREN[k.kleur()]), stap);
            if (stap >= 3 && !geplofd()) {
                smeltingen++;
                smeltT = SMELT_TICKS;
                actie = Actie.GEEN;
                Statussen.geef(this, Status.VERDOOFD, SMELT_TICKS, 1, null);
                speel("smelt");
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 2, getZ(), 4, 1.2, 1, 1.2, 0);
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.8f);
                kaart("vadsrijk.z2bazen.bm.smelt", 0xFF8A30);
            }
        } else {
            stoom++;
            Vec3 v = k.position();
            Telegraaf.cirkel(sl, new Vec3(v.x, getY(), v.z), 6, 20, ROOD);
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, 20);   // T-VFX: wind-up animation (held until the hit / auto-release)
            inslagen.add(new Inslag(new Vec3(v.x, getY(), v.z), 6, new int[1], 20, 0.45, "stoom", true));
            sl.sendParticles(ParticleTypes.CLOUD, v.x, v.y + 0.5, v.z, 60, 1, 0.6, 1, 0.15);
            sl.playSound(null, k.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 2.5f, 0.5f);
            speel("stoom");
            zeg("vadsrijk.z2bazen.bm.fout", ChatFormatting.RED);
            for (int i = 0; i < 3; i++) {
                Boorkop o = kop(i);
                if (o != null) {
                    o.zetHitte(0);
                    o.zetOververhit(false);
                }
            }
            stap = 0;
            paneelT = Math.min(paneelT, 60);
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (toestand() != VECHT) {
            return;
        }
        if (bron instanceof ServerPlayer p) {
            n *= smelt() ? 2.0 : PANTSER;
            if (!smelt()) {
                Tips.toon(p, "vadsrijk.z2bazen.tip.boormeester_pantser");
            }
        }
        super.krijgSchade(n, bron);
    }

    // ---- the fight ------------------------------------------------------------------------------------------------------

    @Override
    protected void signatuur() {
        speel("stoot");
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.6f);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 40, 1, 1, 1, 0.05);
        }
    }

    @Override
    protected void opFase(int fase) {
        speel("stoom");
        if (fase == 2) {
            kaart("vadsrijk.z2bazen.bm.fase2", 0xFFB040);
        } else if (fase == 3) {
            kaart("vadsrijk.z2bazen.bm.fase3", 0xFF5030);
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (smelt()) {
            rustT = 10;
            return;
        }
        if (mijnCd <= 0 && mijnwerkers(sl) < 5) {
            mijnPlekken.clear();
            int n = fase() >= 2 ? 3 : 2;
            for (int i = 0; i < n; i++) {
                Vec3 m = zMidden();
                double a = getRandom().nextDouble() * Math.PI * 2, r = 6 + getRandom().nextDouble() * (zStraal() - 9);
                Vec3 v = new Vec3(m.x + Math.cos(a) * r, m.y, m.z + Math.sin(a) * r);
                mijnPlekken.add(v);
                Telegraaf.cirkel(sl, v, 1.2, 30, PAARS);
            }
            actie = Actie.MIJN;
            actieT = 30;
            mijnCd = 520;
            rustT = 40;
            zeg("vadsrijk.z2bazen.bm.mijnwerkers", ChatFormatting.LIGHT_PURPLE);
            laatsteAanval = "mijnwerkers";
            return;
        }
        if (stootCd <= 0) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : zDoel();
            if (v != null) {
                stootVan = position();
                Vec3 r = v.position().subtract(stootVan).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
                double len = Math.min(16, Math.max(8, v.position().distanceTo(stootVan) + 3));
                Vec3 naar = stootVan.add(r.scale(len));
                stootNaar = voetVan(new Vec3(naar.x, stootVan.y, naar.z));
                Telegraaf.lijn(sl, stootVan, stootNaar, 3.5, 30, ROOD);
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, 30);   // T-VFX: wind-up animation (held until the hit / auto-release)
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "boorstoot", 30);   // T-VFX: cast bar
                setYRot((float) Math.toDegrees(Math.atan2(-r.x, r.z)));
                actie = Actie.STOOT;
                actieT = 30;
                stootCd = fase() >= 3 ? 120 : 170;
                rustT = 60;
                speel("stoot");
                laatsteAanval = "boorstoot";
                return;
            }
        }
        if (regenCd <= 0) {
            List<Vec3> plekken = new ArrayList<>();
            for (ServerPlayer p : l) {
                plekken.add(voetVan(p));
            }
            for (int i = 0; i < (fase() >= 3 ? 4 : 2); i++) {
                Vec3 m = zMidden();
                double a = getRandom().nextDouble() * Math.PI * 2, r = getRandom().nextDouble() * (zStraal() - 3);
                plekken.add(new Vec3(m.x + Math.cos(a) * r, m.y, m.z + Math.sin(a) * r));
            }
            for (Vec3 v : plekken) {
                Telegraaf.cirkel(sl, v, 2.5, 40, ROOD);
                inslagen.add(new Inslag(v, 2.5, new int[1], 40, 1.2, "steenregen", false));
            }
            regenCd = fase() >= 3 ? 140 : 200;
            rustT = 30;
            zeg("vadsrijk.z2bazen.bm.steenregen", ChatFormatting.RED);
            laatsteAanval = "steenregen";
            return;
        }
        rustT = 10;
    }

    private int mijnwerkers(ServerLevel sl) {
        return sl.getEntitiesOfClass(Z2Hulpje.class, getBoundingBox().inflate(40), h -> !h.geplofd() && h.mika() && !h.vluchter).size();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        if (toestand() == VECHT) {
            for (int i = 0; i < 3; i++) {
                Boorkop k = kop(i);
                if (k != null) {
                    k.zetOpPlek(kopPlek(i), getYRot());
                    if (k.oververhit() && tickCount % 4 == 0) {
                        sl.sendParticles(ParticleTypes.FLAME, k.getX(), k.getY() + 0.5, k.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                    }
                    if (!k.oververhit() && tickCount % 20 == 0 && k.hitte() > 0) {
                        k.zetHitte(k.hitte() - 2);
                    }
                }
            }
            if (tickCount % 20 == 0) {
                maakKoppen(sl);
            }
            if (smeltT > 0 && --smeltT == 0) {
                nieuweVolgorde();
                paneelT = 30;
                zeg("vadsrijk.z2bazen.bm.afgekoeld", ChatFormatting.GOLD);
            }
        }
    }

    @Override
    protected void actieTick(ServerLevel sl, List<ServerPlayer> l) {
        stootCd--;
        regenCd--;
        mijnCd--;
        // the control panel: the order, light by light, tone by tone
        if (paneelStap < 0 && --paneelT <= 0) {
            paneelStap = 0;
            paneelKlok = 0;
        }
        if (paneelStap >= 0 && paneelKlok-- <= 0) {
            if (paneelStap < 3) {
                int kleur = volgorde[paneelStap];
                Boorkop k = kop(kleur);
                Vec3 v = k != null ? k.position().add(0, 1.4, 0) : position().add(0, 4, 0);
                sl.sendParticles(new DustParticleOptions(RGB[kleur], 2.5f), v.x, v.y, v.z, 30, 0.3, 0.3, 0.3, 0);
                sl.sendParticles(new DustParticleOptions(RGB[kleur], 2.0f), getX(), getY() + 3.6, getZ(), 15, 0.2, 0.2, 0.2, 0);
                sl.playSound(null, BlockPos.containing(v), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 2f, TOON[kleur]);
                paneelStap++;
                paneelKlok = 14;
            } else {
                paneelStap = -1;
                paneelT = PANEEL_PAUZE;
            }
        }
        for (Iterator<Inslag> it = inslagen.iterator(); it.hasNext(); ) {
            Inslag i = it.next();
            if (++i.t()[0] < i.duur()) {
                continue;
            }
            it.remove();
            if (i.stoom()) {
                sl.sendParticles(ParticleTypes.CLOUD, i.plek().x, i.plek().y + 0.5, i.plek().z, 80, 2.5, 0.5, 2.5, 0.1);
                for (ServerPlayer p : strijders()) {
                    if (p.position().multiply(1, 0, 1).distanceTo(i.plek().multiply(1, 0, 1)) <= i.straal() && Math.abs(p.getY() - i.plek().y) < 3) {
                        zMechaniek(p, i.factor(), i.skill());
                        duw(p, i.plek(), 1.2);
                    }
                }
            } else {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), i.plek().x, i.plek().y + 0.5, i.plek().z, 30,
                        i.straal() * 0.4, 0.3, i.straal() * 0.4, 0.1);
                sl.playSound(null, BlockPos.containing(i.plek()), SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.5f, 0.7f);
                zRond(i.plek(), i.straal(), i.factor(), i.skill(), null, 0, 0.3);
            }
        }
        switch (actie) {
            case STOOT -> {
                if (--actieT == 0) {
                    zSpring(stootNaar, 12, 0.6);
                }
            }
            case MIJN -> {
                if (--actieT <= 0) {
                    actie = Actie.GEEN;
                    for (Vec3 v : mijnPlekken) {
                        Z2Hulpje h = Z2BazenRegistry.MIJNWERKER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
                        if (h != null) {
                            h.snapTo(v.x, v.y, v.z, getRandom().nextFloat() * 360, 0);
                            h.koppel(this);
                            sl.addFreshEntity(h);
                            sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 0.5, v.z, 15, 0.3, 0.3, 0.3, 0.02);
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void zGeland() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        actie = Actie.GEEN;
        for (ServerPlayer p : strijders()) {
            if (inStrook(p.position(), stootVan, stootNaar, 1.75)) {
                zRaak(p, 1.6, "boorstoot");
                duw(p, position(), 1.0);
            }
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), getX(), getY() + 0.3, getZ(), 50, 1.5, 0.3, 1.5, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    @Override
    protected void zLeeg() {
        // nobody left: the machine goes back to sleep (a fresh one at full Vads)
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 40, 1.5, 1, 1.5, 0.02);
            ruimOp(sl);
        }
        discard();
        huidig = null;
        terugOp = 0;
    }

    private void ruimOp(ServerLevel sl) {
        for (int i = 0; i < 3; i++) {
            Boorkop k = kop(i);
            if (k != null) {
                k.discard();
            }
        }
        for (Z2Hulpje h : sl.getEntitiesOfClass(Z2Hulpje.class, getBoundingBox().inflate(48), h -> h.mika() && !h.vluchter)) {
            h.discard();
        }
    }

    /** Defeated: no death. A smoke puff, and he races off on a little mine cart back to the Mika fort. */
    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ruimOp(sl);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 120, 2, 1.5, 2, 0.05);
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.5, getZ(), 60, 2, 1.5, 2, 0.08);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 1.5f);
        // the flight: a Mika on a mine cart, rolling out of the cave (back to the Mika fort), then gone in smoke
        var kar = EntityType.MINECART.create(sl, EntitySpawnReason.EVENT);
        Z2Hulpje mika = Z2BazenRegistry.MIJNWERKER.get().create(sl, EntitySpawnReason.EVENT);
        if (kar != null && mika != null) {
            kar.snapTo(getX(), getY(), getZ(), 180, 0);
            sl.addFreshEntity(kar);
            mika.snapTo(getX(), getY(), getZ(), 180, 0);
            mika.koppel(this);
            mika.setCustomName(Component.translatable("entity.guhs.vr_z2_baas_boormeester"));
            sl.addFreshEntity(mika);
            mika.startRiding(kar, true, true);
            Vec3 uit = Vec3.atBottomCenterOf(Z2BazenPlekken.BOOR_DEUR).subtract(position()).multiply(1, 0, 1);   // out of the cave, to the fort
            mika.rijWeg(uit.lengthSqr() < 1 ? new Vec3(0, 0, 0.45) : uit.normalize().scale(0.45));
        }
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 48) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z2bazen.bm.naam").withStyle(ChatFormatting.RED)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable("vadsrijk.z2bazen.bm.cliff").withStyle(ChatFormatting.WHITE)));
                // (WERELDVERHAAL §10 #11) the slip of the tongue: the only "Keizer" in zone 1-2
                p.sendSystemMessage(Component.translatable("vadsrijk.z2bazen.bm.naam").withStyle(ChatFormatting.RED)
                        .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable("vadsrijk.z2bazen.bm.cliff2").withStyle(ChatFormatting.WHITE)));
                RijkScenes.titel(p, Component.translatable("vadsrijk.z2bazen.bm.cliff_titel").withStyle(ChatFormatting.BOLD),
                        Component.translatable("vadsrijk.z2bazen.bm.cliff_onder"), 80, 0xFFB040);
            }
        }
        terugOp = sl.getGameTime() + Z2BazenPlekken.BOOR_TERUG_SECONDEN * 20L;
        huidig = null;
        discard();
    }

    /** Every 20 ticks: the sleeping machine back at the drill site once the time-out is over. */
    public static void beheer(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        // 1.10.1: an unlocked player who still needs him (secret quest open) at the drill site: back within 30 s, not after 10 min
        if (sl != null && sl.getGameTime() % 20 == 0 && sl.getGameTime() + 600 < terugOp && sl.players().stream()
                .anyMatch(p -> p.position().distanceToSqr(Vec3.atCenterOf(Z2BazenPlekken.BOOR_CENTRUM)) < 40 * 40
                        && Spelers.data(p).extra("z2bazen").getBooleanOr("boormeester_ontgrendeld", false)
                        && nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.questActief(p, "z2_geheim_boormeester"))) {
            terugOp = sl.getGameTime() + 600;
        }
        if (sl == null || sl.getGameTime() % 20 != 0 || sl.getGameTime() < terugOp || !sl.isLoaded(Z2BazenPlekken.BOOR_CENTRUM)) {
            return;
        }
        zoek(sl, true);
    }

    /** Dev: away and back asleep now. */
    public static void reset(ServerLevel sl) {
        Boormeester m = zoek(sl, false);
        if (m != null) {
            m.ruimOp(sl);
            m.discard();
        }
        huidig = null;
        terugOp = 0;
        zoek(sl, true);
    }
}
