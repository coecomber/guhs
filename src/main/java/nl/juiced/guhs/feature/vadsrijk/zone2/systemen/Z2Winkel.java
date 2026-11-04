package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.List;
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
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Zone-2 economy: the tier-2 potions and the Kandijkom shop {@link #KANDIJKRAAM} ("De Kandijkraam", via the economie
 * {@code Winkels} API: z2-npc opens it with {@code Winkels.open(p, Z2Winkel.KANDIJKRAAM)}).
 * <ul>
 *   <li>Grote Vadsdrank: 70 % Vads (zone 1: 40 %);</li>
 *   <li>Grote Energiedrank: full energy (zone 1: 50 %);</li>
 *   <li>Warme Chocomelk: thaws you (Bevroren off), 3 min unfreezable, +15 % Vads. The Kandijkom speciality.</li>
 * </ul>
 */
public final class Z2Winkel {
    public static final String KANDIJKRAAM = "z2_kandijkraam";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public enum Soort {
        GROTE_VADS, GROTE_ENERGIE, CHOCOMELK
    }

    public static final DeferredItem<Drank> GROTE_VADSDRANK = drank("vr_z2_grote_vadsdrank", Soort.GROTE_VADS);
    public static final DeferredItem<Drank> GROTE_ENERGIEDRANK = drank("vr_z2_grote_energiedrank", Soort.GROTE_ENERGIE);
    public static final DeferredItem<Drank> WARME_CHOCOMELK = drank("vr_z2_warme_chocomelk", Soort.CHOCOMELK);

    private Z2Winkel() {
    }

    private static DeferredItem<Drank> drank(String id, Soort s) {
        return ITEMS.registerItem(id, p -> new Drank(p, s), () -> new Item.Properties().stacksTo(16));
    }

    static void registreer() {
        Winkels.registreer(new Winkels.Winkel(KANDIJKRAAM, true, List.of(
                Winkels.itemWaar("grote_vadsdrank", 45, () -> new ItemStack(GROTE_VADSDRANK.get())),
                Winkels.itemWaar("grote_energiedrank", 55, () -> new ItemStack(GROTE_ENERGIEDRANK.get())),
                Winkels.itemWaar("warme_chocomelk", 30, () -> new ItemStack(WARME_CHOCOMELK.get())),
                Winkels.itemWaar("vahoegdrankje", 60, () -> new ItemStack(EcoSpul.VAHOEGDRANKJE.get())),
                Winkels.itemWaar("snelpootjesdrankje", 45, () -> new ItemStack(EcoSpul.SNELPOOTJESDRANKJE.get())),
                Winkels.itemWaar("verbetersteentje", 50, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())))));
    }

    /** A zone-2 potion. Only works in the Vadsrijk. */
    public static class Drank extends Item {
        public final Soort soort;

        public Drank(Properties p, Soort soort) {
            super(p);
            this.soort = soort;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResult.SUCCESS;
            }
            if (!Vadsrijk.in(sp)) {
                sp.sendOverlayMessage(Component.translatable("vadsrijk.economie.drankje.buiten").withStyle(ChatFormatting.LIGHT_PURPLE));
                return InteractionResult.FAIL;
            }
            ItemStack stack = sp.getItemInHand(hand);
            switch (soort) {
                case GROTE_VADS -> Schade.heal(sp, sp, Spelers.maxVads(sp) * 0.70);
                case GROTE_ENERGIE -> {
                    Spelers.data(sp).energie = Spelers.maxEnergie(sp);
                    Spelers.sync(sp);
                }
                case CHOCOMELK -> {
                    Bevroren.ontdooi(sp, 20 * 180);
                    Schade.heal(sp, sp, Spelers.maxVads(sp) * 0.15);
                }
            }
            if (!sp.isCreative()) {
                stack.shrink(1);
            }
            sp.getCooldowns().addCooldown(stack, soort == Soort.CHOCOMELK ? 60 : 100);
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8f, soort == Soort.CHOCOMELK ? 0.9f : 1.2f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(soort == Soort.GROTE_ENERGIE ? ParticleTypes.GLOW : soort == Soort.CHOCOMELK ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.HEART,
                        sp.getX(), sp.getY() + 1.4, sp.getZ(), soort == Soort.CHOCOMELK ? 3 : 8, 0.3, 0.3, 0.3, 0.01);
            }
            sp.sendOverlayMessage(Component.translatable(getDescriptionId() + ".op").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
