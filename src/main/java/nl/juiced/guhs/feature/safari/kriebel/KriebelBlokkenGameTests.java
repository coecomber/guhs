package nl.juiced.guhs.feature.safari.kriebel;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.gametest.GuhTest;

/** Round 4 L3: the info board reads its species (saved and loaded) and finds its text; every new block has a name. */
public class KriebelBlokkenGameTests {
    @GuhTest(template = "empty")
    public static void infobordLeestZijnSoort(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, KriebelBlokken.INFOBORD.get().defaultBlockState());
        InfobordBlockEntity be = (InfobordBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p));
        helper.assertTrue(be != null, "infobord has a block entity");
        helper.assertTrue(be.tekstKey().equals("gui.guhs.safari.infobord.leeg"), "no species: the waiting text");
        be.zetSoort("leeuw");
        helper.assertTrue(be.tekstKey().equals("gui.guhs.safari.infobord.leeuw"), "leeuw: its own text, got " + be.tekstKey());
        CompoundTag tag = be.saveWithoutMetadata(helper.getLevel().registryAccess());
        InfobordBlockEntity kopie = new InfobordBlockEntity(be.getBlockPos(), be.getBlockState());
        kopie.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue("leeuw".equals(kopie.soort()), "species saved and loaded");
        be.zetSoort("onbekend_kriebeltje");
        helper.assertTrue(be.tekstKey().equals("gui.guhs.safari.infobord.algemeen"), "unknown species: the general text");
        for (DeferredBlock<?> b : KriebelBlokken.ALLE) {
            helper.assertTrue(Language.getInstance().has(b.get().getDescriptionId()), "name of " + b.getId());
        }
        helper.succeed();
    }

    /** R5: every park species (the 87 of soorten.json, Kriebelhuis included) has its own info board text, in Dutch and English. */
    @GuhTest(template = "empty")
    public static void infobordTekstVoorElkeSoort(GameTestHelper helper) {
        java.util.Set<String> nederlands = sleutels("nl_nl"), engels = sleutels("en_us");
        java.util.List<String> mist = new java.util.ArrayList<>();
        for (String soort : nl.juiced.guhs.feature.safari.ParkData.get().soorten.keySet()) {
            String k = "gui.guhs.safari.infobord." + soort;
            if (!nederlands.contains(k) || !engels.contains(k)) {
                mist.add(soort);
            }
        }
        helper.assertTrue(nl.juiced.guhs.feature.safari.ParkData.get().soorten.size() >= 87, "all species loaded");
        helper.assertTrue(mist.isEmpty(), "info board text missing for " + mist);
        helper.succeed();
    }

    private static java.util.Set<String> sleutels(String taal) {
        try (java.io.InputStream in = KriebelBlokkenGameTests.class.getResourceAsStream("/assets/guhs/lang/" + taal + ".json")) {
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet();
        } catch (java.io.IOException | RuntimeException e) {
            return java.util.Set.of();
        }
    }
}
