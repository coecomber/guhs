package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlock;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.vadskracht.MachineDeelBlock;

/**
 * A hand tool of the Bouwhulp (deel 2 F9). Its level is that of the Bouwkeet it is linked to (sneak-click the keet with
 * it; an own choice: BOUWHULP_KEUZES.md): the keet's level now when it is loaded, otherwise the level it had when last seen.
 * Without a keet a tool works at Handwerk. Sneak-click in the air: the settings screen.
 */
public abstract class GereedschapItem extends Item {
    public final Gereedschap soort;

    protected GereedschapItem(Properties properties, Gereedschap soort) {
        super(properties);
        this.soort = soort;
    }

    // =====================================================================================================================
    // the keet link (in the item's custom data: Keet = pos, Dim = dimension, KeetNiveau = last seen level)
    // =====================================================================================================================

    public static BouwNiveau niveau(ServerLevel level, ItemStack stack) {
        CompoundTag t = data(stack);
        if (!t.contains("Keet")) {
            return BouwNiveau.HANDWERK;
        }
        BlockPos pos = BlockPos.of(t.getLongOr("Keet", 0L));
        if (level.dimension().identifier().toString().equals(t.getStringOr("Dim", "")) && level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof BouwkeetBlockEntity keet) {
            BouwNiveau n = keet.werkNiveau();
            if (n.nummer() != t.getIntOr("KeetNiveau", 0)) {
                t.putInt("KeetNiveau", n.nummer());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
            }
            return n;
        }
        if (level.dimension().identifier().toString().equals(t.getStringOr("Dim", "")) && level.isLoaded(pos)) {
            return BouwNiveau.HANDWERK;   // the keet is gone
        }
        return BouwNiveau.van(t.getIntOr("KeetNiveau", 0));
    }

    /** (client) the level as last stored on the item. */
    public static int bekendNiveau(ItemStack stack) {
        CompoundTag t = data(stack);
        return t.contains("Keet") ? t.getIntOr("KeetNiveau", 0) : 0;
    }

    static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    private static boolean koppel(ServerPlayer p, ItemStack stack, BlockPos geklikt) {
        ServerLevel level = p.level();
        BlockState s = level.getBlockState(geklikt);
        BlockPos pos = s.getBlock() instanceof MachineDeelBlock ? MachineDeelBlock.kern(s, geklikt) : geklikt;
        if (!(level.getBlockEntity(pos) instanceof BouwkeetBlockEntity keet)) {
            return false;
        }
        if (!BouwkeetBlock.magBewerken(keet, p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwkeet.niet_van_jou"));
            return true;
        }
        CompoundTag t = data(stack);
        t.putLong("Keet", pos.asLong());
        t.putString("Dim", level.dimension().identifier().toString());
        t.putInt("KeetNiveau", keet.werkNiveau().nummer());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.gekoppeld", Component.translatable(keet.werkNiveau().sleutel())));
        return true;
    }

    // =====================================================================================================================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = ctx.getItemInHand();
        if (p.isSecondaryUseActive() && koppel(p, stack, ctx.getClickedPos())) {
            return InteractionResult.SUCCESS;
        }
        BouwNiveau n = niveau(p.level(), stack);
        if (n.compareTo(soort.vanaf) < 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.vanaf", Component.translatable(soort.vanaf.sleutel())));
            return InteractionResult.FAIL;
        }
        opBlok(p, stack, n, ctx.getClickedPos(), ctx.getClickedFace(), p.isSecondaryUseActive());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer p) {
            ItemStack stack = p.getItemInHand(hand);
            GereedschapPayloads.openInstellingen(p, soort, niveau(p.level(), stack));
        }
        return InteractionResult.SUCCESS;
    }

    /** A click on a block (sneak: the pipette, for tools that have one). */
    protected abstract void opBlok(ServerPlayer p, ItemStack stack, BouwNiveau n, BlockPos pos, Direction kant, boolean sluip);

    /** The pipette: the clicked block into a block/mix setting. */
    protected void pipet(ServerPlayer p, String key, BlockPos pos) {
        Block b = p.level().getBlockState(pos).getBlock();
        Optie o = soort.optie(key);
        if (o == null || b.defaultBlockState().hasBlockEntity() || b.asItem() == net.minecraft.world.item.Items.AIR) {
            return;
        }
        Opties.zetBlok(Opties.van(p, soort), o, b);
        BouwSpelers.van(p.level().getServer()).veranderd();
        p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.pipet." + o.soort().name().toLowerCase(java.util.Locale.ROOT), b.getName()));
        if (soort == Gereedschap.VORMSTAF) {
            GereedschapPayloads.stuurStaf(p);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        CompoundTag t = data(stack);
        if (t.contains("Keet")) {
            tooltip.accept(Component.translatable("gui.guhs.bouwhulp.keet", Component.translatable("gui.guhs.bouwkeet.niveau." + t.getIntOr("KeetNiveau", 0)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.accept(Component.translatable("gui.guhs.bouwhulp.geen_keet").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (soort.vanaf != BouwNiveau.HANDWERK) {
            tooltip.accept(Component.translatable("gui.guhs.bouwhulp.vanaf", Component.translatable(soort.vanaf.sleutel())).withStyle(ChatFormatting.GOLD));
        }
    }
}
