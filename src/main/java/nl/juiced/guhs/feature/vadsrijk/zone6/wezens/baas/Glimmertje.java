package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * Kootje Glimmertje (dungeon boss 2 of De Verzonken Sterrenwacht, lv 67): the foreman of the Lampendovers, a Mika with a GIANT
 * candle snuffer. He flees in smoke when beaten.
 * <ul>
 *   <li><b>De vuurschalen</b>: four fire bowls stand in the corners of the hall. Every ~15 s he snuffs one (a purple strip from him
 *       to the bowl, 1.5 s). Right-click a dark bowl twice to light it again (two players run, the rest keep him busy).
 *       Two or more out: it gets dark (Darkness). Three out: his telegraphs turn GREY (hard to see). All four out: he hides in the
 *       dark and can't be hurt.</li>
 *   <li><b>Doverklap</b>: a red circle under his target (1 s): 25 %. <b>Rookbom</b>: a grey circle (1.2 s): 10 % + 2 s blind.</li>
 *   <li><b>50 %</b>: he snuffs two bowls at once and whistles two Lampendovers.</li>
 * </ul>
 */
public class Glimmertje extends Z6Baas {
    private int doofCd = 200, klapCd = 60, rookCd = 140;
    private final List<UUID> schalen = new ArrayList<>();
    /** Gametests. */
    public int gedoofd, aangestoken;

    public Glimmertje(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    public String baasId() {
        return "glimmertje";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "doof", "gooi", "fluit"};
    }

    @Override
    protected double hp(int level) {
        return 11500 + 390 * level;
    }

