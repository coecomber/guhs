package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4Vfx;

/**
 * De Grauwe Zeeslurf (world boss, lv 54, the pink lagoon of the Koraalbaai): a huge de-vadsed sea cow with a jellyfish's frilly
 * veil and a long trunk, grey and sulking at the bottom. You cheer it up (ZONE4_PLAN Z4-3). It swims (no gravity, never leaves
 * the water: its height stays between the lagoon floor and the surface).
 * <ul>
 *   <li><b>Fase 1</b> (100-66 %): <i>Slurfspuit</i> = a red strip from its trunk at someone (1 s), then a jet of grey water: 18 %
 *       + a push; <i>Mokwolk</i> = 2-3 red circles under players (1.2 s): 12 % + 3 s Darkness (grey sulk ink).</li>
 *   <li><b>Fase 2</b> (66-33 %): <i>Het grotje</i>: it sinks into its grotto and is IMMUNE until the 3 grey polyps on the lagoon
 *       floor are broken (dive!). Pink bubble rings on the floor refill your air and heal 3 %. Every 6 s a purple undertow pulls
 *       swimmers toward the grotto (8 % close to it). All polyps gone: it comes up gasping, 5 s stunned, +25 % damage taken.</li>
 *   <li><b>Fase 3</b> (33-0 %): <i>Golfslag</i> = a red ring of 9 around itself (1.4 s): 20 % to everyone inside near the surface
 *       (swim away or dive under); <i>Samen aaien</i> = a yellow stack circle (3 s): enough players inside (2, solo 1) = it giggles,
 *       4 s stunned and +25 % damage; too few = 15 % on everyone.</li>
 * </ul>
 * Beaten it is cheered up: it turns pink (texture zeeslurf_blij), sings and floats for 10 s (moment "zeeslurf_opgevrolijkt").
 */
public class Zeeslurf extends Z4Baas {
    private static final EntityDataAccessor<Boolean> BLIJ = SynchedEntityData.defineId(Zeeslurf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ONDER = SynchedEntityData.defineId(Zeeslurf.class, EntityDataSerializers.BOOLEAN);

    private enum Actie {
        GEEN, SPUIT, WOLK, GROTJE, HAPT, GOLF, AAIEN
    }

    private Actie actie = Actie.GEEN;
    private int actieT, spuitCd = 40, wolkCd = 100, golfCd = 60, aaiCd = 200, zuigT = 120, bubbelT;
    private int verdoofdT;
    private boolean extraSchade;
    @Nullable
    private Vec3 spuitNaar;
    private final List<UUID> polypen = new ArrayList<>();
    /** Gametests: polyps broken this fight. */
    public int polypenKapot;

    public Zeeslurf(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(BLIJ, false);
        b.define(ONDER, false);
    }

    /** Client: cheered up (pink texture). */
    public boolean blij() {
        return entityData.get(BLIJ) || geplofd();
    }

    /** In its grotto (fase 2, immune). */
    public boolean inGrotje() {
        return entityData.get(ONDER);
    }

    @Override
    public String baasId() {
        return "zeeslurf";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"spuit", "wolk", "duik", "golf", "giechel"};
    }

    @Override
    protected double hp(int level) {
        return 7000 + 260 * level;
    }

