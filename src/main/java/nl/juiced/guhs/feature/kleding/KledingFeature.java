package nl.juiced.guhs.feature.kleding;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Clothing as a one-time unlock per player (2.9, DESIGN_29 §10). A clothing item is used up by holding right-click
 * ({@link nl.juiced.guhs.item.GuhClothingItem}, {@link KledingOntgrendel}); from then on the piece is available for all
 * your tamed guhs ({@link KledingUnlocks}). Dressing: only the owner, only with their own unlocks, in the new wardrobe
 * ({@code client.screen.GuhWardrobeScreen}, {@link KledingKast}, favourite outfits {@link KledingFavorieten}). Wild and
 * structure guhs wear clothes for looks but drop nothing. Every piece has one source ({@link KledingBronnen},
 * {@link KledingBronLijst}): the kleermaker has a fixed full offer ({@link KledingKleermaker}), the chef set moved to
 * Bakker Korstje, the straw hat and overalls to Boerin Hooibaal's chores, the pink onesie comes from taming the
 * Brococolief guh. Resources: tools/features/kleding.py.
 */
public final class KledingFeature {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    /** The unlock chime (plop + a sparkly guh jingle). */
    public static final DeferredHolder<SoundEvent, SoundEvent> ONTGRENDEL_SOUND = SOUNDS.register("kleding.ontgrendel",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("kleding.ontgrendel")));
    /** Confetti in guh colours (the unlock, dressing a guh). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CONFETTI = PARTICLES.register("kleding_confetti",
            () -> new SimpleParticleType(false));

    /** The chef set at Bakker Korstje (bakmunten). */
    public static final int PRIJS_KOKSMUTS = 6, PRIJS_KOKSBUIS = 9;
    /** Boerin Hooibaal: the straw hat after your first chore, the overalls after this many. */
    public static final int STROOHOED_KLUSJES = 1, OVERALL_KLUSJES = 3;
    /** Per player: got the pink onesie for taming a Brococolief guh (once). */
    public static final String ONESIE_KEY = "guhs_kleding_onesie";

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        KledingBronLijst.registreer();
        NeoForge.EVENT_BUS.addListener(KledingFeature::onLogin);
        NeoForge.EVENT_BUS.addListener(KledingFeature::onInteract);
        NeoForge.EVENT_BUS.addListener(KledingFeature::onVillagerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KledingFeature::onTame);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KledingPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KledingUnlocks.sync(player);
            KledingFavorieten.sync(player);
        }
    }

    /** The kleermaker has his whole offer before you trade with him. */
    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager) {
            KledingKleermaker.zorgVoorAanbod(villager);
        }
    }

    private static void onVillagerTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Villager villager && villager.tickCount % 40 == 7 && !villager.level().isClientSide()) {
            KledingKleermaker.zorgVoorAanbod(villager);
        }
    }

    /** Taming the secret Brococolief guh: its pink onesie for you (once per player; the guh keeps wearing its own). */
    private static void onTame(AnimalTameEvent event) {
        if (event.isCanceled() || !(event.getAnimal() instanceof GuhEntity guh) || guh.getVariant() != GuhVariant.BROCOCOLIEF
                || !(event.getTamer() instanceof ServerPlayer player)) {
            return;
        }
        brococoliefBeloning(player);
    }

    /** The pink onesie for taming a Brococolief guh (once per player). True when it was given now. */
    public static boolean brococoliefBeloning(ServerPlayer player) {
        if (GuhQuests.saved(player).getBoolean(ONESIE_KEY)) {
            return false;
        }
        GuhQuests.saved(player).putBoolean(ONESIE_KEY, true);
        GuhQuests.give(player, ModItems.clothingItem(GuhClothes.PINK_ONESIE));
        player.sendSystemMessage(Component.translatable("gui.guhs.kleding.onesie").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private KledingFeature() {
    }
}