    @Override
    protected double klap(int level) {
        return 21 + 2.4 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.5};
    }

    /** How many fire bowls are out. */
    public int uit() {
        int n = 0;
        for (Z3Doel d : schalen()) {
            if (d.stand() == 0) {
                n++;
            }
        }
        return n;
    }

    public List<Z3Doel> schalen() {
        List<Z3Doel> l = new ArrayList<>();
        if (level() instanceof ServerLevel sl) {
            for (UUID u : schalen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    l.add(d);
                }
            }
        }
        return l;
    }

    @Override
    protected boolean immuun() {
        return !schalen.isEmpty() && uit() >= 4;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z6wezens.gl.donker";
    }

    private int kleur(int k) {
        return uit() >= 3 ? GRIJS_TELEGRAAF : k;
    }

    private static final int GRIJS_TELEGRAAF = 0x6A6A72;

    /** The four bowls in the corners (once per fight). */
    private void zetSchalen(ServerLevel sl) {
        if (!schalen.isEmpty()) {
            return;
        }
        int o = Math.max(3, (int) zStraal() - 3);
        int[][] hoeken = {{-o, -o}, {o, -o}, {o, o}, {-o, o}};
        for (int i = 0; i < 4; i++) {
            Vec3 v = voetVan(zMidden().add(hoeken[i][0], 0, hoeken[i][1]));
            if (Z6WezensRegistry.VUURSCHAAL.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                d.koppel(this, i, 1000, v);
                d.zetRaakbaar(false);
                d.zetStand(1);
                sl.addFreshEntity(d);
                schalen.add(d.getUUID());
            }
        }
    }

    /** Snuffs this bowl after a purple strip (dev/tests: ticks 0 = at once). */
    public void doof(ServerLevel sl, Z3Doel d, int ticks) {
        if (ticks > 0) {
            Z6Vfx.lijn(sl, voetVan(position()), d.position(), 1.2, ticks, PAARS);
        }
        speel("doof");
        Runnable r = () -> {
            if (d.isAlive() && d.stand() == 1) {
                d.zetStand(0);
                gedoofd++;
                sl.playSound(null, d.blockPosition(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 2f, 0.6f);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, d.getX(), d.getY() + 1.2, d.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
                zeg("vadsrijk.z6wezens.gl.gedoofd", ChatFormatting.GRAY, uit());
            }
        };
        if (ticks > 0) {
            later(ticks, r);
        } else {
            r.run();
        }
    }

    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || !schalen.contains(d.getUUID()) || d.stand() == 1) {
            return;
        }
        int klik = d.getPersistentData().getIntOr("klik", 0) + 1;
        if (klik < 2) {
            d.getPersistentData().putInt("klik", klik);
            p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z6wezens.gl.blaas").withStyle(ChatFormatting.GOLD));
            sl.playSound(null, d.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1f, 1f);
            return;
        }
        d.getPersistentData().putInt("klik", 0);
        d.zetStand(1);
        aangestoken++;
        sl.playSound(null, d.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 1.4f);
        sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1.2, d.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        zeg("vadsrijk.z6wezens.gl.aan", ChatFormatting.GREEN, uit());
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        kaart("vadsrijk.z6wezens.gl.fase2", ROOD);
        speel("fluit");
        int n = 0;
        for (Z3Doel d : schalen()) {
            if (d.stand() == 1 && n < 2) {
                doof(sl, d, tg(30));
                n++;
            }
        }
        for (int i = 0; i < adds(2); i++) {
            if (Z6WezensRegistry.MIKA_LAMPENDOVER.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof VadsrijkMob m) {
                m.snapTo(getX() + (i == 0 ? 2 : -2), getY(), getZ(), 0, 0);
                m.stelIn(66, null, false, Set.of());
                sl.addFreshEntity(m);
                List<ServerPlayer> l = strijders();
                if (!l.isEmpty()) {
                    m.setTarget(l.get(i % l.size()));
                }
            }
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        zetSchalen(sl);
        if (l.isEmpty()) {
            return;
        }
        if (doofCd <= 0) {
            for (Z3Doel d : schalen()) {
                if (d.stand() == 1) {
                    doof(sl, d, tg(30));
                    break;
                }
            }
            doofCd = 300;
            rustT = 40;
            laatsteAanval = "doof";
            return;
        }
        if (rookCd <= 0) {
            ServerPlayer d = l.get(getRandom().nextInt(l.size()));
            Vec3 v = voetVan(d);
            Z6Vfx.cirkel(sl, v, 3.5, tg(24), kleur(Z6Vfx.GRIJS));
            speel("gooi");
            later(tg(24), () -> {
                rook(sl, v, 3);
                for (ServerPlayer p : inCirkel(v, 3.5)) {
                    mech(p, 0.1, "rookbom");
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                }
            });
            rookCd = 220;
            rustT = 30;
            laatsteAanval = "rookbom";
            return;
        }
        if (klapCd <= 0) {
            ServerPlayer d = zDoel() != null ? zDoel() : l.get(0);
            Vec3 v = voetVan(d);
            Z6Vfx.cirkel(sl, v, 3, tg(20), kleur(ROOD));
            Z6Vfx.windup(this, tg(20));
            Z6Vfx.cast(this, "doverklap", tg(20));
            later(tg(20), () -> {
                speel("aanval");
                Z6Vfx.treffer(this, v, 3, ROOD);
                for (ServerPlayer p : inCirkel(v, 3)) {
                    mech(p, 0.25, "doverklap");
                }
            });
            klapCd = 140;
            rustT = 30;
            laatsteAanval = "doverklap";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        doofCd--;
        klapCd--;
        rookCd--;
        if (tickCount % 20 == 0 && uit() >= 2) {
            for (ServerPlayer p : l) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
        }
        for (Z3Doel d : schalen()) {
            if (d.stand() == 1 && tickCount % 8 == 0) {
                sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1.1, d.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : schalen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void opPlof() {
        super.opPlof();
        zegAlle("vadsrijk.z6wezens.gl.weg", ChatFormatting.GRAY);
    }

    /** (dev) is this spot the vuurschalenzaal? */
    static boolean inZaal(Vec3 v) {
        return v.distanceTo(Vec3.atBottomCenterOf(Z6WezensPlekken.VUURSCHALENZAAL)) < Z6WezensPlekken.VUUR_HALF + 3;
    }
}
