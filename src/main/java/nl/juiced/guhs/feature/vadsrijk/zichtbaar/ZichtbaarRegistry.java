package nl.juiced.guhs.feature.vadsrijk.zichtbaar;

import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * U-ZICHTBAAR: equipped Vadsrijk gear visible on the player model (for yourself in F5 and for others). Server: {@link Zichtbaar}
 * (sync), client: {@code zichtbaar.client.ZichtbaarClient} (render layer + cape). Dev commands (op 2) under
 * {@code /guhs vadsrijk zichtbaar}:
 * <pre>
 *   aankleden [cape] [handschoenen] [armband]   a full test set in the 7 virtual slots (what was worn goes into your bag)
 *   uitkleden                                   every virtual slot into your bag
 *   status                                      what the server sends about you (the synced looks)
 * </pre>
 */
public final class ZichtbaarRegistry {
    private ZichtbaarRegistry() {
    }

    public static void register(IEventBus modBus) {
        nl.juiced.guhs.world.TikWacht.post("Zichtbaar.tick", Zichtbaar::tick);
        NeoForge.EVENT_BUS.addListener(Zichtbaar::startTracking);
        NeoForge.EVENT_BUS.addListener(Zichtbaar::login);
        NeoForge.EVENT_BUS.addListener(Zichtbaar::logout);
        NeoForge.EVENT_BUS.addListener(ZichtbaarRegistry::commando);
    }

    public static void payloads(PayloadRegistrar r) {
        ZichtbaarPayloads.register(r);
    }

    // ---- dev commands -------------------------------------------------------------------------------------------------------------

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> van(Slot s,
            com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(BasisTypes.alle().stream().filter(t -> t.slot() == s).map(BasisType::id), b);
    }

    static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("zichtbaar").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("aankleden").executes(c -> aankleden(c, "knuffelcapeje", "pluiswantjes", "pluisarmband"))
                .then(Commands.argument("cape", StringArgumentType.word()).suggests((c, b) -> van(Slot.CAPE, b))
                        .executes(c -> aankleden(c, StringArgumentType.getString(c, "cape"), "pluiswantjes", "pluisarmband"))
                        .then(Commands.argument("handschoenen", StringArgumentType.word()).suggests((c, b) -> van(Slot.HANDSCHOENEN, b))
                                .executes(c -> aankleden(c, StringArgumentType.getString(c, "cape"), StringArgumentType.getString(c, "handschoenen"),
                                        "pluisarmband"))
                                .then(Commands.argument("armband", StringArgumentType.word()).suggests((c, b) -> van(Slot.ARMBAND, b))
                                        .executes(c -> aankleden(c, StringArgumentType.getString(c, "cape"),
                                                StringArgumentType.getString(c, "handschoenen"), StringArgumentType.getString(c, "armband")))))));
        k.then(Commands.literal("uitkleden").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = 0;
            for (Slot s : Slot.SIERADEN) {
                n += Sieraden.afZonderCheck(p, s) ? 1 : 0;
            }
            Zichtbaar.ververs(p);
            int m = n;
            c.getSource().sendSuccess(() -> Component.literal(m + " stuk(ken) uitgetrokken (in je tas).").withStyle(ChatFormatting.GRAY), false);
            return n;
        }));
        k.then(Commands.literal("status").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Zichtbaar.ververs(p);
            List<String> l = Zichtbaar.voorKijker(p).looks();
            for (Slot s : Looks.VOLGORDE) {
                String id = Looks.van(l, s);
                c.getSource().sendSuccess(() -> Component.translatable(s.key()).append(": ")
                        .append(Component.literal(id.isEmpty() ? "-" : id).withStyle(id.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA)), false);
            }
            if (!nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(p)) {
                c.getSource().sendSuccess(() -> Component.literal("(Je bent niet in het Vadsrijk: daar is je Vadsrijk-uitrusting pas zichtbaar.)")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static int aankleden(CommandContext<CommandSourceStack> c, String cape, String handschoenen, String armband) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String[][] set = {{"CAPE", cape}, {"HANDSCHOENEN", handschoenen}, {"ARMBAND", armband}, {"RING", "roze_ring"}, {"RING2", "kristalring"},
                {"KETTING", "knuffelketting"}, {"OORBEL", "pluisoorbel"}};
        RandomSource r = p.getRandom();
        int lv = Math.max(1, Spelers.level(p));
        StringBuilder mis = new StringBuilder();
        for (String[] e : set) {
            Slot s = Slot.valueOf(e[0]);
            BasisType t = BasisTypes.van(e[1]);
            if (t == null || !s.past(t.slot())) {
                mis.append(' ').append(e[1]);
                continue;
            }
            ItemStack oud = Sieraden.get(p, s);
            if (!oud.isEmpty() && !p.getInventory().add(oud.copy())) {
                p.drop(oud.copy(), false);
            }
            Sieraden.zet(p, s, Rollen.stack(Rollen.rol(t, lv, Zeldzaamheid.ZELDZAAM, r)));
        }
        Sieraden.stuur(p);
        Spelers.sync(p);
        Zichtbaar.ververs(p);
        String m = mis.toString();
        c.getSource().sendSuccess(() -> Component.literal("Aangekleed: " + cape + ", " + handschoenen + ", " + armband + ", 2 ringen, ketting, oorbel."
                + (m.isEmpty() ? "" : " Niet gevonden:" + m) + " Druk F5 om jezelf te zien.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }
}
