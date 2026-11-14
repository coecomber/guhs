package nl.juiced.guhs.feature.vadsrijk.zonepoort;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * X-ZONE1: registers the zone gate ({@link ZonePoort}): the server config {@code guhs-vadsrijk-zones.toml}, the safety-net tick and
 * the op command {@code /guhs vadsrijk zones [open <n>]}.
 */
public final class ZonePoortRegistry {
    private ZonePoortRegistry() {
    }

    public static void register(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.SERVER, ZonePoortConfig.SPEC, "guhs-vadsrijk-zones.toml"));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ZonePoort.tik(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> ZonePoort.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(ZonePoortRegistry::commando);
        NeoForge.EVENT_BUS.addListener(LootOpruimen::commando);   // 1101-balans: /guhs vadsrijk loot opruimen (one-time cleanup)
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> z = Commands.literal("zones").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> status(c.getSource()));
        z.then(Commands.literal("open").then(Commands.argument("n", IntegerArgumentType.integer(1, 6)).executes(c -> {
            int n = IntegerArgumentType.getInteger(c, "n");
            ZonePoortConfig.zet(n);
            for (ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers()) {
                Spelers.sync(p);   // (the HUD bar: the soft cap moved; the quest tracker follows within a second by itself)
            }
            c.getSource().sendSuccess(() -> Component.literal("Het Vadsrijk: zones 1-" + n + " open"
                    + (n >= 6 ? " (alles open)." : "; zone " + (n + 1) + "-6 binnenkort. Opgeslagen in guhs-vadsrijk-zones.toml.")), true);
            return n;
        })));
        // X-INT (dev): see the release view as an op in creative. "alsspeler aan" = the gate treats EVERY player as a normal
        // player (also ops/creative; not saved, off again after a restart); "zone1klaar" = your zone-1 main story done + level 20.
        z.then(Commands.literal("alsspeler").then(Commands.literal("aan").executes(c -> alsSpeler(c.getSource(), true)))
                .then(Commands.literal("uit").executes(c -> alsSpeler(c.getSource(), false))));
        z.then(Commands.literal("zone1klaar").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (nl.juiced.guhs.feature.vadsrijk.npc.Quest q : nl.juiced.guhs.feature.vadsrijk.npc.Quests.alle().values()) {
                if (q.hoofd() && nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken.zoneVan(q.id()) == 1
                        && !nl.juiced.guhs.feature.vadsrijk.npc.Questlog.klaar(p, q.id())) {
                    nl.juiced.guhs.feature.vadsrijk.npc.Questlog.voltooi(p, q);
                }
            }
            nl.juiced.guhs.feature.vadsrijk.npc.Questlog.zetVlag(p, nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.ZONE1_VLAG, true);
            if (Spelers.level(p) < 20) {
                Spelers.zetLevel(p, 20);
            }
            Spelers.sync(p);
            c.getSource().sendSuccess(() -> Component.literal("Zone-1-hoofdverhaal klaar (dev), level 20. Ga naar de luchtschipper op De Haven."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(z));
    }

    private static int alsSpeler(CommandSourceStack s, boolean aan) {
        ZonePoort.TEST_STRENG = aan;
        s.sendSuccess(() -> Component.literal(aan ? "Zone-poort: iedereen telt nu als gewone speler (ook ops/creative). Uit: /guhs vadsrijk zones alsspeler uit"
                : "Zone-poort: ops en creative mogen weer overal in."), false);
        return 1;
    }

    private static int status(CommandSourceStack s) {
        int n = ZonePoort.open();
        s.sendSuccess(() -> Component.literal("Het Vadsrijk: zones 1-" + n + " open" + (n >= 6 ? " (alles)." : ", zone " + (n + 1) + "-6 binnenkort; level-plafond "
                + ZonePoort.PLAFOND[n] + ".") + " Ops/creative mogen overal in. Wijzig met /guhs vadsrijk zones open <1-6>."), false);
        return n;
    }
}
