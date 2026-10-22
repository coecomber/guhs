package nl.juiced.guhs.feature.safari.momenten;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.rit.Pad;
import nl.juiced.guhs.feature.safari.spullen.SafariGeluid;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GuhTime;

/**
 * The pinguïnparade (test round 3, redone in test round 4, design ronde 47): once a day ({@link MomentSchema#PARADE_VAN},
 * on the schemabord) a little stoet walks, just like the vadsparade, along two walks ({@link ParadeRoute}): the truck road
 * through the IJskust and a loop through the village.
 * <p>
 * The stoet ({@link #opstelling}): in front the tamboer-guh (sjako, paradejasje, trommeltje) and the vlaggendrager (flag
 * cape, safari hat), then the penguins in a neat row, at the back dancing guhs with balloons. Everyone is scripted: each
 * tick member k stands {@code k * }{@link #AFSTAND} blocks behind the leader along the walk ({@link #posities}), so they
 * never clip into each other. The penguins waddle (a little sway and bob) and now and then do a belly-slide hop; the
 * dancers hop and twirl. The parade has its own tune (flute and drum, {@link SafariGeluid#PARADE}, phrase after phrase at
 * the tamboer). Confetti when it sets off and at the finish, then everybody leaves with a puff.
 * <p>
 * The members are extra entities (penguins: tag {@link #TAG}; guhs: tag {@link #GUH_TAG}, nobody can pick them up), never
 * kept: they leave when the parade is over (also after a restart in the middle of it).
 * <p>
 * The hook for the animal behaviour (J1): {@link #actief} (a parade is on in this level) and {@link #isParadeDier}
 * (this animal walks in it: leave its movement alone).
 */
public final class Parade {
    public static final String TAG = "guhs_safari_parade";
    public static final String GUH_TAG = "guhs_safari_parade_guh";
    /** Blocks between two members of the stoet, measured along the walk. */
    public static final double AFSTAND = 2.0;
    static final int PINGUINS = 6, DANSERS = 3;
    /** Test round 5: two of the six penguins are little ones (the third and the fifth). */
    public static final java.util.Set<Integer> BABYS = java.util.Set.of(2, 4);
    /** Walking speed (blocks per tick): normally {@link #SNEL}, a little faster for a long walk, never faster than {@link #MAX_SNEL}. */
    static final double SNEL = 0.075, MAX_SNEL = 0.11;
    /** Ticks of one phrase of the parade tune (the OGGs are exactly this long). */
    static final int FRASE = 80;
    /** Ticks of the finale (confetti, cheering) before everybody leaves. */
    static final int FINALE = 70;

    private static final DustParticleOptions[] CONFETTI = {
            new DustParticleOptions(0xFF73BF, 1.2f), new DustParticleOptions(0xFFD940, 1.2f), new DustParticleOptions(0x8CE6BF, 1.2f),
            new DustParticleOptions(0x80B2FF, 1.2f), new DustParticleOptions(0xBF80FF, 1.2f)};

    /** Who walks where in the stoet. */
    public enum Rol {
        TAMBOER, VLAG, PINGUIN, DANSER
    }

    /** The order of the stoet, front to back. */
    public static List<Rol> opstelling() {
        List<Rol> uit = new ArrayList<>();
        uit.add(Rol.TAMBOER);
        uit.add(Rol.VLAG);
        for (int i = 0; i < PINGUINS; i++) {
            uit.add(Rol.PINGUIN);
        }
        for (int i = 0; i < DANSERS; i++) {
            uit.add(Rol.DANSER);
        }
        return uit;
    }

