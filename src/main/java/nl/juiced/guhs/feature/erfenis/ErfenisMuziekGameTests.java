package nl.juiced.guhs.feature.erfenis;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.erfenis.vorm.DecoWezens;
import nl.juiced.guhs.feature.slaapzorg.SlaapBlokken;
import nl.juiced.guhs.feature.slaapzorg.SlaapzorgFeature;
import nl.juiced.guhs.feature.slaapzorg.Slapen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Erfenis Muziekdoosje (1.10.10, batch "erfenis_muziek"): a box with the old stamped state (speelt=1, no
 * tick) is not stuck; the dance tune starts, a second start is refused while it plays, a wild guh and a show guh without AI
 * dance (and keep dancing past the first step), nobody is sung asleep, and after the 30 seconds the box is quiet and
 * everyone stops dancing.
 */
public class ErfenisMuziekGameTests {
    private static final String EMPTY = "empty", BATCH = "erfenis_muziek";

    private static GuhEntity guh(GameTestHelper helper, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersistenceRequired();
        return guh;
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = ErfenisMuziek.DUUR + 200)
    public static void muziekdoosjeLaatGuhsDansen(GameTestHelper helper) {
        BlockPos doos = new BlockPos(2, 1, 2);
        SlaapBlokken.Speeldoos blok = SlaapzorgFeature.MUZIEKDOOSJE.get();
        helper.setBlock(doos, blok.defaultBlockState().setValue(SlaapBlokken.Speeldoos.SPEELT, 1));
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(doos);
        helper.assertFalse(blok.speelt(level, abs, helper.getBlockState(doos)), "speelt=1 without a waiting tick (old stamp) is not stuck");
        GuhEntity wild = guh(helper, new BlockPos(1, 1, 3));
        GuhEntity show = guh(helper, new BlockPos(3, 1, 1));
        show.setNoAi(true);
        show.addTag(DecoWezens.TAG);
        helper.runAfterDelay(5, () -> {   // (standing on the ground first)
            ErfenisMuziek.forceer(level, abs);
            helper.assertTrue(ErfenisMuziek.dansHier(level, abs), "a dance box here");
            BlockState s = helper.getBlockState(doos);
            int dansers = blok.speel(level, abs, s);
            helper.assertTrue(dansers == 2, "two guhs dance, got " + dansers);
            helper.assertTrue(helper.getBlockState(doos).getValue(SlaapBlokken.Speeldoos.SPEELT) == ErfenisMuziek.STAPPEN, "it plays");
            helper.assertTrue(blok.speelt(level, abs, helper.getBlockState(doos)), "playing: a second click does nothing");
            helper.assertTrue(wild.emotes.dansOpMuziek() && show.emotes.dansOpMuziek(), "the wild guh and the show guh dance");
            helper.assertFalse(Slapen.isIngezongen(wild), "nobody is sung asleep");
        });
        helper.runAfterDelay(5 + ErfenisMuziek.STAP * 3 + 10, () ->
                helper.assertTrue(wild.emotes.dansOpMuziek() && show.emotes.dansOpMuziek(), "still dancing after three steps"));
        helper.runAfterDelay(5 + ErfenisMuziek.DUUR + 5, () -> {
            BlockState s = helper.getBlockState(doos);
            helper.assertTrue(s.getValue(SlaapBlokken.Speeldoos.SPEELT) == 0 && !blok.speelt(level, abs, s), "the song is over");
            helper.assertFalse(wild.emotes.dansOpMuziek() || show.emotes.dansOpMuziek(), "everyone stopped dancing");
            helper.assertFalse(ErfenisMuziek.dansHier(level, abs), "back to a lullaby box outside De Erfenis");
            helper.succeed();
        });
    }

    /**
     * The Guhdansje music disc: a jukebox disc of song guhs:dansje (the Muziekdoosje's sound event, 31 s), a rare disc for
     * the Disco-dynamo, and the Ballonfestival, guh picnic and Grotdisco chests hold it about as often as intended.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void muziekplaatDansje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.world.item.ItemStack plaat = new net.minecraft.world.item.ItemStack(ErfenisMuziek.MUZIEKPLAAT.get());
        helper.assertTrue(plaat.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE), "a jukebox disc");
        var song = net.minecraft.world.item.JukeboxSong.fromStack(plaat);
        helper.assertTrue(song.isPresent() && song.get().is(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.JUKEBOX_SONG, nl.juiced.guhs.Guhs.id("dansje"))), "the disc points at song guhs:dansje");
        helper.assertTrue(song.get().value().soundEvent().value() == ErfenisMuziek.DANSJE.get()
                && song.get().value().lengthInSeconds() == 31f, "the song plays the Muziekdoosje's tune for 31 s");
        helper.assertTrue(plaat.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, nl.juiced.guhs.Guhs.id("techbron/zeldzame_plaat"))),
                "a rare disc for the Disco-dynamo");
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 1, 1)))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        // (tools/features/erfenis_muziek.py PLAAT_KISTEN: table, chance per chest)
        for (var kist : java.util.Map.of("chests/ballonfestival", 0.15, "chests/guh_picnic", 0.12, "chests/onderwol/grotdisco", 0.25).entrySet()) {
            String tafel = kist.getKey();
            var table = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.LOOT_TABLE, nl.juiced.guhs.Guhs.id(tafel)));
            int keer = 0;
            for (int i = 0; i < 400; i++) {
                for (net.minecraft.world.item.ItemStack s : table.getRandomItems(params)) {
                    if (s.is(ErfenisMuziek.MUZIEKPLAAT.get())) {
                        keer++;
                    }
                }
            }
            double verwacht = 400 * kist.getValue();
            helper.assertTrue(keer >= verwacht * 0.5 && keer <= verwacht * 1.6, tafel + ": about " + (int) verwacht + " of 400 chests, got " + keer);
        }
        helper.succeed();
    }
}