    @Override
    protected double klap(int level) {
        return 18 + 2.2 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(mobLevel());
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    @Override
    protected void registerGoals() {
        // (no FloatGoal: it swims where it wants)
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected double bereik() {
        return 999;   // (the engine never walks it: it swims by itself in vecht)
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN || verdoofdT > 0;
    }

    @Override
    protected boolean immuun() {
        return actie == Actie.GROTJE;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z4bazen.zeeslurf.immuun";
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return extraSchade ? 1.25 : 1;
    }

    @Override
    protected int plofDuur() {
        return 200;   // it stays, pink and singing, for 10 s
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (distanceTo(d) < getBbWidth() / 2 + 2.5) {
            speel("spuit");
            raak(d, 1.0, "slurfklap");
        }
    }

    // ---- the moves ---------------------------------------------------------------------------------------------------------

    private void start(Actie a, int t) {
        actie = a;
        actieT = t;
        rustT = t + 20;
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            kaart("vadsrijk.z4bazen.zeeslurf.fase2", PAARS);
            spreek("vadsrijk.z4bazen.zeeslurf.zeg.grotje");
            grotjeIn(sl);
        } else if (fase == 3) {
            if (actie == Actie.GROTJE) {
                grotjeUit(sl);
            }
            kaart("vadsrijk.z4bazen.zeeslurf.fase3", ROZE);
            spreek("vadsrijk.z4bazen.zeeslurf.zeg.golf");
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty() || actie != Actie.GEEN || verdoofdT > 0) {
            return;
        }
        ServerPlayer d = iemandOpAfstand() ? afstandsDoel() : zDoel();
        if (d == null) {
            d = l.get(getRandom().nextInt(l.size()));
        }
        if (fase() >= 3 && aaiCd <= 0 && l.size() >= 1) {
            Vec3 v = new Vec3(getX() + getRandom().nextInt(7) - 3, Math.min(getY() + 1, zee()), getZ() + getRandom().nextInt(7) - 3);
            start(Actie.AAIEN, tg(60));
            aaiCd = 500;
            zeg("vadsrijk.z4bazen.zeeslurf.aaien", ChatFormatting.YELLOW);
            Z4Vfx.cast(this, "samen_aaien", tg(60));
            stapel(v, 3, 60, 0.4, 0.15, 2, "samen_aaien", ok -> {
                actie = Actie.GEEN;
                if (ok) {
                    verdoofd(sl, 80, "vadsrijk.z4bazen.zeeslurf.giechel");
                }
            });
            laatsteAanval = "aaien";
            return;
        }
        if (fase() >= 3 && golfCd <= 0) {
            Telegraaf(sl, position().with(net.minecraft.core.Direction.Axis.Y, zee()), 9, tg(28), ROOD);
            start(Actie.GOLF, tg(28));
            Z4Vfx.cast(this, "golfslag", tg(28));   // T-VFX one-liners (reflection: active after the merge)
            Z4Vfx.windup(this, tg(28));
            golfCd = 200;
            speel("golf");
            zeg("vadsrijk.z4bazen.zeeslurf.golf", ChatFormatting.RED);
            laatsteAanval = "golf";
            return;
        }
        if (wolkCd <= 0 && getRandom().nextInt(3) == 0) {
            int n = Math.min(3, Math.max(2, l.size()));
            for (int i = 0; i < n; i++) {
                ServerPlayer p = l.get(getRandom().nextInt(l.size()));
                Vec3 plek = voetVan(p);
                inslag(plek, 3, 24, 0.12, "mokwolk", null, new DustParticleOptions(0x6A6878, 2.2f));
                later(tg(24) + 1, () -> {
                    for (ServerPlayer q : inCirkel(plek, 3)) {
                        q.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                    }
                });
            }
            start(Actie.WOLK, tg(24));
            wolkCd = 160;
            speel("wolk");
            laatsteAanval = "mokwolk";
            return;
        }
        if (spuitCd <= 0) {
            Vec3 van = new Vec3(getX(), d.getY(), getZ());
            Vec3 r = d.position().subtract(van).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
            spuitNaar = van.add(r.scale(Math.max(12, d.position().distanceTo(van) + 4)));
            Z4Vfx.lijn(sl, van, spuitNaar, 2.5, tg(20), ROOD);
            start(Actie.SPUIT, tg(20));
            Z4Vfx.windup(this, tg(20));
            spuitCd = fase() >= 3 ? 70 : 90;
            speel("spuit");
            laatsteAanval = "slurfspuit";
            return;
        }
        rustT = 10;
    }

    /** (a tiny alias so the ring reads like the zone-3 bosses) */
    private void Telegraaf(ServerLevel sl, Vec3 c, double r, int t, int kleur) {
        Z4Vfx.cirkel(sl, c, r, t, kleur);
    }

    private void verdoofd(ServerLevel sl, int ticks, String key) {
        verdoofdT = ticks;
        extraSchade = true;
        speel("giechel");
        zeg(key, ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.DOLPHIN_PLAY, SoundSource.HOSTILE, 3f, 0.6f);
    }

    // ---- fase 2: the grotto and its polyps ----------------------------------------------------------------------------------

    private void grotjeIn(ServerLevel sl) {
        start(Actie.GROTJE, 99999);
        entityData.set(ONDER, true);
        speel("duik");
        polypen.clear();
        Vec3 g = grotje();
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + 0.4;
            Vec3 v = g.add(Math.cos(a) * 5, 0, Math.sin(a) * 5);
            v = new Vec3(v.x, bodem(sl, v), v.z);
            if (Z4BazenRegistry.POLIEP.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                d.koppel(this, i, maxVads() * 0.05, v);
                sl.addFreshEntity(d);
                polypen.add(d.getUUID());
            }
        }
        zeg("vadsrijk.z4bazen.zeeslurf.duik", ChatFormatting.AQUA);
    }

