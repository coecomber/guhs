package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.guhpixel.Bouwer;
import nl.juiced.guhs.feature.guhpixel.Films;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.GidsSectie;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Winkel;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;

/**
 * Guhpixel slice "bioscoop": the Guhbioscoop for at home (DESIGN_PX section 4).
 * <ul>
 *   <li>{@link ProjectorBlock} + {@link ProjectorBlockEntity}: pick one of YOUR films, it plays on the screen in front;</li>
 *   <li>{@link DoekBlock}: the Bioscoopdoek, a screen of up to 7 x 4 blocks ({@link Doek});</li>
 *   <li>{@link StoeltjeBlock}: the Bioscoopstoeltje, for players and for guhs ({@link BioscoopGoal});</li>
 *   <li>{@link PopcornmachineBlock} and the bakje popcorn;</li>
 *   <li>the films: data, {@code assets/guhs/guhbioscoop/films/<id>.json} (drawn by client.ProjectorRenderer) with the
 *   server's index {@link FilmInfo}; nine of them, unlocked through {@link Films} (the six joke games by
 *   Grappen.voltooi, the three stories by the progress behind their titles, registered here);</li>
 *   <li>everything is bought in the Guhpixel shop (group guhbioscoop); a Guhdex section; {@code /guhs px bioscoop ...}.</li>
 * </ul>
 * Resources: tools/features/guhpixel_bioscoop.py (+ _tex, _films, _tekst); the film format is described in
 * guhpixel_bioscoop_films.py and in client.FilmData.
 */
