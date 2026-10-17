package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.mikas.EnderMikaEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * einde2 zij (DESIGN 5 "Wat je overhoudt", CONTRACT_E2_B 7.6; fix4 krachten): the four LASTING bending powers, each once
 * its element is learnt ({@link E2Verhaal#elementGeleerd}), anywhere, on the one key of {@link Krachten} (no item):
 * <ul>
 *   <li>{@link #LUCHTSPRONG} air: a leap; once more in mid-air (the double jump); the landing never hurts;</li>
 *   <li>{@link #WATERLOOP} water: walk on water for {@link #WATERLOOP_TICKS} ticks (the client keeps you on the surface,
 *       {@code client.E2ZijClient}); sneak to sink;</li>
 *   <li>{@link #AARDZUIL} earth, the "rotszuil": a rock pillar of {@link #ZUIL_HOOG} blocks bursts up where you look (up to
 *       {@link #ZUIL_BEREIK} blocks away) and throws the monsters on that spot into the air; aimed at your own feet it
 *       lifts you. Sneaking: the "rotsmuur", a wall of {@link #MUUR_BREED} x {@link #MUUR_HOOG} rock in front of you. Both
 *       crumble back after a few seconds: they only fill air (or a plant, which comes back), drop nothing, can't be mined
 *       and never stand in a protected building or someone else's huisje;</li>
 *   <li>{@link #HANDVLAM} fire, the "vuurschicht": a small fire bolt flies {@link #SCHICHT_BEREIK} blocks ahead (flames
 *       every tick, for everybody to see, also in first person); it hurts a monster a little and sets it alight for a
 *       moment, lights a campfire or candle it hits, and never sets a block on fire. Guhs, villagers, the cast and other
 *       animals it simply passes; another player only where the server allows PvP (vanilla's rules decide).</li>
 * </ul>
 * Nothing here ever hurts a guh or changes a protected building.
 */
public final class ZijKrachten {
    public static final String LUCHTSPRONG = "zij_luchtsprong", WATERLOOP = "zij_waterloop", AARDZUIL = "zij_aardzuil", HANDVLAM = "zij_handvlam";
    public static final int WATERLOOP_TICKS = 300, ZUIL_TICKS = 140, ZUIL_HOOG = 4, ZUIL_BEREIK = 12, MUUR_TICKS = 140, MUUR_BREED = 5,
            MUUR_HOOG = 3, SCHICHT_BEREIK = 10;
    /** The fire bolt's damage (one and a half heart) and how long a hit monster burns. */
    public static final float SCHICHT_SCHADE = 3f;
    static final int SCHICHT_BRAND = 2;
    private static final double SCHICHT_SNEL = 1.25;
    static final String GUI = "gui.guhs.einde2_zij.kracht.";
    private static final BlockState STEEN = Blocks.TUFF.defaultBlockState();

    /** Players who used the air leap while in the air (once until they land). */
    private static final Set<UUID> GESPRONGEN = ConcurrentHashMap.newKeySet();
    /** Players whose next landing does not hurt (after a leap of ours). */
    private static final Set<UUID> ZACHT = ConcurrentHashMap.newKeySet();
    /** Player -> game time until which they walk on water. */
    private static final Map<UUID, Long> WATER_TOT = new ConcurrentHashMap<>();

    /** Rising rock: the cells still to fill, one row per tick; {@code tillen} = lift the player standing on it. */
    private record Rots(ServerLevel level, List<List<BlockPos>> rijen, int duur, @Nullable BlockPos tillen, int[] gedaan) {
    }

    private static final Map<UUID, Rots> RIJZEND = new ConcurrentHashMap<>();
    /** What stood where the rock stands now (a plant, else air): it comes back when the rock crumbles. Server thread. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockState>> ONDER = new HashMap<>();

    /** A fire bolt in flight. */
    private static final class Schicht {
        final UUID schutter;
        final ServerLevel level;
        Vec3 pos;
        final Vec3 snel;
        double gevlogen;

        Schicht(UUID schutter, ServerLevel level, Vec3 pos, Vec3 snel) {
            this.schutter = schutter;
            this.level = level;
            this.pos = pos;
            this.snel = snel;
        }
    }

    private static final List<Schicht> SCHICHTEN = new ArrayList<>();

    private ZijKrachten() {
    }

    static void register() {
        Krachten.registreer(new Krachten.Kracht(LUCHTSPRONG, Einde2Ids.Element.LUCHT, "minecraft:feather", 10,
                (p, voet) -> geleerd(p, Einde2Ids.Element.LUCHT) && !p.isPassenger() && !p.isFallFlying()
                        && (p.onGround() || !GESPRONGEN.contains(p.getUUID())),
                (p, plek) -> luchtsprong(p)));
        Krachten.registreer(new Krachten.Kracht(WATERLOOP, Einde2Ids.Element.WATER, "minecraft:heart_of_the_sea", 400,
                (p, voet) -> geleerd(p, Einde2Ids.Element.WATER),
                (p, plek) -> waterloop(p)));
        Krachten.registreer(new Krachten.Kracht(AARDZUIL, Einde2Ids.Element.AARDE, "minecraft:tuff", 60,
                (p, voet) -> geleerd(p, Einde2Ids.Element.AARDE) && !p.isPassenger(),
                (p, plek) -> aarde(p)));
        Krachten.registreer(new Krachten.Kracht(HANDVLAM, Einde2Ids.Element.VUUR, "minecraft:blaze_powder", 16,
                (p, voet) -> geleerd(p, Einde2Ids.Element.VUUR) && !p.isUnderWater(),
                (p, plek) -> vuurschicht(p)));
        Krachten.NA_GEBRUIK.add((p, k) -> {
            if (k.id().startsWith("zij_")) {
                nl.juiced.guhs.quest.GuhAdvancements.grant(p, "einde2_zij_kracht");
            }
        });
    }

    /** (dev run only: {@code /guhs einde2 zij krachten}) the elements a tester switched on by hand, a bit per element. */
    public static final String DEV_ELEMENTEN = "guhs_einde2_zij_dev_elementen";

    /**
     * Has this player learnt the element (the chapter's flag "geleerd", or the chapter done)? In a dev run (and the game
     * tests) also when a tester switched it on with the dev command, so the side places can be played before the chapters
     * are merged.
     */
    public static boolean geleerd(ServerPlayer p, Einde2Ids.Element e) {
        if (E2Verhaal.elementGeleerd(p, e)) {
            return true;
        }
        return !net.neoforged.fml.loading.FMLEnvironment.isProduction()
                && (nl.juiced.guhs.quest.GuhQuests.saved(p).getIntOr(DEV_ELEMENTEN, 0) & (1 << e.ordinal())) != 0;
    }

    /** (dev, tests) switches an element on or off for this player by hand. */
    public static void zetDev(ServerPlayer p, Einde2Ids.Element e, boolean aan) {
        int bits = nl.juiced.guhs.quest.GuhQuests.saved(p).getIntOr(DEV_ELEMENTEN, 0);
        bits = aan ? bits | (1 << e.ordinal()) : bits & ~(1 << e.ordinal());
        nl.juiced.guhs.quest.GuhQuests.saved(p).putInt(DEV_ELEMENTEN, bits);
    }

    /**
     * May a power of ours hurt or throw this entity? Only a monster ({@link Enemy}) that is not one of ours that is kind
     * (a guh, a guh NPC, the cast, an Ender-Mika trader). Players are asked separately (PvP).
     */
    public static boolean vijand(Entity e) {
        return e instanceof LivingEntity l && l.isAlive() && e instanceof Enemy && !(e instanceof GuhEntity) && !(e instanceof GuhNpcEntity)
                && !(e instanceof CastWezen) && !(e instanceof EnderMikaEntity);
    }

    // =====================================================================================================================
    // air
    // =====================================================================================================================

    static void luchtsprong(ServerPlayer p) {
        boolean grond = p.onGround();
        Vec3 kijk = p.getLookAngle();
        Vec3 vooruit = new Vec3(kijk.x, 0, kijk.z);
        vooruit = vooruit.lengthSqr() < 1e-4 ? Vec3.ZERO : vooruit.normalize().scale(grond ? 0.25 : 0.4);
        Vec3 nu = p.getDeltaMovement();
        p.setDeltaMovement(nu.x * 0.5 + vooruit.x, grond ? 0.9 : 0.75, nu.z * 0.5 + vooruit.z);
        p.hurtMarked = true;
        p.resetFallDistance();
        if (!grond) {
            GESPRONGEN.add(p.getUUID());
        }
        ZACHT.add(p.getUUID());
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 10, 0.35, 0.05, 0.35, 0.03);
        level.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    // =====================================================================================================================
    // water
    // =====================================================================================================================

    static void waterloop(ServerPlayer p) {
        long tot = p.level().getGameTime() + WATERLOOP_TICKS;
        WATER_TOT.put(p.getUUID(), tot);
        ZACHT.add(p.getUUID());
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.2, p.getZ(), 24, 0.5, 0.1, 0.5, 0.1);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5f, 1.4f);
        p.sendOverlayMessage(Component.translatable(GUI + "waterloop").withStyle(ChatFormatting.AQUA));
    }

    /** Until which game time this player walks on water (0: not now). */
    public static long waterTot(ServerPlayer p) {
        Long t = WATER_TOT.get(p.getUUID());
        return t == null || t < p.level().getGameTime() ? 0 : t;
    }

    // =====================================================================================================================
    // earth: the rock pillar (tap) and the rock wall (sneak)
    // =====================================================================================================================

    static void aarde(ServerPlayer p) {
        if (p.isShiftKeyDown()) {
            rotsmuur(p);
        } else {
            rotszuil(p);
        }
    }

    /** The ground spot the player aims at, up to {@link #ZUIL_BEREIK} blocks: the block whose top the pillar breaks through. */
    @Nullable
    private static BlockPos grondDoel(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 oog = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(oog, oog.add(p.getLookAngle().scale(ZUIL_BEREIK)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        if (hit.getDirection() == Direction.UP) {
            return hit.getBlockPos();
        }
        BlockPos cel = hit.getBlockPos().relative(hit.getDirection());   // (a wall or a ceiling: the ground in front of it)
        return grondOnder(level, cel, 4);
    }

    /** The first solid block under this (air) cell, at most {@code diep} down; null: none. */
    @Nullable
    private static BlockPos grondOnder(ServerLevel level, BlockPos cel, int diep) {
        BlockPos c = cel;
        for (int i = 0; i <= diep; i++) {
            if (!vrij(level, c)) {
                return i == 0 ? null : c;
            }
            BlockPos onder = c.below();
            if (level.getBlockState(onder).isFaceSturdy(level, onder, Direction.UP)) {
                return onder;
            }
            c = onder;
        }
        return null;
    }

    static void rotszuil(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos basis = grondDoel(p);
        BlockPos voet = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        if (basis == null && p.onGround() && p.getXRot() > 60f) {
            basis = voet;                                               // (looking straight down: your own feet)
        }
        if (basis == null) {
            mis(p);
            return;
        }
        double dx = basis.getX() + 0.5 - p.getX(), dz = basis.getZ() + 0.5 - p.getZ();
        boolean onder = dx * dx + dz * dz < 1.3 * 1.3 && Math.abs(basis.getY() - voet.getY()) <= 1 && p.onGround();
        if (onder) {
            basis = voet;
        }
        // the eruption: the monsters on the spot fly up first
        AABB kolom = new AABB(basis.getX() - 0.4, basis.getY() + 1, basis.getZ() - 0.4, basis.getX() + 1.4, basis.getY() + 1 + ZUIL_HOOG,
                basis.getZ() + 1.4);
        Set<Entity> gegooid = new HashSet<>();
        for (Entity e : level.getEntities(p, kolom, ZijKrachten::vijand)) {
            Vec3 weg = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
            weg = weg.lengthSqr() < 1e-4 ? Vec3.ZERO : weg.normalize().scale(0.35);
            e.setDeltaMovement(weg.x, 1.15, weg.z);
            e.hurtMarked = true;
            gegooid.add(e);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
        }
        List<List<BlockPos>> rijen = new ArrayList<>();
        for (int i = 1; i <= ZUIL_HOOG; i++) {
            BlockPos pos = basis.above(i);
            if (!vrij(level, pos) || !mag(p, pos) || (!onder && botst(level, pos, p, gegooid))) {
                break;
            }
            if (onder && !vrij(level, basis.above(i + 2)) && !level.getBlockState(basis.above(i + 2)).getCollisionShape(level, basis.above(i + 2)).isEmpty()) {
                break;                                                  // (no room for the player's head on top)
            }
            rijen.add(List.of(pos));
        }
        BlockState grond = level.getBlockState(basis);
        BlockState stof = grond.isAir() ? STEEN : grond;
        Vec3 c = Vec3.atBottomCenterOf(basis.above());
        level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, stof), c.x, c.y, c.z, 40, 0.5, 0.2, 0.5, 0.3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stof), c.x, c.y + 0.2, c.z, 30, 0.7, 0.1, 0.7, 0.2);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.2f, 0.6f);
        if (rijen.isEmpty()) {
            if (onder) {
                sprong(p, grond);                                       // (the ground may not change here: the earth throws you up)
            }
            return;
        }
        begin(p, new Rots(level, rijen, ZUIL_TICKS, onder ? basis : null, new int[]{0}));
    }

    /** The rock wall: {@link #MUUR_BREED} wide, {@link #MUUR_HOOG} high, three blocks in front of the player, across the look. */
    static void rotsmuur(ServerPlayer p) {
        ServerLevel level = p.level();
        Direction voor = p.getDirection();
        Direction zij = voor.getClockWise();
        BlockPos midden = BlockPos.containing(p.getX(), p.getY() + 0.5, p.getZ()).relative(voor, 3);
        List<List<BlockPos>> rijen = new ArrayList<>();
        for (int i = 0; i < MUUR_HOOG; i++) {
            rijen.add(new ArrayList<>());
        }
        Set<Entity> gegooid = new HashSet<>();
        int half = MUUR_BREED / 2;
        for (int s = -half; s <= half; s++) {
            BlockPos cel = midden.relative(zij, s);
            BlockPos grond = vrij(level, cel) ? grondOnder(level, cel, 3) : grondOnder(level, cel.above(), 1);
            if (grond == null) {
                continue;
            }
            // monsters right at the wall are shoved back
            for (Entity e : level.getEntities(p, new AABB(grond.above()).expandTowards(0, MUUR_HOOG - 1, 0).inflate(0.6), ZijKrachten::vijand)) {
                if (gegooid.add(e)) {
                    Vec3 weg = new Vec3(voor.getStepX(), 0, voor.getStepZ()).scale(0.9);
                    e.setDeltaMovement(weg.x, 0.45, weg.z);
                    e.hurtMarked = true;
                }
            }
            for (int i = 1; i <= MUUR_HOOG; i++) {
                BlockPos pos = grond.above(i);
                if (!vrij(level, pos) || !mag(p, pos) || botst(level, pos, p, gegooid)) {
                    break;
                }
                rijen.get(i - 1).add(pos);
            }
            BlockState g = level.getBlockState(grond);
            level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, g.isAir() ? STEEN : g), grond.getX() + 0.5, grond.getY() + 1.0,
                    grond.getZ() + 0.5, 12, 0.4, 0.1, 0.4, 0.2);
        }
        rijen.removeIf(List::isEmpty);
        level.playSound(null, midden, SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.3f, 0.5f);
        level.playSound(null, midden, SoundEvents.DRIPSTONE_BLOCK_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
        if (rijen.isEmpty()) {
            p.sendOverlayMessage(Component.translatable(GUI + "aarde_hier").withStyle(ChatFormatting.GRAY));
            return;
        }
        begin(p, new Rots(level, rijen, MUUR_TICKS, null, new int[]{0}));
    }

    private static void begin(ServerPlayer p, Rots r) {
        Rots oud = RIJZEND.put(p.getUUID(), r);
        if (oud != null) {
            stapRots(p.getUUID(), oud, true);
        }
        stapRots(p.getUUID(), r, false);                                // (the first row at once: it bursts up)
    }

    /** Nothing to aim at: no cooldown, a small puff and a word. */
    private static void mis(ServerPlayer p) {
        Krachten.klaar(p, AARDZUIL);
        p.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, STEEN), p.getX(), p.getY() + 0.1, p.getZ(), 8, 0.3, 0.05, 0.3, 0.05);
        p.sendOverlayMessage(Component.translatable(GUI + "aarde_mis").withStyle(ChatFormatting.GRAY));
    }

    /** A cell the rock may fill: air, or a soft plant without water or a block entity (that comes back afterwards). */
    private static boolean vrij(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) {
            return true;
        }
        return st.canBeReplaced() && st.getFluidState().isEmpty() && !st.hasBlockEntity() && !st.is(E2ZijFeature.AARDZUIL.get())
                && st.getCollisionShape(level, pos).isEmpty();
    }

    /** Is somebody (other than the one bending, or a monster that is being thrown) in the way of a rock block? */
    private static boolean botst(ServerLevel level, BlockPos pos, ServerPlayer wie, Set<Entity> gegooid) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), e -> e != wie && !gegooid.contains(e) && !e.isSpectator()).isEmpty();
    }

    /** Where the ground may not change: the earth throws the player up instead (a bit lower than the air leap). */
    private static void sprong(ServerPlayer p, BlockState grond) {
        Vec3 nu = p.getDeltaMovement();
        p.setDeltaMovement(nu.x * 0.4, 0.8, nu.z * 0.4);
        p.hurtMarked = true;
        p.resetFallDistance();
        ZACHT.add(p.getUUID());
        ServerLevel level = p.level();
        BlockState stof = grond.isAir() ? STEEN : grond;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stof), p.getX(), p.getY() + 0.1, p.getZ(), 30, 0.4, 0.1, 0.4, 0.15);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
    }

    /** May this player's earth bending put a block here (never in a protected building or someone else's huisje)? */
    static boolean mag(ServerPlayer p, BlockPos pos) {
        return p.mayBuild() && Bescherming.magWijzigen(p.level(), pos, p.getUUID()) && !nl.juiced.guhs.feature.einde2.Einde2Bescherming.beschermd(p.level(), pos);
    }

    /** One row of rising rock (or all of them: {@code alles}). */
    private static void stapRots(UUID wie, Rots r, boolean alles) {
        ServerLevel level = r.level();
        while (r.gedaan()[0] < r.rijen().size()) {
            int i = r.gedaan()[0]++;
            boolean iets = false;
            for (BlockPos pos : r.rijen().get(i)) {
                if (!vrij(level, pos)) {
                    continue;
                }
                zet(level, pos, r.duur() + i * 4 + Math.floorMod(pos.getX() * 7 + pos.getZ() * 3, 5) * 3);
                iets = true;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, STEEN), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.4,
                        0.1, 0.4, 0.05);
            }
            if (iets) {
                level.playSound(null, r.rijen().get(i).get(0), SoundEvents.TUFF_PLACE, SoundSource.PLAYERS, 0.8f, 0.6f + i * 0.08f);
            }
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(wie);
            BlockPos t = r.tillen();
            if (t != null && iets && p != null && p.level() == level && Math.abs(p.getX() - (t.getX() + 0.5)) < 1.2
                    && Math.abs(p.getZ() - (t.getZ() + 0.5)) < 1.2) {
                p.teleportTo(level, p.getX(), t.getY() + i + 2.0, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), false);
                p.resetFallDistance();
                ZACHT.add(wie);
            }
            if (!alles) {
                break;
            }
        }
        if (r.gedaan()[0] >= r.rijen().size()) {
            RIJZEND.remove(wie, r);
        }
    }

    /** Puts one rock block here, remembering what stood there, crumbling after {@code ticks}. */
    private static void zet(ServerLevel level, BlockPos pos, int ticks) {
        BlockState was = level.getBlockState(pos);
        ONDER.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), was.isAir() ? Blocks.AIR.defaultBlockState() : was);
        level.setBlock(pos, E2ZijFeature.AARDZUIL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.scheduleTick(pos, E2ZijFeature.AARDZUIL.get(), ticks);
    }

    /** (the rock block crumbles) what stood here before: the plant, else air. */
    static BlockState terug(ServerLevel level, BlockPos pos) {
        Map<BlockPos, BlockState> m = ONDER.get(level.dimension());
        BlockState was = m == null ? null : m.remove(pos);
        return was == null || !was.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : was;
    }

    /** (the server stops) every rock block crumbles now, so none outlives a restart. */
    static void allesTerug(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, BlockState>> e : ONDER.entrySet()) {
            ServerLevel level = server.getLevel(e.getKey());
            if (level == null) {
                continue;
            }
            for (BlockPos pos : List.copyOf(e.getValue().keySet())) {
                if (level.isLoaded(pos) && level.getBlockState(pos).is(E2ZijFeature.AARDZUIL.get())) {
                    level.setBlock(pos, terug(level, pos), Block.UPDATE_ALL);
                }
            }
        }
        ONDER.clear();
        RIJZEND.clear();
        SCHICHTEN.clear();
    }

    // =====================================================================================================================
    // fire: the fire bolt
    // =====================================================================================================================

    static void vuurschicht(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 kijk = p.getLookAngle();
        float yaw = (float) Math.toRadians(p.getYRot());
        Vec3 rechts = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.25);   // (from the right paw)
        Vec3 start = p.getEyePosition().add(kijk.scale(0.5)).add(rechts).add(0, -0.2, 0);
        Schicht s = new Schicht(p.getUUID(), level, start, kijk.scale(SCHICHT_SNEL));
        level.sendParticles(ParticleTypes.FLAME, start.x, start.y, start.z, 6, 0.08, 0.08, 0.08, 0.04);
        level.sendParticles(ParticleTypes.LAVA, start.x, start.y, start.z, 1, 0, 0, 0, 0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6f, 1.5f);
        if (!vlieg(s)) {
            SCHICHTEN.add(s);
        }
    }

    /** Every server tick: rising rock rises a row, the bolts in flight fly on. */
    static void serverTick() {
        for (Map.Entry<UUID, Rots> e : List.copyOf(RIJZEND.entrySet())) {
            stapRots(e.getKey(), e.getValue(), false);
        }
        if (SCHICHTEN.isEmpty()) {
            return;
        }
        for (Iterator<Schicht> it = SCHICHTEN.iterator(); it.hasNext(); ) {
            Schicht s = it.next();
            if (vlieg(s)) {
                it.remove();
            }
        }
    }

    /** One tick of a bolt. True: it is done (it hit, it went out or it flew its reach). */
    private static boolean vlieg(Schicht s) {
        ServerLevel level = s.level;
        ServerPlayer schutter = level.getServer().getPlayerList().getPlayer(s.schutter);
        Vec3 van = s.pos;
        double rest = SCHICHT_BEREIK - s.gevlogen;
        Vec3 stap = s.snel.length() > rest ? s.snel.normalize().scale(rest) : s.snel;
        Vec3 naar = van.add(stap);
        BlockHitResult blok = level.clip(new ClipContext(van, naar, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                schutter != null ? net.minecraft.world.phys.shapes.CollisionContext.of(schutter) : net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (blok.getType() == HitResult.Type.BLOCK) {
            naar = blok.getLocation();
        }
        EntityHitResult raak = ProjectileUtil.getEntityHitResult(level, schutter, van, naar, new AABB(van, naar).inflate(1.0),
                e -> e != schutter && !e.isSpectator() && raakbaar(schutter, e), 0.3f);
        if (raak != null) {
            naar = raak.getLocation();
        }
        spoor(level, van, naar);
        s.gevlogen += naar.distanceTo(van);
        s.pos = naar;
        if (raak != null) {
            raakWezen(level, schutter, raak.getEntity(), naar);
            return true;
        }
        if (blok.getType() == HitResult.Type.BLOCK) {
            raakBlok(level, schutter, blok, naar);
            return true;
        }
        if (s.gevlogen >= SCHICHT_BEREIK - 1e-3) {
            level.sendParticles(ParticleTypes.SMOKE, naar.x, naar.y, naar.z, 5, 0.1, 0.1, 0.1, 0.01);
            return true;
        }
        return false;
    }

    /** May the bolt hit this entity: a monster, or another player where PvP is on (vanilla's rule, with teams). */
    private static boolean raakbaar(@Nullable ServerPlayer schutter, Entity e) {
        if (vijand(e)) {
            return true;
        }
        return e instanceof Player ander && schutter != null && ander != schutter && schutter.canHarmPlayer(ander);
    }

    /** The trail of the bolt along this tick's path: flames for everybody, a few sparks and a thin smoke behind. */
    private static void spoor(ServerLevel level, Vec3 van, Vec3 naar) {
        Vec3 d = naar.subtract(van);
        int n = Math.max(1, (int) Math.ceil(d.length() / 0.25));
        for (int i = 0; i < n; i++) {
            Vec3 q = van.add(d.scale((i + 0.5) / n));
            level.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0.005);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SMALL_FLAME, q.x, q.y, q.z, 1, 0.06, 0.06, 0.06, 0.01);
            }
        }
        level.sendParticles(ParticleTypes.SMOKE, van.x, van.y, van.z, 1, 0.03, 0.03, 0.03, 0.005);
    }

    private static void raakWezen(ServerLevel level, @Nullable ServerPlayer schutter, Entity e, Vec3 waar) {
        level.sendParticles(ParticleTypes.FLAME, waar.x, waar.y, waar.z, 14, 0.2, 0.2, 0.2, 0.06);
        level.sendParticles(ParticleTypes.LAVA, waar.x, waar.y, waar.z, 2, 0.1, 0.1, 0.1, 0);
        level.playSound(null, waar.x, waar.y, waar.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        if (!(e instanceof LivingEntity l) || schutter == null) {
            return;
        }
        if (l.hurtServer(level, level.damageSources().playerAttack(schutter), SCHICHT_SCHADE)) {
            l.igniteForSeconds(SCHICHT_BRAND);
        }
    }

    private static void raakBlok(ServerLevel level, @Nullable ServerPlayer schutter, BlockHitResult hit, Vec3 waar) {
        BlockPos pos = hit.getBlockPos();
        BlockState st = level.getBlockState(pos);
        if (!st.getFluidState().isEmpty() && st.getCollisionShape(level, pos).isEmpty()) {
            level.sendParticles(ParticleTypes.CLOUD, waar.x, waar.y, waar.z, 6, 0.15, 0.05, 0.15, 0.02);
            level.playSound(null, waar.x, waar.y, waar.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
            return;
        }
        if (schutter != null && aansteekbaar(st) && mag(schutter, pos)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
            return;
        }
        level.sendParticles(ParticleTypes.FLAME, waar.x, waar.y, waar.z, 10, 0.15, 0.15, 0.15, 0.04);
        level.sendParticles(ParticleTypes.SMOKE, waar.x, waar.y, waar.z, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, waar.x, waar.y, waar.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3f, 1.8f);
    }

    private static boolean aansteekbaar(BlockState st) {
        return st.hasProperty(BlockStateProperties.LIT) && !st.getValue(BlockStateProperties.LIT)
                && (st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof CandleBlock || st.getBlock() instanceof CandleCakeBlock)
                && (!st.hasProperty(BlockStateProperties.WATERLOGGED) || !st.getValue(BlockStateProperties.WATERLOGGED));
    }

    /** (tests) how many fire bolts are in flight. */
    static int schichten() {
        return SCHICHTEN.size();
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    static void tick(ServerPlayer p) {
        UUID id = p.getUUID();
        if (p.onGround() || p.isInWater() || p.onClimbable()) {
            GESPRONGEN.remove(id);
            if (p.onGround() && p.tickCount % 4 == 0) {
                ZACHT.remove(id);
            }
        }
        if (waterTot(p) > 0) {
            p.resetFallDistance();
        }
    }

    /** A landing after a leap of ours does not hurt. */
    static boolean zachteLanding(ServerPlayer p) {
        return ZACHT.contains(p.getUUID()) || waterTot(p) > 0;
    }

    static void vergeet(UUID id) {
        GESPRONGEN.remove(id);
        ZACHT.remove(id);
        WATER_TOT.remove(id);
        Rots r = RIJZEND.remove(id);
        if (r != null) {
            stapRots(id, r, true);
        }
    }

    /** (tests) the rock this player is raising stands up at once. */
    static void zuilNu(ServerPlayer p) {
        Rots r = RIJZEND.get(p.getUUID());
        if (r != null) {
            stapRots(p.getUUID(), r, true);
        }
    }
}
