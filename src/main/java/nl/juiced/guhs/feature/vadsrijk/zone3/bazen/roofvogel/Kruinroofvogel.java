package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.roofvogel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Vast;

/**
 * De Kruinroofvogel, the world boss of the Kale Kruin (lv 38, optional for the main quest, respawn 25 min): a huge grey raptor
 * with rotten feathers that took over the old nest.
 * <ul>
 *   <li>Fase 1: dives along red strips (preferring the player at range: he dives at them), a wing gust that blows everyone toward
 *       the edge (purple ring), talon strikes.</li>
 *   <li>Fase 2 (70 %): Grijpklauw: a purple circle on a player; whoever is still in it is snatched and carried up. Teammates shoot
 *       or hit him free (4 % of his Vads while he carries), solo you click/shift yourself loose; else he drops you after 6 s
 *       (35 %, slow falling: no fall damage).</li>
 *   <li>Fase 3 (35 %): De Achtervolging: he flees to one of his 2 other perches on the crown (~200 blocks), a pink light trail
 *       shows which. Mounts are ALLOWED in his arena during this phase ({@link #jacht}); whoever gets there fast gets 10 s
 *       Vahoeg. The fight goes on at the perch; on foot you can still join.</li>
 * </ul>
 */
public class Kruinroofvogel extends Z3Baas {
    private static final EntityDataAccessor<Boolean> VLIEGT = SynchedEntityData.defineId(Kruinroofvogel.class, EntityDataSerializers.BOOLEAN);
    /** The perch he fled to (null = the Kale Kruin). */
    @Nullable
    private BlockPos zit;
    private long jachtStart = -1;
    private boolean vluchtBezig;
    private final Set<UUID> snel = new HashSet<>(), duikGeraakt = new HashSet<>();

    public enum Actie {
        GEEN, DUIK, WIND, GRIJP, DRAAGT, VLUCHT
    }

    private Actie actie = Actie.GEEN;
    private int actieT, duikCd = 50, windCd = 140, grijpCd = 120;
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    @Nullable
    private UUID gegrepen;
    private double draagSchade;
    /** Gametests: grabs done / freed. */
    public int grepen, bevrijd;

    /** True while any Kruinroofvogel is fleeing / being chased: riding is allowed in his arena (Rijdieren exception). */
    public static volatile boolean jachtActief;