public final class BioscoopSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredBlock<ProjectorBlock> PROJECTOR = BLOCKS.registerBlock("guhbioscoop_projector", ProjectorBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f).noOcclusion().sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(ProjectorBlock.LIT) ? 9 : 0).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<DoekBlock> DOEK = BLOCKS.registerBlock("guhbioscoop_doek", DoekBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).noOcclusion().sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<StoeltjeBlock> STOELTJE = BLOCKS.registerBlock("guhbioscoop_stoeltje", StoeltjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8f).noOcclusion().sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<PopcornmachineBlock> POPCORNMACHINE = BLOCKS.registerBlock("guhbioscoop_popcornmachine", PopcornmachineBlock::new,
            () -> DecoBlock.props().strength(0.8f).sound(SoundType.GLASS).lightLevel(s -> 8).pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<LoreBlockItem> PROJECTOR_ITEM = blokItem("guhbioscoop_projector", PROJECTOR);
    public static final DeferredItem<LoreBlockItem> DOEK_ITEM = blokItem("guhbioscoop_doek", DOEK);
    public static final DeferredItem<LoreBlockItem> STOELTJE_ITEM = blokItem("guhbioscoop_stoeltje", STOELTJE);
    public static final DeferredItem<LoreBlockItem> POPCORNMACHINE_ITEM = blokItem("guhbioscoop_popcornmachine", POPCORNMACHINE);
    /** A bakje popcorn: from the machine; a little snack. */
    public static final DeferredItem<Item> POPCORN = ITEMS.registerItem("guhbioscoop_popcorn", GuhpixelFeature.LoreItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProjectorBlockEntity>> PROJECTOR_BE = BLOCK_ENTITIES.register(
            "guhbioscoop_projector", () -> new BlockEntityType<>(ProjectorBlockEntity::new, PROJECTOR.get()));

    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTOR_AAN = geluid("guhbioscoop.projector_aan");
    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTOR_UIT = geluid("guhbioscoop.projector_uit");
    public static final DeferredHolder<SoundEvent, SoundEvent> POP = geluid("guhbioscoop.pop");
    /**
     * The sounds a film may use in its "geluiden" (played by the client at the screen). A film can name any sound event of
     * the game; these are the cinema's own (sounds.json: tools/features/guhpixel_bioscoop.py FILM_GELUIDEN, the same list).
     */
    public static final List<String> FILM_GELUIDEN = List.of("titel", "guh", "njeg", "vahoeg", "snurk", "gaap", "fanfare", "boem", "pling", "plof",
            "spanning", "piep", "mika", "wind", "klik", "lach", "oeh", "smak");
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> FILM_GELUID = new LinkedHashMap<>();

    static {
        for (String g : FILM_GELUIDEN) {
            FILM_GELUID.put(g, geluid("guhbioscoop.film." + g));
        }
    }

    /** What the Guhbioscoop-set holds: one projector, a whole 7 x 4 screen and four seats. */
    public static final int SET_DOEK = Doek.MAX_B * Doek.MAX_H, SET_STOELTJES = 4, DOEK_PAKJE = 4;
    public static final int PRIJS_SET = 250, PRIJS_SET_NOG = 125, PRIJS_STOELTJE = 15, PRIJS_POPCORNMACHINE = 60, PRIJS_DOEK = 8;

    private static DeferredItem<LoreBlockItem> blokItem(String naam, Supplier<? extends net.minecraft.world.level.block.Block> blok) {
        return ITEMS.registerItem(naam, p -> new LoreBlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        // the three story films follow the progress behind the titles Held van Nomguh / Vriend van Guhtwo / Ohana-guh
        Films.voorwaarde("balto", BaltoVerhaal::isHeld);
        Films.voorwaarde("mewtwo", p -> MewtwoVoortgang.stap(p) >= MewtwoVoortgang.KLAAR);
        Films.voorwaarde("stitch626", p -> Ohana.stap(p) >= Ohana.KLAAR);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(2, new BioscoopGoal(guh)));
        GuhHooks.tick(BioscoopGoal::bewaak);
        winkel();
        GidsBlad.registreer(new Gids());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BioscoopCommando.register(e));
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> vergeet());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> vergeet());
        PxZelftest.registreer("bioscoop", (server, level, meld) -> {
            for (String id : Films.IDS) {
                FilmInfo info = FilmInfo.van(server, id);
                meld.check(info != null && info.duur() >= 600 && info.duur() <= 1200 && info.cues().size() >= 3,
                        "film " + id + (info == null ? " is missing from data/guhs/guhbioscoop/films.json" : ": " + info.duur() + " ticks, " + info.cues().size() + " cues"));
            }
            for (String id : List.of("guhbioscoop_set", "guhbioscoop_stoeltje", "guhbioscoop_popcornmachine", "guhbioscoop_doek")) {
                meld.check(Winkel.van(id) != null, "shop offer " + id);
            }
        });
    }

    private static void vergeet() {
        Voorstelling.wisAlles();
        FilmInfo.vergeet();
        BioscoopGoal.BEZOEKERS.clear();
    }

    private static void winkel() {
        Winkel.aanbod("guhbioscoop_set").groep("guhbioscoop").icoon(() -> new ItemStack(PROJECTOR_ITEM.get()))
                .naam(Component.translatable("gui.guhs.guhbioscoop.winkel.set")).uitleg(Component.translatable("gui.guhs.guhbioscoop.winkel.set.uitleg"))
                .prijs((speler, al) -> al == 0 ? PRIJS_SET : PRIJS_SET_NOG)
                .lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(PROJECTOR_ITEM.get()))
                        & Winkel.geef(speler, new ItemStack(DOEK_ITEM.get(), SET_DOEK))
                        & Winkel.geef(speler, new ItemStack(STOELTJE_ITEM.get(), SET_STOELTJES)))
                .registreer();
        Winkel.aanbod("guhbioscoop_stoeltje").groep("guhbioscoop").icoon(() -> new ItemStack(STOELTJE_ITEM.get()))
                .naam(Component.translatable("block.guhs.guhbioscoop_stoeltje")).uitleg(Component.translatable("gui.guhs.guhbioscoop.winkel.stoeltje.uitleg"))
                .prijs(PRIJS_STOELTJE).lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(STOELTJE_ITEM.get()))).registreer();
        Winkel.aanbod("guhbioscoop_popcornmachine").groep("guhbioscoop").icoon(() -> new ItemStack(POPCORNMACHINE_ITEM.get()))
                .naam(Component.translatable("block.guhs.guhbioscoop_popcornmachine"))
                .uitleg(Component.translatable("gui.guhs.guhbioscoop.winkel.popcornmachine.uitleg"))
                .prijs(PRIJS_POPCORNMACHINE).lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(POPCORNMACHINE_ITEM.get()))).registreer();
        Winkel.aanbod("guhbioscoop_doek").groep("guhbioscoop").icoon(() -> new ItemStack(DOEK_ITEM.get(), DOEK_PAKJE))
                .naam(Component.translatable("gui.guhs.guhbioscoop.winkel.doek")).uitleg(Component.translatable("gui.guhs.guhbioscoop.winkel.doek.uitleg"))
                .prijs(PRIJS_DOEK).lever((speler, aanbod) -> Winkel.geef(speler, new ItemStack(DOEK_ITEM.get(), DOEK_PAKJE))).registreer();
    }

    /** The Guhdex section: films x/9 (locked ones with a hint), how many you watched. */
    private static final class Gids implements GidsSectie {
        @Override
        public String id() {
            return "bioscoop";
        }

        @Override
        public int volgorde() {
            return 70;
        }

        @Override
        public boolean zichtbaar(ServerPlayer p) {
            return true;
        }

        @Override
        public void vul(ServerPlayer p, Bouwer b) {
            b.kop(Component.translatable("gui.guhs.guhbioscoop.gids.kop"));
            b.regel(Component.translatable("gui.guhs.guhbioscoop.gids.uitleg"));
            b.voortgang(Component.translatable("gui.guhs.guhbioscoop.gids.films"), Films.aantal(p), Films.IDS.size());
            b.voortgang(Component.translatable("gui.guhs.guhbioscoop.gids.gekeken"), Bioscoop.aantalGezien(p), Films.IDS.size());
            for (String id : Films.IDS) {
                boolean heeft = Films.heeft(p, id);
                b.plaatje(icoon(id), Bioscoop.naam(id), heeft ? Bioscoop.uitleg(id) : Bioscoop.slot(id), heeft);
            }
        }
    }

    /** The little picture of a film in the Guhdex. */
    static ItemStack icoon(String film) {
        return new ItemStack(switch (film) {
            case "skyblok" -> Items.GRASS_BLOCK;
            case "bedwars" -> Items.RED_BED;
            case "vadsnite" -> Items.ELYTRA;
            case "guhmon" -> Items.GOLD_NUGGET;
            case "bzg" -> Items.WHEAT;
            case "among" -> Items.IRON_TRAPDOOR;
            case "balto" -> Items.SNOWBALL;
            case "mewtwo" -> Items.AMETHYST_SHARD;
            case "stitch626" -> Items.BLUE_DYE;
            default -> Items.PAINTING;
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        BioscoopPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PROJECTOR_ITEM.get()));
        output.accept(new ItemStack(DOEK_ITEM.get()));
        output.accept(new ItemStack(STOELTJE_ITEM.get()));
        output.accept(new ItemStack(POPCORNMACHINE_ITEM.get()));
        output.accept(new ItemStack(POPCORN.get()));
    }

    private BioscoopSlice() {
    }
}
