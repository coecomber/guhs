package nl.juiced.guhs.gametest;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.Owners;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.GuhSavedData;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.GuhTime;

/**
 * 26.1 port (role B): the helpers every slice uses - the gametest registrar itself, 1.0.0 saved data files moving to the
 * 26.1 place, the day clock, owners and entity NBT.
 */
public class PortGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /** A tiny saved data like the real ones (bank, highscores, ...). */
    static final class TestData extends SavedData {
        int value;

        static TestData load(CompoundTag tag) {
            TestData d = new TestData();
            d.value = tag.getIntOr("Value", 0);
            return d;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Value", value);
            return tag;
        }
    }

    static final SavedDataType<TestData> TYPE = GuhSavedData.tagType("port_testdata", TestData::new, TestData::load, TestData::save);

    /** The registrar found the tests (this one included) and the ids are the 1.0.0 names. */
    @GuhTest(template = EMPTY, batch = "port")
    public static void portRegistrarVindtDeTests(GameTestHelper helper) {
        helper.assertTrue(GuhsGameTests.count() > 0 || GametestFilter.active(), "tests found: " + GuhsGameTests.count());
        helper.assertTrue(GuhsGameTests.sanitize("GuhGameTests.tamingWorks").equals("guhgametests.tamingworks"), "1.0.0 style test ids");
        helper.assertTrue(GuhsGameTests.batchId("defaultBatch").toString().equals("guhs:batch/default"), "the default batch");
        helper.assertTrue(GuhsGameTests.structureId("empty").toString().equals("guhs:empty"), "templates live in guhs");
        helper.succeed();
    }

    /** A 1.0.0 world keeps its data: data/guhs_x.dat moves to the 26.1 place (dimensions/.../data/guhs/x.dat) once. */
    @GuhTest(template = EMPTY, batch = "port")
    public static void portOudeSavedDataVerhuist(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().overworld();
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path newFile = DimensionType.getStorageFolder(level.dimension(), root).resolve("data").resolve("guhs").resolve("port_testdata.dat");
        Path oldFile = root.resolve("data").resolve("guhs_port_testdata.dat");
        Files.deleteIfExists(newFile);
        Files.createDirectories(oldFile.getParent());
        CompoundTag file = NbtUtils.addCurrentDataVersion(new CompoundTag());
        CompoundTag data = new CompoundTag();
        data.putInt("Value", 42);
        file.put("data", data);
        NbtIo.writeCompressed(file, oldFile);
        TestData loaded = GuhSavedData.get(level, TYPE, "guhs_port_testdata");
        helper.assertTrue(loaded.value == 42, "the 1.0.0 value came along: " + loaded.value);
        helper.assertTrue(Files.exists(newFile) && !Files.exists(oldFile), "the file moved to " + newFile);
        helper.succeed();
    }

    /** The day clock helpers read and set the overworld clock like 1.21.1's getDayTime/setDayTime. */
    @GuhTest(template = EMPTY, batch = "port")
    public static void portDagklok(GameTestHelper helper) {
        var level = helper.getLevel();
        long was = GuhTime.dayTime(level);
        GuhTime.setDayTime(level, 18000L + 24000L * 3);
        helper.assertTrue(GuhTime.timeOfDay(level) == 18000L, "midnight: " + GuhTime.timeOfDay(level));
        helper.assertTrue(GuhTime.day(level) == 3 && GuhTime.moonPhase(level) == 3, "day 3, moon phase 3");
        helper.assertTrue(Math.abs(GuhTime.celestialAngle(level) - 0.5f) < 0.01f, "celestial angle at midnight is 0.5");
        GuhTime.setDayTime(level, 6000L);
        helper.assertTrue(level.getOverworldClockTime() == 6000L, "noon on the overworld clock");
        GuhTime.setDayTime(level, was);
        helper.succeed();
    }

    /** Owners (getOwnerUUID is gone in 26.1) and a guh saved to a tag and loaded again (EntityNbt). */
    @GuhTest(template = EMPTY, batch = "port")
    public static void portEigenaarEnEntityNbt(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(player);
        guh.setVariant(GuhVariant.GHOST);
        helper.assertTrue(player.getUUID().equals(guh.getOwnerUUID()) && Owners.isOwner(guh, player.getUUID()), "owner");
        CompoundTag tag = Nbt.saveWithoutId(guh);
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Nbt.load(copy, tag);
        helper.assertTrue(copy.getVariant() == GuhVariant.GHOST && player.getUUID().equals(copy.getOwnerUUID()), "saved and loaded: " + tag);
        copy.setOwnerUUID(null);
        helper.assertTrue(copy.getOwnerUUID() == null, "owner cleared");
        helper.succeed();
    }

    private PortGameTests() {
    }
}
