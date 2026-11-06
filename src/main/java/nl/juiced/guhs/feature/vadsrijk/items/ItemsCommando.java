package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.Locale;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Smid;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * T-ITEMS dev commands (op 2), all under {@code /guhs vadsrijk items}:
 * <pre>
 *   geef &lt;basistype&gt; [zeldzaamheid] [level]   one rolled piece (any base type; default Zeldzaam, your level)
 *   nieuwe [zeldzaamheid]                    all 16 new gloves/capes/armbands at your level
 *   ringen                                   two rings (to test the 2nd ring slot)
 *   slots                                    what you wear in the 7 virtual slots
 *   uittrekken                               every virtual slot into your bag
 *   stats                                    your stats now (incl. the 4 new ones)
 *   vertrouwd &lt;xp&gt;                           item xp on everything you wear
 *   legendarisch                             a Legendarisch weapon of your class, Vertrouwd 5 (ready to grow)
 *   pity &lt;n&gt;                                 the pity counter of the piece in your hand
 *   talisman &lt;n&gt; | groeipluis &lt;n&gt;          materials
 *   smidspul                                 steentjes, knabbels, draad, 5 talismannen, 5 groeipluis, 50 zilver
 *   drankjes                                 fill the drankjesriem
 *   smid                                     open the smid
 * </pre>
 */
