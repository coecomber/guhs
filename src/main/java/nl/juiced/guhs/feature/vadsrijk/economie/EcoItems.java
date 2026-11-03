package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;

/** The economie slice's item classes: coins, potions, the materials with a tooltip, the mount whistle and the saddle unlocks. */
public final class EcoItems {
    private EcoItems() {
    }

    private static void zeg(Player p, String key, Object... args) {
        if (p instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** An item with one grey tooltip line {@code <description id>.uitleg}. */
    public static class Uitleg extends Item {
        public Uitleg(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
            long w = Waarde.verkoop(stack);
            if (w > 0) {
                tooltip.accept(Component.translatable("vadsrijk.economie.tooltip.waarde", Spelers.munten(w)).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** A Vadsmunt (brons/zilver/goud with a guh face). Right-click: into your wallet (sneak: every coin in your bag). */
    public static class Munt extends Item {
        public final long waarde;

        public Munt(Properties p, long waarde) {
            super(p);
            this.waarde = waarde;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (!Vadsrijk.in(sp)) {
                zeg(sp, "vadsrijk.economie.munt.buiten");
                return InteractionResult.FAIL;
            }
            long n = 0;
            if (sp.isShiftKeyDown()) {
                for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                    ItemStack s = sp.getInventory().getItem(i);
                    if (s.getItem() instanceof Munt m) {
                        n += m.waarde * s.getCount();
                        sp.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }
            } else {
                ItemStack s = sp.getItemInHand(hand);
                n = waarde * s.getCount();
                sp.setItemInHand(hand, ItemStack.EMPTY);
            }
            if (n > 0) {
                Spelers.geefMunten(sp, n);
                level.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
                zeg(sp, "vadsrijk.economie.munt.erin", Spelers.munten(n));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("vadsrijk.economie.munt.uitleg", Spelers.munten(waarde)).withStyle(ChatFormatting.GOLD));
        }
    }

    /** A potion: Vads, energie, or a buff status. Only works in the Vadsrijk. */
    public static class Drankje extends Item {
        public enum Soort { VADS, ENERGIE, VAHOEG, SNELPOOTJES }

        public final Soort soort;

        public Drankje(Properties p, Soort soort) {
            super(p);
            this.soort = soort;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (!Vadsrijk.in(sp)) {
                zeg(sp, "vadsrijk.economie.drankje.buiten");
                return InteractionResult.FAIL;
            }
            ItemStack stack = sp.getItemInHand(hand);
            switch (soort) {
                case VADS -> Schade.heal(sp, sp, Spelers.maxVads(sp) * 0.40);
                case ENERGIE -> {
                    var d = Spelers.data(sp);
                    d.energie = Math.min(Spelers.maxEnergie(sp), Spelers.energie(sp) + Spelers.maxEnergie(sp) * 0.5);
                    Spelers.sync(sp);
                }
                case VAHOEG -> Statussen.geef(sp, Status.VAHOEG, 20 * 60, 1, sp);
                case SNELPOOTJES -> Statussen.geef(sp, Status.SNELPOOTJES, 20 * 60, 1, sp);
            }
            if (!sp.isCreative()) {
                stack.shrink(1);
            }
            sp.getCooldowns().addCooldown(stack, soort == Soort.VADS || soort == Soort.ENERGIE ? 100 : 40);
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(soort == Soort.ENERGIE ? ParticleTypes.GLOW : ParticleTypes.HEART, sp.getX(), sp.getY() + 1.4, sp.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
            }
            zeg(sp, "vadsrijk.economie.drankje." + soort.name().toLowerCase(java.util.Locale.ROOT) + ".op");
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** The rijfluitje: calls your active mount (again: sends it home). */
    public static class Fluitje extends Item {
        public Fluitje(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                Rijdieren.fluit(sp);
                sp.getCooldowns().addCooldown(sp.getItemInHand(hand), 30);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.vr_item_rijfluitje.uitleg").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    /** A saddle that unlocks a mount at the stal (rare loot: the Klodderros from Koning Klodder). */
    public static class Zadel extends Item {
        public final RijdierSoort soort;

        public Zadel(Properties p, RijdierSoort soort) {
            super(p);
            this.soort = soort;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                if (Rijdieren.geef(sp, soort)) {
                    if (!sp.isCreative()) {
                        sp.getItemInHand(hand).shrink(1);
                    }
                } else {
                    zeg(sp, "vadsrijk.economie.rijdier.al");
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("vadsrijk.economie.zadel.uitleg", Component.translatable(soort.naamKey())).withStyle(ChatFormatting.GRAY));
        }
    }
}
