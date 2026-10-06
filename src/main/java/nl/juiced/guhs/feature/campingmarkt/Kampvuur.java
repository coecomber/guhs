package nl.juiced.guhs.feature.campingmarkt;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kamperen.LuisterGoal;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (camping-markt): the big camp fire of the Grillcamping ({@link CampingmarktBlocks.KampvuurBlock}).
 * <ul>
 *   <li>Step 3 of the camping questline: the bundle of fire wood on it lights it and the camp fire party starts: the
 *       residents come and sit around it and hop, a little tune plays, sparks fly ({@link #feest}).</li>
 *   <li>It burns {@link #BRAND_TICKS} ticks and then goes out by itself ({@link Herstel}), so the next player can light it
 *       for their own party. Whoever has done step 3 pokes it up again with a click (for the marshmallows of step 4, and
 *       later for fun); wood of another player on a burning fire counts for that player too.</li>
 *   <li>Roasting ({@link RoosterstokItem}) works over this fire and over any burning vanilla camp fire.</li>
 * </ul>
 * Outside a protected building it is a deco block: flint and steel lights it for good, a shovel puts it out.
 */
public final class Kampvuur {
    /** How long the fire burns after wood was put on it or it was poked up (two minutes). */
    public static final int BRAND_TICKS = 2400;
    /** The burning fire ticks once per this many ticks. */
    public static final int FEEST_STAP = 10;
    /** Residents this close come to the fire. */
    public static final int FEEST_BEREIK = 20;
    /** The camp fire tune: note block semitones (0..24), one per {@link #FEEST_STAP}; -1 = a rest. */
    private static final int[] WIJSJE = {12, -1, 12, 14, 16, -1, 12, -1, 16, -1, 14, -1, 7, -1, -1, -1, 12, -1, 12, 14, 16, -1, 12, -1, 11, -1, 7, -1, -1, -1, -1, -1,
            12, -1, 12, 14, 16, 17, 16, 14, 12, 11, 9, -1, 7, 9, 11, -1, 12, -1, 7, -1, 4, -1, 7, -1, 12, -1, -1, -1, -1, -1, -1, -1};

    private Kampvuur() {
    }

    /** Is there a fire to roast over here: this slice's camp fire burning, or a lit vanilla camp fire? */
    public static boolean brandt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CampingmarktBlocks.KampvuurBlock) {
            return state.getValue(CampingmarktBlocks.KampvuurBlock.BRANDT);
        }
        return state.is(BlockTags.CAMPFIRES) && CampfireBlock.isLitCampfire(state);
    }

    /** The burning fire this player looks at (within five blocks), or null. Works on both sides. */
    @Nullable
    public static BlockPos inZicht(Player player) {
        Vec3 oog = player.getEyePosition();
        Vec3 eind = oog.add(player.getLookAngle().scale(5.0));
        BlockHitResult hit = player.level().clip(new ClipContext(oog, eind, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && brandt(player.level(), hit.getBlockPos()) ? hit.getBlockPos() : null;
    }

    /** A player clicked the big camp fire. */
    public static void klik(ServerPlayer p, BlockPos pos, BlockState state, InteractionHand hand) {
        ServerLevel level = p.level();
        if (!(state.getBlock() instanceof CampingmarktBlocks.KampvuurBlock)) {
            return;
        }
        boolean brandt = state.getValue(CampingmarktBlocks.KampvuurBlock.BRANDT);
        ItemStack stack = p.getItemInHand(hand);
        Verhaallijn lijn = CampingmarktFeature.CAMPING;
        int stap = lijn.stap(p);
        boolean gebouw = Bescherming.beschermd(level, pos);
        if (stack.is(CampingmarktFeature.BRANDHOUT.get())) {
            if (stap != 3) {
                Kamperen.meld(p, "vuur.hout_later", ChatFormatting.LIGHT_PURPLE);
                return;
            }
            GuhQuests.take(p, CampingmarktFeature.BRANDHOUT.get(), 64);
            steekAan(level, pos, true);
            lijn.verder(p, 3);
            Kamperen.meld(p, "vuur.feest", ChatFormatting.GOLD);
            GuhQuests.hint(p, "quest.guhs.campingmarkt.hint.marshmallows");
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            return;
        }
        if (!gebouw && stack.is(Items.FLINT_AND_STEEL) && !brandt) {
            // at home: lit for good
            level.setBlock(pos, state.setValue(CampingmarktBlocks.KampvuurBlock.BRANDT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), FEEST_STAP);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
            stack.hurtAndBreak(1, p, hand);
            return;
        }
        if (!gebouw && stack.is(ItemTags.SHOVELS) && brandt) {
            level.setBlock(pos, state.setValue(CampingmarktBlocks.KampvuurBlock.BRANDT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.2f);
            return;
        }
        if (!brandt) {
            if (stap >= 4) {
                steekAan(level, pos, false);
                Kamperen.meld(p, "vuur.opgepookt", ChatFormatting.GOLD);
            } else {
                Kamperen.meld(p, stap == 3 ? "vuur.hout_in_hand" : "vuur.uit", ChatFormatting.LIGHT_PURPLE);
            }
            return;
        }
        if (stack.getItem() instanceof RoosterstokItem) {
            RoosterstokItem.begin(p, hand, pos);
            return;
        }
        Kamperen.meld(p, "vuur.warm", ChatFormatting.GOLD);
    }

    /**
     * Lights the camp fire (or keeps it going): it burns {@link #BRAND_TICKS} from now and then goes out by itself.
     * feest: with fireworks, the party of step 3.
     */
    public static void steekAan(ServerLevel level, BlockPos pos, boolean feest) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CampingmarktBlocks.KampvuurBlock blok)) {
            return;
        }
        boolean was = state.getValue(CampingmarktBlocks.KampvuurBlock.BRANDT);
        if (!was) {
            level.setBlock(pos, state.setValue(CampingmarktBlocks.KampvuurBlock.BRANDT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, blok, FEEST_STAP);
        }
        Herstel.na(level, pos, state.setValue(CampingmarktBlocks.KampvuurBlock.BRANDT, false), BRAND_TICKS);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 0.9f);
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 30, 0.35, 0.3, 0.35, 0.04);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.2, 0.1, 0.2, 0.0);
        if (feest) {
            level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.sendParticles(ParticleTypes.FIREWORK, x, y + 2.5, z, 60, 1.2, 1.0, 1.2, 0.12);
            for (GuhEntity guh : bewoners(level, pos)) {
                level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }

    private static java.util.List<GuhEntity> bewoners(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(FEEST_BEREIK, 8, FEEST_BEREIK), g -> g.isAlive() && Kamperen.kampeerder(g) >= 0);
    }

    /**
     * One beat of a burning camp fire ({@link #FEEST_STAP} ticks): the next note of the tune, sparks, and the residents of
     * the camping come to sit around it and hop to the music.
     */
    public static void feest(ServerLevel level, BlockPos pos) {
        long beat = level.getGameTime() / FEEST_STAP;
        int noot = WIJSJE[(int) Math.floorMod(beat, (long) WIJSJE.length)];
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        if (noot >= 0) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.RECORDS, 0.55f, (float) Math.pow(2.0, (noot - 12) / 12.0));
            level.sendParticles(ParticleTypes.NOTE, x + (level.getRandom().nextDouble() - 0.5) * 3, y + 1.4, z + (level.getRandom().nextDouble() - 0.5) * 3, 0,
                    level.getRandom().nextInt(25) / 24.0, 0, 0, 1);
        }
        if (beat % 2 != 0) {
            return;
        }
        for (GuhEntity guh : bewoners(level, pos)) {
            if (guh.isOrderedToSit()) {
                if (level.getRandom().nextInt(6) == 0) {
                    level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 0, level.getRandom().nextInt(25) / 24.0,
                            0, 0, 1);
                }
                continue;
            }
            LuisterGoal.luister(guh, pos, FEEST_STAP * 6);
            if (guh.onGround() && guh.distanceToSqr(x, y, z) < 36 && level.getRandom().nextInt(3) == 0) {
                guh.getJumpControl().jump();
            }
        }
    }
}
