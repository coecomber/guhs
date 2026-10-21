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
}
