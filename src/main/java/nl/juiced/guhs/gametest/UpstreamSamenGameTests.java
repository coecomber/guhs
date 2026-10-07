package nl.juiced.guhs.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.registry.ModEntities;

/**
 * bbq2, written at the merge of upstream (1.2.9 - 1.3.2: the Guhpixel update, the Superkompas ticks, the rooms inside the
 * Guhhuisjes) into bbq2: the seams between the two updates that neither side could test (batch "upstreamsamen").
 * <ul>
 *     <li>the synced guh flags: every bit has one owner (guhpixel took 24..28, bbq2 defines none of its own);</li>
 *     <li>the NPC kinds: bbq2's story kinds stand before guhpixel's marker blocks (guhpixel's tests and the Guhdex skip
 *     "everything from LOBBY_WELKOMSTGUH on"), and none of them has a character page;</li>
 *     <li>the Superkompas: "Mijn verhaal" is no place (1.3.1 ticks off every PLACE you stood in, by structure id), every
 *     place a bbq2 slice added is a real structure id and can be ticked off, the lists of 1.3.1 are what they were;</li>
 *     <li>the room inside a Guhhuisje (1.3.2) writes what a resident is doing as {@code gui.guhs.klus.<id>} on the note
 *     on its bed and on the prikbord: every chore, also the two of bbq2 (machines, plantage), has that text;</li>
 *     <li>the 1.2.9 rule (a click with an item on your OWN tamed guh only reaches the server when
 *     {@link GuhEntity#heeftEigenKlik} knows the item): the clothes of bbq2 are known, like all clothes.</li>
 * </ul>
 * The drooling wild guhs of the Knabbelring next to the stand-in guhs of 1.3.x are tested where the ring is
 * (RingGameTests.ringCastEnKwijlen).
 */
public class UpstreamSamenGameTests {
    private static final String BATCH = "upstreamsamen";