    public Kruinroofvogel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VLIEGT, false);
    }

    public boolean vliegt() {
        return entityData.get(VLIEGT);
    }

    private void zetVliegt(boolean v) {
        entityData.set(VLIEGT, v);
        setNoGravity(v);
    }

    public Actie actie() {
        return actie;
    }

    public boolean jacht() {
        return zit != null;
    }

    @Nullable
    public BlockPos zit() {
        return zit;
    }

    @Override
    public String baasId() {
        return "kruinroofvogel";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"duik", "wind", "grijp"};
    }

    @Override
    protected double hp(int level) {
        return 6000 + 240 * level;
    }

    @Override
    protected double klap(int level) {
        return 18 + 2.1 * level;
    }

    @Override
    public int xp() {
        return nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Xp.baas(Math.min(50, mobLevel()));
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.7, 0.35};
    }

    @Override
    protected double jaagSnelheid() {
        return 1.2;
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN;
    }

    @Override
    protected boolean immuun() {
        return actie == Actie.VLUCHT;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z3bazen.kr.vlucht_immuun";
    }

    // ---- the fight moves to the perch in fase 3 -------------------------------------------------------------------------

    @Override
    public Vec3 zMidden() {
        if (actie == Actie.VLUCHT) {
            return position();   // (no leash while he flies over)
        }
        return zit != null ? Vec3.atBottomCenterOf(zit) : super.zMidden();
    }

    @Override
    public double zStraal() {
        return zit != null ? Z3BazenPlekken.ZIT_STRAAL : super.zStraal();
    }

    @Override
    public boolean zDoetMee(Player p) {
        if (p.isSpectator() || !p.isAlive()) {
            return false;
        }
        if (zit != null) {
            Vec3 m = Vec3.atBottomCenterOf(zit);
            double dx = p.getX() - m.x, dz = p.getZ() - m.z;
            if (dx * dx + dz * dz <= Math.pow(Z3BazenPlekken.ZIT_STRAAL, 2) && Math.abs(p.getY() - m.y) < 24) {
                return true;
            }
            return actie == Actie.VLUCHT && arena() != null && arena().binnen(p.getX(), p.getY(), p.getZ());
        }
        return super.zDoetMee(p);
    }

    @Override
    public boolean inGevecht(Player p) {
        return zDoetMee(p);
    }

    @Override
    public void schaal(int spelers) {
        super.schaal(zit != null ? Math.max(spelers, strijders().size()) : spelers);
    }

    // ---- the moves --------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 3f, 0.5f);
        if (fase == 2) {
            kaart("vadsrijk.z3bazen.kr.fase2", PAARS);
            grijpCd = 40;
        } else if (fase == 3) {
            vlucht(sl);
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        ServerPlayer d = zDoel();
        if (fase() >= 2 && grijpCd <= 0) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : l.get(getRandom().nextInt(l.size()));
            if (v != null) {
                naar = voetVan(v);
                gegrepen = v.getUUID();
                Telegraaf.cirkel(sl, naar, 2.2, tg(28), PAARS);
                start(Actie.GRIJP, tg(28));
                grijpCd = 260;
                speel("grijp");
                zeg("vadsrijk.z3bazen.kr.grijp", ChatFormatting.LIGHT_PURPLE, v.getDisplayName());
                laatsteAanval = "grijp";
                return;
            }
        }
        if (windCd <= 0 && getRandom().nextInt(3) == 0) {
            Telegraaf.cirkel(sl, position(), 13, tg(30), PAARS);
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, tg(30));   // T-VFX: wind-up animation (held until the hit / auto-release)
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "vleugelslag", tg(30));   // T-VFX: cast bar
            start(Actie.WIND, tg(30));
            windCd = 220;
            speel("wind");
            zeg("vadsrijk.z3bazen.kr.wind", ChatFormatting.AQUA);
            laatsteAanval = "wind";
            return;
        }
        if (duikCd <= 0) {
            ServerPlayer v = iemandOpAfstand() || getRandom().nextBoolean() ? afstandsDoel() : d;
            if (v != null) {
                van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
                naar = voetVan(van.add(r.scale(Math.max(10, voetVan(v).distanceTo(van) + 5))));
                Telegraaf.lijn(sl, van, naar, 3.5, tg(30), ROOD);
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, tg(30));   // T-VFX: wind-up animation (held until the hit / auto-release)
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "duikvlucht", tg(30));   // T-VFX: cast bar
                start(Actie.DUIK, tg(30) + 14);
                duikGeraakt.clear();
                duikCd = fase() >= 3 ? 90 : 120;
                zetVliegt(true);
                speel("duik");
                laatsteAanval = "duik";
                return;
            }
        }
        rustT = 10;
    }

    private void start(Actie a, int t) {
        actie = a;
        actieT = t;
        getNavigation().stop();
        rustT = t + 20;
    }

    /** (K-INT) The chase flight goes on while nobody has reached the perch yet (vecht only loops over l there). */
    @Override
    protected boolean vechtZonderStrijders() {
        return zit != null && jachtStart >= 0;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        duikCd--;
        windCd--;
        grijpCd--;
        if (zit != null && jachtStart >= 0) {
            vluchtSpoor(sl);
            // whoever gets to the perch fast gets 10 s Vahoeg
            if (sl.getGameTime() - jachtStart < 20 * 32) {
                for (ServerPlayer p : l) {
                    if (snel.add(p.getUUID()) && p.distanceToSqr(Vec3.atBottomCenterOf(zit)) < Math.pow(Z3BazenPlekken.ZIT_STRAAL, 2)) {
                        Statussen.geef(p, Status.VAHOEG, 200, 0.25, p);
                        p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z3bazen.kr.snel").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
        }
        if (actie == Actie.GEEN) {
            return;
        }
        actieT--;
        switch (actie) {
            case DUIK -> {
                int duur = 14;
                if (actieT < duur) {
                    double f = 1 - (double) actieT / duur;
                    Vec3 v = van.lerp(naar, f).add(0, 1.5 * Math.sin(Math.PI * f), 0);
                    setPos(v.x, v.y, v.z);
                    setDeltaMovement(Vec3.ZERO);
                    sl.sendParticles(new DustParticleOptions(0x8A8A96, 2.0f), getX(), getY() + 1, getZ(), 6, 1, 0.5, 1, 0.01);
                    for (ServerPlayer p : l) {
                        if (p.distanceToSqr(this) < 2.6 * 2.6 && !Z3Vast.vast(p) && duikGeraakt.add(p.getUUID())) {
                            mech(p, 0.35, "duik");
                            duw(p, position(), 1.0);
                        }
                    }
                }
                if (actieT <= 0) {
                    actie = Actie.GEEN;
                    zetVliegt(false);
                }
            }
            case WIND -> {
                if (actieT <= 0) {
                    actie = Actie.GEEN;
                    sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 80, 6, 0.5, 6, 0.3);
                    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.7f);
                    for (ServerPlayer p : l) {
                        if (p.distanceToSqr(this) < 13 * 13) {
                            mech(p, 0.15, "wind");
                            duw(p, position(), 2.2);
                        }
                    }
                }
            }
            case GRIJP -> {
                if (actieT <= 0) {
                    ServerPlayer p = gegrepen != null && sl.getEntity(gegrepen) instanceof ServerPlayer sp ? sp : null;
                    if (p != null && strijders().contains(p) && p.position().distanceToSqr(naar.x, p.getY(), naar.z) < 2.4 * 2.4) {
                        pak(sl, p);
                    } else {
                        actie = Actie.GEEN;
                        zeg("vadsrijk.z3bazen.kr.mis", ChatFormatting.GREEN);
                    }
                }
            }
            case DRAAGT -> {
                ServerPlayer p = gegrepen != null && sl.getEntity(gegrepen) instanceof ServerPlayer sp ? sp : null;
                if (p == null || !Z3Vast.vast(p)) {
                    land(sl);
                    return;
                }
                // up he goes, circling
                double f = Math.min(1, (120 - actieT) / 40.0);
                Vec3 m = zMidden();
                double a = tickCount * 0.05;
                Vec3 v = new Vec3(m.x + Math.cos(a) * 5, m.y + 1 + f * 9, m.z + Math.sin(a) * 5);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                if (actieT % 10 == 0) {
                    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2f, 1.1f);
                }
            }
            case VLUCHT -> {
                // the flight to the perch: up, over, down (~10 s)
                int duur = 200;
                double f = 1 - Math.max(0, (double) actieT / duur);
                Vec3 m = Vec3.atBottomCenterOf(zit);
                // (P-VERHUIS) the revamped tree stands between the Kale Kruin and the perches: fly round it (a bend outside the crown)
                Vec3 v = Kruinroofvogel.omDeBoom(van, m, f);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                if (actieT % 4 == 0) {
                    sl.sendParticles(new DustParticleOptions(ROZE, 2.4f), getX(), getY() + 1, getZ(), 6, 0.8, 0.5, 0.8, 0);
                }
                if (actieT % 12 == 0) {
                    sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.9f);
                }
                if (actieT <= 0) {
                    setPos(m.x, m.y, m.z);
                    actie = Actie.GEEN;
                    zetVliegt(false);
                    vluchtBezig = false;
                    jachtActief = false;
                    kaart("vadsrijk.z3bazen.kr.geland", ROZE);
                    rustT = 30;
                }
            }
            default -> actie = Actie.GEEN;
        }
    }

    private void pak(ServerLevel sl, ServerPlayer p) {
        grepen++;
        draagSchade = 0;
        start(Actie.DRAAGT, 120);
        zetVliegt(true);
        Z3Vast.pak(this, p, "klauw", () -> position().add(0, -2.6, 0), 8, 120, 0.04, () -> {
            // not freed in time: he drops you
            mech(p, 0.35, "val");
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z3bazen.kr.losgelaten").withStyle(ChatFormatting.RED));
        });
        sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 2f, 0.6f);
    }

    private void land(ServerLevel sl) {
        actie = Actie.GEEN;
        zetVliegt(false);
        gegrepen = null;
        Vec3 m = voetVan(position());
        zSpring(new Vec3(m.x, zMidden().y, m.z), 16, 2);
        rustT = 40;
    }

    @Override
    public void losgekomen(ServerPlayer p, String soort) {
        bevrijd++;
        if (actie == Actie.DRAAGT && level() instanceof ServerLevel sl) {
            land(sl);
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        double voor = vads();
        super.krijgSchade(n, bron);
        if (actie == Actie.DRAAGT && !geplofd()) {
            draagSchade += voor - vads();
            if (draagSchade >= maxVads() * 0.04 && gegrepen != null && level() instanceof ServerLevel sl && sl.getEntity(gegrepen) instanceof ServerPlayer p) {
                Z3Vast.los(p);
                bevrijd++;
                zeg("vadsrijk.z3bazen.kr.losgeschoten", ChatFormatting.GREEN);
                Statussen.geef(this, Status.VERDOOFD, 40, 1, null);
                land(sl);
            }
        }
    }

    /** Fase 3: he flees to one of his other perches. */
    public void vlucht(ServerLevel sl) {
        if (gegrepen != null && sl.getEntity(gegrepen) instanceof ServerPlayer p) {
            Z3Vast.los(p);
        }
        zit = getRandom().nextBoolean() ? Z3BazenPlekken.ROOF_ZIT_1 : Z3BazenPlekken.ROOF_ZIT_2;
        van = position();
        jachtStart = sl.getGameTime();
        jachtActief = true;
        vluchtBezig = true;
        snel.clear();
        start(Actie.VLUCHT, 200);
        zetVliegt(true);
        kaart("vadsrijk.z3bazen.kr.fase3", ROZE);
        zeg("vadsrijk.z3bazen.kr.achtervolg", ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 3f, 0.5f);
    }

    /** The pink light trail from the Kale Kruin to the perch (30 s). */
    private void vluchtSpoor(ServerLevel sl) {
        long t = sl.getGameTime() - jachtStart;
        if (t > 20 * 30 || t % 10 != 0 || zit == null) {
            return;
        }
        Vec3 a = Vec3.atBottomCenterOf(Z3BazenPlekken.ROOF_CENTRUM).add(0, 2, 0), b = Vec3.atBottomCenterOf(zit).add(0, 2, 0);
        double len = a.distanceTo(b);
        DustParticleOptions roze = new DustParticleOptions(ROZE, 2.6f);
        for (double s = 0; s < len; s += 2.5) {
            Vec3 v = a.lerp(b, s / len).add(0, 6 * Math.sin(Math.PI * s / len), 0);
            sl.sendParticles(roze, v.x, v.y, v.z, 1, 0.1, 0.1, 0.1, 0);
        }
    }

    /** Is riding allowed here right now? (the chase: in his arena while he flees, so you can mount up and follow). */
    public static boolean magRijden(Vec3 v) {
        if (!jachtActief) {
            return false;
        }
        BlockPos c = Z3BazenPlekken.ROOF_CENTRUM;
        double dx = v.x - c.getX(), dz = v.z - c.getZ();
        return dx * dx + dz * dz <= Math.pow(Z3BazenPlekken.ROOF_STRAAL + 4, 2) && Math.abs(v.y - c.getY()) < 30;
    }

    @Override
    protected void zLeeg() {
        jachtActief = false;
        super.zLeeg();
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd()) {
            jachtActief = false;
            zetVliegt(false);
            if (level() instanceof ServerLevel sl) {
                Z3Vast.losAlles(getUUID(), sl.getServer());
                zeg("vadsrijk.z3bazen.kr.verslagen", ChatFormatting.GOLD);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (vluchtBezig) {
            jachtActief = false;
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk"), vlieg = RawAnimation.begin().thenLoop("vlieg");
        controllers.add(new AnimationController<Kruinroofvogel>("beweeg", 4,
                state -> state.setAndContinue(state.animatable().vliegt() ? vlieg : state.isMoving() ? walk : idle)));
        AnimationController<Kruinroofvogel> a = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String n : actieAnimaties()) {
            a.triggerableAnim(n, RawAnimation.begin().thenPlay(n));
        }
        controllers.add(a);
    }

    /** (P-VERHUIS) The chase flight from a to b round the tree: a quadratic curve through a point outside the crown (r >= 150) at the
     *  middle angle, high above both ends, so he never flies through the trunk or the crown storeys. */
    static Vec3 omDeBoom(Vec3 a, Vec3 b, double f) {
        double sx = nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken.STAM.getX(), sz = nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken.STAM.getZ();
        double aa = Math.atan2(a.z - sz, a.x - sx), ab = Math.atan2(b.z - sz, b.x - sx);
        double d = Math.atan2(Math.sin(ab - aa), Math.cos(ab - aa));
        double mid = aa + d / 2, r = Math.max(150, Math.max(Math.hypot(a.x - sx, a.z - sz), Math.hypot(b.x - sx, b.z - sz)) + 20);
        Vec3 c = new Vec3(sx + Math.cos(mid) * r * 1.3, Math.max(a.y, b.y) + 30, sz + Math.sin(mid) * r * 1.3);
        double g = 1 - f;
        return a.scale(g * g).add(c.scale(2 * g * f)).add(b.scale(f * f));
    }
}
