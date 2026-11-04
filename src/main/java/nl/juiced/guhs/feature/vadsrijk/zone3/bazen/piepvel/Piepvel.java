package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.piepvel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Blokken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Figurant;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Scenes;

/**
 * Graaf Piepvel, de Pink van de Grauwe Keizer-Mika: the central boss of zone 3 (lv 42, the Pelszaal at the top of the
 * Lantaarntoren, respawn 10 min). Vain, squeaks posh, loves dark and cold. His cape is made of the shed winter coats of
 * pieppiepmuisjes he kept in the cold; the sad, bald mice sit in the hanging cages.
 * <ul>
 *   <li>Fase 1 Capezwier (100-70 %): he spins with his cape out (a rotating red cone), grey fluff balls roll across the hall in
 *       straight lines, and every 20 s his "deftige entree": a leap at the player furthest away.</li>
 *   <li>Fase 2 De kooitjes (70-40 %): he hides behind a vads shield fed by the mice's sadness (grey light lines from the 4 cages
 *       to his cape). Break the 4 cage locks (they hang at different heights; ranged reaches them, melee jumps): each freed cage =
 *       -25 % shield, and the mice gnaw a hole in his cape (one cape panel goes). No time pressure between the locks.</li>
 *   <li>Fase 3 Donker (40-0 %): he blows out every lantern. You see only his glowing eyes (Darkness). Relight the posts
 *       (right-click): only near a lit post is he visible and hit for full damage (soft lantern rule: elsewhere -50 %). Now and
 *       then he blows at a lit post (purple circle): stand in it to shield the flame (25 %), or it goes out. At 10 % open the big
 *       cage by the throne (right-click): every mouse storms his cape and it falls to pieces.</li>
 *   <li>Enrage (6 min): the hall freezes (Bevroren + 8 % pulses).</li>
 *   <li>Defeat: the count in his shirt ("Mijn cape!"), then flash F1: a long grey ARM of the Keizer reaches in through a knothole
 *       and snatches him away ({@link Z3Scenes#armFlash}). The mice are free, their fur grows back, they squeak off happily.</li>
 * </ul>
 */
public class Piepvel extends Z3Baas {
    public static final String H_PALEN = "z3_piepvel_palen";
    private static final EntityDataAccessor<Integer> CAPE = SynchedEntityData.defineId(Piepvel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SCHILD = SynchedEntityData.defineId(Piepvel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ZICHTBAAR = SynchedEntityData.defineId(Piepvel.class, EntityDataSerializers.BOOLEAN);
    private final List<Z3Doel> kooien = new ArrayList<>();
    @Nullable
    private Z3Doel groteKooi;
    private final Map<Integer, List<Z3Figurant>> muizen = new HashMap<>();
    private final List<Z3Figurant> vrij = new ArrayList<>();
    private int zwierCd = 60, bolCd = 30, entreeCd = 400, blaasCd = 300, windCd = 200;
    private int zwierT = -1;
    private double zwierHoek;
    private final Set<UUID> zwierGeraakt = new HashSet<>();
    private boolean opgezet, finale, capeWeg;
    @Nullable
    private BlockPos blaasPaal;
    /** Gametests: cages freed, posts blown out. */
    public int bevrijd, uitgeblazen;

    public Piepvel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CAPE, 0b1111);
        b.define(SCHILD, false);
        b.define(ZICHTBAAR, true);
    }

    /** Bitmask of the cape panels still there (bit i = cape_(i+1)); 0 = in his shirt. */
    public int cape() {
        return entityData.get(CAPE);
    }

    public boolean schild() {
        return entityData.get(SCHILD);
    }

    /** Client: false in the dark away from the light (only his eyes). */
    public boolean zichtbaar() {
        return entityData.get(ZICHTBAAR);
    }

