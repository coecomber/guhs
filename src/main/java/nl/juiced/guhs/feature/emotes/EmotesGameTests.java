package nl.juiced.guhs.feature.emotes;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Guh emotes: once / keep going / stop, getting hurt, not while ridden or for someone else's guh, the favourite (saved),
 * the quests, dancing at a jukebox, wild guhs waving (shy ones hiding their eyes), personalities, and the animations.
 */
public class EmotesGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    /** A stone floor (the empty template has none), so the guhs stand on something. */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    /** A tamed guh that stays where it is. */
    private static GuhEntity tamed(GameTestHelper helper, ServerPlayer owner) {
        floor(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), POS);
        guh.tame(owner);
        guh.setWandering(false);
        guh.setTeleportEnabled(false);
        return guh;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static String state(GuhEntity g) {
        return "ground=" + g.onGround() + " noAi=" + g.isNoAi() + " target=" + g.getTarget() + " water=" + g.isInWater()
                + " vehicle=" + g.isVehicle() + " passenger=" + g.isPassenger() + " type=" + g.getType() + " alive=" + g.isAlive()
                + " hidden=" + g.getHiddenBy() + " sit=" + g.isOrderedToSit() + " y=" + g.getY();
    }

    private static boolean ask(ServerPlayer p, GuhEntity guh, int action, Emote emote) {
        return EmotePayload.apply(p, new EmotePayload(guh.getId(), action, emote == null ? -1 : emote.ordinal()));
    }

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void emoteOnceEndsByItself(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        GuhEntity guh = tamed(helper, owner);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(ask(owner, guh, EmotePayload.NOW, Emote.ZWAAIEN), "the owner can ask for a wave " + state(guh));
            helper.assertTrue(guh.emotes.current() == Emote.ZWAAIEN && !guh.emotes.isLooping(), "it waves (once)");
            helper.assertTrue(guh.getEmoteData() != 0, "synced");
            helper.assertTrue(advancement(owner, "emote_gedaan"), "the first-emote quest");
            helper.assertTrue((GuhQuests.saved(owner).getIntOr("guhs_emotes_done", 0) & (1 << Emote.ZWAAIEN.ordinal())) != 0, "remembered");
        });
        helper.runAfterDelay(5 + Emote.ZWAAIEN.onceTicks + 3, () -> {
            helper.assertTrue(guh.emotes.current() == null, "done waving: " + guh.emotes.current());
            leave(helper, owner);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 300)
    public static void loopedEmoteStopsWhenAskedOrHurt(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        GuhEntity guh = tamed(helper, owner);
        helper.runAfterDelay(5, () -> helper.assertTrue(ask(owner, guh, EmotePayload.LOOP, Emote.DANSEN), "keep dancing"));
        helper.runAfterDelay(5 + Emote.DANSEN.onceTicks + 40, () -> {
            helper.assertTrue(guh.emotes.current() == Emote.DANSEN && guh.emotes.isLooping(), "still dancing");
            helper.assertTrue(ask(owner, guh, EmotePayload.STOP, null) && guh.emotes.current() == null, "stopped");
            helper.assertTrue(ask(owner, guh, EmotePayload.LOOP, Emote.SLAPEN), "sleep");
            guh.hurt(guh.damageSources().generic(), 1f);
        });
        helper.runAfterDelay(5 + Emote.DANSEN.onceTicks + 42, () -> {
            helper.assertTrue(guh.emotes.current() == null, "getting hurt wakes it up");
            leave(helper, owner);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void noEmoteWhileRiddenOrForSomeoneElse(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        ServerPlayer other = player(helper);
        GuhEntity guh = tamed(helper, owner);
        guh.setGuhScale(1.5f);
        guh.equipSaddle(new ItemStack(Items.SADDLE), null);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!ask(other, guh, EmotePayload.NOW, Emote.VAHOEG) && guh.emotes.current() == null, "not someone else's guh");
            helper.assertTrue(!ask(other, guh, EmotePayload.FAVORITE, Emote.VAHOEG) && guh.emotes.favorite() == null, "no favourite either");
            helper.assertTrue(!ask(owner, guh, EmotePayload.NOW, null), "a bad emote number is refused");
            helper.assertTrue(ask(owner, guh, EmotePayload.LOOP, Emote.ROLLEN), "rolling");
            owner.startRiding(guh, true, true);
        });
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(guh.isVehicle(), "ridden");
            helper.assertTrue(guh.emotes.current() == null, "getting on stops the emote");
            helper.assertTrue(!ask(owner, guh, EmotePayload.NOW, Emote.ROLLEN), "and no emote while ridden");
            owner.stopRiding();
            leave(helper, owner, other);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void favouriteEmoteIsSaved(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        GuhEntity guh = tamed(helper, owner);
        helper.assertTrue(ask(owner, guh, EmotePayload.FAVORITE, Emote.ROLLEN) && guh.emotes.favorite() == Emote.ROLLEN, "favourite set");
        helper.assertTrue(guh.getFavoriteEmote() == Emote.ROLLEN.ordinal(), "synced");
        helper.assertTrue(advancement(owner, "emote_lievelings"), "the favourite quest");
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(guh);
        helper.assertTrue("rollen".equals(tag.getStringOr("FavoriteEmote", "")), "saved: " + tag.getStringOr("FavoriteEmote", ""));
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(copy, tag);
        helper.assertTrue(copy.emotes.favorite() == Emote.ROLLEN, "loaded again");
        helper.assertTrue(ask(owner, guh, EmotePayload.FAVORITE, null) && guh.emotes.favorite() == null, "no favourite");
        leave(helper, owner);
        helper.succeed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 200)
    public static void allSevenEmotesGrantTheTalentQuest(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        GuhEntity guh = tamed(helper, owner);
        helper.runAfterDelay(5, () -> {
            // 2.10: the three hartjes emotes are unlocks (samen): locked at first, and they don't count for "alle"
            nl.juiced.guhs.feature.samen.SamenBeloning.wisEmotes(owner);
            for (Emote e : Emote.values()) {
                if (!e.kiesbaar()) {   // 1.2.0: petting is a tap, the picker can't ask for it
                    helper.assertTrue(!ask(owner, guh, EmotePayload.NOW, e) && guh.emotes.current() != e, "not pickable: " + e);
                    continue;
                }
                if (nl.juiced.guhs.feature.samen.SamenBeloning.isBandEmote(e)) {
                    helper.assertTrue(!ask(owner, guh, EmotePayload.NOW, e) && guh.emotes.current() != e, "locked: " + e);
                    continue;
                }
                if (Emote.alleenVoor(e) != null) {   // 3.0: the ukelele is the 626-guh's only (and not part of "alle")
                    helper.assertTrue(!ask(owner, guh, EmotePayload.NOW, e) && guh.emotes.current() != e, "not for a normal guh: " + e);
                    continue;
                }
                helper.assertTrue(!advancement(owner, "emote_alle"), "not all yet before " + e);
                helper.assertTrue(ask(owner, guh, EmotePayload.NOW, e) && guh.emotes.current() == e, "does " + e);
            }
            helper.assertTrue(advancement(owner, "emote_alle"), "all (but the hartjes emotes and the ukelele)");
            guh.setVariant(nl.juiced.guhs.entity.GuhVariant.STITCH626);
            helper.assertTrue(ask(owner, guh, EmotePayload.NOW, Emote.UKELELE) && guh.emotes.current() == Emote.UKELELE, "the 626-guh plays the ukelele");
            nl.juiced.guhs.feature.samen.SamenBeloning.geef(owner, nl.juiced.guhs.feature.band.BandNiveau.ZIELSGUH, null);
            for (Emote e : new Emote[]{Emote.HARTJES, Emote.KNUFFELDANSJE, Emote.BFF_KNUFFEL}) {
                helper.assertTrue(ask(owner, guh, EmotePayload.NOW, e) && guh.emotes.current() == e, "unlocked: " + e);
            }
            leave(helper, owner);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY, timeoutTicks = 400)
    public static void guhsDanceNearAPlayingJukebox(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        GuhEntity guh = tamed(helper, owner);
        BlockPos pos = new BlockPos(0, 1, 4);
        helper.setBlock(pos, Blocks.JUKEBOX);
        JukeboxBlockEntity jukebox = helper.getBlockEntity(pos, JukeboxBlockEntity.class);
        jukebox.setSongItemWithoutPlaying(new ItemStack(Items.MUSIC_DISC_CAT));
        jukebox.getSongPlayer().stop(helper.getLevel(), jukebox.getBlockState());
        helper.assertTrue(GuhEmotes.playingJukebox(helper.getLevel(), guh.blockPosition(), GuhEmotes.JUKEBOX_RANGE) == null, "quiet");
        jukebox.tryForcePlaySong();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(guh.emotes.current() == Emote.DANSEN
                        && guh.emotes.source() == GuhEmotes.Source.JUKEBOX, "dances to the music: " + guh.emotes.current()))
                .thenExecute(() -> {
                    helper.assertTrue(advancement(owner, "emote_jukebox"), "the jukebox quest");
                    jukebox.getSongPlayer().stop(helper.getLevel(), jukebox.getBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(guh.emotes.current() == null, "the music stopped, so does the guh"))
                .thenExecute(() -> leave(helper, owner))
                .thenSucceed();
    }

    @GuhTest(template = EMPTY, timeoutTicks = 100)
    public static void wildGuhsWaveAndShyOnesHideTheirEyes(GameTestHelper helper) {
        floor(helper);
        ServerPlayer p = player(helper);
        GuhEntity friendly = helper.spawn(ModEntities.GUH.get(), POS);
        friendly.setPersonality(GuhPersonality.PLAYFUL);
        GuhEntity shy = helper.spawn(ModEntities.GUH.get(), POS.east());
        shy.setPersonality(GuhPersonality.SHY);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(friendly.emotes.greet(p) && friendly.emotes.current() == Emote.ZWAAIEN, "a wave");
            helper.assertTrue(p.getUUID().equals(friendly.emotes.lookTarget()), "looking at the player");
            helper.assertTrue(advancement(p, "emote_gezwaaid"), "the wave quest");
            helper.assertTrue(!shy.emotes.greet(p) && shy.emotes.current() == null, "a shy guh keeps its distance");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get()));
            helper.assertTrue(shy.emotes.greet(p) && shy.emotes.current() == Emote.VERLEGEN, "unless you bring knabbels: then it's shy");
            leave(helper, p);
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void emotesFitThePersonality(GameTestHelper helper) {
        for (GuhPersonality personality : GuhPersonality.values()) {
            helper.assertTrue(!GuhEmotes.personalityEmotes(personality).isEmpty(), "emotes for " + personality);
            for (int roll = 0; roll < 100; roll++) {
                Emote e = GuhEmotes.randomFor(personality, false, roll);
                helper.assertTrue(GuhEmotes.personalityEmotes(personality).contains(e), personality + " does " + e);
            }
        }
        helper.assertTrue(GuhEmotes.personalityEmotes(GuhPersonality.SHY).equals(java.util.List.of(Emote.VERLEGEN)), "shy guhs are shy");
        helper.assertTrue(GuhEmotes.randomFor(GuhPersonality.PLAYFUL, true, 0) == Emote.SLAPEN, "sleepy at night");
        helper.succeed();
    }

    /** The seven animations are in the guh animation file (made by tools/features/emotes.py). */
    @GuhTest(template = EMPTY)
    public static void emoteAnimationsExist(GameTestHelper helper) {
        var in = EmotesGameTests.class.getResourceAsStream("/assets/guhs/geckolib/animations/entity/guh.animation.json");
        helper.assertTrue(in != null, "the animation file");
        JsonObject animations = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                .getAsJsonObject("animations");
        for (Emote e : Emote.values()) {
            helper.assertTrue(animations.has("animation.guh.emote_" + e.id()), "animation for " + e);
        }
        helper.assertTrue(animations.has("animation.guh.idle") && animations.has("animation.guh.sit"), "the old ones are still there");
        helper.succeed();
    }
}
