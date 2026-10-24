package nl.juiced.guhs.feature.toveren;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Glinsteraltaar's block entity: only the floating book's animation (client side, the same as vanilla's
 * {@code EnchantingTableBlockEntity#bookAnimationTick}: it turns to the nearest player within 3 blocks, opens and flips
 * its pages) and an optional custom name (an altar renamed in an anvil shows that name in its screen).
 */
public class GlinsteraltaarBlockEntity extends BlockEntity implements Nameable {
    private static final Component NAAM = Component.translatable("block.guhs.glinsteraltaar");
    private static final RandomSource RANDOM = RandomSource.create();
    public int time;
    public float flip, oFlip, flipT, flipA;
    public float open, oOpen;
    public float rot, oRot, tRot;
    private @Nullable Component name;

    public GlinsteraltaarBlockEntity(BlockPos pos, BlockState state) {
        super(ToverenFeature.GLINSTERALTAAR_BE.get(), pos, state);
    }

    public static void boekTick(Level level, BlockPos pos, BlockState state, GlinsteraltaarBlockEntity be) {
        be.oOpen = be.open;
        be.oRot = be.rot;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            be.tRot = (float) Mth.atan2(player.getZ() - (pos.getZ() + 0.5), player.getX() - (pos.getX() + 0.5));
            be.open += 0.1F;
            if (be.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float old = be.flipT;
                do {
                    be.flipT += RANDOM.nextInt(4) - RANDOM.nextInt(4);
                } while (old == be.flipT);
            }
        } else {
            be.tRot += 0.02F;
            be.open -= 0.1F;
        }
        be.rot = Mth.wrapDegrees(be.rot * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        be.tRot = Mth.wrapDegrees(be.tRot * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        float rotDir = Mth.wrapDegrees((be.tRot - be.rot) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        be.rot += rotDir * 0.4F;
        be.open = Mth.clamp(be.open, 0.0F, 1.0F);
        be.time++;
        be.oFlip = be.flip;
        float diff = Mth.clamp((be.flipT - be.flip) * 0.4F, -0.2F, 0.2F);
        be.flipA += (diff - be.flipA) * 0.9F;
        be.flip += be.flipA;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.name = parseCustomNameSafe(input, "CustomName");
    }

    @Override
    public Component getName() {
        return name != null ? name : NAAM;
    }

    @Override
    public @Nullable Component getCustomName() {
        return name;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.name = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("CustomName");
    }
}
