package nl.juiced.guhs.feature.kleding;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * Using up a clothing item (2.9, {@link nl.juiced.guhs.item.GuhClothingItem}): the unlock with its party (plop, a chime,
 * confetti, the piece popping up big on your screen, "Ontgrendeld: X! VAHOEG!"), the kleding advancements, and every
 * "get this outfit" advancement of the other features (they check your inventory: an unlocked piece counts as "had it").
 */
public final class KledingOntgrendel {
    /** Unlock counts with an advancement (grote_guhspelen/kleding_&lt;name&gt;). */
    public static final int VEEL = 25, HEEL_VEEL = 60;

    /** Unlocks the piece for this player (server). True when it was new: then it's used up by the caller. */
    public static boolean ontgrendel(ServerPlayer player, GuhClothes c) {
        if (c.slot == GuhClothes.Slot.HAAR) {
            return false;
        }
        if (!KledingUnlocks.ontgrendel(player, c)) {
            alGehad(player, c);
            return false;
        }
        feest(player, c);
        naOntgrendel(player, c);
        return true;
    }

    /** "Deze heb je al, njeg! Geef hem aan een vriend." */
    public static void alGehad(ServerPlayer player, GuhClothes c) {
        player.displayClientMessage(Component.translatable("gui.guhs.kleding.al_gehad", naam(c)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    /** The name of a piece (its item name). */
    public static Component naam(GuhClothes c) {
        return new ItemStack(ModItems.clothingItem(c)).getHoverName();
    }

    private static void feest(ServerPlayer player, GuhClothes c) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1f, 1.4f);            // plop!
        level.playSound(null, player.blockPosition(), KledingFeature.ONTGRENDEL_SOUND.get(), SoundSource.PLAYERS, 1f, 1f);
        level.sendParticles(KledingFeature.CONFETTI.get(), player.getX(), player.getY() + 1.6, player.getZ(), 40, 0.5, 0.3, 0.5, 0.15);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                12, 0.6, 0.6, 0.6, 0.0);
        ModNetworking.sendTo(player, new KledingPayloads.Ontgrendeld(c.id()));
        player.sendSystemMessage(Component.translatable("gui.guhs.kleding.ontgrendeld", naam(c).copy().withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        int aantal = KledingUnlocks.alle(player).size();
        if (aantal == 1) {
            player.sendSystemMessage(Component.translatable("gui.guhs.kleding.eerste_tip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    /** The advancements after an unlock (also called by the tests). */
    public static void naOntgrendel(ServerPlayer player, GuhClothes c) {
        int aantal = KledingUnlocks.alle(player).size();
        grant(player, "kleding_eerste");
        GuhAdvancements.grant(player, "kleding_eerste");
        if (c.slot == GuhClothes.Slot.OREN) {
            grant(player, "kleding_oren");
        }
        if (aantal >= VEEL) {
            grant(player, "kleding_veel");
        }
        if (aantal >= HEEL_VEEL) {
            grant(player, "kleding_heel_veel");
        }
        if (KledingBronnen.bron(c) != null && KledingBronnen.van(KledingBronnen.bron(c)).stream().allMatch(p -> KledingUnlocks.heeft(player, p))
                && KledingBronnen.van(KledingBronnen.bron(c)).size() >= 2) {
            grant(player, "kleding_set");
        }
        setAdvancements(player);
    }

    /** Grants a visible advancement of the Grote Guhspelen tab (grote_guhspelen/&lt;name&gt;, criterion "done"). */
    public static void grant(ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    /**
     * The other features' "get the whole outfit" advancements check your inventory (minecraft:inventory_changed with
     * clothing items). Now that you use pieces up, they'd never all be in your inventory at once: so every such
     * criterion counts as met when each of its items is a piece you unlocked (or have in your inventory).
     */
    public static void setAdvancements(ServerPlayer player) {
        for (AdvancementHolder holder : player.server.getAdvancements().getAllAdvancements()) {
            if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                continue;
            }
            for (Map.Entry<String, Criterion<?>> entry : holder.value().criteria().entrySet()) {
                if (entry.getValue().triggerInstance() instanceof InventoryChangeTrigger.TriggerInstance inv && heeftAlles(player, inv.items())) {
                    player.getAdvancements().award(holder, entry.getKey());
                }
            }
        }
    }

    private static boolean heeftAlles(ServerPlayer player, List<ItemPredicate> predicates) {
        if (predicates.isEmpty()) {
            return false;
        }
        boolean kleding = false;
        for (ItemPredicate predicate : predicates) {
            boolean ok = false;
            for (GuhClothes c : KledingUnlocks.alle(player)) {
                if (predicate.test(new ItemStack(ModItems.clothingItem(c)))) {
                    ok = kleding = true;
                    break;
                }
            }
            if (!ok) {
                for (ItemStack stack : player.getInventory().items) {
                    if (!stack.isEmpty() && predicate.test(stack)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (!ok) {
                return false;
            }
        }
        return kleding;
    }

    private KledingOntgrendel() {
    }
}
