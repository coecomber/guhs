package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-Z4BOOT: wires the Zeilbootje in (the entity + icon item, the payloads, the dev command {@code /guhs vadsrijk boot ...}), the
 * login rule (logged out at sea = back on the last dock), the "at sea" flag, and the rule that no other boat may float in the
 * Vadsrijk: vanilla boats/rafts and modded boats (ATM) can't be placed there and are refused when they would join the level.
 */
public final class BootRegistry {
    private BootRegistry() {
    }

    public static void register(IEventBus modBus) {
        BootSpul.ENTITIES.register(modBus);
        BootSpul.ITEMS.register(modBus);
        // T-INT: t-z4wereld's Kringstroom hook: the sailboat does its own current (BootZee = plan4.STROOM), so the world's push skips it
        nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4Stroming.EIGEN = e -> e instanceof BootEntity;
        // T-INT: t-z4verhaal's roles: z4_hq02 gives the boat, Botenguh Zeiltje (the dock NPC) opens the boat's own screen
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("z4_zeilboot_geef", Boot::geef);
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("z4_zeilboot", p -> Boot.scherm(p, ""));
        nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.zonderGarderobe(nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.ZEILBOOTJE);   // T-INT: no patterns/tack (own looks screen)
        // (attributes: EconomieRegistry registers them for every RijdierSoort, the boat included)
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BootCommando.register(e));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                Boot.spelerTik(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Boot.ingelogd(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Boot.PROEF.remove(e.getEntity().getUUID()));
        // no other boats in the Vadsrijk
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && Vadsrijk.in(e.getLevel()) && vreemdeBoot(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> {
            if (Vadsrijk.in(e.getEntity()) && vreemdBootItem(e.getItemStack())) {
                weiger(e.getEntity());
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.FAIL);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (Vadsrijk.in(e.getEntity()) && vreemdBootItem(e.getItemStack())) {
                weiger(e.getEntity());
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.FAIL);
            }
        });
    }

    public static void payloads(PayloadRegistrar r) {
        BootPayloads.register(r);
    }

    private static void weiger(Player p) {
        if (p instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("vadsrijk.z4boot.geen_vreemde_boot").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** A boat that is not ours: every vanilla boat/raft (AbstractBoat) and any other mod's entity called *boat* / *raft*. */
    public static boolean vreemdeBoot(Entity e) {
        if (e instanceof Player || e instanceof RijdierEntity || e.getClass().getName().startsWith("nl.juiced.guhs.feature.vadsrijk.")) {
            return false;
        }
        if (e instanceof AbstractBoat) {
            return true;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id != null && bootNaam(id.getPath());
    }

    /** A boat item that is not ours: vanilla BoatItem, or any other mod's item called *boat* / *raft*. */
    public static boolean vreemdBootItem(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        if (s.getItem() instanceof BoatItem) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id != null && !id.getPath().startsWith("vr_") && bootNaam(id.getPath());
    }

    private static boolean bootNaam(String pad) {
        return pad.endsWith("boat") || pad.endsWith("_raft") || pad.equals("raft") || pad.contains("boat_") || pad.contains("_boat");
    }
}
