package nl.juiced.guhs.feature.vogels;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.RawAnimation;

/**
 * The kaasmeesje: a great tit in cheese colours. It likes to hang upside down under leaves (it looks for a leaf with air
 * below it), and it pecks at ripe knabbelbessenstruiken (it only nibbles: the berries stay for you). Give it berries and it
 * sings its tsjie-tsjie-bee song.
 */
public class KaasmeesjeEntity extends Vogeltje {
    /** How far it looks for a leaf to hang under or a berry bush. */
    public static final int ZOEK = 8;

    public KaasmeesjeEntity(EntityType<? extends Vogeltje> type, Level level) {
        super(type, level);
    }

    @Override
    public String naam() {
        return "kaasmeesje";
    }

    @Override
    protected double vliegSnelheid() {
        return 0.3;
    }

    @Override
    public boolean lekker(ItemStack stack) {
        return stack.is(VogelTags.BESSEN);
    }

    @Override
    protected SoundEvent roep() {
        return VogelsFeature.MEES.get();
    }

    public boolean hangt() {
        return houding() == HANGT;
    }

    // --- where it likes to sit -------------------------------------------------------------------------------------------
    @Override
    @Nullable
    protected BlockPos eigenLandplek() {
        int r = random.nextInt(10);
        if (r < 5) {
            BlockPos hang = hangplek(blockPosition(), ZOEK);
            if (hang != null) {
                zetHouding(HANGT);
                return hang;
            }
        } else if (r < 7) {
            BlockPos bes = bessenstruik(blockPosition(), 10);
            if (bes != null) {
                zetHouding(STAAT);
                return bes;
            }
        }
        return null;
    }

    /** An air block right under a leaf block (and a little above the ground), near `from`; null when there's none. */
    @Nullable
    public BlockPos hangplek(BlockPos from, int r) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; i++) {
            int x = from.getX() + random.nextInt(r * 2 + 1) - r;
            int z = from.getZ() + random.nextInt(r * 2 + 1) - r;
            // from a bit above it down: the underside of a leaf with two air blocks under it
            for (int y = from.getY() + 7; y > from.getY() - 6 && y > level().getMinY() + 2; y--) {
                p.set(x, y, z);
                if (level().getBlockState(p).is(BlockTags.LEAVES) && level().getBlockState(p.below()).isAir()
                        && level().getBlockState(p.below(2)).isAir()) {
                    return p.below().immutable();
                }
            }
        }
        return null;
    }

    /** A ripe knabbelbessenstruik near `from` (its block: it lands in it), or null. */
    @Nullable
    public BlockPos bessenstruik(BlockPos from, int r) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-r, -4, -r), from.offset(r, 3, r))) {
            BlockState s = level().getBlockState(p);
            if (s.getBlock() instanceof KnabbelbessenstruikBlock && s.getValue(KnabbelbessenstruikBlock.AGE) >= 2) {
                double d = p.distSqr(from);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    protected void geland(BlockPos plek) {
        if (hangt()) {
            for (ServerPlayer p : kijkers(12)) {
                GidsFeature.grant(p, "diertjes/vogels_ondersteboven");
            }
        }
    }

    @Override
    protected void zitStap() {
        if (hangt()) {
            BlockPos above = blockPosition().above();
            if (!level().getBlockState(above).is(BlockTags.LEAVES) && !level().getBlockState(BlockPos.containing(getX(), getY() + getBbHeight() + 0.1, getZ())).is(BlockTags.LEAVES)) {
                zetHouding(STAAT);            // the leaf is gone: let go
                startVliegen(null, 60);
            }
            return;
        }
        BlockState in = level().getBlockState(blockPosition());
        if (in.getBlock() instanceof KnabbelbessenstruikBlock && random.nextInt(50) == 0 && level() instanceof ServerLevel server) {
            triggerAnim("actie", "peck");
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, VadswoudFeature.KNABBELBESSEN.get().asItem()),
                    getX(), getY() + 0.3, getZ(), 4, 0.1, 0.1, 0.1, 0.03);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.NEUTRAL, 0.3f, 1.8f);
        }
    }

    @Override
    public void startVliegen(@Nullable net.minecraft.world.phys.Vec3 weg, int tijd) {
        if (hangt()) {
            setPos(getX(), getY() - 0.3, getZ());      // drops off the leaf first
        }
        super.startVliegen(weg, tijd);
    }

    @Override
    protected void gevoerd(ServerPlayer player, ItemStack stack) {
        triggerAnim("actie", "peck");
        level().playSound(null, getX(), getY(), getZ(), VogelsFeature.MEES.get(), SoundSource.NEUTRAL, 0.9f, 1.25f);
    }

    @Override
    protected RawAnimation beweging(AnimationTest<Vogeltje> state) {
        if (!vliegt() && hangt()) {
            return anim("hang", true);
        }
        return super.beweging(state);
    }
}
