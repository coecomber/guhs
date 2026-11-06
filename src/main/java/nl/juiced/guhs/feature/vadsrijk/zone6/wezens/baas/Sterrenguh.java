package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensApi;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * De Verzonken Sterrenguh (dungeon end boss of De Verzonken Sterrenwacht, lv 68): an old giant guh of star stone, gone grey in the
 * sunken dome. A VICTIM: you cheer him up, he glows again and hands you the <b>Sterrenkompas</b> ({@link Z6WezensApi#KOMPAS}).
 * <ul>
 *   <li><b>De telescopen</b>: four brass telescopes stand round the dome (N, E, S, W). Right-click one to turn it a quarter. When
 *       ALL FOUR point at his heart, their beams meet: his heart lights up and he takes DOUBLE damage for 10 s; then a star shock
 *       (a red ring) knocks the telescopes askew again. While they are not aimed, his star stone keeps most of it off (35 %).</li>
 *   <li><b>Sterrengruis</b>: red circles rain round the dome (1.2 s): 15 % each; more and more as he despairs (fase 2, 3).</li>
 *   <li><b>Wanhoopsgolf</b> (fase 2+): a red ring from him outward, safe close to him (1.4 s): 25 %.</li>
 * </ul>
 */
public class Sterrenguh extends Z6Baas {
    private static final EntityDataAccessor<Boolean> HART = SynchedEntityData.defineId(Sterrenguh.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BLIJ = SynchedEntityData.defineId(Sterrenguh.class, EntityDataSerializers.BOOLEAN);
    public static final int HART_DUUR = 200;
    private int gruisCd = 60, golfCd = 160, hartT;
    private final List<UUID> telescopen = new ArrayList<>();
    /** The stand (0..3) that aims at him, per telescope. */
    private final int[] juist = {2, 3, 0, 1};
    /** Gametests. */
    public int harten;

    public Sterrenguh(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(HART, false);
        b.define(BLIJ, false);
    }

    public boolean hartAan() {
        return entityData.get(HART);
    }

    public boolean blij() {
        return entityData.get(BLIJ) || geplofd();
    }

    @Override
    public String baasId() {
        return "sterrenguh";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "strooi", "golf", "hart", "blij"};
    }

    @Override
    protected double hp(int level) {
        return 13000 + 420 * level;
    }

    @Override
    protected double klap(int level) {
        return 22 + 2.5 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.ROZE;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.3};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.5;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return hartAan() ? 2.0 : 0.35;
    }

    @Override
    protected int plofDuur() {
        return 20 * 12;
    }

    public List<Z3Doel> telescopen() {
        List<Z3Doel> l = new ArrayList<>();
        if (level() instanceof ServerLevel sl) {
            for (UUID u : telescopen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    l.add(d);
                }
            }
        }
        return l;
    }

    /** How many telescopes aim at him now. */
    public int gericht() {
        int n = 0;
        for (Z3Doel d : telescopen()) {
            if (d.stand() == juist[d.nummer]) {
                n++;
            }
        }
        return n;
    }

    private void zetTelescopen(ServerLevel sl) {
        if (!telescopen.isEmpty()) {
            return;
        }
        double r = Math.max(4, zStraal() - 3);
        double[][] o = {{0, -r}, {r, 0}, {0, r}, {-r, 0}};   // N, E, S, W
        for (int i = 0; i < 4; i++) {
            Vec3 v = voetVan(zMidden().add(o[i][0], 0, o[i][1]));
            if (Z6WezensRegistry.TELESCOOP.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                d.koppel(this, i, 1000, v);
                d.zetRaakbaar(false);
                d.zetStand((juist[i] + 1 + getRandom().nextInt(3)) % 4);
                sl.addFreshEntity(d);
                telescopen.add(d.getUUID());
            }
        }
    }

    /** Knocks every telescope askew (never on its right stand). */
    private void scheef() {
        for (Z3Doel d : telescopen()) {
            d.zetStand((juist[d.nummer] + 1 + getRandom().nextInt(3)) % 4);
        }
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || !telescopen.contains(d.getUUID()) || hartAan()) {
            return;
        }
        d.zetStand((d.stand() + 1) % 4);
        sl.playSound(null, d.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, 0.8f);
        boolean goed = d.stand() == juist[d.nummer];
        p.sendOverlayMessage(Component.translatable(goed ? "vadsrijk.z6wezens.sg.telescoop_goed" : "vadsrijk.z6wezens.sg.telescoop", gericht())
                .withStyle(goed ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        if (gericht() >= 4) {
            hart(sl);
        }
    }

    /** All four aimed: the beams meet on his heart (dev/tests too). */
    public void hart(ServerLevel sl) {
        harten++;
        entityData.set(HART, true);
        hartT = HART_DUUR;
        speel("hart");
        for (Z3Doel d : telescopen()) {
            Z6Vfx.straal(sl, d.position().add(0, 1.6, 0), position().add(0, getBbHeight() * 0.6, 0), 0.6, Z6Vfx.STER, HART_DUUR);
        }
        Z6Vfx.zuil(sl, position(), getBbHeight() + 8, Z6Vfx.STER, 40);
        kaart("vadsrijk.z6wezens.sg.hart", GEEL);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.2f);
    }

    private void hartUit(ServerLevel sl) {
        entityData.set(HART, false);
        Vec3 c = voetVan(position());
        Z6Vfx.donut(sl, c, 2.5, zStraal(), tg(24), ROOD);
        zeg("vadsrijk.z6wezens.sg.schok", ChatFormatting.RED);
        later(tg(24), () -> {
            Z6Vfx.golf(sl, c, zStraal(), Z6Vfx.STER);
            for (ServerPlayer p : strijders()) {
                double d = Math.sqrt(p.distanceToSqr(c.x, p.getY(), c.z));
                if (d > 2.5) {
                    mech(p, 0.15, "sterrenschok");
                }
            }
            scheef();
        });
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z6wezens.sg.fase" + fase, fase == 3 ? ROOD : PAARS);
        praat("sg.wanhoop" + fase);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        zetTelescopen(sl);
        if (l.isEmpty()) {
            return;
        }
        if (fase() >= 2 && golfCd <= 0) {
            Vec3 c = voetVan(position());
            Z6Vfx.donut(sl, c, 3, zStraal() + 1, tg(28), ROOD);
            Z6Vfx.windup(this, tg(28));
            Z6Vfx.cast(this, "wanhoopsgolf", tg(28));
            later(tg(28), () -> {
                speel("golf");
                Z6Vfx.treffer(this, c, zStraal(), ROOD);
                for (ServerPlayer p : strijders()) {
                    if (Math.sqrt(p.distanceToSqr(c.x, p.getY(), c.z)) > 3) {
                        mech(p, 0.25, "wanhoopsgolf");
                    }
                }
            });
            golfCd = 320;
            rustT = tg(28) + 20;
            laatsteAanval = "wanhoopsgolf";
            return;
        }
        if (gruisCd <= 0) {
            int n = 3 + 2 * (fase() - 1) + l.size();
            speel("strooi");
            for (int i = 0; i < n; i++) {
                Vec3 v = i < l.size() ? voetVan(l.get(i)) : voetVan(ring(zMidden(), getRandom().nextDouble() * (zStraal() - 1), getRandom().nextDouble() * Math.PI * 2));
                inslag(v, 2.2, tg(24), 0.15, "sterrengruis", null, ParticleTypes.END_ROD);
            }
            gruisCd = 150 - 20 * fase();
            rustT = 30;
            laatsteAanval = "sterrengruis";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        gruisCd--;
        golfCd--;
        if (hartT > 0 && --hartT == 0) {
            hartUit(sl);
        }
        if (tickCount % 10 == 0) {
            for (Z3Doel d : telescopen()) {   // a faint beam where each telescope points (gold when right)
                double a = Math.PI / 2 * d.stand() - Math.PI / 2;
                Vec3 van = d.position().add(0, 1.6, 0);
                boolean goed = d.stand() == juist[d.nummer];
                Vec3 naar = goed ? position().add(0, getBbHeight() * 0.6, 0) : van.add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5);
                Z6Vfx.straal(sl, van, naar, 0.15, goed ? Z6Vfx.SAFFRAAN : 0x8A8AA0, 10);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : telescopen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }

    // ---- the end: he glows again and gives the Sterrenkompas ------------------------------------------------------------------------

    @Override
    protected void naVerslagen(ServerPlayer p) {
        Z6WezensApi.kompas(p);
        ItemStack k = new ItemStack(Z6WezensRegistry.STERRENKOMPAS.get());
        if (!p.getInventory().contains(k)) {
            if (!p.getInventory().add(k)) {
                p.drop(k, false);
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.z6wezens.sg.kompas").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    protected void opPlof() {
        entityData.set(HART, false);
        entityData.set(BLIJ, true);
        super.opPlof();
        speel("blij");
        zegAlle("vadsrijk.z6wezens.sg.blij", ChatFormatting.LIGHT_PURPLE);
        if (level() instanceof ServerLevel sl) {
            Z6Vfx.zuil(sl, position(), 20, Z6Vfx.STER, 120);
        }
    }
}
