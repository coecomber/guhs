package nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf;

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
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.beleving.Beleving;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Hulpje;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Toegang;

/**
 * Kapitein Roquefort, the last boss of the Kaaszeppelinwerf (lv 40, Vuur), on the deck of his flagship.
 * <ul>
 *   <li>Fase 1: sabre slashes and the boarding charge (a red strip through a player, preferring the one at range); deckhands.</li>
 *   <li>Fase 2 (66 %): the deck tilts (screen shake, everyone slides, crates slide down red lanes). Pop the 4 balloon segments
 *       (purple markers, they hang at the deck's edge: shoot them or jump): each pops = 6 % of his Vads. He can't drop below 33 %
 *       while one is left.</li>
 *   <li>Fase 3 (33 %): the zeppelin sinks: walk to the wreck beside the deck (pink circle + a pink light pillar; its floor lies
 *       3 below the deck, you step off the edge) within 10 s ({@link #ZINK_TICKS}; the card and a countdown stay on screen that
 *       long). Whoever is still on the deck then takes 15 % after 1 and 2 s, 30 % every second after that, and is thrown toward
 *       the wreck. The fight goes on on the wreck. Vahoeg: a second zeppelin shells the wreck.</li>
 *   <li>Defeat: he floats off under his umbrella in a smoke puff.</li>
 * </ul>
 */
public class Roquefort extends Z3Baas {
    private static final EntityDataAccessor<Boolean> PARAPLU = SynchedEntityData.defineId(Roquefort.class, EntityDataSerializers.BOOLEAN);
    private final List<Z3Doel> ballonnen = new ArrayList<>();
    private int enterCd = 60, kantelCd = 0, matroosCd = 200, salvoCd = 100, zinkT = -1;
    private boolean opWrak;
    /** The sinking telegraph: 10 s (it was 6: from the far rail the wreck is 36 blocks away, and nobody found it in time). */
    public static final int ZINK_TICKS = 200;
    /** Safe in fase 3: within WRAK_VEILIG of the wreck's centre (its floor is a disc of 6.5), or on its hull rim (lower, up to WRAK_ROMP). */
    public static final double WRAK_VEILIG = 7, WRAK_ROMP = 10;
    /** A sinking hit: the first ZINK_ZACHT ones (1 and 2 s after the deck sank) are halved. */
    public static final double ZINK_SCHADE = 0.3;
    public static final int ZINK_ZACHT = 2;
    /** The wreck's stand position (gametests move it next to their own deck). */
    public BlockPos wrak = Z3BazenPlekken.ROQUEFORT_WRAK;
    /** Ticks since the deck sank (the sinking hits count from here, so the first one comes a full second after it). */
    private int wrakT;
    /** Gametests: the sinking hits per player (the fraction asked of {@link #mech}), in order. */
    public final Map<UUID, List<Double>> zinkHits = new HashMap<>();
    private Vec3 van = Vec3.ZERO, naar = Vec3.ZERO;
    private int enterT = -1;
    private final Set<UUID> enterGeraakt = new HashSet<>();
    private Vec3 kantel = Vec3.ZERO;
    private int kantelT;
    /** Gametests: balloons popped. */
    public int geprikt;

