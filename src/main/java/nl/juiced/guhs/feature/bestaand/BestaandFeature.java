package nl.juiced.guhs.feature.bestaand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
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
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (bestaand): two questlines in the two EXISTING buildings of the Guhbarbecuether (DESIGN_130 par. 3). Nothing here is
 * worldgen: the NPCs and their props come through {@link Bezetting}, so they also appear at the Spiesburchten and
 * Mika-grillpaleizen that were generated long ago.
 * <ul>
 *   <li>The <b>Wachter-guh</b> ({@link Wachter}, questline {@link #WACHTER}) in his wachthokje next to the statue of every
 *       Spiesburcht: light the four bridge fires ({@link Vuren}, block {@link #VUURKORF}) with his loaned
 *       {@link #AANSTEEKSPIES}, weed the Mikakruid out of the pindasaus-tuintje ({@link Tuintje}). Reward: the recipe card
 *       of the {@link #ZIELIG_LANTAARNTJE} (the soul lantern parody), two of them, and the wachterspak.</li>
 *   <li>The captive <b>Knuffelmaker-guh</b> ({@link Knuffelmaker}, questline {@link #KNUFFELMAKER}) in his naaihoek on the
 *       first floor of every Mika-grillpaleis: free the three plush guhs from their cages ({@link Kooien}: pay one vahoege
 *       vads per cage, or sneak and pick the lock while no Nether-Mika looks), bring thread. Reward: the knuffelpatroon and
 *       the three plush deco blocks.</li>
 * </ul>
 * Everything a player does is theirs alone: the world never changes. A fire that a player lit, a weed they pulled and a
 * plush they freed are block states shown to that player only ({@link Schijn}); everybody else still finds the fire cold and
 * the cage full. So an unlimited number of players can do both questlines at the same building at the same time, and the
 * two buildings (which anyone may break, CONTRACT_130 D7) hold nothing that can be taken away: a fire bowl is not broken
 * in survival, and a cage or a fire that is gone all the same is simply counted as done by its NPC.
 * <p>
 * The empty throne sign of the grillpaleis stays: the Grote Nether-Mika lives in the castle of Super Guhrio.
 * Resources: tools/features/bestaand.py (+ bestaand_tex.py, bestaand_bouw.py with the spots of {@link Plekken}).
 */
public final class BestaandFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The ids of what Bezetting brings to the two buildings (their tags, and the keys of SavedData guhs:bezetting). */
    public static final String WACHTER_ID = "bestaand_wachter", WACHTHOKJE_ID = "bestaand_wachthokje", BRUGVUUR_ID = "bestaand_brugvuur_",
            KNUFFELMAKER_ID = "bestaand_knuffelmaker", NAAIHOEK_ID = "bestaand_naaihoek";
    public static final String SPIESBURCHT = "spiesburcht", GRILLPALEIS = "mika_grillpaleis";

    /** What a ransom may be paid with (a vahoege vads or a vadsstaaf: "Losgeld: 1 vads per knuffel", says the old sign). */
    public static final TagKey<Item> LOSGELD = TagKey.create(Registries.ITEM, Guhs.id("bestaand_losgeld"));
    /** What lights a bridge fire (the Wachter-guh's skewer, and anything else that makes fire). */
    public static final TagKey<Item> AANSTEKERS = TagKey.create(Registries.ITEM, Guhs.id("bestaand_aanstekers"));

    // --- blocks ----------------------------------------------------------------------------------------------------------
    /** A bridge fire: a quest prop (no item, not broken in survival); lit only in the eyes of who lit it. */
    public static final DeferredBlock<VuurkorfBlock> VUURKORF = BLOCKS.registerBlock("bestaand_vuurkorf", VuurkorfBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion()
                    .sound(SoundType.LANTERN).pushReaction(PushReaction.BLOCK).lightLevel(s -> s.getValue(VuurkorfBlock.LIT) ? 15 : 0)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VuurkorfBlockEntity>> VUURKORF_BE = BLOCK_ENTITIES.register(
            "bestaand_vuurkorf", () -> new BlockEntityType<>(VuurkorfBlockEntity::new, VUURKORF.get()));
    /** A tuft of Mikakruid: never really in the world, only shown to the player who still has to pull it. */
    public static final DeferredBlock<MikakruidBlock> MIKAKRUID = BLOCKS.registerBlock("bestaand_mikakruid", MikakruidBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak().noLootTable().noOcclusion()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY));
    /** The Wachter-guh's lantern, the soul lantern parody: it looks pitiful until you pat it. */
    public static final DeferredBlock<LantaarntjeBlock> ZIELIG_LANTAARNTJE = BLOCKS.registerBlock("bestaand_zielig_lantaarntje",
            LantaarntjeBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f, 3.5f).noOcclusion()
                    .sound(SoundType.LANTERN).pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(LantaarntjeBlock.BLIJ) ? 15 : 10));
    /** The plush deco blocks of the Knuffelmaker-guh. */
    public static final DeferredBlock<KnuffelBlock> KNUFFELGUH = knuffel("bestaand_knuffelguh", MapColor.COLOR_PINK, 1.25f);
    public static final DeferredBlock<KnuffelBlock> KNUFFELMIKA = knuffel("bestaand_knuffelmika", MapColor.COLOR_BLACK, 0.7f);
    public static final DeferredBlock<KnuffelBlock> KNUFFELROOKGUH = knuffel("bestaand_knuffelrookguh", MapColor.COLOR_LIGHT_GRAY, 1.6f);
    /** The plush of cage i (in the naaihoek, for the player who freed it). */
    public static final List<DeferredBlock<KnuffelBlock>> KNUFFELS = List.of(KNUFFELGUH, KNUFFELMIKA, KNUFFELROOKGUH);

    private static DeferredBlock<KnuffelBlock> knuffel(String id, MapColor kleur, float toon) {
        return BLOCKS.registerBlock(id, p -> new KnuffelBlock(p, toon), () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.4f)
                .noOcclusion().sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).ignitedByLava());
    }

    // --- items -----------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> ZIELIG_LANTAARNTJE_ITEM = ITEMS.registerSimpleBlockItem(ZIELIG_LANTAARNTJE);
    public static final DeferredItem<BlockItem> KNUFFELGUH_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELGUH);
    public static final DeferredItem<BlockItem> KNUFFELMIKA_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELMIKA);
    public static final DeferredItem<BlockItem> KNUFFELROOKGUH_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELROOKGUH);
    /** The Wachter-guh's skewer with a glowing coal (loaned: item tag guhs:loaned): lights a bridge fire. */
    public static final DeferredItem<Item> AANSTEEKSPIES = ITEMS.registerSimpleItem("bestaand_aansteekspies", () -> new Item.Properties().stacksTo(1));
    /** The recipe cards (CONTRACT_130 D5): an ingredient of their recipes that stays in the crafting grid. */
    public static final DeferredItem<ReceptItem> RECEPT_LANTAARN = ITEMS.registerItem("bestaand_recept_lantaarn", ReceptItem::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<ReceptItem> RECEPT_KNUFFEL = ITEMS.registerItem("bestaand_recept_knuffel", ReceptItem::new,
            () -> new Item.Properties().stacksTo(1));

    // --- the questlines ----------------------------------------------------------------------------------------------------
    /** The steps of {@link #WACHTER} after the first talk (0). */
    public static final int WACHTER_VUREN = 1, WACHTER_MELDEN = 2, WACHTER_TUIN = 3, WACHTER_BELONING = 4;
    /** The steps of {@link #KNUFFELMAKER} after the first talk (0). */
    public static final int KNUFFELMAKER_KOOIEN = 1, KNUFFELMAKER_DRAAD = 2;
    /** How much thread the Knuffelmaker-guh wants. */
    public static final int DRAAD = 4;

    /** "De wacht bij de Spiesburcht": 0 meet him, 1 the four fires, 2 report, 3 the weeds, 4 the reward; 5 = done. */
    public static final Verhaallijn WACHTER = Verhaallijn.maak("wachter", "barbecue").stappen(5).icoon("guhs:bestaand_zielig_lantaarntje")
            .nodig((p, stap) -> switch (stap) {
                case WACHTER_VUREN -> List.of(Verhaallijn.nodig("minecraft:campfire", "gui.guhs.bestaand.nodig.vuren", Vuren.aantal(p), Vuren.AANTAL));
                case WACHTER_TUIN -> List.of(Verhaallijn.nodig("guhs:pindascheutjes", "gui.guhs.bestaand.nodig.kruid", Tuintje.aantal(p), Tuintje.AANTAL));
                default -> List.of();
            })
            .beloningen(p -> {
                boolean klaar = BestaandFeature.WACHTER.klaar(p);
                return List.of(Verhaallijn.beloning("guhs:bestaand_recept_lantaarn", klaar), Verhaallijn.beloning("guhs:bestaand_zielig_lantaarntje", klaar),
                        Verhaallijn.beloning("guhs:bestaand_wachtershelm", "gui.guhs.bestaand.beloning.wachterspak", klaar));
            })
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, SPIESBURCHT, Component.translatable("structure.guhs.spiesburcht")))
            .registreer();
    /** "De gestolen knuffels": 0 meet him, 1 the three cages, 2 thread; 3 = done. */
    public static final Verhaallijn KNUFFELMAKER = Verhaallijn.maak("knuffelmaker", "barbecue").stappen(3).icoon("guhs:bestaand_knuffelguh")
            .nodig((p, stap) -> switch (stap) {
                case KNUFFELMAKER_KOOIEN -> List.of(Verhaallijn.nodig("guhs:roosterijzer_tralies", "gui.guhs.bestaand.nodig.knuffels", Kooien.aantal(p),
                        Kooien.AANTAL));
                case KNUFFELMAKER_DRAAD -> List.of(Verhaallijn.nodig("minecraft:string", GuhQuests.count(p, Items.STRING), DRAAD));
                default -> List.of();
            })
            .beloningen(p -> {
                boolean klaar = BestaandFeature.KNUFFELMAKER.klaar(p);
                return List.of(Verhaallijn.beloning("guhs:bestaand_recept_knuffel", klaar),
                        Verhaallijn.beloning("guhs:bestaand_knuffelguh", "gui.guhs.bestaand.beloning.knuffels", klaar));
            })
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, GRILLPALEIS, Component.translatable("structure.guhs.mika_grillpaleis")))
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        bezetting();
        NpcRollen.zet(GuhNpcEntity.Kind.WACHTERGUH, new Wachter());
        NpcRollen.zet(GuhNpcEntity.Kind.KNUFFELMAKERGUH, new Knuffelmaker());
        KledingBronnen.bron(GuhClothes.BESTAAND_WACHTERSHELM, "bestaand");
        KledingBronnen.bron(GuhClothes.BESTAAND_WACHTERSMANTEL, "bestaand");
        // what each player sees for themselves
        Schijn.registreer(Vuren::vul);
        Schijn.registreer(Tuintje::vul);
        Schijn.registreer(Kooien::vul);
        NeoForge.EVENT_BUS.addListener(Schijn::opTick);
        NeoForge.EVENT_BUS.addListener(Schijn::opWeg);
        NeoForge.EVENT_BUS.addListener(Schijn::opDimensie);
        NeoForge.EVENT_BUS.addListener(Schijn::opStop);
        NeoForge.EVENT_BUS.addListener(Schijn::opChunk);
        NeoForge.EVENT_BUS.addListener(Schijn::opBlokUpdate);
        NeoForge.EVENT_BUS.addListener(BestaandEvents::rechtsklik);
        NeoForge.EVENT_BUS.addListener(BestaandEvents::linksklik);
        NeoForge.EVENT_BUS.addListener(BestaandFeature::commando);
    }

    /**
     * What belongs at the two buildings, also at the copies that were generated long ago (Bezetting looks while a player is
     * near). Safe to call again (the game tests take registrations away and put them back).
     */
    static void bezetting() {
        // the Spiesburcht: the Wachter-guh in his hokje next to the statue, a fire bowl on every bridge
        Bezetting.npc(WACHTER_ID, SPIESBURCHT, null, Plekken.WACHTER, GuhNpcEntity.Kind.WACHTERGUH, null, 180f);
        Bezetting.blokken(WACHTHOKJE_ID, SPIESBURCHT, null, List.of(Plekken.WACHTHOKJE), Guhs.id("bestaand_wachthokje"));
        for (int k = 0; k < Plekken.BRUGVUREN.size(); k++) {
            Bezetting.blokken(BRUGVUUR_ID + k, SPIESBURCHT, null, Plekken.BRUGVUREN.get(k), Guhs.id("bestaand_brugvuur_" + k));
        }
        // the Mika-grillpaleis: the Knuffelmaker-guh in his naaihoek on the first floor
        Bezetting.npc(KNUFFELMAKER_ID, GRILLPALEIS, null, Plekken.KNUFFELMAKER, GuhNpcEntity.Kind.KNUFFELMAKERGUH, null, 180f);
        Bezetting.blokken(NAAIHOEK_ID, GRILLPALEIS, null, List.of(Plekken.NAAIHOEK), Guhs.id("bestaand_naaihoek"));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ZIELIG_LANTAARNTJE_ITEM.get()));
        output.accept(new ItemStack(KNUFFELGUH_ITEM.get()));
        output.accept(new ItemStack(KNUFFELMIKA_ITEM.get()));
        output.accept(new ItemStack(KNUFFELROOKGUH_ITEM.get()));
        output.accept(new ItemStack(RECEPT_LANTAARN.get()));
        output.accept(new ItemStack(RECEPT_KNUFFEL.get()));
    }

    /**
     * /guhs bestaand (operators; for AutoCheck scripts and dev checks): "stand" says where you are in both questlines, "wis"
     * forgets them for you (the fires go out and the plush guhs are back in their cages, for you alone), "vuur &lt;nr&gt;" /
     * "kooi &lt;nr&gt;" light a bridge fire / free a plush for you without a click, and "ga &lt;plek&gt;" puts you in front of
     * a spot of the copy you are at (hokje, brug0..3, tuin, kooi0..2, naaihoek), looking at it.
     */
    private static void commando(RegisterCommandsEvent event) {
        var ga = Commands.literal("ga");
        for (String plek : GA.keySet()) {
            ga.then(Commands.literal(plek).executes(c -> ga(c, plek)));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bestaand").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(BestaandFeature::stand))
                .then(Commands.literal("wis").executes(BestaandFeature::wis))
                .then(Commands.literal("vuur").then(Commands.argument("nr", IntegerArgumentType.integer(0, Vuren.AANTAL - 1))
                        .executes(c -> vlag(c, WACHTER, "vuur_"))))
                .then(Commands.literal("kooi").then(Commands.argument("nr", IntegerArgumentType.integer(0, Kooien.AANTAL - 1))
                        .executes(c -> vlag(c, KNUFFELMAKER, "kooi_"))))
                .then(ga)));
    }

    /** A spot of "/guhs bestaand ga": in which building, where you stand and what you look at (template coordinates). */
    private record Ga(String structuur, BlockPos sta, BlockPos kijk) {
    }

    private static final Map<String, Ga> GA = gaPlekken();

    private static Map<String, Ga> gaPlekken() {
        Map<String, Ga> uit = new LinkedHashMap<>();
        uit.put("hokje", new Ga(SPIESBURCHT, Plekken.WACHTER.offset(0, 0, -5), Plekken.WACHTER.above()));
        for (int k = 0; k < Plekken.BRUGVUREN.size(); k++) {
            // (five steps towards the keep along the bridge, in the middle of the walkway)
            BlockPos vuur = Plekken.BRUGVUREN.get(k).get(0), m = Plekken.BURCHT_MIDDEN;
            boolean langsX = Math.abs(vuur.getX() - m.getX()) > Math.abs(vuur.getZ() - m.getZ());
            BlockPos sta = langsX ? new BlockPos(vuur.getX() - 5 * Integer.signum(vuur.getX() - m.getX()), vuur.getY(), m.getZ())
                    : new BlockPos(m.getX(), vuur.getY(), vuur.getZ() - 5 * Integer.signum(vuur.getZ() - m.getZ()));
            uit.put("brug" + k, new Ga(SPIESBURCHT, sta, vuur.above(Vuren.KORF_HOOGTE)));
        }
        uit.put("tuin", new Ga(SPIESBURCHT, Plekken.BURCHT_MIDDEN.atY(Plekken.MIKAKRUID.get(0).getY()), Plekken.MIKAKRUID.get(0)));
        BlockPos paleis = new BlockPos(36, Plekken.NAAIHOEK.getY(), 36);
        for (int i = 0; i < Plekken.KOOIEN.size(); i++) {
            BlockPos kooi = Plekken.KOOIEN.get(i);
            uit.put("kooi" + i, new Ga(GRILLPALEIS, kooi.offset(4 * Integer.signum(paleis.getX() - kooi.getX()), 0, 4 * Integer.signum(paleis.getZ() - kooi.getZ())),
                    kooi.above()));
        }
        uit.put("naaihoek", new Ga(GRILLPALEIS, Plekken.NAAIHOEK.offset(2, 0, -3), Plekken.KNUFFELMAKER.above()));
        return uit;
    }

    private static int ga(CommandContext<CommandSourceStack> c, String plek) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Ga ga = GA.get(plek);
        ServerLevel level = p.level();
        StructureStart start = Bezetting.start(level, ga.structuur, p.blockPosition());
        BlockPos sta = start == null ? null : Kopieen.wereld(start, null, ga.sta), kijk = start == null ? null : Kopieen.wereld(start, null, ga.kijk);
        if (sta == null || kijk == null) {
            c.getSource().sendFailure(Component.literal("No guhs:" + ga.structuur + " within " + Bezetting.BEREIK + " blocks: go there first (/locate structure guhs:"
                    + ga.structuur + ")"));
            return 0;
        }
        Vec3 van = Vec3.atBottomCenterOf(sta), naar = Vec3.atCenterOf(kijk).subtract(van.add(0, p.getEyeHeight(), 0));
        float yaw = (float) (Mth.atan2(naar.z, naar.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(naar.y, naar.horizontalDistance()) * Mth.RAD_TO_DEG);
        p.teleportTo(level, van.x, van.y, van.z, Set.of(), yaw, pitch, false);
        Schijn.straks(p);
        c.getSource().sendSuccess(() -> Component.literal("bestaand: " + plek + " at " + sta.toShortString() + " (copy " + start.getChunkPos() + ", turned "
                + Kopieen.draai(start, null) + ")"), false);
        return 1;
    }

    /** Sets the flag {@code naam<nr>} of this line for the caller: a fire that burns, a plush that is free (no step, no reward). */
    private static int vlag(CommandContext<CommandSourceStack> c, Verhaallijn lijn, String naam) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        lijn.vlag(p, naam + IntegerArgumentType.getInteger(c, "nr"), true);
        Schijn.ververs(p);
        return stand(c);
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.bestaand.commando.stand", WACHTER.stap(p), Vuren.aantal(p), Tuintje.aantal(p),
                KNUFFELMAKER.stap(p), Kooien.aantal(p)), false);
        return 1 + WACHTER.stap(p) * 10 + KNUFFELMAKER.stap(p);
    }

    private static int wis(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        WACHTER.wis(p);
        KNUFFELMAKER.wis(p);
        Kooien.vergeet(p);
        Schijn.ververs(p);
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.bestaand.commando.gewist", p.getDisplayName()), false);
        return 1;
    }

    private BestaandFeature() {
    }
}
