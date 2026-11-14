package nl.juiced.guhs.feature.vadsrijk.huisdieren;

import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * Het Vadsrijk, G-PETS: the pets ({@link Huisdieren}). Registers the pet entity, the hooks (a boss's first kill unlocks its pet:
 * {@code Haken.VERSLAGEN}; leaving the Vadsrijk sends the pet home, coming in brings it back), the follow tick, the payloads and the
 * dev command {@code /guhs vadsrijk huisdier geef <id> | alle | reset | scherm | kies <id|geen>}.
 */
public final class HuisdierenRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<HuisdierEntity>> HUISDIER = ENTITIES.register("vr_huisdier",
            () -> EntityType.Builder.<HuisdierEntity>of(HuisdierEntity::new, MobCategory.MISC).sized(0.4f, 0.45f).eyeHeight(0.3f)
                    .clientTrackingRange(8).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_huisdier"))));

    private HuisdierenRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(HUISDIER.get(), HuisdierEntity.attributen().build()));
        Huisdieren.alle();   // (loads the standard pets)
        Haken.VERSLAGEN.add(Huisdieren::verslagen);
        Haken.BINNEN.add((p, binnen) -> {
            if (binnen) {
                Huisdieren.inhalen(p);
            } else {
                Huisdieren.weg(p, false);
            }
        });
        nl.juiced.guhs.world.TikWacht.post("HuisdierenRegistry:56", e -> Huisdieren.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Huisdieren.uitgelogd(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(HuisdierenRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        HuisdierPayloads.register(registrar);
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("huisdier").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("geef").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Huisdieren.alle().stream().map(Huisdier::id), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(c, "id");
                    if (Huisdieren.van(id) == null) {
                        c.getSource().sendFailure(Component.literal("onbekend huisdier"));
                        return 0;
                    }
                    if (!Huisdieren.geef(p, id)) {
                        c.getSource().sendFailure(Component.literal("heb je al"));
                        return 0;
                    }
                    return 1;
                })));
        m.then(Commands.literal("kies").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                        java.util.stream.Stream.concat(Huisdieren.alle().stream().map(Huisdier::id), Arrays.stream(new String[] {"geen"})), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(c, "id");
                    if (!Huisdieren.kies(p, id.equals("geen") ? null : id)) {
                        c.getSource().sendFailure(Component.literal("dat huisdier heb je niet"));
                        return 0;
                    }
                    Huisdieren.bijwerken(p);
                    return 1;
                })));
        m.then(Commands.literal("alle").executes(c -> {
            Huisdieren.alles(c.getSource().getPlayerOrException());
            return 1;
        }));
        m.then(Commands.literal("reset").executes(c -> {
            Huisdieren.reset(c.getSource().getPlayerOrException());
            c.getSource().sendSuccess(() -> Component.literal("huisdieren gewist"), false);
            return 1;
        }));
        m.then(Commands.literal("scherm").executes(c -> {
            Huisdieren.scherm(c.getSource().getPlayerOrException());
            return 1;
        }));
        // (AutoCheck) the pet runs back to you from far away: puts it 20 blocks away so the next tick teleports it
        m.then(Commands.literal("weg").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            HuisdierEntity e = Huisdieren.uit(p);
            if (e == null) {
                c.getSource().sendFailure(Component.literal("geen huisdier uit"));
                return 0;
            }
            e.snapTo(p.getX() + 20, p.getY() + 1, p.getZ(), 0, 0);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }
}