    public Roquefort(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PARAPLU, false);
    }

    public boolean paraplu() {
        return entityData.get(PARAPLU);
    }

    @Override
    public String baasId() {
        return "roquefort";
    }

    @Override
    public String kerker() {
        return "werf";
    }

    @Override
    public boolean eindbaas() {
        return true;
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"sabel", "slag", "sprong", "worp"};
    }

    @Override
    protected double hp(int level) {
        return 6200 + 260 * level;
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
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.66, 0.33};
    }

    @Override
    protected double vadsVloer() {
        return fase() == 2 && ballonnen.stream().anyMatch(b -> !b.isRemoved() && b.raakbaar()) ? 0.335 : 0;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z3bazen.rq.ballonnen_eerst";
    }

    @Override
    public Vec3 zMidden() {
        return opWrak ? wrakPlek() : super.zMidden();
    }

    private Vec3 wrakPlek() {
        return Vec3.atBottomCenterOf(wrak);
    }

    /** Gametests: ticks left of the sinking telegraph (-1 = not sinking), and is the deck gone? */
    public int zinkRest() {
        return zinkT;
    }

    public boolean opWrak() {
        return opWrak;
    }

    /**
     * Is this spot safe once the deck has sunk? On the wreck's floor (within {@link #WRAK_VEILIG} of its centre), on its hull rim
     * (the hull is wider than the floor and 2-3 lower: nobody can climb back from there, so it counts as "on the wreck"), or
     * fallen far below it.
     */
    public boolean veiligOpWrak(Vec3 v) {
        Vec3 w = wrakPlek();
        double dx = v.x - w.x, dz = v.z - w.z, d2 = dx * dx + dz * dz;
        return d2 <= WRAK_VEILIG * WRAK_VEILIG || v.y <= w.y - 12 || d2 <= WRAK_ROMP * WRAK_ROMP && v.y < w.y - 0.4;
    }

    /** From the sinking on, the whole wreck is part of the fight: a third of its floor lies outside the arena's disc round the deck. */
    private boolean bijWrak(Player p) {
        if (zinkT < 0 && !opWrak) {
            return false;
        }
        Vec3 w = wrakPlek();
        double dx = p.getX() - w.x, dz = p.getZ() - w.z, dy = p.getY() - w.y;
        return dx * dx + dz * dz <= WRAK_ROMP * WRAK_ROMP && dy > -Z3BazenPlekken.KERKER_ONDER && dy < Z3BazenPlekken.KERKER_BOVEN;
    }

    @Override
    public boolean zDoetMee(Player p) {
        return super.zDoetMee(p) || !p.isSpectator() && p.isAlive() && bijWrak(p) && (!(p instanceof ServerPlayer sp) || Z3Toegang.mag(sp, this));
    }

    @Override
    public boolean inGevecht(Player p) {
        return super.inGevecht(p) || !p.isSpectator() && bijWrak(p);
    }

    @Override
    public double zStraal() {
        return opWrak ? 9 : super.zStraal();
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        speel("sabel");
        raak(d, 1.1, "sabel");
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (fase == 2) {
            kaart("vadsrijk.z3bazen.rq.fase2", PAARS);
            spawnBallonnen(sl);
            kantelCd = 0;
        } else if (fase == 3) {
            ballonnen.forEach(b -> b.discard());
            int t = tg(ZINK_TICKS);
            zinkT = t;
            wrakT = 0;
            zinkHits.clear();
            Vec3 w = wrakPlek();
            Telegraaf.cirkel(sl, w, 6, t, ROZE);
            // red = the deck, but never over the wreck: decals follow the ground, so a circle round the whole deck also painted
            // two thirds of the wreck's floor (and the pink circle on it) red. The biggest circle that stays clear of the wreck:
            Vec3 m = super.zMidden(), weg = m.subtract(w).multiply(1, 0, 1);
            double straal = super.zStraal(), ver = weg.length(), r = Math.min(straal, (ver + straal - WRAK_VEILIG - 0.5) / 2);
            if (ver > 0.01 && r >= 3) {
                Vec3 c = w.add(weg.normalize().scale(WRAK_VEILIG + 0.5 + r));
                Telegraaf.cirkel(sl, new Vec3(c.x, m.y, c.z), r, t, ROOD);
            }
            nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "zinkend_schip", t);   // T-VFX: cast bar
            for (ServerPlayer p : sl.players()) {   // the card stays for the whole telegraph (a plain kaart is gone after 2.5 s)
                if (zDoetMee(p)) {
                    RijkScenes.titel(p, Component.translatable("vadsrijk.z3bazen.rq.fase3").withStyle(ChatFormatting.BOLD),
                            Component.translatable("vadsrijk.z3bazen.rq.fase3.onder"), t, ROOD);
                }
            }
            zinkMelding();
            wrakBaken(sl);
        }
    }

    /** The instruction with the seconds left, above the hotbar (sent again every second: an overlay line fades after 3 s). */
    private void zinkMelding() {
        zeg("vadsrijk.z3bazen.rq.zinkt", ChatFormatting.RED, (zinkT + 19) / 20);
    }

    /** "Go here": a pink light pillar on the wreck. Sent again every 2 s (each one fades in and out, and reaches new arrivals). */
    private void wrakBaken(ServerLevel sl) {
        Vfx.zuil(sl, wrakPlek(), 24, ROZE, 80);
    }

    private void spawnBallonnen(ServerLevel sl) {
        Vec3 m = super.zMidden();
        for (int i = 0; i < 4; i++) {
            Z3Doel b = Z3BazenRegistry.BALLON.get().create(sl, EntitySpawnReason.EVENT);
            if (b != null) {
                double a = Math.PI / 4 + i * Math.PI / 2;
                Vec3 v = ring(m, zStraal() - 3, a).add(0, 4.5 + (i % 2) * 1.5, 0);
                b.koppel(this, i, maxVads() * 0.05, v);
                sl.addFreshEntity(b);
                ballonnen.add(b);
            }
        }
        zeg("vadsrijk.z3bazen.rq.ballonnen", ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        geprikt++;
        sl.sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 0.5, d.getZ(), 30, 0.6, 0.6, 0.6, 0.08);
        sl.playSound(null, d.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.8f);
        d.discard();
        long over = ballonnen.stream().filter(b -> !b.isRemoved() && b.raakbaar()).count();
        zeg("vadsrijk.z3bazen.rq.geprikt", ChatFormatting.GREEN, 4 - over);
        Statussen.geef(this, Status.VERDOOFD, 20, 1, null);
        krijgSchade(maxVads() * 0.06, bron instanceof ServerPlayer p ? p : null);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (fase() == 2 && kantelCd <= 0) {
            kantel(sl, l);
            kantelCd = 180;
            rustT = 40;
            laatsteAanval = "kantel";
            return;
        }
        if (matroosCd <= 0 && !opWrak) {
            for (int i = 0; i < adds(2); i++) {
                Z3Hulpje.maak(sl, Z3BazenRegistry.MATROOS.get(), this, ring(zMidden(), 6, getRandom().nextDouble() * Math.PI * 2));
            }
            zeg("vadsrijk.z3bazen.rq.matrozen", ChatFormatting.GOLD);
            matroosCd = 400;
            rustT = 20;
            laatsteAanval = "matrozen";
            return;
        }
        if (enterCd <= 0) {
            ServerPlayer v = iemandOpAfstand() || getRandom().nextBoolean() ? afstandsDoel() : zDoel();
            if (v != null) {
                van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                naar = voetVan(van.add(r.scale(Math.max(8, voetVan(v).distanceTo(van) + 4))));
                Telegraaf.lijn(sl, van, naar, 3, tg(25), ROOD);
                nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, tg(25));   // T-VFX: wind-up animation (held until the hit / auto-release)
                enterT = tg(25) + 10;
                enterGeraakt.clear();
                speel("sprong");
                enterCd = 130;
                rustT = enterT + 15;
                laatsteAanval = "enter";
                return;
            }
        }
        rustT = 10;
    }

    private void kantel(ServerLevel sl, List<ServerPlayer> l) {
        double a = getRandom().nextDouble() * Math.PI * 2;
        kantel = new Vec3(Math.cos(a), 0, Math.sin(a));
        kantelT = 60;
        Vec3 m = zMidden();
        double r = zStraal() - 1;
        Vec3 dwars = new Vec3(-kantel.z, 0, kantel.x);
        for (int i = -1; i <= 1; i++) {
            Vec3 o = dwars.scale(i * 5 + (getRandom().nextDouble() - 0.5) * 2);
            rol(m.add(o).subtract(kantel.scale(r)), m.add(o).add(kantel.scale(r)), 2.6, 30, 24, 0.3, "kist",
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BARREL.defaultBlockState()), null);
        }
        for (ServerPlayer p : l) {
            Beleving.schud(p, 0.9f, 50);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
        zeg("vadsrijk.z3bazen.rq.kantel", ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        enterCd--;
        kantelCd--;
        matroosCd--;
        salvoCd--;
        if (tickCount % 10 == 0) {
            for (Z3Doel b : ballonnen) {   // the purple markers under the balloons
                if (!b.isRemoved()) {
                    sl.sendParticles(new DustParticleOptions(PAARS, 1.8f), b.getX(), b.getY() - 0.2, b.getZ(), 8, 0.5, 0.1, 0.5, 0);
                }
            }
        }
        if (kantelT > 0) {
            kantelT--;
            for (ServerPlayer p : l) {   // everyone slides down the tilted deck
                p.push(kantel.x * 0.035, 0, kantel.z * 0.035);
                p.hurtMarked = true;
            }
        }
        if (enterT >= 0) {
            enterT--;
            if (enterT < 10) {
                double f = 1 - enterT / 10.0;
                Vec3 v = van.lerp(naar, f);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (ServerPlayer p : l) {
                    if (p.distanceToSqr(this) < 2.4 * 2.4 && enterGeraakt.add(p.getUUID())) {
                        mech(p, 0.35, "enter");
                        duw(p, position(), 1.1);
                    }
                }
            }
        }
        if (zinkT >= 0) {
            zinkT--;
            if (zinkT > 0 && zinkT % 20 == 0) {
                zinkMelding();
            }
            if (zinkT == 0) {
                opWrak = true;
                wrakT = 0;
                zSpring(wrakPlek(), 20, 6);
                sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 3f, 0.4f);
            }
        }
        if ((zinkT > 0 || opWrak) && tickCount % 40 == 0) {
            wrakBaken(sl);
        }
        if (opWrak && ++wrakT % 20 == 0) {
            Vec3 w = wrakPlek();
            double f = wrakT / 20 <= ZINK_ZACHT ? ZINK_SCHADE / 2 : ZINK_SCHADE;
            for (ServerPlayer p : l) {   // still on the sinking deck
                if (!veiligOpWrak(p.position())) {
                    zinkHits.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(f);
                    mech(p, f, "zinkt");
                    Vec3 r = w.subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(r.x * 0.9, 0.5, r.z * 0.9);
                    p.hurtMarked = true;
                }
            }
        }
        if (opWrak && vahoeg() && salvoCd <= 0) {
            salvoCd = 100;
            for (ServerPlayer p : l) {
                inslag(voetVan(p), 2.5, 30, 0.25, "salvo", Status.BRANDEND, ParticleTypes.LAVA);
            }
        }
    }

    // the defeat: up and away under his umbrella
    @Override
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            entityData.set(PARAPLU, true);
            setNoGravity(true);
            rook(sl, position(), 1);
            spreek("vadsrijk.z3bazen.rq.vlucht");
        }
    }

    @Override
    protected int plofDuur() {
        return 90;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd() && level() instanceof ServerLevel sl) {
            setDeltaMovement(Vec3.ZERO);
            setPos(getX() + 0.06, getY() + 0.12, getZ());
            if (tickCount % 3 == 0) {
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.01);
            }
            if (plofTicks() >= 88) {
                rook(sl, position(), 1);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        ballonnen.forEach(b -> b.discard());
        super.remove(reason);
    }
}