    /** Where the n members stand when the leader is {@code s} blocks along the walk: each {@link #AFSTAND} behind the one in front. */
    public static List<Vec3> posities(Pad pad, double s, int n) {
        List<Vec3> uit = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            uit.add(pad.op(s - k * AFSTAND));
        }
        return uit;
    }

    /** One walking stoet. */
    private static final class Stoet {
        final String naam;
        final Pad pad;
        final List<Rol> rollen = opstelling();
        final UUID[] leden = new UUID[rollen.size()];
        final double[] y = new double[rollen.size()];
        final int[] glij = new int[rollen.size()];
        double s, snel = SNEL, eind;
        boolean gemaakt, weg;
        int ticks, finale;

        Stoet(String naam, List<Vec3> punten) {
            this.naam = naam;
            this.pad = new Pad(punten, false);
            this.eind = pad.lengte();
        }
    }

    private static final List<Stoet> STOETEN = new ArrayList<>();
    private static long dagGestart = Long.MIN_VALUE;

    /** Is the penguin parade on in this level now? */
    public static boolean actief(net.minecraft.world.level.Level level) {
        return Safari.echt(level) && MomentSchema.parade((int) GuhTime.timeOfDay(level));
    }

    /** Does this animal walk in the parade (its walking is scripted here)? */
    public static boolean isParadeDier(Entity e) {
        return e instanceof SafariDier && e.entityTags().contains(TAG);
    }

    static boolean isParadeGuh(Entity e) {
        return e instanceof GuhEntity && e.entityTags().contains(GUH_TAG);
    }

    /** The parade guhs are nobody's: no picking up, no feeding, no clothes (Momenten forwards both interact events). */
    static void opInteractie(PlayerInteractEvent event, Entity doel) {
        if (isParadeGuh(doel) && event instanceof net.neoforged.bus.api.ICancellableEvent c) {
            c.setCanceled(true);
            if (event instanceof PlayerInteractEvent.EntityInteract ei) {
                ei.setCancellationResult(InteractionResult.SUCCESS);
            } else if (event instanceof PlayerInteractEvent.EntityInteractSpecific es) {
                es.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    static void tick(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return;
        }
        if (server.getTickCount() % 10 == 5) {
            boolean aan = actief(level) && !level.players().isEmpty();
            if (!aan) {
                if (!STOETEN.isEmpty() || server.getTickCount() % 400 == 5) {
                    stop(level);
                }
                return;
            }
            long dag = GuhTime.day(level);
            if (dagGestart != dag) {
                stop(level);
                dagGestart = dag;
                int over = MomentSchema.PARADE_VAN + MomentSchema.PARADE_DUUR - (int) GuhTime.timeOfDay(level);
                maak("ijskust", ParadeRoute.ijskust(), over);
                maak("dorp", ParadeRoute.dorp(), over);
            }
        }
        for (Stoet st : STOETEN) {
            loop(level, st);
        }
    }

    /** A new stoet on this walk, with a speed (and if needed a shorter walk) so it reaches the end within the parade time. */
    private static void maak(String naam, List<Vec3> punten, int over) {
        if (punten.size() < 2) {
            return;
        }
        Stoet st = new Stoet(naam, punten);
        double begin = (st.rollen.size() - 1) * AFSTAND;
        if (st.eind <= begin + 4) {
            return;
        }
        int tijd = Math.max(200, over - FINALE - 120);
        st.snel = Mth.clamp((st.eind - begin) / tijd, SNEL, MAX_SNEL);
        st.eind = Math.min(st.eind, begin + st.snel * tijd);
        st.s = begin;
        STOETEN.add(st);
    }

    private static void loop(ServerLevel level, Stoet st) {
        if (st.weg) {
            return;
        }
        Vec3 kop = st.pad.op(st.s);
        if (!level.isLoaded(BlockPos.containing(kop)) || !level.isLoaded(BlockPos.containing(st.pad.op(0)))) {
            return;                                       // (nobody near: the stoet waits)
        }
        if (!st.gemaakt) {
            st.gemaakt = true;
            if (!stel(level, st)) {
                weg(level, st);
                return;
            }
            confetti(level, st.pad.op(st.s - st.rollen.size() * AFSTAND / 2), 3.0);
            level.playSound(null, kop.x, kop.y, kop.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.RECORDS, 2f, 1.1f);
        }
        st.ticks++;
        if (st.finale == 0) {
            st.s = Math.min(st.eind, st.s + st.snel);
            if (st.s >= st.eind) {
                st.finale = 1;
            }
        }
        List<Vec3> plek = posities(st.pad, st.s, st.rollen.size());
        Entity tamboer = null;
        for (int k = 0; k < st.leden.length; k++) {
            Entity e = st.leden[k] == null ? null : level.getEntity(st.leden[k]);
            if (e == null || e.isRemoved()) {
                continue;
            }
            if (k == 0) {
                tamboer = e;
            }
            zet(level, st, k, e, plek.get(k));
        }
        if (st.finale == 0 && tamboer != null && st.ticks % FRASE == 1) {
            int frase = (st.ticks / FRASE) % SafariGeluid.PARADE.size();
            level.playSound(null, tamboer.getX(), tamboer.getY(), tamboer.getZ(), SafariGeluid.PARADE.get(frase).get(), SoundSource.RECORDS, 2.5f, 1f);
        }
        if (tamboer != null && st.finale == 0 && st.ticks % 20 == 0) {
            level.sendParticles(ParticleTypes.NOTE, tamboer.getX(), tamboer.getY() + 1.4, tamboer.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
        }
        if (st.finale > 0) {
            finale(level, st);
        }
    }

    /** Puts member k on its spot of the walk: penguins waddle (now and then a belly-slide hop), dancers hop and twirl. */
    private static void zet(ServerLevel level, Stoet st, int k, Entity e, Vec3 p) {
        Rol rol = st.rollen.get(k);
        double doelY = grondY(level, p);
        st.y[k] = Math.abs(doelY - st.y[k]) > 3 ? doelY : st.y[k] + Mth.clamp(doelY - st.y[k], -0.35, 0.6);
        float yaw = st.pad.richting(st.s - k * AFSTAND, e.getYRot());
        double hop = 0;
        float draai = 0;
        double t = st.ticks;
        boolean loopt = st.finale == 0;
        switch (rol) {
            case TAMBOER -> {
            }
            case VLAG -> hop = loopt ? 0.04 * Math.abs(Math.sin(t * Math.PI / 10)) : 0;
            case PINGUIN -> {
                if (loopt && st.glij[k] == 0 && e instanceof SafariDier d && d.getRandom().nextInt(500) == 0) {
                    st.glij[k] = 16;
                    d.speelActie("buikglij", 16);
                }
                if (st.glij[k] > 0) {
                    hop = 0.35 * Math.sin(Math.PI * (16 - st.glij[k]) / 16.0);
                    st.glij[k]--;
                } else if (loopt) {
                    hop = 0.05 * Math.abs(Math.sin(t * Math.PI / 6 + k));
                    draai = (float) (9 * Math.sin(t * Math.PI / 6 + k));
                }
            }
            case DANSER -> {
                hop = 0.2 * Math.abs(Math.sin(t * Math.PI / 10 - k * 0.7));
                draai = (float) (35 * Math.sin(t * Math.PI / 40 + k));
                if (e instanceof GuhEntity g && (st.ticks + k * 13) % 40 == 0) {
                    g.triggerAnim("action", "happy");
                }
            }
        }
        e.setPos(p.x, st.y[k] + hop, p.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot(yaw + draai);
        if (e instanceof net.minecraft.world.entity.LivingEntity le) {
            le.setYBodyRot(yaw + draai);
            le.setYHeadRot(yaw + draai);
        }
        if (e instanceof SafariDier d && st.ticks % 10 == 0) {
            d.zetThuis(e.blockPosition(), 3, -1);
        }
    }

    /** Makes the members, all on their spot, with a puff. False when an entity could not be made. */
    private static boolean stel(ServerLevel level, Stoet st) {
        List<Vec3> plek = posities(st.pad, st.s, st.rollen.size());
        int pinguin = 0, danser = 0;
        for (int k = 0; k < st.rollen.size(); k++) {
            Vec3 p = plek.get(k);
            Entity e = switch (st.rollen.get(k)) {
                case PINGUIN -> pinguin(level, BlockPos.containing(p), pinguin++);
                case TAMBOER -> guh(level, 1.15f, GuhClothes.VADSPARADE_SJAKO, GuhClothes.VADSPARADE_JASJE, GuhClothes.VADSPARADE_TROMMELTJE);
                case VLAG -> guh(level, 1.05f, GuhClothes.CIRCUIT_VLAGCAPE, GuhClothes.SAFARI_HOEDJE);
                case DANSER -> guh(level, 0.95f, GuhClothes.MEW_BALLONNETJE, danser++ % 2 == 0 ? GuhClothes.PARTY_HAT : GuhClothes.BALLONPET);
            };
            if (e == null) {
                return false;
            }
            st.y[k] = grondY(level, p);
            e.snapTo(p.x, st.y[k], p.z, st.pad.richting(st.s - k * AFSTAND, 0f), 0f);
            level.addFreshEntity(e);
            st.leden[k] = e.getUUID();
            level.sendParticles(ParticleTypes.POOF, p.x, st.y[k] + 0.4, p.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
        return true;
    }

    private static SafariDier pinguin(ServerLevel level, BlockPos plek, int i) {
        SafariDier p = SafariFeature.DIER.get().create(level, EntitySpawnReason.EVENT);
        if (p != null) {
            p.zet("pinguin", BABYS.contains(i) ? "baby" : i % 2 == 0 ? "man" : "vrouw", plek, 3, -1, -1, 0);
            p.addTag(TAG);
            p.setNoAi(true);
            p.setNoGravity(true);
            p.setInvulnerable(true);
        }
        return p;
    }

    private static GuhEntity guh(ServerLevel level, float schaal, GuhClothes... kleren) {
        GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.EVENT);
        if (g != null) {
            g.setVariant(GuhVariant.NORMAL);
            g.setGuhScale(schaal);
            for (GuhClothes c : kleren) {
                g.wear(c);
            }
            g.setNoAi(true);
            g.setNoGravity(true);
            g.setInvulnerable(true);
            g.addTag(GUH_TAG);
        }
        return g;
    }

    /** The finish: confetti and cheering, then everybody leaves with a puff. */
    private static void finale(ServerLevel level, Stoet st) {
        Vec3 kop = st.pad.op(st.s);
        if (st.finale == 1) {
            level.playSound(null, kop.x, kop.y, kop.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.RECORDS, 2.5f, 1f);
            level.playSound(null, kop.x, kop.y, kop.z, SoundEvents.PLAYER_LEVELUP, SoundSource.RECORDS, 1.2f, 1.2f);
            for (UUID id : st.leden) {
                Entity e = id == null ? null : level.getEntity(id);
                if (e instanceof GuhEntity g) {
                    g.triggerAnim("action", "happy");
                } else if (e instanceof SafariDier d) {
                    d.speelActie("blij", FINALE);
                }
            }
        }
        if (st.finale % 20 == 1) {
            confetti(level, kop, 3.0);
            confetti(level, st.pad.op(st.s - st.rollen.size() * AFSTAND / 2), 3.0);
        }
        if (++st.finale > FINALE) {
            weg(level, st);
        }
    }

    private static void weg(ServerLevel level, Stoet st) {
        st.weg = true;
        for (UUID id : st.leden) {
            Entity e = id == null ? null : level.getEntity(id);
            if (e != null) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.4, e.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
                e.discard();
            }
        }
    }

    private static void confetti(ServerLevel level, Vec3 c, double straal) {
        for (DustParticleOptions kleur : CONFETTI) {
            level.sendParticles(kleur, c.x, c.y + 2.0, c.z, 14, straal / 2, 1.0, straal / 2, 0.15);
        }
        level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 2.5, c.z, 16, straal / 2, 0.6, straal / 2, 0.08);
    }

    /** The parade is over (or was cut off by a restart): everybody goes home with a puff. */
    private static void stop(ServerLevel level) {
        STOETEN.clear();
        dagGestart = Long.MIN_VALUE;
        List<Entity> weg = new java.util.ArrayList<>();          // (collect first: discarding while iterating crashed the server)
        for (Entity e : level.getAllEntities()) {
            if (isParadeDier(e) || isParadeGuh(e)) {
                weg.add(e);
            }
        }
        for (Entity e : weg) {
            level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.4, e.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            e.discard();
        }
    }

    /** The walking surface near this point of the walk (under an arch or a roof too), else the top of the terrain. */
    static double grondY(ServerLevel level, Vec3 p) {
        int x = Mth.floor(p.x), z = Mth.floor(p.z), y0 = Mth.floor(p.y + 0.01);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = y0 + 2; y >= y0 - 4; y--) {
            m.set(x, y, z);
            if (!level.getBlockState(m).getCollisionShape(level, m).isEmpty()) {
                continue;
            }
            m.set(x, y - 1, z);
            BlockState onder = level.getBlockState(m);
            VoxelShape vorm = onder.getCollisionShape(level, m);
            if (!vorm.isEmpty()) {
                return y - 1 + vorm.max(Direction.Axis.Y);
            }
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private Parade() {
    }
}
