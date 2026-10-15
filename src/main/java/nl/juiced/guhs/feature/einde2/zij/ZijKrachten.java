package nl.juiced.guhs.feature.einde2.zij;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * einde2 zij (DESIGN 5 "Wat je overhoudt", CONTRACT_E2_B 7.6): the four LASTING bending powers, light ones, each once its
 * element is learnt ({@link E2Verhaal#elementGeleerd}), anywhere, on the one key of {@link Krachten} (no item):
 * <ul>
 *   <li>{@link #LUCHTSPRONG} air: a leap; once more in mid-air (the double jump); the landing never hurts;</li>
 *   <li>{@link #WATERLOOP} water: walk on water for {@link #WATERLOOP_TICKS} ticks (the client keeps you on the surface,
 *       {@code client.E2ZijClient}); sneak to sink;</li>
 *   <li>{@link #AARDZUIL} earth: one small pillar of {@link #ZUIL_HOOG} blocks where you look, or under your own feet (it
 *       lifts you), that crumbles after {@link #ZUIL_TICKS} ticks and drops nothing; where the ground is protected (a
 *       building, somebody's huisje) the earth only throws you up a bit;</li>
 *   <li>{@link #HANDVLAM} fire: lights a candle or campfire you look at (where you may change blocks), else a small flame in
 *       your paw for {@link #HANDVLAM_TICKS} ticks that lights up the dark around you (for you: {@link Schijn}).</li>
 * </ul>
 * Nothing here can hurt anybody or change a protected building.
 */
public final class ZijKrachten {
    public static final String LUCHTSPRONG = "zij_luchtsprong", WATERLOOP = "zij_waterloop", AARDZUIL = "zij_aardzuil", HANDVLAM = "zij_handvlam";
    public static final int WATERLOOP_TICKS = 300, HANDVLAM_TICKS = 600, ZUIL_TICKS = 160, ZUIL_HOOG = 3;
    static final String GUI = "gui.guhs.einde2_zij.kracht.";

    /** Players who used the air leap while in the air (once until they land). */
    private static final Set<UUID> GESPRONGEN = ConcurrentHashMap.newKeySet();
    /** Players whose next landing does not hurt (after a leap of ours). */
    private static final Set<UUID> ZACHT = ConcurrentHashMap.newKeySet();
    /** Player -> game time until which they walk on water / carry a flame. */
    private static final Map<UUID, Long> WATER_TOT = new ConcurrentHashMap<>(), VLAM_TOT = new ConcurrentHashMap<>();
    /** Player -> the spot of the flame's light they see now. */
    private static final Map<UUID, BlockPos> VLAM_LICHT = new ConcurrentHashMap<>();

    /** A rising pillar: one block every two ticks. */
    private record Zuil(ServerLevel level, BlockPos basis, int hoogte, boolean onder, int[] gedaan) {
    }

    private static final Map<UUID, Zuil> RIJZEND = new ConcurrentHashMap<>();

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
        Krachten.registreer(new Krachten.Kracht(AARDZUIL, Einde2Ids.Element.AARDE, "minecraft:packed_mud", 100,
                (p, voet) -> geleerd(p, Einde2Ids.Element.AARDE) && !p.isPassenger(),
                (p, plek) -> aardzuil(p)));
        Krachten.registreer(new Krachten.Kracht(HANDVLAM, Einde2Ids.Element.VUUR, "minecraft:blaze_powder", 30,
                (p, voet) -> geleerd(p, Einde2Ids.Element.VUUR) && !p.isUnderWater(),
                (p, plek) -> handvlam(p)));
        Schijn.registreer(ZijKrachten::vlamLicht);
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
    // earth
    // =====================================================================================================================

    static void aardzuil(ServerPlayer p) {
        ServerLevel level = p.level();
        HitResult hit = p.pick(6.0, 1f, false);
        BlockPos basis = null;
        boolean onder = true;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK && bh.getDirection() == Direction.UP) {
            double dx = bh.getBlockPos().getX() + 0.5 - p.getX(), dz = bh.getBlockPos().getZ() + 0.5 - p.getZ();
            if (dx * dx + dz * dz > 1.6 * 1.6) {
                basis = bh.getBlockPos();
                onder = false;
            }
        }
        if (onder) {
            BlockPos voet = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
            if (!p.onGround() || level.getBlockState(voet).isAir()) {
                sprong(p, level.getBlockState(voet).isAir() ? Blocks.PACKED_MUD.defaultBlockState() : level.getBlockState(voet));
                return;
            }
            basis = voet;
        }
        int hoogte = 0;
        for (int i = 1; i <= ZUIL_HOOG; i++) {
            BlockPos pos = basis.above(i);
            BlockState st = level.getBlockState(pos);
            boolean vrij = st.isAir() || (st.canBeReplaced() && st.getFluidState().isEmpty());
            if (!vrij || !mag(p, pos) || (!onder && botst(level, pos, p))) {
                break;
            }
            if (onder && !level.getBlockState(basis.above(i + 2)).getCollisionShape(level, basis.above(i + 2)).isEmpty()) {
                break;                                                  // (no room for the player's head on top)
            }
            hoogte = i;
        }
        if (hoogte == 0) {
            sprong(p, level.getBlockState(basis));
            return;
        }
        Zuil oud = RIJZEND.put(p.getUUID(), new Zuil(level, basis, hoogte, onder, new int[]{0}));
        if (oud != null) {
            stapZuil(p.getUUID(), oud, true);
        }
        level.playSound(null, basis.above(), SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
    }

    /** Is a player (other than the one bending) in the way of a pillar block? */
    private static boolean botst(ServerLevel level, BlockPos pos, ServerPlayer wie) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(pos), e -> e != wie).isEmpty();
    }

    /** Where the ground may not change: the earth throws the player up instead (a bit lower than the air leap). */
    private static void sprong(ServerPlayer p, BlockState grond) {
        Vec3 nu = p.getDeltaMovement();
        p.setDeltaMovement(nu.x * 0.4, 0.8, nu.z * 0.4);
        p.hurtMarked = true;
        p.resetFallDistance();
        ZACHT.add(p.getUUID());
        ServerLevel level = p.level();
        BlockState stof = grond.isAir() ? Blocks.PACKED_MUD.defaultBlockState() : grond;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stof), p.getX(), p.getY() + 0.1, p.getZ(), 30, 0.4, 0.1, 0.4, 0.15);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
    }

    /** May this player's earth bending put a block here (never in a protected building or someone else's huisje)? */
    static boolean mag(ServerPlayer p, BlockPos pos) {
        return p.mayBuild() && Bescherming.magWijzigen(p.level(), pos, p.getUUID()) && !nl.juiced.guhs.feature.einde2.Einde2Bescherming.beschermd(p.level(), pos);
    }

    /** One step of a rising pillar (or all at once: {@code alles}). */
    private static void stapZuil(UUID wie, Zuil z, boolean alles) {
        ServerLevel level = z.level();
        while (z.gedaan()[0] < z.hoogte()) {
            int i = ++z.gedaan()[0];
            BlockPos pos = z.basis().above(i);
            BlockState st = level.getBlockState(pos);
            if (!(st.isAir() || (st.canBeReplaced() && st.getFluidState().isEmpty()))) {
                z.gedaan()[0] = z.hoogte();
                break;
            }
            level.setBlock(pos, E2ZijFeature.AARDZUIL.get().defaultBlockState(), Block.UPDATE_ALL);
            level.scheduleTick(pos, E2ZijFeature.AARDZUIL.get(), ZUIL_TICKS + i * 3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_MUD.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 0.9,
                    pos.getZ() + 0.5, 8, 0.4, 0.1, 0.4, 0.05);
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(wie);
            if (z.onder() && p != null && p.level() == level && Math.abs(p.getX() - (z.basis().getX() + 0.5)) < 1.2
                    && Math.abs(p.getZ() - (z.basis().getZ() + 0.5)) < 1.2) {
                p.teleportTo(level, p.getX(), pos.getY() + 1.0, p.getZ(), Set.of(), p.getYRot(), p.getXRot(), false);
                p.resetFallDistance();
            }
            if (!alles) {
                break;
            }
        }
        if (z.gedaan()[0] >= z.hoogte()) {
            RIJZEND.remove(wie, z);
        }
    }

    // =====================================================================================================================
    // fire
    // =====================================================================================================================

    static void handvlam(ServerPlayer p) {
        ServerLevel level = p.level();
        HitResult hit = p.pick(5.0, 1f, false);
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            BlockState st = level.getBlockState(pos);
            if (aansteekbaar(st) && mag(p, pos)) {
                level.setBlock(pos, st.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
                level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
                return;
            }
        }
        UUID id = p.getUUID();
        if (VLAM_TOT.remove(id) != null) {
            Schijn.straks(p);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.6f);
            return;
        }
        VLAM_TOT.put(id, level.getGameTime() + HANDVLAM_TICKS);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        Schijn.straks(p);
    }

    private static boolean aansteekbaar(BlockState st) {
        return st.hasProperty(BlockStateProperties.LIT) && !st.getValue(BlockStateProperties.LIT)
                && (st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof CandleBlock || st.getBlock() instanceof CandleCakeBlock)
                && (!st.hasProperty(BlockStateProperties.WATERLOGGED) || !st.getValue(BlockStateProperties.WATERLOGGED));
    }

    /** Does this player carry the flame now? */
    public static boolean vlam(ServerPlayer p) {
        Long t = VLAM_TOT.get(p.getUUID());
        return t != null && t >= p.level().getGameTime();
    }

    /** {@link Schijn}: the light of the flame, at the player's head, only where the real world is air. */
    private static void vlamLicht(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        BlockPos licht = VLAM_LICHT.get(p.getUUID());
        if (licht != null && vlam(p) && p.level().getBlockState(licht).isAir()) {
            gewenst.put(licht, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13));
        }
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
        Zuil z = RIJZEND.get(id);
        if (z != null && p.tickCount % 2 == 0) {
            stapZuil(id, z, false);
        }
        if (waterTot(p) > 0) {
            p.resetFallDistance();
        }
        Long vlamTot = VLAM_TOT.get(id);
        if (vlamTot != null) {
            if (vlamTot < p.level().getGameTime() || p.isUnderWater()) {
                VLAM_TOT.remove(id);
                VLAM_LICHT.remove(id);
                Schijn.straks(p);
                if (p.isUnderWater()) {
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.6f);
                }
            } else {
                BlockPos hoofd = BlockPos.containing(p.getEyePosition());
                if (!hoofd.equals(VLAM_LICHT.get(id))) {
                    VLAM_LICHT.put(id, hoofd);
                    if (p.tickCount % 3 == 0) {
                        Schijn.straks(p);
                    }
                }
                if (p.tickCount % 3 == 0) {
                    Vec3 hand = hand(p);
                    p.level().sendParticles(ParticleTypes.SMALL_FLAME, hand.x, hand.y, hand.z, 2, 0.04, 0.05, 0.04, 0.005);
                    if (p.tickCount % 9 == 0) {
                        p.level().sendParticles(ParticleTypes.FLAME, hand.x, hand.y + 0.05, hand.z, 1, 0.02, 0.03, 0.02, 0.003);
                    }
                }
            }
        }
    }

    /** About where the player's right paw is. */
    private static Vec3 hand(ServerPlayer p) {
        float yaw = (float) Math.toRadians(p.yBodyRot);
        double rx = -Math.cos(yaw) * 0.38, rz = -Math.sin(yaw) * 0.38;
        double fx = -Math.sin(yaw) * 0.3, fz = Math.cos(yaw) * 0.3;
        return new Vec3(p.getX() + rx + fx, p.getY() + 1.05, p.getZ() + rz + fz);
    }

    /** A landing after a leap of ours does not hurt. */
    static boolean zachteLanding(ServerPlayer p) {
        return ZACHT.contains(p.getUUID()) || waterTot(p) > 0;
    }

    static void vergeet(UUID id) {
        GESPRONGEN.remove(id);
        ZACHT.remove(id);
        WATER_TOT.remove(id);
        VLAM_TOT.remove(id);
        VLAM_LICHT.remove(id);
        Zuil z = RIJZEND.remove(id);
        if (z != null) {
            stapZuil(id, z, true);
        }
    }

    /** (tests) the pillar this player is raising stands up at once. */
    static void zuilNu(ServerPlayer p) {
        Zuil z = RIJZEND.get(p.getUUID());
        if (z != null) {
            stapZuil(p.getUUID(), z, true);
        }
    }

    /** (dev, tests) the flame's light spot of this player (null: none). */
    @Nullable
    static BlockPos lichtVan(ServerPlayer p) {
        return VLAM_LICHT.get(p.getUUID());
    }
}
