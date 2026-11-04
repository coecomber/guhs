package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import nl.juiced.guhs.feature.vadsrijk.economie.Drinken;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Zone-2 economy: the tier-2 potions and the Kandijkom shop {@link #KANDIJKRAAM} ("De Kandijkraam", via the economie
 * {@code Winkels} API: z2-npc opens it with {@code Winkels.open(p, Z2Winkel.KANDIJKRAAM)}).
 * <ul>
 *   <li>Groot Vadsdrankje (id vr_z2_grote_vadsdrank): 700 Vads (balans 2026-11: flat numbers, see economie {@code Drinken});</li>
 *   <li>Groot Energiedrankje (id vr_z2_grote_energiedrank): +120 energy;</li>
 *   <li>Warme Chocomelk: 250 Vads, thaws you (Bevroren off), 3 min unfreezable. The Kandijkom speciality.</li>
 * </ul>
 * All three: 2 s to drink, in the shared 10 s potion cooldown.
 * <ul>
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
        return ITEMS.registerItem(id, p -> new Drank(p, s), () -> Drinken.eigenschappen(true));
    }

    static void registreer() {
        Winkels.registreer(new Winkels.Winkel(KANDIJKRAAM, true, List.of(
                Winkels.itemWaar("vadsdrankje", 40, () -> new ItemStack(EcoSpul.VADSDRANKJE.get())),
                Winkels.itemWaar("grote_vadsdrank", 300, () -> new ItemStack(GROTE_VADSDRANK.get())),   // (balans 2026-11: 3 zilver)
                Winkels.itemWaar("energiedrankje", 20, () -> new ItemStack(EcoSpul.ENERGIEDRANKJE.get())),
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
            if (Drinken.buiten(player, "vadsrijk.economie.drankje.buiten")) {
                return InteractionResult.FAIL;
            }
            return super.use(level, player, hand);   // the Consumable component: 2 s of drinking
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
            if (entity instanceof ServerPlayer sp && Vadsrijk.in(sp)) {
                werk(sp);
            }
            return super.finishUsingItem(stack, level, entity);
        }

        /** What it does (also called by the gametest). */
        public void werk(ServerPlayer sp) {
            switch (soort) {
                case GROTE_VADS -> Schade.heal(sp, sp, Drinken.GROOT_VADS);
                case GROTE_ENERGIE -> Drinken.energie(sp, Drinken.GROOT_ENERGIE);
                case CHOCOMELK -> {
                    Bevroren.ontdooi(sp, 20 * 180);
                    Schade.heal(sp, sp, Drinken.CHOCOMELK_VADS);
                }
            }
            if (sp.level() instanceof ServerLevel sl) {
                sl.sendParticles(soort == Soort.GROTE_ENERGIE ? ParticleTypes.GLOW : soort == Soort.CHOCOMELK ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.HEART,
                        sp.getX(), sp.getY() + 1.4, sp.getZ(), soort == Soort.CHOCOMELK ? 3 : 8, 0.3, 0.3, 0.3, 0.01);
            }
            sp.sendOverlayMessage(Component.translatable(getDescriptionId() + ".op").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.drankje.wacht").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