public final class ItemsCommando {
    private ItemsCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("items").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("geef").then(Commands.argument("basistype", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(BasisTypes.alle().stream().map(BasisType::id), b))
                .executes(c -> geef(c, Zeldzaamheid.ZELDZAAM, -1))
                .then(Commands.argument("zeldzaamheid", StringArgumentType.word()).suggests(ItemsCommando::zeldzaamheden)
                        .executes(c -> geef(c, Zeldzaamheid.van(StringArgumentType.getString(c, "zeldzaamheid")), -1))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, nl.juiced.guhs.feature.vadsrijk.kern.Levels.MAX))
                                .executes(c -> geef(c, Zeldzaamheid.van(StringArgumentType.getString(c, "zeldzaamheid")),
                                        IntegerArgumentType.getInteger(c, "level")))))));
        k.then(Commands.literal("nieuwe").executes(c -> nieuwe(c, Zeldzaamheid.ZELDZAAM))
                .then(Commands.argument("zeldzaamheid", StringArgumentType.word()).suggests(ItemsCommando::zeldzaamheden)
                        .executes(c -> nieuwe(c, Zeldzaamheid.van(StringArgumentType.getString(c, "zeldzaamheid"))))));
        k.then(Commands.literal("ringen").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (int i = 0; i < 2; i++) {
                geefStuk(p, "roze_ring", Zeldzaamheid.ZELDZAAM, Spelers.level(p));
            }
            return 1;
        }));
        k.then(Commands.literal("slots").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Slot s : Slot.SIERADEN) {
                ItemStack st = Sieraden.get(p, s);
                c.getSource().sendSuccess(() -> Component.translatable(s.key()).append(": ").append(st.isEmpty() ? Component.literal("-") : st.getHoverName()),
                        false);
            }
            return 1;
        }));
        k.then(Commands.literal("uittrekken").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Slot s : Slot.SIERADEN) {
                Sieraden.afZonderCheck(p, s);
            }
            return 1;
        }));
        k.then(Commands.literal("stats").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Stats s = Spelers.stats(p);
            for (Stat st : Stat.values()) {
                double v = s.get(st);
                c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, st.procent ? "%.1f%% " : "%.0f ", v))
                        .withStyle(st.ordinal() >= Stat.VERDEDIGING_NEGEREN.ordinal() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE)
                        .append(Component.translatable(st.key()).withStyle(ChatFormatting.GRAY)), false);
            }
            return 1;
        }));
        k.then(Commands.literal("vertrouwd").then(Commands.argument("xp", IntegerArgumentType.integer(1, 5000)).executes(c -> {
            Erfstukken.geefXp(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "xp"));
            return 1;
        })));
        k.then(Commands.literal("legendarisch").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String klasse = Spelers.klasse(p).id();
            BasisType wapen = BasisTypes.alle().stream().filter(t -> t.slot() == Slot.WAPEN && klasse.equals(t.klasse())).findFirst()
                    .orElse(BasisTypes.van("kussenzwaard"));
            Uitrusting u = Rollen.rol(wapen, Math.max(1, Spelers.level(p)), Zeldzaamheid.LEGENDARISCH, p.getRandom());
            u = u.metErf(u.erf().metXp(Erfstukken.DREMPELS[Erfstukken.MAX_VERTROUWD - 1]));
            EcoUtil.geefOfBewaar(p, Rollen.stack(u));
            return 1;
        }));
        k.then(Commands.literal("pity").then(Commands.argument("n", IntegerArgumentType.integer(0, 30)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ItemStack s = p.getMainHandItem();
            Uitrusting u = Rollen.van(s);
            if (u != null) {
                s.set(KernRegistry.UITRUSTING.get(), u.metErf(u.erf().metPity(IntegerArgumentType.getInteger(c, "n"))));
            }
            return u == null ? 0 : 1;
        })));
        k.then(Commands.literal("talisman").then(Commands.argument("n", IntegerArgumentType.integer(1, 64)).executes(c -> {
            EcoUtil.geefOfBewaar(c.getSource().getPlayerOrException(), new ItemStack(ItemsRegistry.KNUFFELTALISMAN.get(), IntegerArgumentType.getInteger(c, "n")));
            return 1;
        })));
        k.then(Commands.literal("groeipluis").then(Commands.argument("n", IntegerArgumentType.integer(1, 64)).executes(c -> {
            EcoUtil.geefOfBewaar(c.getSource().getPlayerOrException(), new ItemStack(ItemsRegistry.GROEIPLUIS.get(), IntegerArgumentType.getInteger(c, "n")));
            return 1;
        })));
        k.then(Commands.literal("smidspul").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 64));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.HERROLKNABBEL.get(), 32));
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.PLUISDRAAD.get(), 32));
            EcoUtil.geefOfBewaar(p, new ItemStack(ItemsRegistry.KNUFFELTALISMAN.get(), 5));
            EcoUtil.geefOfBewaar(p, new ItemStack(ItemsRegistry.GROEIPLUIS.get(), 5));
            Spelers.geefMunten(p, 5000);
            return 1;
        }));
        k.then(Commands.literal("drankjes").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Drankjesriem.leg(p, new ItemStack(EcoSpul.VADSDRANKJE.get(), 8));
            Drankjesriem.leg(p, new ItemStack(EcoSpul.ENERGIEDRANKJE.get(), 8));
            Drankjesriem.leg(p, new ItemStack(EcoSpul.VAHOEGDRANKJE.get(), 4));
            Drankjesriem.stuur(p);
            return 1;
        }));
        k.then(Commands.literal("smid").executes(c -> {
            Smid.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> zeldzaamheden(CommandContext<CommandSourceStack> c,
            com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(java.util.Arrays.stream(Zeldzaamheid.values()).map(z -> z.name().toLowerCase(Locale.ROOT)), b);
    }

    private static int geef(CommandContext<CommandSourceStack> c, Zeldzaamheid z, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "basistype");
        if (BasisTypes.van(id) == null) {
            c.getSource().sendFailure(Component.literal("onbekend basistype: " + id));
            return 0;
        }
        geefStuk(p, id, z, level < 1 ? Math.max(1, Spelers.level(p)) : level);
        return 1;
    }

    private static int nieuwe(CommandContext<CommandSourceStack> c, Zeldzaamheid z) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (String id : ItemsRegistry.STUKKEN.keySet()) {
            geefStuk(p, id, z, Math.max(1, Spelers.level(p)));
        }
        return 1;
    }

    static void geefStuk(ServerPlayer p, String id, Zeldzaamheid z, int level) {
        BasisType t = BasisTypes.van(id);
        if (t != null) {
            EcoUtil.geefOfBewaar(p, Rollen.stack(Rollen.rol(t, level, z, p.getRandom())));
        }
    }
}
