package nl.juiced.guhs.feature.guhwinter.npc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Guhwinter 1.10.10, slice npc ({@code nl.juiced.guhs.feature.guhwinter.npc}): the guhs of the Winterplein. The entity type
 * {@code guhwinter_guh} ({@link WinterGuh}), the keeper ({@link GuhwinterNpcs}, every second), the clicks, and
 * {@code /guhs winter npc}. Texts and look textures: tools/features/guhwinter_npc.py. Client: client/GuhwinterNpcClient.
 */
public final class GuhwinterNpcFeature {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WinterGuh>> GUH = ENTITIES.register("guhwinter_guh",
            () -> EntityType.Builder.<WinterGuh>of(WinterGuh::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10)
                    .noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwinter_guh"))));

    private GuhwinterNpcFeature() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(GUH.get(), GuhEntity.createAttributes().build()));
        nl.juiced.guhs.world.TikWacht.post("GuhwinterNpcs", e -> GuhwinterNpcs.tick(e.getServer()));
        // only the current entity of each name may join (a copy a chunk saved, e.g. an old Ozovads, is thrown away)
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && !GuhwinterNpcs.magErbij(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        // a click on one of ours: only ours (no feeding, taming, riding, leashing, dressing, picking up), before anyone else
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.EntityInteractSpecific e) -> {
            if (onderschep(e.getEntity(), e.getTarget(), e.getHand())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (PlayerInteractEvent.EntityInteract e) -> {
            if (onderschep(e.getEntity(), e.getTarget(), e.getHand())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> NpcPraat.vergeet(e.getEntity().getUUID()));
        NpcCommando.register();
    }

    /** Is this a click on one of ours? Then (server side, main hand) it is handled here. */
    private static boolean onderschep(Player wie, Entity doel, InteractionHand hand) {
        if (!(doel instanceof WinterGuh) && (wie.level().isClientSide() || !GuhwinterNpcs.isNpc(doel))) {
            return false;   // (the client does not know a horse's tags: the server stops it)
        }
        if (!wie.level().isClientSide() && hand == InteractionHand.MAIN_HAND && wie instanceof ServerPlayer sp) {
            GuhwinterNpcs.klik(sp, doel);
        }
        return true;
    }
}
