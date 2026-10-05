package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.Lobby;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxTest;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.feature.guhpixel.blok.PortaalBlock;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the guhpixel slice "lobby" (batch px_lobby; run with {@code -Pgt=px_lobby}): the final lobby template
 * (anchors stay free, the shop where the contract says, the ten knabbels, the parkour blocks, the door home), the golden
 * knabbels per player (also through a real right-click under the safe rules), the parkour's state machine (checkpoints,
 * personal best, the first-finish reward, flying, two runners at once), the greeter and the Netwerkkabeltje, the lobby
 * guhs and their chat, the shared and the personal boards, the Guhdex section, the rank advancement and titles, and the
 * Guh-internetcafé (its template placed for real: the Beheerder, the sleepers, the screen that unlocks Guhpixel). The game
 * test server has no guhpixel dimension: every test marks its own box ({@link PxTest#gebied}).
 */
public class PxLobbyGameTests {
    private static final String BATCH = "px_lobby";
    private static final String PLEIN = "lobby_test_plein";

    private static boolean adv(ServerPlayer p, String naam) {
        var houder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return houder != null && p.getAdvancements().getOrStartProgress(houder).isDone();
    }

    // =====================================================================================================================
    // the template
    // =====================================================================================================================

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void lobbyTemplateHeeftAlles(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Lobby.TEMPLATE).orElse(null);
        helper.assertTrue(t != null, "the lobby template exists");
        Vec3i maat = t.getSize();
        helper.assertTrue(maat.getX() == 97 && maat.getZ() == 97 && maat.getY() <= 96, "97 x (max 96) x 97, got " + maat);
        CompoundTag tag = t.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        Map<BlockPos, String> blokken = new HashMap<>();
        Map<BlockPos, CompoundTag> staten = new HashMap<>();
        for (Tag raw : tag.getListOrEmpty("blocks")) {
            CompoundTag b = (CompoundTag) raw;
            int[] pos = b.getListOrEmpty("pos").stream().mapToInt(x -> ((NumericTag) x).intValue()).toArray();
            CompoundTag staat = palette.getCompoundOrEmpty(b.getIntOr("state", 0));
            BlockPos wereld = new BlockPos(pos[0], pos[1], pos[2]).offset(Guhpixel.LOBBY_MIN);
            blokken.put(wereld, staat.getStringOr("Name", ""));
            staten.put(wereld, staat);
        }
        helper.assertTrue(blokken.size() > 20000, "a real island, not the placeholder: " + blokken.size() + " blocks");
        helper.assertTrue(Lobby.versie(helper.getLevel().getServer()) >= 2, "the template's version was bumped past the placeholder's");
        // every anchor pad: a floor, and four free blocks (only the door home may stand in one)
        for (LobbyPlek plek : LobbyPlek.values()) {
            BlockPos a = plek.blok();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    helper.assertTrue(!blokken.getOrDefault(a.offset(dx, -1, dz), "minecraft:air").equals("minecraft:air"), plek + ": a floor under " + a.offset(dx, -1, dz));
                    for (int dy = 0; dy < 4; dy++) {
                        String vrij = blokken.getOrDefault(a.offset(dx, dy, dz), "minecraft:air");
                        helper.assertTrue(vrij.equals("minecraft:air") || vrij.equals("guhs:guhpixel_portaal"), plek + ": free at " + a.offset(dx, dy, dz) + ", not " + vrij);
                    }
                }
            }
        }
        // the door home: the contract's nine blocks, and here 5 wide and 4 high
        for (int x = -2; x <= 2; x++) {
            for (int y = 100; y <= 103; y++) {
                BlockPos pos = new BlockPos(x, y, 30);
                helper.assertTrue("guhs:guhpixel_portaal".equals(blokken.get(pos))
                        && "uit".equals(staten.get(pos).getCompoundOrEmpty("Properties").getStringOr("soort", "")), "the exit portal at " + pos);
            }
        }
        // the shop stands where the contract says (x -44..-25, z -6..18), open in front of the Verkoper-guh
        for (BlockPos muur : List.of(new BlockPos(-44, 103, 0), new BlockPos(-44, 103, 18), new BlockPos(-30, 103, -6), new BlockPos(-30, 103, 18),
                new BlockPos(-25, 106, 6), new BlockPos(-35, 107, 6))) {
            helper.assertTrue(!blokken.getOrDefault(muur, "minecraft:air").equals("minecraft:air"), "a shop wall or roof at " + muur);
        }
        for (int z = 1; z <= 11; z++) {
            helper.assertTrue(!blokken.containsKey(new BlockPos(-25, 101, z)), "the shop's doorway is open at z " + z);
        }
        // the map: ten knabbels with their numbers, the parkour blocks, room for the lobby guhs
        LobbyKaart.Kaart kaart = LobbyKaart.van(helper.getLevel().getServer());
        helper.assertTrue(kaart.knabbels().size() == Knabbels.AANTAL && kaart.chatguhs().size() == Chatguhs.NAMEN && kaart.tussen().size() == LobbyParkour.TUSSENPUNTEN,
                "lobby_kaart.json is complete: " + kaart);
        Set<BlockPos> knabbels = new HashSet<>();
        for (Map.Entry<BlockPos, String> e : blokken.entrySet()) {
            if (e.getValue().equals("guhs:lobby_gouden_knabbel")) {
                knabbels.add(e.getKey());
            }
        }
        helper.assertTrue(knabbels.equals(new HashSet<>(kaart.knabbels())), "exactly the ten knabbels of the map are in the template: " + knabbels);
        for (int i = 0; i < Knabbels.AANTAL; i++) {
            BlockPos pos = kaart.knabbels().get(i);
            helper.assertTrue(String.valueOf(i).equals(staten.get(pos).getCompoundOrEmpty("Properties").getStringOr("nummer", "")), "knabbel " + i + " at " + pos);
            helper.assertTrue(blokken.containsKey(pos.below()), "knabbel " + i + " lies on something");
        }
        helper.assertTrue("guhs:lobby_parkour_start".equals(blokken.get(kaart.start())) && kaart.start().equals(LobbyPlek.PARKOUR_START.blok().below()),
                "the start block is the floor under the PARKOUR_START anchor");
        helper.assertTrue("guhs:lobby_parkour_finish".equals(blokken.get(kaart.finish())) && kaart.finish().getX() < -25 && kaart.finish().getY() >= 104,
                "the finish block lies on the shop roof: " + kaart.finish());
        for (BlockPos pos : kaart.tussen()) {
            helper.assertTrue("guhs:lobby_parkour_tussenpunt".equals(blokken.get(pos)), "a checkpoint at " + pos);
        }
        helper.assertTrue(blokken.values().stream().filter(b -> b.startsWith("guhs:lobby_parkour_")).count() == 2 + LobbyParkour.TUSSENPUNTEN, "no other parkour blocks");
        for (LobbyKaart.Plek plek : kaart.chatguhs()) {
            helper.assertTrue(blokken.containsKey(plek.pos().below()) && !blokken.containsKey(plek.pos()) && !blokken.containsKey(plek.pos().above()),
                    "a lobby guh can sit at " + plek.pos());
        }
        // the logo (gold letters), signs, no NPCs, no fluids
        helper.assertTrue(blokken.entrySet().stream().filter(e -> e.getValue().equals("minecraft:gold_block") && e.getKey().getZ() == -37).count() > 100,
                "the PIXEL half of the logo is there");
        helper.assertTrue(blokken.values().stream().filter(b -> b.endsWith("_sign")).count() >= 15, "the lobby's signs");
        helper.assertTrue(tag.getListOrEmpty("entities").stream().noneMatch(e -> ((CompoundTag) e).getCompoundOrEmpty("nbt").getStringOr("id", "")
                .equals("guhs:guh_npc")), "no NPC in the template");
        helper.assertTrue(blokken.values().stream().noneMatch(b -> b.equals("minecraft:water") || b.equals("minecraft:lava")), "no free water or lava");
        helper.succeed();
    }

    // =====================================================================================================================
    // the golden knabbels
    // =====================================================================================================================

    @GuhTest(template = PLEIN, batch = BATCH)
    public static void knabbelsPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        helper.assertTrue(Knabbels.gevonden(p) == 0 && !Knabbels.alleGevonden(p), "nothing found yet");
        // number 0 through a real right-click: the safe rules let this block through (block tag guhpixel_bruikbaar)
        BlockPos nul = helper.absolutePos(new BlockPos(3, 2, 8));
        BlockState staat = level.getBlockState(nul);
        helper.assertTrue(staat.is(LobbySlice.GOUDEN_KNABBEL.get()) && staat.getValue(GoudenKnabbelBlock.NUMMER) == 0, "knabbel 0 stands in the test room: " + staat);
        p.snapTo(nul.getX() + 0.5, nul.getY(), nul.getZ() + 1.5);
        p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(nul), Direction.UP, nul, false));
        helper.assertTrue(Knabbels.heeft(p, 0) && Knabbels.gevonden(p) == 1 && Muntjes.saldo(p) == Knabbels.MUNTJES, "a click finds it: 10 muntjes");
        helper.assertTrue(level.getBlockState(nul).is(LobbySlice.GOUDEN_KNABBEL.get()), "the knabbel is never taken away");
        helper.assertTrue(!p.gameMode.destroyBlock(nul) && level.getBlockState(nul).is(LobbySlice.GOUDEN_KNABBEL.get()), "and cannot be broken");
        p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(nul), Direction.UP, nul, false));
        helper.assertTrue(!Knabbels.pak(p, nul, 0) && Muntjes.saldo(p) == Knabbels.MUNTJES, "the same one pays once");
        // the other player: their own count
        helper.assertTrue(!Knabbels.heeft(q, 0) && Knabbels.pak(q, nul, 0) && Muntjes.saldo(q) == Knabbels.MUNTJES && Knabbels.gevonden(q) == 1, "per player");
        helper.assertTrue(!Knabbels.pak(p, nul, -1) && !Knabbels.pak(p, nul, Knabbels.AANTAL), "numbers outside 0..9 are nothing");
        // a player who has not unlocked Guhpixel finds nothing
        Toegang.vergrendel(q);
        helper.assertTrue(!Knabbels.pak(q, nul, 1) && Knabbels.gevonden(q) == 1, "locked: nothing");
        // all ten: 100 muntjes, the advancement, the title
        helper.assertTrue(!adv(p, "lobby_knabbels") && !Titels.heeft(p, Titels.van("lobby_knabbelspeurder")), "not yet");
        for (int i = 1; i < Knabbels.AANTAL; i++) {
            BlockPos pos = helper.absolutePos(new BlockPos(3 + i, 2, 8));
            helper.assertTrue(level.getBlockState(pos).getValue(GoudenKnabbelBlock.NUMMER) == i, "knabbel " + i + " in the test room");
            helper.assertTrue(Knabbels.pak(p, pos, i), "knabbel " + i);
        }
        helper.assertTrue(Knabbels.alleGevonden(p) && Muntjes.saldo(p) == Knabbels.AANTAL * Knabbels.MUNTJES && Muntjes.totaal(p) == 100, "ten knabbels = 100 muntjes");
        helper.assertTrue(adv(p, "lobby_knabbels") && Titels.heeft(p, Titels.van("lobby_knabbelspeurder")), "the advancement and the title");
        helper.assertTrue(!adv(q, "lobby_knabbels") && !Titels.heeft(q, Titels.van("lobby_knabbelspeurder")), "only for who found them");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the parkour
    // =====================================================================================================================

    /** Puts the player on the block at this relative position (null: in the air above it) at time nu. */
    private static void op(GameTestHelper helper, ServerPlayer p, int x, boolean staat, long nu) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 2));
        p.snapTo(pos.getX() + 0.5, pos.getY() + (staat ? 1 : 2.2), pos.getZ() + 0.5);
        LobbyParkour.stap(p, staat ? helper.getLevel().getBlockState(pos) : null, pos, nu);
    }

    @GuhTest(template = PLEIN, batch = BATCH)
    public static void parkourLoopEnRecord(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        int start = 2, finish = 13;
        int[] tussen = {5, 8, 11};
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(start, 1, 2))).is(LobbySlice.PARKOUR_START.get())
                && helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(finish, 1, 2))).is(LobbySlice.PARKOUR_FINISH.get()), "the test course stands");
        helper.assertTrue(LobbyParkour.best(p) == 0 && !LobbyParkour.gehaald(p), "never run");
        // the finish without a start is nothing
        op(helper, p, finish, true, 0);
        helper.assertTrue(!LobbyParkour.bezig(p) && LobbyParkour.best(p) == 0, "no run without the start block");
        // on the start block the clock waits; it starts when the player steps off
        op(helper, p, start, true, 100);
        op(helper, p, start, true, 150);
        helper.assertTrue(!LobbyParkour.bezig(p), "armed, not running");
        op(helper, p, start, false, 160);
        helper.assertTrue(LobbyParkour.bezig(p), "stepped off: running");
        // a short cut: straight to the finish
        op(helper, p, finish, true, 200);
        helper.assertTrue(LobbyParkour.bezig(p) && LobbyParkour.best(p) == 0, "no finish without the checkpoints");
        // the same checkpoint three times is one checkpoint
        op(helper, p, 11, true, 210);
        op(helper, p, 11, false, 212);
        op(helper, p, 11, true, 214);
        op(helper, p, finish, true, 220);
        helper.assertTrue(LobbyParkour.best(p) == 0, "one checkpoint is not three");
        op(helper, p, 8, true, 230);
        op(helper, p, 5, true, 240);
        op(helper, p, finish, true, 260);
        helper.assertTrue(!LobbyParkour.bezig(p) && LobbyParkour.best(p) == 100 && LobbyParkour.keren(p) == 1, "finished in 260 - 160 ticks: " + LobbyParkour.best(p));
        helper.assertTrue(Muntjes.saldo(p) == LobbyParkour.MUNTJES && Muntjes.isVerdiend(p, LobbyParkour.SLEUTEL) && adv(p, "lobby_parkour")
                && LobbyParkour.gehaald(p) && Titels.heeft(p, Titels.van("lobby_dakhaas")), "the first finish: 50 muntjes, the advancement, the title");
        helper.assertTrue(LobbyParkour.tijd(100).equals("0:05.00") && LobbyParkour.tijd(1234).equals("1:01.70"), "times read m:ss.hh");
        // a faster run: a new record, no second reward. Two players run at the same time, each on their own clock.
        op(helper, p, start, true, 1000);
        op(helper, q, start, true, 1000);
        op(helper, p, start, false, 1001);
        op(helper, q, start, false, 1011);
        for (int i = 0; i < 3; i++) {
            op(helper, p, tussen[i], true, 1010 + i * 5);
            op(helper, q, tussen[i], true, 1050 + i * 5);
        }
        op(helper, p, finish, true, 1041);
        helper.assertTrue(LobbyParkour.best(p) == 40 && LobbyParkour.keren(p) == 2 && Muntjes.saldo(p) == LobbyParkour.MUNTJES, "40 ticks: a record, paid once");
        helper.assertTrue(LobbyParkour.bezig(q) && LobbyParkour.best(q) == 0, "the other runner is still on the way");
        op(helper, q, finish, true, 1111);
        helper.assertTrue(LobbyParkour.best(q) == 100 && Muntjes.saldo(q) == LobbyParkour.MUNTJES && LobbyParkour.best(p) == 40, "their own time and their own 50 muntjes");
        // a slower run keeps the record
        op(helper, p, start, true, 2000);
        op(helper, p, start, false, 2001);
        for (int i = 0; i < 3; i++) {
            op(helper, p, tussen[i], true, 2100 + i);
        }
        op(helper, p, finish, true, 2301);
        helper.assertTrue(LobbyParkour.best(p) == 40 && LobbyParkour.keren(p) == 3, "slower: the record stays");
        // flying ends a run, and so does being moved far away in one go
        op(helper, p, start, true, 3000);
        op(helper, p, start, false, 3001);
        p.getAbilities().flying = true;
        op(helper, p, 5, true, 3010);
        p.getAbilities().flying = false;
        helper.assertTrue(!LobbyParkour.bezig(p), "flying does not count");
        op(helper, p, finish, true, 3020);
        helper.assertTrue(LobbyParkour.keren(p) == 3, "and the finish after it is nothing");
        op(helper, p, start, true, 4000);
        op(helper, p, start, false, 4001);
        BlockPos ver = helper.absolutePos(new BlockPos(2, 2, 15));
        p.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
        LobbyParkour.stap(p, null, ver, 4002);
        helper.assertTrue(!LobbyParkour.bezig(p), "teleported: the run is over");
        // falling off: standing on the plain ground again, away from the start plate, ends the run (beside the plate it goes on)
        op(helper, p, start, true, 5000);
        op(helper, p, start, false, 5001);
        BlockPos naast = helper.absolutePos(new BlockPos(4, 1, 4));
        p.snapTo(naast.getX() + 0.5, naast.getY() + 1, naast.getZ() + 0.5);
        LobbyParkour.stap(p, helper.getLevel().getBlockState(naast), naast, 5005);
        helper.assertTrue(LobbyParkour.bezig(p), "a step beside the plate is still a run");
        BlockPos grond = helper.absolutePos(new BlockPos(10, 1, 6));
        p.snapTo(grond.getX() + 0.5, grond.getY() + 1, grond.getZ() + 0.5);
        LobbyParkour.stap(p, helper.getLevel().getBlockState(grond), grond, 5010);
        helper.assertTrue(!LobbyParkour.bezig(p), "on the ground far from the plate: fallen off");
        // the world's top 3 knows both
        List<Scorebord.Entry> top = Scorebord.top(helper.getLevel().getServer(), LobbyParkour.BORD);
        helper.assertTrue(!top.isEmpty() && top.get(0).score() <= 40, "the best time went to the board: " + top);
        LobbyParkour.reset(p);
        helper.assertTrue(LobbyParkour.best(p) == 0 && LobbyParkour.gehaald(p), "(dev) reset forgets the time, not the paid reward");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the NPCs
    // =====================================================================================================================

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, int x, int z) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos pos = helper.absolutePos(new BlockPos(x, 2, z));
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    private static int kabeltjes(ServerPlayer p) {
        return p.getInventory().countItem(GuhpixelFeature.NETWERKKABELTJE.get());
    }

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void welkomstguhGeeftKabeltje(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        GuhNpcEntity guh = npc(helper, GuhNpcEntity.Kind.LOBBY_WELKOMSTGUH, 4, 4);
        NpcRole rol = Features.role(GuhNpcEntity.Kind.LOBBY_WELKOMSTGUH);
        helper.assertTrue(rol != null, "the greeter has a role");
        ResourceKey<Recipe<?>> recept = ResourceKey.create(Registries.RECIPE, Guhs.id("guhpixel_poort"));
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().byKey(recept).isPresent(), "the gate's recipe exists");
        helper.assertTrue(kabeltjes(p) == 0 && !Welkomstguh.begroet(p) && !p.getRecipeBook().contains(recept), "a new visitor");
        rol.talk(guh, p);
        helper.assertTrue(kabeltjes(p) == 1 && Toegang.kabeltjeGehad(p) && Welkomstguh.begroet(p), "the first talk gives the Netwerkkabeltje");
        helper.assertTrue(p.getRecipeBook().contains(recept) && adv(p, "lobby_welkom"), "and the recipe of the Guhpixel-poort");
        rol.talk(guh, p);
        helper.assertTrue(kabeltjes(p) == 1, "once");
        rol.antwoord(guh, p, Welkomstguh.KABEL);
        helper.assertTrue(kabeltjes(p) == 1, "not while the player still has one");
        p.getInventory().clearContent();
        rol.antwoord(guh, p, Welkomstguh.KABEL);
        helper.assertTrue(kabeltjes(p) == 1, "lost: a new one on request");
        rol.antwoord(guh, p, Welkomstguh.WAT);
        rol.antwoord(guh, p, Welkomstguh.RANGEN);
        helper.assertTrue(kabeltjes(p) == 1 && kabeltjes(q) == 0, "explaining gives nothing; the other player has none");
        // somebody who never walked through the big screen gets no cable (and so no gate)
        Toegang.vergrendel(q);
        rol.talk(guh, q);
        rol.antwoord(guh, q, Welkomstguh.KABEL);
        helper.assertTrue(kabeltjes(q) == 0 && !Welkomstguh.begroet(q) && !q.getRecipeBook().contains(recept), "locked: no cable, no recipe");
        Toegang.ontgrendel(q);
        rol.talk(guh, q);
        helper.assertTrue(kabeltjes(q) == 1 && q.getRecipeBook().contains(recept), "unlocked: the cable");
        // the Verkoper-guh opens the shop (no exception, and it stays a role of its own)
        GuhNpcEntity verkoper = npc(helper, GuhNpcEntity.Kind.LOBBY_VERKOPER_GUH, 8, 4);
        NpcRole winkel = Features.role(GuhNpcEntity.Kind.LOBBY_VERKOPER_GUH);
        helper.assertTrue(winkel != null && NpcRollen.van(verkoper) == LobbyRollen.VERKOPER && NpcRollen.van(guh) == Welkomstguh.ROL, "the Verkoper-guh has its own role");
        winkel.talk(verkoper, p);
        Praat.vergeet(p);
        Praat.vergeet(q);
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    @GuhTest(template = "px_test_96", batch = BATCH)
    public static void lobbyguhsEnBorden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper), buiten = PxTest.speler(helper);
        BlockPos oorsprong = helper.absolutePos(new BlockPos(48, 2, 48));   // (this test's "spawn point")
        LobbyKaart.Kaart kaart = LobbyKaart.van(level.getServer());
        // one lobby guh per spot, each with its own gamer name; asking again makes no doubles
        helper.assertTrue(Chatguhs.zorg(level, oorsprong) == Chatguhs.NAMEN, "six lobby guhs sit down");
        helper.assertTrue(Chatguhs.zorg(level, oorsprong) == Chatguhs.NAMEN, "and again");
        List<GuhNpcEntity> guhs = level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), e -> e.getKind() == GuhNpcEntity.Kind.LOBBY_CHATGUH);
        helper.assertTrue(guhs.size() == Chatguhs.NAMEN, "no doubles: " + guhs.size());
        Set<Component> namen = new HashSet<>();
        for (GuhNpcEntity g : guhs) {
            namen.add(g.getCustomName());
        }
        helper.assertTrue(namen.size() == Chatguhs.NAMEN && namen.contains(Chatguhs.naam(0)) && namen.contains(Chatguhs.naam(5)), "six different names: " + namen);
        Vec3 eerste = LobbyBorden.plek(oorsprong, kaart.chatguhs().get(0).pos().getX() + 0.5, kaart.chatguhs().get(0).pos().getY(), kaart.chatguhs().get(0).pos().getZ() + 0.5);
        helper.assertTrue(guhs.stream().anyMatch(g -> g.position().distanceTo(eerste) < 1.5), "the first one sits on the map's first spot");
        // (a loaded NPC has its kind's name again: the next look puts the gamer name back)
        guhs.get(0).setKind(GuhNpcEntity.Kind.LOBBY_CHATGUH);
        Chatguhs.zorg(level, oorsprong);
        helper.assertTrue(guhs.get(0).getCustomName() != null && namen.contains(guhs.get(0).getCustomName()), "the name comes back");
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.LOBBY_CHATGUH) != null && NpcRollen.van(guhs.get(0)) == Chatguhs.ROL, "a click talks");
        Features.role(GuhNpcEntity.Kind.LOBBY_CHATGUH).talk(guhs.get(0), p);
        // the chat: only for who is on the plaza; 30 to 60 seconds apart; never the same line twice in a row
        buiten.snapTo(buiten.getX(), buiten.getY() + 200, buiten.getZ());
        helper.assertTrue(Chatguhs.publiek(level).contains(p) && Chatguhs.publiek(level).contains(q) && !Chatguhs.publiek(level).contains(buiten), "the audience");
        helper.assertTrue(Chatguhs.zeg(level, oorsprong, 2, 4) >= 2, "a line reaches the players on the plaza");
        RandomSource random = RandomSource.create(42);
        int vorige = -1;
        Set<Integer> gezien = new HashSet<>();
        for (int i = 0; i < 600; i++) {
            int wacht = Chatguhs.volgendeWacht(random);
            helper.assertTrue(wacht >= 600 && wacht <= 1200, "30 to 60 seconds: " + wacht);
            int regel = Chatguhs.volgendeRegel(random);
            helper.assertTrue(regel != vorige && regel >= 0 && regel < Chatguhs.REGELS, "never twice in a row: " + regel);
            vorige = regel;
            gezien.add(regel);
        }
        helper.assertTrue(gezien.size() == Chatguhs.REGELS, "every line comes by: " + gezien.size());
        helper.assertTrue(Chatguhs.regel(1, 0).getString().contains(": "), "a chat line has a name and a text");
        // the shared labels: one text display each, not doubled by the next refresh
        LobbyBorden.gedeeld(level, oorsprong);
        LobbyBorden.gedeeld(level, oorsprong);
        List<Display.TextDisplay> teksten = level.getEntitiesOfClass(Display.TextDisplay.class, helper.getBounds().inflate(0, 8, 0), d -> d.entityTags().contains(Scorebord.TAG));
        helper.assertTrue(teksten.size() == 4, "four shared labels (online, parkour, the door home, the empty stall): " + teksten.size());
        helper.assertTrue(LobbyBorden.online(level.getServer()).getString().length() > 10, "the online board has a text");
        // the personal boards: each player their own numbers at the same spot, a packet only when something changed
        helper.assertTrue(Zweeftekst.zichtbaar(p, LobbyBorden.STATS) == null && !Zweeftekst.ziet(p), "nothing yet");
        LobbyBorden.persoonlijk(p, oorsprong);
        LobbyBorden.persoonlijk(q, oorsprong);
        helper.assertTrue(LobbyBorden.stats(p).equals(Zweeftekst.zichtbaar(p, LobbyBorden.STATS)) && Zweeftekst.zichtbaar(p, LobbyBorden.PARKOUR) != null, "p sees their stats");
        Vec3 bord = LobbyBorden.statsPlek(oorsprong);
        helper.assertTrue(!Zweeftekst.toon(p, LobbyBorden.STATS, bord, LobbyBorden.stats(p)), "unchanged: no packet");
        Muntjes.verdienEens(p, "lobby:knabbel_3", Knabbels.MUNTJES);
        helper.assertTrue(!LobbyBorden.stats(p).equals(Zweeftekst.zichtbaar(p, LobbyBorden.STATS)), "the numbers changed");
        helper.assertTrue(Zweeftekst.toon(p, LobbyBorden.STATS, bord, LobbyBorden.stats(p)) && LobbyBorden.stats(p).equals(Zweeftekst.zichtbaar(p, LobbyBorden.STATS)),
                "changed: one update");
        helper.assertTrue(!LobbyBorden.stats(q).equals(LobbyBorden.stats(p)) && LobbyBorden.stats(q).equals(Zweeftekst.zichtbaar(q, LobbyBorden.STATS)),
                "q still sees their own board");
        Zweeftekst.weg(p);
        helper.assertTrue(!Zweeftekst.ziet(p) && Zweeftekst.ziet(q), "walking away takes only your own texts");
        Zweeftekst.vergeet(q);
        PxTest.klaar(helper, p, q, buiten);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Guhdex, ranks, titles
    // =====================================================================================================================

    @GuhTest(template = "px_test_16", batch = BATCH)
    public static void gidsRangEnTitels(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        helper.assertTrue(GidsBlad.secties().stream().anyMatch(s -> s.id().equals("lobby") && s.volgorde() == 10 && !s.zonderToegang()), "the lobby section is registered first");
        ListTag rijen = GidsBlad.stand(p).getListOrEmpty("Rijen");
        helper.assertTrue(rijen.toString().contains("gui.guhs.lobby.gids.kop") && rijen.toString().contains("gui.guhs.lobby.gids.knabbels"), "the section's rows: " + rijen.size());
        Toegang.vergrendel(q);
        String dicht = GidsBlad.stand(q).getListOrEmpty("Rijen").toString();
        helper.assertTrue(!dicht.contains("gui.guhs.lobby.gids.kop") && dicht.contains("gui.guhs.guhpixel.gids.zoek_cafe"), "locked: only the hint to the café");
        // ranks: the advancement at [MVG], the title at [MVG++]
        for (String id : List.of("lobby_knabbelspeurder", "lobby_dakhaas", "lobby_mvg")) {
            helper.assertTrue(Titels.van(id) != null && !Titels.heeft(p, Titels.van(id)), "title " + id + " exists and is not earned yet");
        }
        Muntjes.zet(p, Rang.MVG.vanaf() - 1);
        LobbySlice.controleerRang(p);
        helper.assertTrue(!adv(p, "lobby_rang_mvg"), "not yet [MVG]");
        Muntjes.zet(p, Rang.MVG.vanaf());
        LobbySlice.controleerRang(p);
        helper.assertTrue(adv(p, "lobby_rang_mvg") && !LobbySlice.isMvgPlusPlus(p), "[MVG]: the advancement");
        Muntjes.zet(p, Rang.MVG_PLUS_PLUS.vanaf());
        helper.assertTrue(LobbySlice.isMvgPlusPlus(p) && Titels.heeft(p, Titels.van("lobby_mvg")), "[MVG++]: the title");
        Muntjes.zet(q, Rang.MVG_PLUS_PLUS.vanaf());
        LobbySlice.controleerRang(q);
        helper.assertTrue(!adv(q, "lobby_rang_mvg") && !LobbySlice.isMvgPlusPlus(q), "a rank only counts for who unlocked Guhpixel");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Guh-internetcafé (the real template is this test's room)
    // =====================================================================================================================

    @GuhTest(template = "internetcafe", batch = BATCH)
    public static void internetcafeMetBeheerderEnBeeldscherm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<GuhNpcEntity> npcs = level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1));
        long beheerders = npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.INTERNETCAFE_BEHEERDER).count();
        long slapers = npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.INTERNETCAFE_SLAPER).count();
        helper.assertTrue(beheerders == 1 && slapers == 7, "one Beheerder-guh and seven sleepers: " + beheerders + " / " + slapers);
        // the giant screen: 5 wide and 4 high, soort=in (template y 5..8 = relative y 6..9)
        int scherm = 0, computers = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(20, 16, 18)))) {
            BlockState s = level.getBlockState(pos);
            if (s.is(GuhpixelFeature.PORTAAL.get()) && s.getValue(PortaalBlock.SOORT) == PortaalBlock.Soort.IN) {
                scherm++;
            } else if (s.is(LobbySlice.COMPUTER.get())) {
                computers++;
            }
        }
        helper.assertTrue(scherm == 20 && computers == 10, "20 portal blocks and 10 old computers: " + scherm + " / " + computers);
        for (int z = 5; z <= 16; z++) {
            for (int y = 6; y <= 8; y++) {
                BlockState s = level.getBlockState(helper.absolutePos(new BlockPos(10, y, z)));
                helper.assertTrue(s.isAir() || s.is(net.minecraft.world.level.block.Blocks.LIGHT_BLUE_CARPET), "the way from the door to the screen is free at z " + z + " y " + y + ": " + s);
            }
        }
        // walking through the screen unlocks Guhpixel, for this player only (there is no guhpixel dimension on the test server)
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        Toegang.vergrendel(p);
        Toegang.vergrendel(q);
        BlockPos portaal = helper.absolutePos(new BlockPos(10, 6, 4));
        helper.assertTrue(level.getBlockState(portaal).is(GuhpixelFeature.PORTAAL.get()), "the screen's middle");
        GuhpixelFeature.PORTAAL.get().getPortalDestination(level, p, portaal);
        helper.assertTrue(Toegang.heeft(p) && !Toegang.heeft(q) && adv(p, "guhpixel_ontgrendeld"), "through the screen: unlocked");
        // the Beheerder-guh and a sleeper talk
        GuhNpcEntity beheerder = npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.INTERNETCAFE_BEHEERDER).findFirst().orElseThrow();
        NpcRole rol = Features.role(GuhNpcEntity.Kind.INTERNETCAFE_BEHEERDER);
        GuhNpcEntity slaper = npcs.stream().filter(n -> n.getKind() == GuhNpcEntity.Kind.INTERNETCAFE_SLAPER).findFirst().orElseThrow();
        helper.assertTrue(rol != null && NpcRollen.van(beheerder) == LobbyRollen.BEHEERDER && NpcRollen.van(slaper) == LobbyRollen.SLAPER, "their roles");
        rol.talk(beheerder, q);
        rol.antwoord(beheerder, q, LobbyRollen.Beheerder.HOE);
        rol.antwoord(beheerder, q, LobbyRollen.Beheerder.TRAAG);
        rol.antwoord(beheerder, q, LobbyRollen.Beheerder.SLAPERS);
        helper.assertTrue(adv(q, "internetcafe_beheerder") && !Toegang.heeft(q), "talking is not unlocking");
        LobbyRollen.SLAPER.talk(slaper, q);
        LobbyRollen.SLAPER.tick(slaper);
        helper.assertTrue(slaper.isNoAi() && !beheerder.isNoAi(), "the sleepers keep facing their screens");
        // worldgen: the random set and the guaranteed copy
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        helper.assertTrue(sets.containsKey(Guhs.id("internetcafe")) && sets.containsKey(Guhs.id("internetcafe_gegarandeerd")), "both structure sets are loaded");
        helper.assertTrue(sets.getValue(Guhs.id("internetcafe")).placement() instanceof RandomSpreadStructurePlacement r && r.spacing() == 56 && r.separation() == 20,
                "the random set: every 56 chunks or so");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).containsKey(Guhs.id("internetcafe")), "the structure itself");
        Praat.vergeet(q);
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }
}