    /** Every flag bit of the synced int "KnusVlaggen" is one bit and has one owner. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upstreamsamenGuhVlaggenBotsenNiet(GameTestHelper helper) {
        Map<Integer, String> bits = new HashMap<>();
        bits.put(GuhHooks.GLANZEND, "GuhHooks.GLANZEND");
        vlag(helper, bits, GuhHooks.PYJAMA, "GuhHooks.PYJAMA");
        vlag(helper, bits, GuhHooks.IJSHOEDJE, "GuhHooks.IJSHOEDJE");
        vlag(helper, bits, GuhHooks.BLOSJES, "GuhHooks.BLOSJES");
        vlag(helper, bits, SleepInNestGoal.OOGJES_DICHT, "SleepInNestGoal.OOGJES_DICHT");
        for (Class<?> c : List.of(BandVlaggen.class, VerhaalVlaggen.class, PxVlaggen.class)) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()) && f.getType() == int.class) {
                    try {
                        vlag(helper, bits, f.getInt(null), c.getSimpleName() + "." + f.getName());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        helper.assertTrue(bits.size() >= 21, "the 21 known flags (4 knus, 1 nest, 6 band, 5 verhaal, 5 guhpixel): " + bits.size());
        helper.succeed();
    }

    private static void vlag(GameTestHelper helper, Map<Integer, String> bits, int bit, String naam) {
        helper.assertTrue(Integer.bitCount(bit) == 1, naam + " is one bit");
        String al = bits.put(bit, naam);
        helper.assertTrue(al == null, naam + " and " + al + " are the same bit");
    }

    /** bbq2's NPC kinds stand between the last story kind of 3.0 and the first kind of guhpixel, and have no Guhdex page. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upstreamsamenKindsVoorGuhpixel(GameTestHelper helper) {
        int tiki = GuhNpcEntity.Kind.TIKIGUH.ordinal(), lobby = GuhNpcEntity.Kind.LOBBY_WELKOMSTGUH.ordinal();
        for (GuhNpcEntity.Kind k : List.of(GuhNpcEntity.Kind.GUHDALF, GuhNpcEntity.Kind.SMIKAGOL, GuhNpcEntity.Kind.PADGUH, GuhNpcEntity.Kind.PERZIKGUH,
                GuhNpcEntity.Kind.MARKTMEESTER_MIKA, GuhNpcEntity.Kind.TORENWACHTERGUH, GuhNpcEntity.Kind.PEPERTELERGUH, GuhNpcEntity.Kind.ARAGUH)) {
            helper.assertTrue(k.ordinal() > tiki && k.ordinal() < lobby, k + " stands after TIKIGUH and before the guhpixel blocks");
        }
        for (GuhNpcEntity.Kind k : GuhNpcEntity.Kind.values()) {
            if (k.ordinal() > tiki) {
                helper.assertTrue(GuhVariant.ofCharacter(k) == null, k + " has no character page in the Guhdex");
            }
        }
        // (the kind is saved by its id and synced by ordinal: both sides run the same build, so the order is free to choose)
        helper.assertTrue(GuhNpcEntity.Kind.REISBUREAU_AGENT.ordinal() > lobby, "the guhpixel kinds are still behind theirs");
        helper.succeed();
    }

    /** The Superkompas of both updates: 1.3.1's lists and ticks, bbq2's places and "Mijn verhaal". */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upstreamsamenSuperkompas(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        SuperkompasItem.Category knus = null, verhalen = null, barbecue = null, minigames = null;
        for (SuperkompasItem.Category c : SuperkompasItem.CATEGORIES) {
            switch (c.id()) {
                case "knus" -> knus = c;
                case "verhalen" -> verhalen = c;
                case "barbecue" -> barbecue = c;
                case "minigames" -> minigames = c;
                default -> {
                }
            }
            for (String id : c.structures()) {
                helper.assertTrue(!id.equals(SuperkompasItem.DOEL) && Identifier.tryParse("guhs:" + id) != null && id.equals(id.toLowerCase(java.util.Locale.ROOT)),
                        "a place of the tab " + c.id() + " is a structure id: " + id);
                helper.assertTrue(Language.getInstance().has("structure.guhs." + id), "the name of " + id);
            }
        }
        helper.assertTrue(knus != null && verhalen != null && barbecue != null && minigames != null, "the four tabs");
        helper.assertTrue(knus.structures().contains("reisbureau") && minigames.structures().containsAll(List.of("internetcafe", "reisbureau")),
                "guhpixel: the Reisbureau and the Guh-internetcafe");
        helper.assertTrue(!verhalen.structures().contains("guhwaii_capsule") && verhalen.structures().contains("guhwaii_ohana"),
                "1.3.1: the capsule is no place of its own");
        helper.assertTrue(barbecue.structures().containsAll(List.of("barbecueput", "spiesburcht", "mika_grillpaleis"))
                && barbecue.structures().size() > 3, "bbq2: the buildings of the Barbecuether were added behind the three old ones: " + barbecue.structures());
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().noneMatch(c -> c.structures().contains(SuperkompasItem.DOEL)),
                "\"Mijn verhaal\" is a choice of the compass, not a place that can be ticked off");
        // a place that a bbq2 slice added is ticked off like every other place (1.3.1: "s:" + structure id)
        String nieuw = barbecue.structures().get(barbecue.structures().size() - 1);
        helper.assertTrue(!SpelGroepen.structuurBezocht(p, nieuw) && SpelGroepen.bezoekStructuur(p, nieuw) && SpelGroepen.structuurBezocht(p, nieuw)
                && !SpelGroepen.bezoekStructuur(p, nieuw), nieuw + " is remembered once");
        helper.assertTrue(SpelGroepen.kijk(p).isEmpty(), "nothing new on an empty test floor (the look walks every place of every tab)");
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** What the room inside a Guhhuisje writes about a resident at work: a text for every chore, bbq2's two included. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upstreamsamenKlusTekstenVoorDeKamer(GameTestHelper helper) {
        Language lang = Language.getInstance();
        List<String> ids = Klusjes.alle().stream().map(Klus::id).toList();
        helper.assertTrue(ids.containsAll(List.of("machines", "plantage")), "bbq2's chores are registered: " + ids);
        int echt = 0;
        for (String id : ids) {
            if (id.endsWith("_test")) {
                continue;   // (a chore that a game test registered for itself)
            }
            echt++;
            helper.assertTrue(lang.has("gui.guhs.klus." + id), "the name of the chore " + id + " (the note on the bed, the prikbord)");
        }
        helper.assertTrue(echt >= 10, "the real chores: " + echt + " of " + ids);
        for (nl.juiced.guhs.feature.huisje.BinnenInrichting.Waar waar : nl.juiced.guhs.feature.huisje.BinnenInrichting.Waar.values()) {
            // (asleep in its bed: no note)
            helper.assertTrue(waar == nl.juiced.guhs.feature.huisje.BinnenInrichting.Waar.SLAAPT
                    || lang.has("gui.guhs.huisje.binnen.briefje." + waar.name().toLowerCase(java.util.Locale.ROOT)), "the note " + waar);
        }
        helper.succeed();
    }

    /** 1.2.9: with a piece of clothing of bbq2 in the hand a click on your own guh dresses it (it is not swallowed as a pet). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void upstreamsamenEigenKlikKleding(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), 1, 2, 1);
        guh.tame(p);
        int n = 0;
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof GuhClothingItem kleding)) {
                continue;
            }
            String id = kleding.getClothes().id();
            if (!(id.startsWith("ring_") || id.startsWith("guhriobeloning_") || id.startsWith("paleizen_") || id.startsWith("bestaand_")
                    || id.startsWith("campingmarkt_") || id.startsWith("torenpeper_"))) {
                continue;
            }
            n++;
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(!guh.heeftEigenKlik(stack, p), id + " is not unlocked yet: a click is a pet");
            nl.juiced.guhs.feature.kleding.KledingUnlocks.ontgrendel(p, kleding.getClothes());
            helper.assertTrue(guh.heeftEigenKlik(stack, p), id + " is unlocked: the click dresses the guh");
        }
        helper.assertTrue(n == 17, "the seventeen clothes of bbq2 are items: " + n);
        guh.discard();
        helper.getLevel().removePlayerImmediately(p, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    private UpstreamSamenGameTests() {
    }
}