    private void grotjeUit(ServerLevel sl) {
        actie = Actie.GEEN;
        entityData.set(ONDER, false);
        for (UUID u : polypen) {
            if (sl.getEntity(u) instanceof Z3Doel d) {
                d.discard();
            }
        }
        polypen.clear();
        Z4Vfx.plons(sl, position().add(0, 2, 0), 3);
        verdoofd(sl, 100, "vadsrijk.z4bazen.zeeslurf.adem");
        rustT = 100;
    }

    private double zeeY = Double.NaN;

    /**
     * The water surface (y of the top water block above the fight's middle), found once by looking for the water; without water
     * there (a misplaced arena) the POI height relative to the arena centre.
     */
    public double zee() {
        if (Double.isNaN(zeeY) && level() instanceof ServerLevel sl) {
            Vec3 m = zMidden();
            BlockPos.MutableBlockPos b = new BlockPos.MutableBlockPos();
            for (int y = net.minecraft.util.Mth.floor(m.y) + 16; y > m.y - 30; y--) {
                b.set(net.minecraft.util.Mth.floor(m.x), y, net.minecraft.util.Mth.floor(m.z));
                if (!sl.getFluidState(b).isEmpty()) {
                    zeeY = y;
                    break;
                }
            }
            if (Double.isNaN(zeeY)) {
                zeeY = m.y + (Z4BazenPlekken.ZEE_Y - Z4BazenPlekken.ZEESLURF_CENTRUM.getY());
            }
        }
        return Double.isNaN(zeeY) ? Z4BazenPlekken.ZEE_Y : zeeY;
    }

    /** The grotto: the same offset from the fight's middle as the POI grotto from the arena centre (also right in a test). */
    public Vec3 grotje() {
        BlockPos o = Z4BazenPlekken.ZEESLURF_GROTJE.subtract(Z4BazenPlekken.ZEESLURF_CENTRUM);
        return zMidden().add(o.getX(), o.getY(), o.getZ());
    }

