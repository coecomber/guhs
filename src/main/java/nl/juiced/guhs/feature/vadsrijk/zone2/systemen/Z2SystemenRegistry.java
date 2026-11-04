package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Het Vadsrijk zone 2, slice "systemen" (Z2_BRIEF): level cap 35 (kern Levels), the zone-2 tree rows ({@link Boomrijen}), the
 * 3rd job advancement ({@link Advancement3}), gem sockets + raw crystals + the juwelier ({@link Kristallen}, {@link Juwelier}),
 * the status Bevroren ({@link Bevroren}), zone-2 weapons ({@link Z2Wapens}) and the Kandijkom shop ({@link Z2Winkel}).
 * Registered after every zone-1 slice (it adds to the klassen trees and the economie shops).
 * Commands (op 2): {@code /guhs vadsrijk z2systemen juwelier | winkel | kristal <soort> <kwaliteit> [n] | sokjes <n> | bevries [s]}.
 */
public final class Z2SystemenRegistry {
    private Z2SystemenRegistry() {
    }

    public static void register(IEventBus modBus) {
        Kristallen.ITEMS.register(modBus);
        Kristallen.COMPONENTS.register(modBus);
        Z2Wapens.ITEMS.register(modBus);
        Z2Winkel.ITEMS.register(modBus);
        Z2Wapens.registreer();
        Z2Winkel.registreer();
        Kristallen.init();
        Bevroren.init();
        Boomrijen.init();
        Advancement3.init();
        Haken.VERSLAGEN.add(Z2SystemenRegistry::kristalDrop);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Bevroren.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Juwelier.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(Z2SystemenRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        JuwelierPayloads.register(registrar);
    }

    /** Raw crystals from zone-2 foes: normal 8 % ruw, elite 35 % geslepen, boss always stralend (personal, per helper). */
    static void kristalDrop(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (m.mobLevel() < Z2SystemenPlekken.ZONE2_LEVEL || !Z2SystemenPlekken.inZone2(m.position())) {
            return;
        }
        boolean baas = m instanceof VadsrijkBaas;
        int q = Kristallen.dropKwaliteit(m.elite(), baas);
        double kans = baas ? 1.0 : m.elite() ? 0.35 : 0.08;
        List<String> soorten = List.copyOf(Kristallen.SOORTEN.keySet());
        for (ServerPlayer p : v.helpers()) {
            if (p.getRandom().nextDouble() >= kans) {
                continue;
            }
            ItemStack s = Kristallen.stack(soorten.get(p.getRandom().nextInt(soorten.size())), q, 1);
            LootBundel.laatVallen(p, m.position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), q);
        }
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("z2systemen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("juwelier").executes(c -> {
            Juwelier.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(Commands.literal("winkel").executes(c -> {
            Winkels.open(c.getSource().getPlayerOrException(), Z2Winkel.KANDIJKRAAM);
            return 1;
        }));
        k.then(Commands.literal("kristal").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Kristallen.SOORTEN.keySet(), b))
                .then(Commands.argument("kwaliteit", IntegerArgumentType.integer(0, 2)).executes(c -> kristal(c.getSource(),
                        StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "kwaliteit"), 1))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 64)).executes(c -> kristal(c.getSource(),
                                StringArgumentType.getString(c, "soort"), IntegerArgumentType.getInteger(c, "kwaliteit"), IntegerArgumentType.getInteger(c, "n")))))));
        k.then(Commands.literal("sokjes").then(Commands.argument("n", IntegerArgumentType.integer(0, 2)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ItemStack s = p.getItemInHand(InteractionHand.MAIN_HAND);
            Uitrusting u = Rollen.van(s);
            if (u == null) {
                return 0;
            }
            s.set(KernRegistry.UITRUSTING.get(), u.metSokjes(Collections.nCopies(IntegerArgumentType.getInteger(c, "n"), "")));
            return 1;
        })));
        k.then(Commands.literal("bevries").executes(c -> {
            Bevroren.geef(c.getSource().getPlayerOrException(), 100, null);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int kristal(CommandSourceStack src, String soort, int q, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ItemStack s = Kristallen.stack(soort, q, n);
        if (s.isEmpty()) {
            src.sendFailure(Component.literal("Onbekend kristal: " + soort));
            return 0;
        }
        p.getInventory().add(s);
        return 1;
    }
}
