package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.hart;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenApi;

/**
 * De Oude Nestwachter (Hart van de Boom boss 2, lv 49, Natuur/Roze): an ancient tree-folk guardian of wood and Amberroze, gone
 * grey. He guards the egg against everyone.
 * <ul>
 *   <li>Jaarring-golven: the floor is year rings that light up in turns: stand on a PINK ring when the wave goes (the red ones take
 *       40 %). Fase 2 faster; fase 3 two waves in a row that swap.</li>
 *   <li>Schouderstoot: a red strip, then he charges and shoves whoever is hit far toward the edge (30 %). Stamp around himself.</li>
 *   <li>The end: he is not destroyed. He turns pink again, bows, and lets you through to the egg (the hook "nestwachter_gered" for
 *       the npc slice: the Nestwachter NPC and the fifth heart splinter).</li>
 * </ul>
 */
public class Nestwachter extends Z3Baas {
    private static final EntityDataAccessor<Boolean> ROZE_WEER = SynchedEntityData.defineId(Nestwachter.class, EntityDataSerializers.BOOLEAN);
    public static final double RING_BREED = 3;
    private int golfCd = 80, stootCd = 120, stampCd = 60, tweede = -1;
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    private int stootT = -1;
    private boolean laatsteEven;
    private final Set<UUID> stootGeraakt = new HashSet<>();
    /** Gametests: waves done, last wave's red-ring hits. */
    public int golven, laatsteGeraakt;

    public Nestwachter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ROZE_WEER, false);
    }

    public boolean rozeWeer() {
        return entityData.get(ROZE_WEER);
    }

    @Override
    public String baasId() {
        return "nestwachter";
    }

    @Override
    public String kerker() {
        return "hart";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"stoot", "stamp", "buig"};
    }

    @Override
    protected double hp(int level) {
        return 8000 + 310 * level;
    }

    @Override
    protected double klap(int level) {
        return 20 + 2.3 * level;
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
        return new double[] {0.66, 0.33};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.9;
    }

    /** Which ring (0 = the middle) a spot is on. */
    public int ring(Vec3 v) {
        Vec3 m = zMidden();
        double d = Math.sqrt(Math.pow(v.x - m.x, 2) + Math.pow(v.z - m.z, 2));
        return (int) (d / RING_BREED);
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z3bazen.nw.fase" + fase, fase == 3 ? ROOD : PAARS);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (golfCd <= 0) {
            golf(sl, getRandom().nextBoolean());
            if (fase() >= 3) {
                tweede = tg(60) + 10;
            }
            golfCd = fase() >= 2 ? 170 : 220;
            rustT = 50;
            laatsteAanval = "golf";
            return;
        }
        if (stootCd <= 0) {
            ServerPlayer v = iemandOpAfstand() || getRandom().nextBoolean() ? afstandsDoel() : zDoel();
            if (v != null) {
                van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                naar = voetVan(van.add(r.scale(Math.max(8, voetVan(v).distanceTo(van) + 3))));
                Telegraaf.lijn(sl, van, naar, 3, tg(28), ROOD);
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, tg(28));   // T-VFX: wind-up animation (held until the hit / auto-release)
                stootT = tg(28) + 12;
                stootGeraakt.clear();
                speel("stoot");
                stootCd = 150;
                rustT = stootT + 15;
                laatsteAanval = "stoot";
                return;
            }
        }
        if (stampCd <= 0 && zDoel() != null && zDoel().distanceTo(this) < 6) {
            speel("stamp");
            inslag(position(), 4, 22, 0.3, "stamp", Status.PLAKKERIG, ParticleTypes.CRIT);
            stampCd = 100;
            rustT = 30;
            laatsteAanval = "stamp";
            return;
        }
        rustT = 10;
    }

    /** A year-ring wave: the even (or odd) rings are pink = safe, the others red. */
    public void golf(ServerLevel sl, boolean evenVeilig) {
        int w = tg(60);
        int ringen = (int) Math.ceil(zStraal() / RING_BREED);
        Vec3 m = zMidden();
        for (int i = 0; i < ringen; i++) {
            boolean veilig = (i % 2 == 0) == evenVeilig;
            double r = Math.max(0.6, i * RING_BREED + RING_BREED / 2);
            Telegraaf.cirkel(sl, m, r, w, veilig ? ROZE : ROOD);
        }
        zeg("vadsrijk.z3bazen.nw.golf", ChatFormatting.LIGHT_PURPLE);
        later(w, () -> {
            golven++;
            laatsteGeraakt = 0;
            for (ServerPlayer p : strijders()) {
                int r = ring(p.position());
                if ((r % 2 == 0) != evenVeilig && Math.abs(p.getY() - m.y) < 4) {
                    mech(p, 0.4, "jaarring");
                    laatsteGeraakt++;
                }
            }
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.5f);
            for (int i = 0; i < ringen; i++) {
                boolean veilig = (i % 2 == 0) == evenVeilig;
                if (!veilig) {
                    double r = i * RING_BREED + RING_BREED / 2;
                    for (int k = 0; k < 40; k++) {
                        double a = Math.PI * 2 * k / 40;
                        sl.sendParticles(ParticleTypes.CRIT, m.x + Math.cos(a) * r, m.y + 0.3, m.z + Math.sin(a) * r, 1, 0, 0.3, 0, 0.05);
                    }
                }
            }
        });
        laatsteEven = evenVeilig;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        golfCd--;
        stootCd--;
        stampCd--;
        if (tweede > 0 && --tweede == 0) {
            golf(sl, !laatsteEven);   // fase 3: the second wave swaps
        }
        if (stootT >= 0) {
            stootT--;
            if (stootT < 12) {
                double f = 1 - stootT / 12.0;
                Vec3 v = van.lerp(naar, f);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (ServerPlayer p : l) {
                    if (p.distanceToSqr(this) < 2.8 * 2.8 && stootGeraakt.add(p.getUUID())) {
                        mech(p, 0.3, "schouderstoot");
                        duw(p, zMidden(), 2.4);   // far toward the edge
                    }
                }
            }
        }
        if (tickCount % 15 == 0) {
            sl.sendParticles(new DustParticleOptions(0xC0A070, 1.2f), getX(), getY() + 2.5, getZ(), 4, 0.8, 0.8, 0.8, 0);
        }
    }

    // ---- the end: pink again, a bow, and through to the egg ---------------------------------------------------------------

    @Override
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            entityData.set(ROZE_WEER, true);
            speel("buig");
            sl.sendParticles(new DustParticleOptions(ROZE, 2.4f), getX(), getY() + 2, getZ(), 120, 1.2, 1.6, 1.2, 0.02);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 3.5, getZ(), 8, 0.8, 0.4, 0.8, 0);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 60, 1.5, 1.5, 1.5, 0.05);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2f, 0.7f);
        }
    }

    @Override
    protected int plofDuur() {
        return 20 * 20;
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            later2(sl, 50, () -> spreek("vadsrijk.z3bazen.nw.dank"));
            later2(sl, 140, () -> spreek("vadsrijk.z3bazen.nw.splinter"));
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 40 * 40) {
                    Z3BazenApi.moment(p, "nestwachter_gered");
                }
            }
        }
    }

    private static void later2(ServerLevel sl, int t, Runnable r) {
        nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry.later(sl.getServer(), t, r);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd()) {
            setDeltaMovement(Vec3.ZERO);   // he stays and bows (no happy hops for a giant)
            if (tickCount % 10 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(new DustParticleOptions(ROZE, 1.4f), getX(), getY() + 2.5, getZ(), 6, 0.8, 1, 0.8, 0);
            }
        }
    }
}
