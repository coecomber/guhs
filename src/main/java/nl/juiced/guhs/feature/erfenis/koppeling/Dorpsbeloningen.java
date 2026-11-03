package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.erfenis.ErfenisEconomie;

/**
 * The village rewards of the FTB chapter (ronde 15): the Guhldenzakje in three sizes (100 / 500 / 2000 Guhlden, right-click =
 * into your village's Hoeve via {@code ErfenisEconomie.geef(.., "guhlden", n)}; sneak = the whole stack) and the Decobon (one
 * free deco piece; right-click = the bon goes into your village, where the core's decowinkel uses it: {@link #decobonnen} /
 * {@link #neemDecobon}). Both only work once you own an erfenis (bought the letter's opknapper); before that they wait in your
 * pockets.
 */
public final class Dorpsbeloningen {
    private Dorpsbeloningen() {
    }

    // =====================================================================================================================
    // the API for the core (decowinkel)
    // =====================================================================================================================

    /** How many Decobonnen this player has in their village. */
    public static int decobonnen(MinecraftServer s, UUID speler) {
        KoppelingData.Speler sp = KoppelingData.get(s).spelerAls(speler);
        return sp == null ? 0 : sp.decobonnen;
    }

    /** Uses one Decobon (the decowinkel gives a piece for free); false: none left. */
    public static boolean neemDecobon(MinecraftServer s, UUID speler) {
        KoppelingData d = KoppelingData.get(s);
        KoppelingData.Speler sp = d.spelerAls(speler);
        if (sp == null || sp.decobonnen <= 0) {
            return false;
        }
        sp.decobonnen--;
        d.setDirty();
        return true;
    }

    public static void geefDecobon(MinecraftServer s, UUID speler, int n) {
        KoppelingData d = KoppelingData.get(s);
        d.speler(speler).decobonnen += n;
        d.setDirty();
    }

    // =====================================================================================================================
    // the items
    // =====================================================================================================================

    public static class Guhldenzakje extends Item {
        private final int guhlden;

        public Guhldenzakje(Properties p, int guhlden) {
            super(p);
            this.guhlden = guhlden;
        }

        public int guhlden() {
            return guhlden;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer p) {
                if (!Makelaarsbrief.gekocht(p.level().getServer(), p.getUUID())) {
                    p.sendOverlayMessage(Component.translatable("erfenis.koppeling.zakje.geen_erfenis").withStyle(ChatFormatting.LIGHT_PURPLE));
                    return InteractionResult.FAIL;
                }
                ItemStack stack = p.getItemInHand(hand);
                int n = p.isShiftKeyDown() ? stack.getCount() : 1;
                ErfenisEconomie.geef(p.level().getServer(), p.getUUID(), "guhlden", guhlden * n);
                stack.consume(n, p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.7f, 1.6f);
                p.sendOverlayMessage(Component.translatable("erfenis.koppeling.zakje.gestort", guhlden * n).withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.guhldenzakje.lore", guhlden).withStyle(ChatFormatting.GRAY));
        }
    }

    public static class Decobon extends Item {
        public Decobon(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer p) {
                MinecraftServer s = p.level().getServer();
                if (!Makelaarsbrief.gekocht(s, p.getUUID())) {
                    p.sendOverlayMessage(Component.translatable("erfenis.koppeling.zakje.geen_erfenis").withStyle(ChatFormatting.LIGHT_PURPLE));
                    return InteractionResult.FAIL;
                }
                ItemStack stack = p.getItemInHand(hand);
                int n = p.isShiftKeyDown() ? stack.getCount() : 1;
                geefDecobon(s, p.getUUID(), n);
                stack.consume(n, p);
                p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.5f);
                p.sendOverlayMessage(Component.translatable("erfenis.koppeling.decobon.ingewisseld", decobonnen(s, p.getUUID()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.decobon.lore").withStyle(ChatFormatting.GRAY));
        }
    }
}