    /** The lagoon floor under a point (first solid block going down from the surface). */
    private double bodem(ServerLevel sl, Vec3 v) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = (int) zee(); y > zee() - 32; y--) {
            m.set(net.minecraft.util.Mth.floor(v.x), y - 1, net.minecraft.util.Mth.floor(v.z));
            if (!sl.getBlockState(m).getCollisionShape(sl, m).isEmpty()) {
                return y;
            }
        }
        return v.y;
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        polypenKapot++;
        Z4Vfx.feest(sl, d.position().add(0, 0.5, 0), 1.2);
        sl.playSound(null, d.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE, 2f, 1.2f);
        d.discard();
        polypen.remove(d.getUUID());
        int over = polypen.size();
        zeg("vadsrijk.z4bazen.zeeslurf.poliep", ChatFormatting.LIGHT_PURPLE, over);
        if (over == 0 && actie == Actie.GROTJE) {
            grotjeUit(sl);
        }
    }

    // ---- the per-tick logic --------------------------------------------------------------------------------------------------

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        spuitCd--;
        wolkCd--;
        golfCd--;
        aaiCd--;
        if (verdoofdT > 0 && --verdoofdT == 0) {
            extraSchade = false;
        }
        zwem(sl);
        if (actie == Actie.GROTJE) {
            grotjeTick(sl, l);
            return;
        }
        if (actie == Actie.GEEN || --actieT > 0) {
            return;
        }
        switch (actie) {
            case SPUIT -> {
                Vec3 van = new Vec3(getX(), spuitNaar.y, getZ());
                Z4Vfx.wind(sl, van, spuitNaar);
                sl.sendParticles(ParticleTypes.SPLASH, spuitNaar.x, spuitNaar.y + 0.5, spuitNaar.z, 40, 1, 0.3, 1, 0.2);
                sl.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 2f, 1.6f);
                for (ServerPlayer p : l) {
                    if (inBaan(p.position(), van, spuitNaar, 2.5) && Math.abs(p.getY() - spuitNaar.y) < 4) {
                        mech(p, 0.18, "slurfspuit");
                        duw(p, van, 1.0);
                    }
                }
            }
            case GOLF -> {
                Z4Vfx.plons(sl, position().with(net.minecraft.core.Direction.Axis.Y, zee() + 0.5), 5);
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 3f, 0.5f);
                Z4Vfx.treffer(this, position().with(net.minecraft.core.Direction.Axis.Y, zee() + 1), 9, ROOD);
                for (ServerPlayer p : l) {
                    double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                    if (dx * dx + dz * dz <= 81 && p.getY() > zee() - 3) {   // dive under the wave = safe
                        mech(p, 0.20, "golfslag");
                        duw(p, position(), 1.2);
                    }
                }
            }
            default -> {
            }
        }
        if (actie != Actie.AAIEN) {
            actie = Actie.GEEN;
        }
    }

    /** Fase 2: bubble rings for air, the undertow, a polyp count on the bar. */
    private void grotjeTick(ServerLevel sl, List<ServerPlayer> l) {
        if (--bubbelT <= 0) {
            bubbelT = 80;
            for (UUID u : polypen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    Vec3 b = d.position().add(2.2, 0, 0);
                    Z4Vfx.cirkel(sl, b, 1.8, 80, ROZE);
                }
            }
        }
        if (tickCount % 5 == 0) {
            for (UUID u : polypen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    Vec3 b = d.position().add(2.2, 0, 0);
                    sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, b.x, b.y + 0.5, b.z, 6, 0.6, 0.6, 0.6, 0.05);
                    for (ServerPlayer p : l) {
                        if (p.position().distanceTo(b) < 2.4) {
                            p.setAirSupply(p.getMaxAirSupply());
                            if (tickCount % 20 == 0) {
                                heelSpeler(p, 0.03);
                            }
                        }
                    }
                }
            }
        }
        if (--zuigT <= 0) {
            zuigT = 120;
            Vec3 g = grotje();
            Z4Vfx.cirkel(sl, g, 6, 20, PAARS);
            later(20, () -> {
                for (ServerPlayer p : strijders()) {
                    if (p.isInWater() && p.position().distanceTo(g) < 16) {
                        Vec3 r = g.subtract(p.position()).normalize();
                        p.push(r.x * 0.5, r.y * 0.3, r.z * 0.5);
                        p.hurtMarked = true;
                        if (p.position().distanceTo(g) < 3.5) {
                            mech(p, 0.08, "zuigstroom");
                        }
                    }
                }
                sl.playSound(null, g.x, g.y, g.z, SoundEvents.CONDUIT_AMBIENT, SoundSource.HOSTILE, 2f, 0.6f);
            });
        }
    }

    private void heelSpeler(ServerPlayer p, double f) {
        nl.juiced.guhs.feature.vadsrijk.kern.Schade.heal(null, p, nl.juiced.guhs.feature.vadsrijk.kern.Spelers.maxVads(p) * f);
        if (p.level() instanceof ServerLevel sl) {
            Getallen.tekst(sl, p.getX(), p.getY() + 2.1, p.getZ(), net.minecraft.network.chat.Component.translatable("vadsrijk.z4bazen.zeeslurf.lucht"), 0.6f);
        }
    }

    /** Swims: toward its target (or its grotto in fase 2), always in the lagoon water, never out of the arena. */
    private void zwem(ServerLevel sl) {
        Vec3 doel;
        ServerPlayer d = zDoel();
        if (actie == Actie.GROTJE) {
            doel = grotje().add(0, 0.5, 0);
        } else if (verdoofdT > 0 || d == null) {
            doel = position();
        } else {
            Vec3 naar = d.position().subtract(position()).multiply(1, 0, 1);
            double stop = getBbWidth() / 2 + 1.5;
            doel = naar.length() < stop ? position() : d.position().subtract(naar.normalize().scale(stop));
            doel = new Vec3(doel.x, d.getY() - 1, doel.z);
        }
        Vec3 m = zMidden();
        Vec3 r = doel.subtract(m).multiply(1, 0, 1);
        double max = zStraal() - 4;
        if (r.length() > max) {
            doel = m.add(r.normalize().scale(max)).add(0, doel.y - m.y, 0);
        }
        double laag = bodem(sl, doel) + 0.3, hoog = zee() - getBbHeight() * 0.5;
        doel = new Vec3(doel.x, Math.max(laag, Math.min(hoog, doel.y)), doel.z);
        Vec3 v = doel.subtract(position());
        double snel = actie == Actie.GROTJE ? 0.25 : 0.12;
        Vec3 stap = v.lengthSqr() > snel * snel ? v.normalize().scale(snel) : v;
        setDeltaMovement(getDeltaMovement().scale(0.5).add(stap.scale(0.5)));
        if (stap.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-stap.x, stap.z));
            setYRot(yaw);
            setYBodyRot(yaw);
        }
        if (tickCount % 12 == 0) {
            sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + getBbHeight() * 0.6, getZ(), 6, getBbWidth() * 0.4, 0.4, getBbWidth() * 0.4, 0.02);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : polypen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void opPlof() {
        entityData.set(BLIJ, true);
        entityData.set(ONDER, false);
        super.opPlof();
        if (level() instanceof ServerLevel sl) {
            zegAlle("vadsrijk.z4bazen.zeeslurf.blij", ChatFormatting.LIGHT_PURPLE);
            sl.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 3f, 0.6f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (geplofd() && level() instanceof ServerLevel sl && tickCount % 20 == 0) {   // it sings while it floats up, pink and happy
            sl.sendParticles(ParticleTypes.NOTE, getX(), getY() + getBbHeight() + 0.5, getZ(), 3, 1.2, 0.4, 1.2, 1);
            setDeltaMovement(0, 0.02, 0);
        }
    }

    /** Is p within the strip a->b of width w (flat)? */
    public static boolean inBaan(Vec3 p, Vec3 a, Vec3 b, double w) {
        Vec3 ab = b.subtract(a).multiply(1, 0, 1), ap = p.subtract(a).multiply(1, 0, 1);
        double l2 = ab.lengthSqr();
        if (l2 < 1e-4) {
            return false;
        }
        double t = ap.dot(ab) / l2;
        return t >= 0 && t <= 1 && ap.subtract(ab.scale(t)).length() <= w / 2;
    }
}
