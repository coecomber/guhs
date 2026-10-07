package nl.juiced.guhs.feature.guhriobeloning;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;

/**
 * bbq2 (guhrio-beloning): everything of Super Guhrio that is not a level. On the forecourt of the Kasteel van de Grote
 * Nether-Mika and in the tower room behind the duel:
 * <ul>
 *     <li><b>Pad-guh</b> ({@link Padguh}): the intro ("the princess was taken for a piece of cake"), the running gag after
 *     each world, his shop ({@link Winkel}: the level coins of {@code GuhrioKasteel} buy outfits and building blocks), the
 *     overview of your big vadsmunten per level, and the golden cap for all eighteen.</li>
 *     <li>The <b>highscore board</b> ({@link ScorebordBlock}): a floating board with the server records, and a click for
 *     your own times next to them, per level and for the whole castle in one go.</li>
 *     <li><b>Prinses Perzikguh</b> ({@link Perzikguh}) in the tower room, and next to her the Guhshi who "is yours" once
 *     per player after the duel ({@link Guhshi}: a story copy to click; your own Guhshi comes saddled, flutters over gaps
 *     when you ride him and licks up kaasknabbels from a distance).</li>
 *     <li>The <b>building blocks</b>: the {@link VraagblokBlock} (one knabbel per day per player), the
 *     {@link VlaggenmastBlock} and the {@link PijpBlock}, a green pipe that really takes you to the nearest pipe of its
 *     colour within {@link Pijpreis#BEREIK} blocks ({@link Pijpreis}).</li>
 *     <li>The <b>outfits</b> (GuhClothes GUHRIOBELONING_*, source "guhrio_beloning"): red cap with moustache, green cap,
 *     turtle shell (the shop), the princess crown (from the princess) and the golden cap (all 18 big vadsmunten).</li>
 * </ul>
 * Everything is per player: coins, purchases, the daily knabbel, Guhshi. Nothing here hurts anybody.
 * The questline {@link #LIJN} ("guhrio_beloning", group "guhrio") is the Guhdex page of all this; while it is not done
 * it lists the eighteen big vadsmunten per level, ticked when you have them.
 * Resources: tools/features/guhrio_beloning.py (+ guhrio_beloning_bouw, _tex, _modellen, _wiki).
 */
