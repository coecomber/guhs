package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 phase B, slice h1_lucht (CONTRACT_E2_B 8): the core of chapter 1. The headless test server has no Guheinde, so the
 * places are not asked here; what is tested is the state machine around them.
 * <ul>
 *   <li>{@code einde2H1LuchtKaasbol}: of two players only the one who beat Opper-Mika may open a kaasbol; opening it puts
 *       that one (and only that one) at step 1, unveils the air temple for them and gives them Appa;</li>
 *   <li>{@code einde2H1LuchtEinde}: the end of the chapter (flag geleerd, the element counts as learnt, the line done, the
 *       rewards once), the recipe of the Oog van Vadsig with grillspiespoeder (and not without), the scenes and powers.</li>
 * </ul>
 */
public class E2H1LuchtGameTests {
    private static final String BATCH = "einde2_h1_lucht";
    private static final String EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H1LuchtKaasbol(GameTestHelper helper) {
        ServerPlayer held = speler(helper), nieuw = speler(helper);
        String tempel = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
        try {
            E2VerhaalDev.wis(held, E2Verhaal.LUCHT);
            E2VerhaalDev.wis(nieuw, E2Verhaal.LUCHT);
            GuhQuests.saved(held).putInt(GuheindeGevecht.WINS, 1);
            helper.assertTrue(E2Verhaal.lijn(E2Verhaal.LUCHT) == E2H1Lucht.LIJN && E2H1Lucht.LIJN.stappen() == E2H1Lucht.STAPPEN,
                    "the line guhvatar_lucht is registered with its ten steps");
            helper.assertTrue(Kaasbol.mag(held) && !Kaasbol.mag(nieuw), "only who beat Opper-Mika may open a kaasbol");
            helper.assertTrue(!E2Verhaal.open(held, tempel) && !E2Verhaal.open(nieuw, tempel), "the temple is veiled for both");
            // opening: both scenes seen -> step 1, Appa, the flight (the test server has no Guheinde: the flight cannot start)
            Kaasbol.geopend(held);
            helper.assertTrue(E2H1Lucht.LIJN.stap(held) == E2H1Lucht.VLUCHT && E2H1Lucht.LIJN.stap(nieuw) == E2H1Lucht.KAASBOL,
                    "the one who opened it is at step 1, the other one still at 0");
            helper.assertTrue(E2Verhaal.open(held, tempel) && !E2Verhaal.open(nieuw, tempel), "the veil opens for the one who opened it only");
            helper.assertTrue(Appa.heeft(held) && !Appa.heeft(nieuw), "Appa-guh is theirs");
            helper.assertTrue(!Kaasbol.mag(held), "no second kaasbol");
            helper.assertTrue(E2H1Lucht.LIJN.doel(held) != null && E2H1Lucht.LIJN.doel(held).structuur().equals(tempel), "the compass points at the temple");
            // the stable: the apples needed are shown in the Guhdex
            E2VerhaalDev.zetTot(held, E2Verhaal.LUCHT, E2H1Lucht.STAL);
            helper.assertTrue(E2H1Lucht.LIJN.stand(held).nodig().size() == 1, "step 7 asks for three apples");
            helper.succeed();
        } finally {
            AppaGuhEntity appa = Appa.geladen(held);
            if (appa != null) {
                appa.discard();
            }
            Kaasbol.vergeet(held.getUUID());
            held.discard();
            nieuw.discard();
        }
    }

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2H1LuchtEinde(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, E2H1Lucht.ZUKA);
            E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT, true);
            helper.assertTrue(E2H1Lucht.LIJN.sleutel(p).equals("9_vlucht") && !E2Verhaal.elementGeleerd(p, Einde2Ids.Element.LUCHT),
                    "on the run: the variant text, air not learnt yet");
            Tempel.afronden(p);
            helper.assertTrue(E2H1Lucht.LIJN.klaar(p) && E2Verhaal.klaar(p, E2Verhaal.LUCHT) && E2Verhaal.vlag(p, E2Verhaal.LUCHT, E2Verhaal.V_GELEERD)
                    && E2Verhaal.elementGeleerd(p, Einde2Ids.Element.LUCHT) && !E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT),
                    "the chapter is done, air is learnt, the escape is over");
            helper.assertTrue(E2Verhaal.open(p, Einde2Ids.NOORDELIJKE_WATERSTAM), "the Noordelijke Waterstam opens");
            int pak = GuhQuests.count(p, ModItems.clothingItem(GuhClothes.GUHVATAR_LUCHTNOMADE));
            int scooters = GuhQuests.count(p, E2H1LuchtFeature.LUCHTSCOOTER_ITEM.get());
            helper.assertTrue(pak == 1 && scooters == 2, "the rewards: the robe and two luchtscooters (" + pak + ", " + scooters + ")");
            Tempel.afronden(p);
            helper.assertTrue(GuhQuests.count(p, E2H1LuchtFeature.LUCHTSCOOTER_ITEM.get()) == 2, "the rewards come once");
            // the Oog van Vadsig: guhkristal + kaasknabbel + Mika-traan + grillspiespoeder, and the old three no more
            RecipeHolder<?> oog = helper.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, Guhs.id("oog_van_vadsig"))).orElse(null);
            helper.assertTrue(oog != null && oog.value() instanceof CraftingRecipe, "the recipe is there");
            CraftingRecipe r = (CraftingRecipe) oog.value();
            List<ItemStack> vier = List.of(item("guh_kristal"), item("kaas_knabbels"), item("mika_traan"), item("grillspiespoeder"));
            List<ItemStack> drie = List.of(item("guh_kristal"), item("kaas_knabbels"), item("mika_traan"), ItemStack.EMPTY);
            helper.assertTrue(r.matches(CraftingInput.of(2, 2, vier), helper.getLevel()), "with grillspiespoeder: an Oog van Vadsig");
            helper.assertTrue(!r.matches(CraftingInput.of(2, 2, drie), helper.getLevel()), "without it: nothing");
            helper.assertTrue(r.assemble(CraftingInput.of(2, 2, vier)).is(GuheindeFeature.OOG_VAN_VADSIG.get()), "the same Eye as before");
            // the scenes and the two powers are there (both sides know the scenes by id)
            for (Cutscene s : H1Scenes.ALLE) {
                helper.assertTrue(Cutscene.van(s.id()) == s && s.lijn().equals(E2Verhaal.LUCHT), "scene " + s.id());
            }
            helper.assertTrue(Krachten.van(H1Krachten.LUCHTBAL) != null && Krachten.van(H1Krachten.LUCHTSPRONG) != null
                    && !H1Krachten.kan(p, p.blockPosition()), "the powers exist, and only work in the temple");
            helper.succeed();
        } finally {
            p.discard();
        }
    }

    private static ItemStack item(String id) {
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Guhs.id(id)));
    }
}