    @Override
    public String baasId() {
        return "piepvel";
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"capezwier", "worp", "sprong", "buig"};
    }

    @Override
    protected double hp(int level) {
        return 7200 + 300 * level;
    }

    @Override
    protected double klap(int level) {
        return 18 + 2.2 * level;
    }

    @Override
    public int xp() {
        return 5200 + 160 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.7, 0.4};
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 6;
    }

    @Override
    protected void enrage(ServerLevel sl) {
        kaart("vadsrijk.z3bazen.pv.enrage", Z2Baas.IJSBLAUW);
    }

    @Override
    protected void enragePuls(ServerLevel sl, List<ServerPlayer> l) {
        for (ServerPlayer p : l) {
            mech(p, 0.08, "kou");
            Statussen.geef(p, Z2Baas.bevroren(), 50, 0.5, this);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.7, 0.5, 0.02);
        }
    }

    @Override
    protected boolean immuun() {
        return schild() || finale;
    }

    @Override
    protected String immuunKey() {
        return schild() ? "vadsrijk.z3bazen.pv.schild" : "vadsrijk.z3bazen.pv.finale";
    }

    @Override
    protected double vadsVloer() {
        return fase() >= 3 && !finale && !capeWeg ? 0.10 : fase() == 2 && schild() ? 0.405 : 0;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return fase() >= 3 && !inLicht() ? 0.5 : 1;
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        p.sendSystemMessage(Component.translatable(titelKey()).withStyle(ChatFormatting.GOLD).append(": ")
                .append(Component.translatable(eersteKeer ? "vadsrijk.z3bazen.pv.intro" : "vadsrijk.z3bazen.pv.intro_weer").withStyle(ChatFormatting.WHITE)));
    }

    // ---- the hall: cages, mice, lantern posts ---------------------------------------------------------------------------

    public List<Z3Doel> kooien() {
        return kooien;
    }

    @Nullable
    public Z3Doel groteKooi() {
        return groteKooi;
    }

    public List<BlockPos> palen() {
        List<BlockPos> l = new ArrayList<>();
        for (int[] o : Z3BazenPlekken.PIEP_PALEN) {
            l.add(blok(o));
        }
        return l;
    }

    private void zetPaal(ServerLevel sl, BlockPos b, boolean aan) {
        var s = sl.getBlockState(b);
        if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof Z3Blokken.Lantaarnpaal) {
            Herstel.zet(sl, H_PALEN, b, Z3BazenRegistry.LANTAARNPAAL.get().defaultBlockState().setValue(Z3Blokken.Lantaarnpaal.LIT, aan));
        }
    }

    public int palenAan(ServerLevel sl) {
        int n = 0;
        for (BlockPos b : palen()) {
            n += Z3Blokken.Lantaarnpaal.aan(sl, b) ? 1 : 0;
        }
        return n;
    }

    /** Is he near a lit post (the soft lantern rule)? */
    public boolean inLicht() {
        if (!(level() instanceof ServerLevel sl)) {
            return true;
        }
        for (BlockPos b : palen()) {
            if (Z3Blokken.Lantaarnpaal.aan(sl, b) && b.distToCenterSqr(getX(), getY(), getZ()) < 6.5 * 6.5) {
                return true;
            }
        }
        return false;
    }

    private void opzetten(ServerLevel sl) {
        Herstel.terug(sl, H_PALEN);
        for (BlockPos b : palen()) {
            zetPaal(sl, b, true);
        }
        Vec3 m = zMidden();
        for (int i = 0; i < Z3BazenPlekken.PIEP_KOOIEN.length; i++) {
            Vec3 v = punt(Z3BazenPlekken.PIEP_KOOIEN[i]);
            Z3Doel k = Z3BazenRegistry.KOOI.get().create(sl, EntitySpawnReason.EVENT);
            if (k != null) {
                k.koppel(this, i, hp(Math.min(50, mobLevel())) * 0.06, v);
                k.zetRaakbaar(false);   // the locks open in fase 2
                k.kijk((float) Math.toDegrees(Math.atan2(-(m.x - v.x), m.z - v.z)));
                sl.addFreshEntity(k);
                kooien.add(k);
                List<Z3Figurant> l = new ArrayList<>();
                for (int j = 0; j < 2; j++) {
                    Z3Figurant f = Z3Figurant.muisje(sl, v.add(j == 0 ? -0.25 : 0.25, 0.1, j == 0 ? 0.1 : -0.15), true);
                    if (f != null) {
                        f.zetSchaal(0.9f);
                        l.add(f);
                    }
                }
                muizen.put(i, l);
            }
        }
        Vec3 g = punt(Z3BazenPlekken.PIEP_GROTE_KOOI);
        groteKooi = Z3BazenRegistry.KOOI.get().create(sl, EntitySpawnReason.EVENT);
        if (groteKooi != null) {
            groteKooi.koppel(this, 4, 1000, g);
            groteKooi.zetRaakbaar(false);
            sl.addFreshEntity(groteKooi);
            List<Z3Figurant> l = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                Z3Figurant f = Z3Figurant.muisje(sl, g.add((j % 2 - 0.5) * 0.5, 0.1, (j / 2 - 0.5) * 0.5), true);
                if (f != null) {
                    f.zetSchaal(0.9f);
                    l.add(f);
                }
            }
            muizen.put(4, l);
        }
    }

    private void opruimen() {
        kooien.forEach(k -> k.discard());
        kooien.clear();
        if (groteKooi != null) {
            groteKooi.discard();
        }
        muizen.values().forEach(l -> l.forEach(f -> f.discard()));
        muizen.clear();
    }

    // ---- the fight ------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            kaart("vadsrijk.z3bazen.pv.fase2", PAARS);
            entityData.set(SCHILD, true);
            zwierT = -1;
            for (Z3Doel k : kooien) {
                k.zetRaakbaar(true);
            }
            zSpring(punt(Z3BazenPlekken.PIEP_TROON).add(0, 0, 2), 18, 4);
            spreek("vadsrijk.z3bazen.pv.schild_op");
        } else if (fase == 3) {
            kaart("vadsrijk.z3bazen.pv.fase3", 0x404060);
            for (BlockPos b : palen()) {
                zetPaal(sl, b, false);
            }
            sl.playSound(null, blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 3f, 0.5f);
            spreek("vadsrijk.z3bazen.pv.donker");
            blaasCd = 300;
        }
    }

    @Override
    public boolean paalAan(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        if (fase() < 3 || !palen().contains(pos)) {
            return false;
        }
        zetPaal(sl, pos, true);
        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1.4f);
        sl.sendParticles(new DustParticleOptions(0xFFC870, 1.6f), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0);
        return true;
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() == 1 && zwierCd <= 0) {
            zwierT = tg(20) + 60;
            zwierHoek = getRandom().nextDouble() * Math.PI * 2;
            zwierGeraakt.clear();
            speel("capezwier");
            zeg("vadsrijk.z3bazen.pv.zwier", ChatFormatting.RED);
            zwierCd = 220;
            rustT = zwierT + 10;
            laatsteAanval = "capezwier";
            return;
        }
        if (fase() != 2 && entreeCd <= 0) {
            ServerPlayer v = verste(l);
            if (v != null) {
                Vec3 doel = voetVan(v);
                Telegraaf.cirkel(sl, doel, 3.5, tg(24), ROOD);
                speel("sprong");
                later(tg(24), () -> zSpring(doel, 14, 5));
                spreek("vadsrijk.z3bazen.pv.entree");
                entreeCd = 400;
                rustT = 50;
                laatsteAanval = "entree";
                return;
            }
        }
        if (fase() >= 3 && blaasCd <= 0) {
            List<BlockPos> aan = palen().stream().filter(b -> Z3Blokken.Lantaarnpaal.aan(sl, b)).toList();
            if (!aan.isEmpty()) {
                blaasPaal = aan.get(getRandom().nextInt(aan.size()));
                Vec3 v = Vec3.atBottomCenterOf(blaasPaal);
                Telegraaf.cirkel(sl, v, 2, tg(60), PAARS);
                Telegraaf.lijn(sl, position(), v, 1.2, tg(60), PAARS);
                zeg("vadsrijk.z3bazen.pv.blaas", ChatFormatting.LIGHT_PURPLE);
                later(tg(60), () -> blaas(sl));
                blaasCd = 280;
                rustT = 30;
                laatsteAanval = "blaas";
                return;
            }
        }
        if (fase() == 2 && windCd <= 0) {
            Telegraaf.cirkel(sl, position(), 6, tg(30), PAARS);
            later(tg(30), () -> {
                for (ServerPlayer p : inCirkel(position(), 6)) {
                    mech(p, 0.15, "kille_wind");
                    duw(p, position(), 1.6);
                    Statussen.geef(p, Z2Baas.bevroren(), 40, 0.4, this);
                }
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 80, 3, 0.5, 3, 0.15);
            });
            windCd = 240;
            rustT = 40;
            laatsteAanval = "kille_wind";
            return;
        }
        if (bolCd <= 0) {
            int n = fase() == 2 ? 3 : 2;
            Vec3 m = zMidden();
            for (int i = 0; i < n && i < l.size() + 1; i++) {
                ServerPlayer p = i < l.size() ? l.get((i + getRandom().nextInt(l.size())) % l.size()) : null;
                Vec3 doel = p != null ? voetVan(p) : ring(m, 6, getRandom().nextDouble() * Math.PI * 2);
                Vec3 r = doel.subtract(position()).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                Vec3 a = new Vec3(getX(), m.y, getZ());
                Vec3 b = voetVan(a.add(r.scale(zStraal() * 2)));
                rol(a, new Vec3(b.x, m.y, b.z), 2.4, 26, 24, 0.25, "pluisbol", new DustParticleOptions(0xA8A8B4, 2.4f), Status.PLAKKERIG);
            }
            speel("worp");
            bolCd = fase() == 2 ? 70 : 100;
            rustT = 30;
            laatsteAanval = "pluisbol";
            return;
        }
        rustT = 10;
    }

    @Nullable
    private ServerPlayer verste(List<ServerPlayer> l) {
        ServerPlayer best = null;
        double d = -1;
        for (ServerPlayer p : l) {
            double x = p.distanceToSqr(this);
            if (x > d) {
                d = x;
                best = p;
            }
        }
        return best;
    }

    private void blaas(ServerLevel sl) {
        if (blaasPaal == null || !Z3Blokken.Lantaarnpaal.aan(sl, blaasPaal)) {
            return;
        }
        Vec3 v = Vec3.atBottomCenterOf(blaasPaal);
        List<ServerPlayer> in = inCirkel(v, 2);
        if (!in.isEmpty()) {
            for (ServerPlayer p : in) {
                mech(p, 0.25, "blaas");
            }
            zeg("vadsrijk.z3bazen.pv.beschermd", ChatFormatting.GREEN);
        } else {
            zetPaal(sl, blaasPaal, false);
            uitgeblazen++;
            sl.playSound(null, blaasPaal, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 2f, 0.6f);
            sl.sendParticles(ParticleTypes.SMOKE, v.x, v.y + 1.2, v.z, 20, 0.2, 0.3, 0.2, 0.02);
        }
        blaasPaal = null;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        zwierCd--;
        bolCd--;
        entreeCd--;
        blaasCd--;
        windCd--;
        if (!opgezet) {
            opgezet = true;
            opzetten(sl);
        }
        // the cape spin: a rotating red cone
        if (zwierT >= 0) {
            zwierT--;
            boolean actief = zwierT < 60;
            zwierHoek += actief ? 0.11 : 0.03;
            getNavigation().stop();
            if (zwierT % 3 == 0) {
                DustParticleOptions rood = new DustParticleOptions(ROOD, 1.6f);
                for (double da : new double[] {-0.6, 0, 0.6}) {
                    double a = zwierHoek + da;
                    for (double r = 1; r <= 9; r += 0.8) {
                        sl.sendParticles(rood, getX() + Math.cos(a) * r, getY() + 0.15, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
                    }
                }
            }
            if (actief) {
                for (ServerPlayer p : l) {
                    Vec3 d = p.position().subtract(position()).multiply(1, 0, 1);
                    double afst = d.length();
                    if (afst < 9 && afst > 0.1) {
                        double a = Math.atan2(d.z, d.x), diff = Math.atan2(Math.sin(a - zwierHoek), Math.cos(a - zwierHoek));
                        if (Math.abs(diff) < 0.62 && zwierGeraakt.add(p.getUUID())) {
                            mech(p, 0.2, "capezwier");
                            duw(p, position(), 0.9);
                        }
                    }
                }
                if (zwierT % 20 == 0) {
                    zwierGeraakt.clear();   // one hit per player per second
                    sl.playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 0.8f, 1.6f);
                }
            }
        }
        // fase 2: grey light lines from the cages to his cape
        if (schild() && tickCount % 4 == 0) {
            DustParticleOptions grijs = new DustParticleOptions(0x9A9AAE, 1.2f);
            for (Z3Doel k : kooien) {
                if (k.raakbaar() && !k.isRemoved()) {
                    Vec3 a = k.position().add(0, 0.6, 0), b = position().add(0, 1, 0);
                    for (double s = 0; s <= 1; s += 0.08) {
                        Vec3 v = a.lerp(b, s);
                        sl.sendParticles(grijs, v.x, v.y, v.z, 1, 0, 0, 0, 0);
                    }
                }
            }
            sl.sendParticles(new DustParticleOptions(0xB0A0E0, 1.4f), getX(), getY() + 1, getZ(), 6, 1.2, 1, 1.2, 0);
        }
        // fase 3: the dark
        if (fase() >= 3 && !geplofd()) {
            boolean licht = inLicht();
            if (zichtbaar() != licht) {
                entityData.set(ZICHTBAAR, licht);
            }
            if (tickCount % 20 == 0) {
                for (ServerPlayer p : l) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                }
            }
            if (tickCount % 3 == 0) {   // his glowing eyes
                Vec3 o = getLookAngle().multiply(1, 0, 1).normalize().scale(0.6);
                sl.sendParticles(new DustParticleOptions(0xFF4040, 1.0f), getX() + o.x, getY() + 1.15, getZ() + o.z, 2, 0.15, 0.02, 0.15, 0);
            }
            if (groteKooi != null && groteKooi.raakbaar() && tickCount % 10 == 0) {
                Telegraaf.cirkel(sl, groteKooi.position(), 1.6, 10, PAARS);
            }
        }
        // freed mice run to his cape and nibble
        vrij.removeIf(f -> f.isRemoved());
    }

    @Override
    public void doelKapot(Z3Doel k, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl) || k.nummer > 3) {
            return;
        }
        bevrijd++;
        k.speel("open");
        k.zetRaakbaar(false);
        sl.playSound(null, k.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.5f, 1.3f);
        sl.playSound(null, k.blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.NEUTRAL, 1f, 2f);
        for (Z3Figurant f : muizen.getOrDefault(k.nummer, List.of())) {
            f.anim("walk");
            f.snelheid = 0.3;
            f.doel = position().add(getRandom().nextGaussian() * 0.5, 0.2, getRandom().nextGaussian() * 0.5);
            vrij.add(f);
        }
        later(30, () -> {
            entityData.set(CAPE, cape() & ~(1 << Math.max(0, Math.min(3, Integer.bitCount(0b1111 & ~cape())))));
            sl.sendParticles(new DustParticleOptions(0xE0E0EA, 1.8f), getX(), getY() + 1, getZ(), 40, 0.8, 0.6, 0.8, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.5f, 1.4f);
            for (Z3Figurant f : muizen.getOrDefault(k.nummer, List.of())) {
                f.doel = Vec3.atBottomCenterOf(Z3BazenPlekken.PIEP_DEUR).add(getRandom().nextGaussian(), 0, getRandom().nextGaussian());
                f.leven = 80;
            }
        });
        long over = kooien.stream().filter(Z3Doel::raakbaar).count();
        zeg("vadsrijk.z3bazen.pv.kooi_open", ChatFormatting.GREEN, 4 - over);
        spreek("vadsrijk.z3bazen.pv.au");
        if (over == 0) {
            entityData.set(SCHILD, false);
            kaart("vadsrijk.z3bazen.pv.schild_weg", ROZE);
        }
    }

    /** The big cage (at 10 %): one click and every mouse storms his cape. */
    @Override
    public void doelGebruikt(Z3Doel k, ServerPlayer p) {
        if (k != groteKooi || !k.raakbaar() || finale || !(level() instanceof ServerLevel sl)) {
            if (k == groteKooi && !finale) {
                p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.pv.nog_niet").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        finale = true;
        k.zetRaakbaar(false);
        k.speel("open");
        Statussen.geef(this, Status.VERDOOFD, 80, 1, null);
        spreek("vadsrijk.z3bazen.pv.nee");
        Vec3 bij = position();
        List<Z3Figurant> storm = new ArrayList<>(muizen.getOrDefault(4, List.of()));
        for (int i = 0; i < 10; i++) {
            Z3Figurant f = Z3Figurant.muisje(sl, k.position().add(getRandom().nextGaussian() * 0.8, 0, getRandom().nextGaussian() * 0.8), true);
            if (f != null) {
                storm.add(f);
            }
        }
        for (Z3Figurant f : storm) {
            f.anim("walk");
            f.snelheid = 0.35;
            f.doel = bij.add(getRandom().nextGaussian() * 0.6, 0.3 + getRandom().nextDouble(), getRandom().nextGaussian() * 0.6);
            vrij.add(f);
        }
        muizen.put(4, storm);
        sl.playSound(null, k.blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 2f, 1.8f);
        later(40, () -> {
            entityData.set(CAPE, 0);
            sl.sendParticles(new DustParticleOptions(0xE0E0EA, 2.2f), getX(), getY() + 1, getZ(), 120, 1.2, 1, 1.2, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 2f, 0.8f);
            capeWeg = true;
            finale = false;
            verslagen(p);
        });
    }

    // ---- the defeat: the arm of the Keizer ---------------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        // no smoke: he is snatched away (Z3Scenes.armFlash moves him)
        entityData.set(CAPE, 0);
        entityData.set(ZICHTBAAR, true);
        setNoGravity(true);
    }

    @Override
    protected int plofDuur() {
        return Z3Scenes.ARM_TICKS + 40;
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            spreek("vadsrijk.z3bazen.pv.mijn_cape");
            // every mouse is free: their fur grows back, they squeak off happily
            List<Z3Figurant> alle = new ArrayList<>(vrij);
            muizen.values().forEach(alle::addAll);
            for (Z3Figurant f : alle) {
                if (!f.isRemoved()) {
                    f.zetTextuur("textures/entity/pieppiepmuisje.png");
                    f.anim("blij");
                    f.doel = Vec3.atBottomCenterOf(Z3BazenPlekken.PIEP_DEUR).add(getRandom().nextGaussian() * 2, 0, getRandom().nextGaussian() * 2);
                    f.snelheid = 0.12;
                    f.leven = 200 + getRandom().nextInt(80);
                }
            }
            kooien.forEach(k -> k.speel("open"));
            Z3Scenes.armFlash(sl, this, alle);
            for (BlockPos b : palen()) {
                zetPaal(sl, b, true);
            }
            BazenLater.later(sl.getServer(), 20 * 30, () -> Herstel.terug(sl, H_PALEN));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd()) {
            setDeltaMovement(Vec3.ZERO);
        } else if (!level().isClientSide() && fase() >= 3 && vads() <= maxVads() * 0.105 && groteKooi != null && !groteKooi.raakbaar() && !finale
                && !capeWeg) {
            groteKooi.zetRaakbaar(true);   // (also while stunned)
            zeg("vadsrijk.z3bazen.pv.grote_kooi", ChatFormatting.LIGHT_PURPLE);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !geplofd()) {
            Herstel.terug(sl, H_PALEN);
            opruimen();
        } else {
            kooien.forEach(k -> k.discard());
            if (groteKooi != null) {
                groteKooi.discard();
            }
        }
        super.remove(reason);
    }

    /** (the zone-1 helper: run later on the server) */
    static final class BazenLater {
        static void later(net.minecraft.server.MinecraftServer s, int t, Runnable r) {
            nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry.later(s, t, r);
        }
    }
}
