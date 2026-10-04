package nl.juiced.guhs.feature.guheinde;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
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
        NeoForge.EVENT_BUS.addListener(MagereCellen::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MagereCellen::onServerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GuheindeEvents::onTame);
    }

    /** Grants one of the Guheinde advancements (tab guheinde, all granted from code). */
    public static void advancement(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("guheinde/" + name));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    /** The portal in a Knabbelkelder is open and this player was part of it (or goes through it): both advancements. */
    public static void portaalOpen(ServerPlayer player) {
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "guheinde_portaal");
        advancement(player, "guheinde_portaal");   // (the shown one: guheinde/, not quest/)
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
                if (feedMager(player, guh)) {
                    hand.consume(1, player);
                }
            } else {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guheinde.mager.honger").withStyle(ChatFormatting.GRAY));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        // (any hand but a taming snack: the book he gives you may well end up in your hand)
        // (1.2.7) somebody else's tamed king still tells the story; your own only while he has something new to tell
        if (guh.getVariant() == GuhVariant.KONING && !GuheindeReis.isKnabbel(hand) && !player.isShiftKeyDown()
                && (!guh.isTame() || !guh.isOwnedBy(player) || heeftNieuws(player))) {
            talkToKoning(player, guh);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * VAHOEG! A starved guh from the Knabbelkelder gets a kaasknabbel: a colourful guh (a random one) hops out and runs
     * off. 1.2.7: the grey guh itself stays in its cell for the next player (it remembers who fed it: once per player).
     * False: this player fed this one already (no knabbel eaten, nothing counted).
     */
    public static boolean feedMager(ServerPlayer player, GuhEntity guh) {
        if (MagereCellen.heeftGevoerd(guh, player.getUUID())) {
            MagereCellen.alGevoerd(player, guh);
            return false;
        }
        MagereCellen.onthoud(guh, player.getUUID());
        MagereCellen.bevrijd(player, guh);
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
        return true;
    }

    /** Has the Koningguh something new for this player (the story, or the knighthood after a win)? */
    public static boolean heeftNieuws(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        int state = saved.getIntOr(KONING, 0);
        return state == 0 || state < 2 && saved.getIntOr(GuheindeGevecht.WINS, 0) > 0;
    }

    /** Player data (GuhQuests.saved): the Koningguh's outfit was handed out (once per player). */
    public static final String PAKJE = "guhs_koning_pakje";

    /**
     * 1.2.7: the king's own outfit (crown, mantle, medallion) for this player: the pieces they don't have yet, once per
     * player (when they tame a Koningguh, or when he knights them). There is one treasure chest per castle and it holds
     * one piece: without this only the first player could ever get the set. Returns how many pieces were given.
     */
    public static int koningPakje(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (saved.getBooleanOr(PAKJE, false)) {
            return 0;
        }
        saved.putBoolean(PAKJE, true);
        int n = 0;
        for (nl.juiced.guhs.entity.GuhClothes c : java.util.List.of(nl.juiced.guhs.entity.GuhClothes.KONING_KROON,
                nl.juiced.guhs.entity.GuhClothes.KONING_MANTEL, nl.juiced.guhs.entity.GuhClothes.KONING_KETTING)) {
            net.minecraft.world.item.Item item = ModItems.clothingItem(c);
            if (!nl.juiced.guhs.feature.kleding.KledingUnlocks.heeft(player, c) && GuhQuests.count(player, item) == 0) {
                GuhQuests.give(player, item);
                n++;
            }
        }
        if (n > 0) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.guheinde.koning.pakje").withStyle(ChatFormatting.GOLD));
        }
        return n;
    }

    /** Taming a Koningguh: his outfit for you (once per player; the king keeps wearing his own). */
    private static void onTame(net.neoforged.neoforge.event.entity.living.AnimalTameEvent event) {
        if (!event.isCanceled() && event.getAnimal() instanceof GuhEntity guh && guh.getVariant() == GuhVariant.KONING
                && event.getTamer() instanceof ServerPlayer player) {
            koningPakje(player);
        }
    }

    /**
     * The Koningguh (on his throne, wild or somebody's) and the Guheinde: first he tells you the story and gives you the book about it;
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
            koningPakje(player);
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
