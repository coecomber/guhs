package nl.juiced.guhs.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import nl.juiced.guhs.Guhs;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The Guhmension super compass: right-click it to choose what to look for (from a list of structures, by category);
 * then it points to the nearest one, like the old guh compasses it replaces.
 */
public class SuperkompasItem extends GuhCompassItem {
    /**
     * A subheading inside a category (2.9: the Minigames tab has "Klassiekers", "Knuffeldal" and "De Grote Guhspelen"); id
     * null = no heading. Lang: gui.guhs.superkompas.kopje.&lt;id&gt;.
     */
    public record Kopje(@Nullable String id, List<String> structures) {
        public Kopje {
            structures = List.copyOf(structures);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.superkompas.kopje." + id);
        }
    }

    /**
     * A tab of the menu (2.9: an icon tab, name on hover): its id (lang gui.guhs.superkompas.&lt;id&gt; + .tooltip), its icon
     * (item id; a vanilla stand-in while it doesn't exist) and its structures (structure ids in the guhs namespace), in
     * subheadings.
     */
    public record Category(String id, String icon, net.minecraft.world.item.Item standIn, List<Kopje> kopjes) {
        public Category {
            kopjes = List.copyOf(kopjes);
        }

        /** All structures of this tab, in order (every subheading after the other). */
        public List<String> structures() {
            return kopjes.stream().flatMap(k -> k.structures().stream()).toList();
        }

