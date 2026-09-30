package nl.juiced.guhs.feature.guheinde;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.GuhWorldData;
import org.joml.Vector3f;

/**
 * The Guheinde's small rules outside the fight: Mika's cry Mika-tranen, the Koningguh tells the story (and knights you
 * after the win), magere guhs go VAHOEG when you feed them, and the Knabbelkroon hits Mika's harder.
 */
public final class GuheindeEvents {
    /** 1 in N ordinary Mika's drops a Mika-traan (looting helps); Big Mika always drops a few. */
    public static final int TRAAN_CHANCE = 8;
    /** Player data (GuhQuests.saved): the Koningguh's story (0 = not told, 1 = told, 2 = knighted); magere guhs fed. */
    public static final String KONING = "guhs_guheinde_koning", GERED = "guhs_guheinde_gered";
    public static final int BEVRIJDER = 6;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuheindeEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, GuheindeEvents::onInteract);
        NeoForge.EVENT_BUS.addListener(GuheindeEvents::onDamage);
    }

    /** Grants one of the Guheinde advancements (tab guheinde, all granted from code). */
    public static void advancement(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("guheinde/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    // --- Mika-tranen ---------------------------------------------------------------------------------------------------

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof MikaEntity mika) || mika instanceof MikaLarfjeEntity || mika.level().isClientSide()) {
            return;
        }
        int count;
        if (mika.isBoss()) {
            count = 2 + mika.getRandom().nextInt(2);
        } else {
            int looting = 0;
            if (event.getSource().getEntity() instanceof LivingEntity killer) {
                var enchantments = mika.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                        enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer);
            }
            count = mika.getRandom().nextInt(Math.max(1, TRAAN_CHANCE - looting * 2)) == 0 ? 1 : 0;
        }
        if (count > 0) {
            event.getDrops().add(new ItemEntity(mika.level(), mika.getX(), mika.getY() + 0.5, mika.getZ(), new ItemStack(GuheindeFeature.MIKA_TRAAN.get(), count)));
        }
    }

    /** Beating the Mika-baas three times in a row at steen-papier-schaar-VADS: he cries two Mika-tranen. */
    public static void onRpsBeaten(ServerPlayer player) {
        ItemStack tears = new ItemStack(GuheindeFeature.MIKA_TRAAN.get(), 2);
        if (!player.getInventory().add(tears)) {
            player.drop(tears, false);
        }
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guheinde.mika_huilt").withStyle(ChatFormatting.DARK_PURPLE));
    }

    // --- the Koningguh and the magere guhs ----------------------------------------------------------------------------

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof GuhEntity guh)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack hand = player.getMainHandItem();
        if (guh.getVariant() == GuhVariant.MAGER) {
            if (GuheindeReis.isKnabbel(hand)) {
                hand.consume(1, player);
                feedMager(player, guh);
            } else {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guheinde.mager.honger").withStyle(ChatFormatting.GRAY));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // (any hand but a taming snack: the book he gives you may well end up in your hand)
        if (guh.getVariant() == GuhVariant.KONING && !guh.isTame() && !GuheindeReis.isKnabbel(hand) && !player.isShiftKeyDown()) {
            talkToKoning(player, guh);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** VAHOEG! A starved guh from the Knabbelkelder gets a kaasknabbel: it gets its colour (a random guh) and runs off. */
    public static void feedMager(ServerPlayer player, GuhEntity guh) {
        GuhVariant now = GuhVariant.roll(guh.getRandom());
        guh.setVariant(now);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 8, 0.4, 0.3, 0.4, 0);
        level.sendParticles(new DustParticleOptions(0xFF8CBF /* 1, 0.55, 0.75 */, 1.5f), guh.getX(), guh.getY() + 0.5, guh.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1.2f);
        guh.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 2));
        Vec3 away = guh.position().subtract(player.position()).normalize();
        guh.setDeltaMovement(away.x * 0.6, 0.5, away.z * 0.6);
        guh.hurtMarked = true;
        GuhQuests.say(player, guh, "gui.guhs.guheinde.mager.vahoeg");
        // the Guhdex: the page of the magere guh, with its star for saving one
        GuhWorldData data = GuhWorldData.get(player.level().getServer());
        GuhWorldData.PlayerData p = data.player(player.getUUID());
        p.seen.add(GuhVariant.MAGER);
        p.tamed.add(GuhVariant.MAGER);
        data.setDirty();
        CompoundTag saved = GuhQuests.saved(player);
        int gered = saved.getIntOr(GERED, 0) + 1;
        saved.putInt(GERED, gered);
        advancement(player, "guheinde_gered");
        if (gered >= BEVRIJDER) {
            advancement(player, "guheinde_bevrijder");
        }
    }

    /**
     * The Koningguh (wild, on his throne) and the Guheinde: first he tells you the story and gives you the book about it;
     * after you've beaten Opper-Mika he knights you: Ridder van het Guheinde.
     */
    public static void talkToKoning(ServerPlayer player, GuhEntity koning) {
        CompoundTag saved = GuhQuests.saved(player);
        int state = saved.getIntOr(KONING, 0);
        int wins = saved.getIntOr(GuheindeGevecht.WINS, 0);
        koning.playSound(ModSounds.GUH_AMBIENT.get(), 1f, 0.7f);
        if (state == 0) {
            for (int i = 1; i <= 4; i++) {
                GuhQuests.say(player, koning, "gui.guhs.guheinde.koning.verhaal" + i);
            }
            player.getInventory().placeItemBackInInventory(Guhboek.GUHEINDE.stack());
            saved.putInt(KONING, 1);
            advancement(player, "guheinde_opdracht");
        } else if (wins > 0 && state < 2) {
            GuhQuests.say(player, koning, "gui.guhs.guheinde.koning.ridder1");
            GuhQuests.say(player, koning, "gui.guhs.guheinde.koning.ridder2");
            player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1, 0.6, 0.3);
            player.level().playSound(null, player, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
            GuhQuests.give(player, ModItems.GEFRITUURDE_KAASKNABBELS.get());
            saved.putInt(KONING, 2);
            advancement(player, "guheinde_ridder");
        } else {
            String hint = wins > 0 ? "gui.guhs.guheinde.koning.dank"
                    : GuhQuests.count(player, GuheindeFeature.OOG_VAN_VADSIG.get()) > 0 ? "gui.guhs.guheinde.koning.hint_oog"
                    : "gui.guhs.guheinde.koning.hint_traan";
            GuhQuests.say(player, koning, hint);
        }
    }

    // --- the Knabbelkroon against Mika's --------------------------------------------------------------------------------

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && KnabbelkroonItem.wears(attacker)
                && (event.getEntity() instanceof MikaEntity || event.getEntity() instanceof OpperMikaEntity)) {
            event.setAmount(event.getAmount() * KnabbelkroonItem.MIKA_DAMAGE);
        }
    }

    private GuheindeEvents() {
    }
}