public final class GuhrioBeloningFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The ids of what Bezetting keeps at every castle (the template brings them; Bezetting brings them back). */
    public static final String PADGUH_ID = "guhriobeloning_padguh", PERZIKGUH_ID = "guhriobeloning_perzikguh", GUHSHI_ID = "guhriobeloning_guhshi";
    /** Where they are, in the coordinates of the whole castle (tools/features/guhrio_beloning_bouw.py PLEKKEN; the generator's self-check compares them). */
    public static final BlockPos PADGUH_PLEK = new BlockPos(51, 28, 120);
    public static final BlockPos PERZIKGUH_PLEK = new BlockPos(63, 43, 63);
    public static final BlockPos GUHSHI_PLEK = new BlockPos(68, 43, 65);
    /** The clothes source of the five outfits (= the castle's group in the Minigames tab of the Guhdex). */
    public static final String BRON = GuhrioKasteel.GROEP;

    // --- the building blocks ----------------------------------------------------------------------------------------------
    /** The ?-block to build with: one kaasknabbel per day per player (bump it with your head, or click it). */
    public static final DeferredBlock<VraagblokBlock> VRAAGBLOK = BLOCKS.registerBlock("guhriobeloning_vraagblok", VraagblokBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).sound(SoundType.STONE).lightLevel(s -> 6));
    /** The flagpole to build with: a foot, a pole, a golden ball with a flag on top. */
    public static final DeferredBlock<VlaggenmastBlock> VLAGGENMAST = BLOCKS.registerBlock("guhriobeloning_vlaggenmast", VlaggenmastBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.0f, 6f).sound(SoundType.COPPER).noOcclusion()
                    .pushReaction(PushReaction.DESTROY).isValidSpawn((s, l, p, e) -> false));
    /** The green pipe that really works. */
    public static final DeferredBlock<PijpBlock> PIJP = BLOCKS.registerBlock("guhriobeloning_pijp", PijpBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(s -> s.getValue(PijpBlock.KLEUR).getMapColor()).strength(1.5f, 6f).sound(SoundType.COPPER)
                    .noOcclusion().isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    /** The highscore board of the castle. */
    public static final DeferredBlock<ScorebordBlock> SCOREBORD = BLOCKS.registerBlock("guhriobeloning_scorebord", ScorebordBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.0f, 6f).sound(SoundType.WOOD).lightLevel(s -> 8));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScorebordBlock.Entity>> SCOREBORD_BE = BLOCK_ENTITIES.register(
            "guhriobeloning_scorebord", () -> new BlockEntityType<>(ScorebordBlock.Entity::new, SCOREBORD.get()));

    public static final DeferredItem<BlockItem> VRAAGBLOK_ITEM = ITEMS.registerSimpleBlockItem(VRAAGBLOK);
    public static final DeferredItem<BlockItem> VLAGGENMAST_ITEM = ITEMS.registerSimpleBlockItem(VLAGGENMAST);
    public static final DeferredItem<BlockItem> PIJP_ITEM = ITEMS.registerSimpleBlockItem(PIJP);
    public static final DeferredItem<BlockItem> SCOREBORD_ITEM = ITEMS.registerSimpleBlockItem(SCOREBORD);

    /** The five outfits, in the order of the Guhdex page. */
    public static final List<GuhClothes> OUTFITS = List.of(GuhClothes.GUHRIOBELONING_RODE_PET, GuhClothes.GUHRIOBELONING_GROENE_PET,
            GuhClothes.GUHRIOBELONING_SCHILD, GuhClothes.GUHRIOBELONING_PRINSESSENKROON, GuhClothes.GUHRIOBELONING_GOUDEN_PET);

    // --- the questline (the Guhdex page) ------------------------------------------------------------------------------------
    /** The steps of {@link #LIJN}: meet Pad-guh, Guhshi is yours, the golden cap; 3 = done. */
    public static final int STAP_PADGUH = 0, STAP_GUHSHI = 1, STAP_GOUDEN_PET = 2;

    /** "Pad-guh's winkel & beloningen": the Guhdex page with your big vadsmunten per level and the rewards of the castle. */
    public static final Verhaallijn LIJN = Verhaallijn.maak("guhrio_beloning", "guhrio").stappen(3).icoon("guhs:guhriobeloning_rode_pet")
            .nodig(GuhrioBeloningFeature::nodig).beloningen(GuhrioBeloningFeature::beloningen)
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, GuhrioKasteel.STRUCTUUR,
                    Component.translatable("structure.guhs." + GuhrioKasteel.STRUCTUUR)))
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        // who lives at the castle (also at castles whose inhabitants got lost)
        Bezetting.npc(PADGUH_ID, GuhrioKasteel.STRUCTUUR, null, PADGUH_PLEK, GuhNpcEntity.Kind.PADGUH, null, 0f);
        Bezetting.npc(PERZIKGUH_ID, GuhrioKasteel.STRUCTUUR, null, PERZIKGUH_PLEK, GuhNpcEntity.Kind.PERZIKGUH, null, 0f);
        Bezetting.wezen(GUHSHI_ID, GuhrioKasteel.STRUCTUUR, null, GUHSHI_PLEK, Guhshi::kopie);
        NpcRollen.zet(GuhNpcEntity.Kind.PADGUH, new Padguh());
        NpcRollen.zet(GuhNpcEntity.Kind.PERZIKGUH, new Perzikguh());
        Praat.luister(Padguh.SLEUTEL, Padguh::antwoord);
        Praat.luister(Guhshi.SLEUTEL, Guhshi::antwoord);
        // Guhshi: the copy in the tower room, and what a Guhshi of your own does
        VerhaalGuhs.opKlik(VerhaalGuh.GUHSHI, Guhshi::klikKopie);
        VariantGedragen.zet(GuhVariant.GUHSHI, new Guhshi.Gedrag());
        GuhrioKasteel.BIJ_DUEL.add(Guhshi::duelGewonnen);
        GuhrioSpel.BIJ_KLAAR.add(Winkel::fooi);
        GuhrioSpel.BIJ_KLAAR.add(Padguh::levelKlaar);
        // the outfits
        KledingBronnen.bron(GuhClothes.GUHRIOBELONING_RODE_PET, BRON, KledingBronnen.prijs("gui.guhs.guhriobeloning.prijs.munten", Winkel.Waar.RODE_PET.prijs));
        KledingBronnen.bron(GuhClothes.GUHRIOBELONING_GROENE_PET, BRON, KledingBronnen.prijs("gui.guhs.guhriobeloning.prijs.munten", Winkel.Waar.GROENE_PET.prijs));
        KledingBronnen.bron(GuhClothes.GUHRIOBELONING_SCHILD, BRON, KledingBronnen.prijs("gui.guhs.guhriobeloning.prijs.munten", Winkel.Waar.SCHILD.prijs));
        KledingBronnen.bron(GuhClothes.GUHRIOBELONING_PRINSESSENKROON, BRON, KledingBronnen.prijs("gui.guhs.guhriobeloning.prijs.kroon"));
        KledingBronnen.bron(GuhClothes.GUHRIOBELONING_GOUDEN_PET, BRON, KledingBronnen.prijs("gui.guhs.guhriobeloning.prijs.gouden_pet"));
        NeoForge.EVENT_BUS.register(GuhrioBeloningEvents.class);
        NeoForge.EVENT_BUS.addListener(GuhrioBeloningFeature::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhrioBeloningPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VRAAGBLOK_ITEM.get()));
        output.accept(new ItemStack(VLAGGENMAST_ITEM.get()));
        output.accept(new ItemStack(PIJP_ITEM.get()));
        output.accept(new ItemStack(SCOREBORD_ITEM.get()));
    }

    // =====================================================================================================================
    // the Guhdex page
    // =====================================================================================================================

    /**
     * Brings the questline up to date with what the player really has (the three things can happen in any order): Pad-guh
     * met, Guhshi tamed, the golden cap. Call it after any of them.
     */
    public static void bijwerken(ServerPlayer p) {
        if (LIJN.stap(p) == STAP_PADGUH && LIJN.vlag(p, Padguh.ONTMOET)) {
            LIJN.begin(p);
            LIJN.verder(p, STAP_PADGUH);
        }
        if (LIJN.stap(p) == STAP_GUHSHI && VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI)) {
            LIJN.verder(p, STAP_GUHSHI);
        }
        if (LIJN.stap(p) == STAP_GOUDEN_PET && LIJN.vlag(p, Padguh.GOUDEN_PET)) {
            LIJN.verder(p, STAP_GOUDEN_PET);
        }
    }

    /** While the page is not done: every big vadsmunt of the castle, level by level, ticked when it is yours. */
    private static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        List<VerhaalStand.Nodig> uit = new ArrayList<>();
        for (int i = 0; i < GuhrioKasteel.LEVELS.size(); i++) {
            int heeft = GuhrioKasteel.vadsmunten(p, GuhrioKasteel.LEVELS.get(i));
            for (int n = 0; n < 3; n++) {
                boolean had = (heeft >> n & 1) != 0;
                uit.add(Verhaallijn.nodig(had ? "guhs:guhrio_vadsmunt" : "guhs:guhrio_vadsmunt_schim",
                        "gui.guhs.guhriobeloning.vads." + (i / 2 + 1) + "_" + (i % 2 + 1) + "." + n, had ? 1 : 0, 1));
            }
        }
        return uit;
    }

    /**
     * The secrets of the castle that have a quest in the quest book: the secret room of 1-1 and of 1-2 (world 1 remembers
     * them) and the warp room of world 2. The Guhdex says which you found, not where they are.
     */
    public static final int GEHEIMEN = 3;

    /** Guhshi, the five outfits, whether you ever bought something from Pad-guh, and the castle's secrets you found. */
    private static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        List<VerhaalStand.Beloning> uit = new ArrayList<>();
        uit.add(Verhaallijn.beloning("guhs:guhrio_guhshi_ei", "gui.guhs.guhriobeloning.beloning.guhshi", VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI)));
        for (GuhClothes c : OUTFITS) {
            uit.add(Verhaallijn.beloning("guhs:" + c.id(), "gui.guhs.guhriobeloning.beloning." + c.id(), KledingUnlocks.heeft(p, c)));
        }
        uit.add(Verhaallijn.beloning("guhs:guhriobeloning_pijp", "gui.guhs.guhriobeloning.beloning.bouwblokken", LIJN.vlag(p, Winkel.BLOK_GEKOCHT)));
        uit.add(Verhaallijn.beloning("guhs:guhrio_vraagblok", "gui.guhs.guhriobeloning.geheim.1_1",
                nl.juiced.guhs.feature.guhriow1.Binnentuin.geheimGevonden(p, nl.juiced.guhs.feature.guhriow1.Binnentuin.LEVEL_1)));
        uit.add(Verhaallijn.beloning("guhs:guhrio_vraagblok", "gui.guhs.guhriobeloning.geheim.1_2",
                nl.juiced.guhs.feature.guhriow1.Binnentuin.geheimGevonden(p, nl.juiced.guhs.feature.guhriow1.Binnentuin.LEVEL_2)));
        uit.add(Verhaallijn.beloning("guhs:guhrio_pijp", "gui.guhs.guhriobeloning.geheim.warp", nl.juiced.guhs.feature.guhriow2.GuhrioW2.warpGevonden(p)));
        return uit;
    }

    // =====================================================================================================================
    // /guhs guhriobeloning (operators; for AutoCheck scripts and dev checks; literal texts)
    // =====================================================================================================================

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("guhriobeloning").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(GuhrioBeloningFeature::stand))
                .then(Commands.literal("wis").executes(GuhrioBeloningFeature::wis))
                .then(Commands.literal("munten").then(Commands.argument("n", IntegerArgumentType.integer(0, 9999)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhrioSpel.spaar(p).putInt("Munten", IntegerArgumentType.getInteger(c, "n"));
                    return stand(c);
                })))
                .then(Commands.literal("duel").executes(c -> {
                    GuhrioKasteel.winDuel(c.getSource().getPlayerOrException());
                    return stand(c);
                }))
                .then(Commands.literal("guhshi").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    VerhaalGuhs.geefVrij(p, VerhaalGuh.GUHSHI);
                    boolean gelukt = Guhshi.geef(p, p.position().add(p.getLookAngle().multiply(2, 0, 2))) != null;
                    c.getSource().sendSuccess(() -> Component.literal(gelukt ? "Guhshi is yours" : "You already have your Guhshi (wis first)"), false);
                    return gelukt ? 1 : 0;
                }))
                .then(Commands.literal("dag").executes(c -> {
                    Vraagblok.vergeet(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("The ?-block has a knabbel for you again"), false);
                    return 1;
                }))));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        c.getSource().sendSuccess(() -> Component.literal("guhriobeloning: step " + LIJN.stap(p) + " of 3, " + GuhrioSpel.munten(p) + " coins, "
                + GuhrioKasteel.alleVadsmunten(p) + "/18 vadsmunten, duel " + (GuhrioKasteel.duelGewonnen(p) ? "won" : "not won") + ", Guhshi "
                + (VerhaalGuhs.heeftGetemd(p, VerhaalGuh.GUHSHI) ? "tamed" : VerhaalGuhs.isVrij(p, VerhaalGuh.GUHSHI) ? "free" : "not yet")), false);
        return 1 + LIJN.stap(p);
    }

    private static int wis(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        LIJN.wis(p);
        VerhaalGuhs.vergeet(p, VerhaalGuh.GUHSHI);
        Vraagblok.vergeet(p);
        c.getSource().sendSuccess(() -> Component.literal("guhriobeloning: forgotten for ").append(p.getDisplayName()), false);
        return 1;
    }

    private GuhrioBeloningFeature() {
    }
}
