package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import nl.juiced.guhs.feature.bouwhulp.GereedschapFeature;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Plaatsen;

/**
 * A printed blueprint (design 2.6): made at the Ontwerpbureau from an own, catalogue or library blueprint, or bought in the
 * safari giftshop (the park buildings, design 2.9). Click a block: the blueprint stands there as a ghost, facing you. It
 * refers to its blueprint (it is not used up); removed blueprints can't be placed any more.
 */
public class BlauwdrukItem extends Item {
    public BlauwdrukItem(Properties properties) {
        super(properties);
    }

    public static ItemStack van(Blauwdruk b) {
        ItemStack s = new ItemStack(GereedschapFeature.BLAUWDRUK.get());
        CompoundTag t = new CompoundTag();
        t.putString("Blauwdruk", b.id);
        t.putString("Naam", b.naam());
        t.putString("Maker", b.makerNaam);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    /** A catalogue blueprint item (the giftshop). */
    public static ItemStack catalogus(String id) {
        Blauwdruk b = Catalogus.get(id);
        return b == null ? ItemStack.EMPTY : van(b);
    }

    private static CompoundTag data(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        String id = data(ctx.getItemInHand()).getStringOr("Blauwdruk", "");
        Blauwdruk b = id.isEmpty() ? null : Blauwdrukken.zoek(p.level().getServer(), id);
        if (b == null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.blauwdruk.kwijt"));
            return InteractionResult.FAIL;
        }
        Plaatsen.spook(p, b.ontwerp, Plaatsen.oorsprong(b.ontwerp, ctx.getClickedPos(), ctx.getClickedFace()), Plaatsen.naarSpeler(p),
                Blauwdrukken.naam(b).getString());
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag t = data(stack);
        String id = t.getStringOr("Blauwdruk", "");
        if (id.isEmpty()) {
            return super.getName(stack);
        }
        Component naam = id.startsWith("c:") ? Component.translatable("gui.guhs.blauwdruk.cat." + id.substring(2)) : Component.literal(t.getStringOr("Naam", "?"));
        return Component.translatable("item.guhs.blauwdruk.van", naam);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.blauwdruk.lore").withStyle(ChatFormatting.GRAY));
        String maker = data(stack).getStringOr("Maker", "");
        if (!maker.isEmpty()) {
            tooltip.accept(Component.translatable("item.guhs.blauwdruk.maker", maker).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