        /** The tab's icon. */
        public ItemStack icoon() {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(icon));
            return new ItemStack(item == net.minecraft.world.item.Items.AIR ? standIn : item);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.superkompas." + id);
        }
    }

    private static Category cat(String id, String icon, net.minecraft.world.item.Item standIn, String... structures) {
        return new Category(id, icon, standIn, List.of(new Kopje(null, List.of(structures))));
    }

    /**
     * What you can look for. Filler and decoration builds (block guh, fossils, the arch, statues, fountains) aren't in it.
     * (2.9) One Minigames tab with every game: the classics, the Knuffeldal games (their places also stay under Knus) and De
     * Grote Guhspelen.
     */
    private static final List<Category> VASTE_CATEGORIES = List.of(
            cat("avontuur", "guhs:kaashouweel", net.minecraft.world.item.Items.IRON_PICKAXE, "guh_caves", "challenging_guh_caves", "evil_mika_home",
                    "kaasmijn", "moerasheks_hut", "kaasknabbel_nest",   // 2.8.1 Piep
                    "bleke_open_plek", "houthakkershutje"),   // 1.2.8 het Bleekwoud
            cat("quests", "minecraft:writable_book", net.minecraft.world.item.Items.WRITABLE_BOOK, "vadsig_heiligdom", "mika_kamp", "guh_picnic", "sleehut"),
            new Category("minigames", "guhs:discomunt", net.minecraft.world.item.Items.JUKEBOX, List.of(
                    new Kopje("klassiekers", List.of("verstopguh_huis", "guh_kermis", "vadsig_eetfestijn", "mika_mep_hal", "guh_disco", "guhvis_vijver",
                            "guh_beauty_theater", "guh_racebaan", "guh_golfbaan")),
                    new Kopje("knuffeldal", List.of("knuffeldal_stadje", "knuffelbad")),
                    new Kopje("grote_guhspelen", List.of("sjoelhuisje", "guhdoolhof", "knabbelkatapult", "knabbelspelen", "elfguhjestocht", "guh_circuit")),
                    new Kopje("verhalen", List.of("nomguh", "guhwaii_surfstrand")),   // 3.0 (Guhverhalen)
                    new Kopje("guhpixel", List.of("internetcafe", "reisbureau")))),   // guhpixel: the Guh-internetcafe; 1.3.1: + the Reisbureau
            cat("wonderen", "guhs:guh_kristal", net.minecraft.world.item.Items.AMETHYST_SHARD, "guh_kasteel", "zwevende_eilanden",
                    "hamster_house_extra_extra_large", "guhramid", "guhbibliotheek", "onderwater"),
            cat("wonen", "guhs:knuffelsteen_gezicht", net.minecraft.world.item.Items.OAK_DOOR, "guh_village", "hamster_house", "hamster_house_medium",
                    "hamster_house_large", "boomhutdorp"),
            cat("einde", "guhs:enderguh_ei", net.minecraft.world.item.Items.ENDER_EYE, "knabbelkelder", "guheinde_terugpoort"),   // (2.8: the terugpoort, in the Guheinde itself)
            cat("ondergrond", "guhs:gatenkaas", net.minecraft.world.item.Items.LANTERN, "stille_voorraadkelder", "gatenkaas_mijnschacht"),
            cat("barbecue", "guhs:grillspies", net.minecraft.world.item.Items.CAMPFIRE, "barbecueput", "spiesburcht", "mika_grillpaleis"),
            // 2.8 (Knuffeldal): the cozy places
            cat("knus", "guhs:knuffel_normal", net.minecraft.world.item.Items.PINK_BED, "knuffeldal_stadje", "guhboerderij", "guh_sterrenwacht",
                    "ballonfestival", "kampeerplekje", "knuffelbad", "reisbureau"),   // (guhpixel: the Reisbureau)
            // 3.0 (Guhverhalen): the story places
            cat("verhalen", "guhs:baltoguh_beeldje", net.minecraft.world.item.Items.BOOK, "nomguh", "kloon_eiland", "hemelkapelletje", "guhwaii_ohana",
                    "guhwaii_surfstrand", "knuffeldal_stadje"));   // (1.3.1: the capsule is part of the Ohana questline, no place of its own)
    /**
     * The tabs of the menu (bbq2: {@link #voegToe} adds structures to a tab; the fixed list above keeps its shape, the
     * self-check of tools/features/gids.py reads it).
     */
    public static final List<Category> CATEGORIES = new java.util.concurrent.CopyOnWriteArrayList<>(VASTE_CATEGORIES);

    /**
     * bbq2: adds a structure (a guhs structure id without namespace) to a tab of the menu, after what is there (from your
     * Feature.register; common code, both sides): {@code SuperkompasItem.voegToe("barbecue", "pepertuin")}. Lang
     * structure.guhs.&lt;id&gt; (+ .tooltip). A structure behind Guhdalfs sluier only shows once it is open for the player.
     */
    public static void voegToe(String tab, String structuur) {
        synchronized (CATEGORIES) {
            for (int i = 0; i < CATEGORIES.size(); i++) {
                Category c = CATEGORIES.get(i);
                if (!c.id().equals(tab)) {
                    continue;
                }
                if (c.structures().contains(structuur)) {
                    return;
                }
                List<Kopje> kopjes = new java.util.ArrayList<>(c.kopjes());
                int los = -1;
                for (int k = 0; k < kopjes.size(); k++) {
                    if (kopjes.get(k).id() == null) {
                        los = k;
                    }
                }
                if (los < 0) {
                    kopjes.add(new Kopje(null, List.of(structuur)));
                } else {
                    List<String> lijst = new java.util.ArrayList<>(kopjes.get(los).structures());
                    lijst.add(structuur);
                    kopjes.set(los, new Kopje(null, lijst));
                }
                CATEGORIES.set(i, new Category(c.id(), c.icon(), c.standIn(), kopjes));
                return;
            }
        }
        throw new IllegalArgumentException("The Superkompas has no tab " + tab);
    }

    /** The first tab that has this structure (-1: none). */
    public static int categoryOf(@Nullable String structure) {
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (structure != null && CATEGORIES.get(i).structures().contains(structure)) {
                return i;
            }
        }
        return -1;
    }

    public SuperkompasItem(Properties properties) {
        super(null, properties);
    }

    public static boolean allowed(String structure) {
        return CATEGORIES.stream().anyMatch(c -> c.structures().contains(structure));
    }

    @Nullable
    public static String chosen(ItemStack stack) {
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr("Structure", "");
        return id.isEmpty() ? null : id;
    }

    /** Sets what this compass looks for (and makes it look again right away). */
    public static void choose(ItemStack stack, String structure) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("Structure", structure);
            nl.juiced.guhs.feature.bio.kompas.BiomeKompas.wis(tag);   // biomes3: a structure instead of a biome
            tag.remove("SearchedAt");
            tag.remove("Target");
        });
        stack.remove(DataComponents.LODESTONE_TRACKER);
    }

    @Nullable
    @Override
    protected ResourceKey<Structure> target(ItemStack stack) {
        String id = chosen(stack);
        return id == null ? null : ResourceKey.create(Registries.STRUCTURE, Guhs.id(id));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            nl.juiced.guhs.client.GuhsClientHooks.openSuperkompas(hand, chosen(player.getItemInHand(hand)));
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public Component getName(ItemStack stack) {
        Component biome = nl.juiced.guhs.feature.bio.kompas.BiomeKompas.naam(stack);   // biomes3: a biome is chosen
        if (biome != null) {   // biomes3
            return biome;   // biomes3
        }   // biomes3
        String id = chosen(stack);
        return id == null ? super.getName(stack)
                : Component.translatable("item.guhs.guhmensie_superkompas.named", Component.translatable("structure.guhs." + id));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhmensie_superkompas.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.guh_compass.how").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static Identifier key(String structure) {
        return Guhs.id(structure);
    }
}
