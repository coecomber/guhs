package nl.juiced.guhs.feature.snuffel;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the kern of Het Snuffeleiland (batches {@code snuffel}, {@code snuffel_reis} and {@code snuffel_station}; run with
 * {@code -Pgt=SnuffelGameTests}). The game test server has no datapack dimensions, so every test marks its own little
 * island in the middle of the bare floor {@code snuffel_test_eiland} ({@link Proef}): 13 x 13 blocks, with a rim of test
 * floor around it that is NOT island (that is "home").
 * <p>
 * What they prove: the inventory safe (an exact round trip, also for what the island gives); the dog form in every way in
 * and out (the boat, the memory card, a teleport, a logout and login, an island that is gone at login, a death and its
 * respawn, a login on the death screen, a change of dimension); the island's rules; the companion that only its own
 * player is sent; the ranks and what a rank can smell; sniffing, digging and per-player scents; the tree's stages, good
 * deeds, the growth scene; the residents and the tree that the island keeps; exams; where travel puts you (the beach,
 * the harbour, the last spot, exactly home, a home that was built over); the Guhstation; the choice; the questline's end;
 * and that every model, animation and text the data names is really in the jar.
 */
public class SnuffelGameTests {
    private static final String BATCH = "snuffel", ALLEEN = "snuffel_reis", STATION = "snuffel_station", VLOER = "snuffel_test_eiland";
    private static final AtomicInteger NR = new AtomicInteger();

    /** One test's island and players. */
    private static final class Proef {
        final GameTestHelper helper;
        final Eiland.Plaats plaats;
        final List<ServerPlayer> spelers = new ArrayList<>();

        Proef(GameTestHelper helper) {
            this(helper, List.of(), List.of());
        }

        Proef(GameTestHelper helper, List<Eiland.BewonerPlek> bewoners, List<Eiland.BronPlek> bronnen) {
            this.helper = helper;
            Eiland.Opzet opzet = new Eiland.Opzet(1, BlockPos.ZERO, new Vec3i(13, 5, 13), List.of(), new Eiland.Punt(new Vec3(6.5, 1, 10.5), 180f),
                    new Eiland.Punt(new Vec3(10.5, 1, 6.5), 90f), new BlockPos(6, 1, 2), 0, 1, bewoners, bronnen, List.of());
            this.plaats = Eiland.test(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 4)), opzet);
        }

        /** "Home": a spot on the test floor outside the island (0 = the north-west corner, 1 = the south-east corner). */
        Vec3 thuis(int welke) {
            BlockPos b = helper.absolutePos(welke == 0 ? new BlockPos(1, 2, 1) : new BlockPos(19, 2, 19));
            return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        }

        /** A spot on the island (island coordinates, feet on the floor). */
        Vec3 op(double x, double z) {
            return plaats.wereld(new Vec3(x, 1, z));
        }

        /** A survival mock player standing at home. */
        ServerPlayer speler(int welke) {
            ServerPlayer p = GuhMockPlayer.of(helper);
            p.setGameMode(GameType.SURVIVAL);
            Vec3 t = thuis(welke);
            p.snapTo(t.x, t.y, t.z, 33f, -12f);
            p.setOnGround(true);
            spelers.add(p);
            return p;
        }

        void zet(ServerPlayer p, Vec3 waar) {
            p.teleportTo(helper.getLevel(), waar.x, waar.y, waar.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
            p.setOnGround(true);
        }

        /** What the server does for a real player every tick. */
        void tick(ServerPlayer p) {
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        }

        void klaar() {
            for (ServerPlayer p : spelers) {
                Maatjes.weg(p);
                Hondvorm.vergeet(p);
                Snuffelen.vergeet(p.getUUID());
                GuhstationBlock.vergeet(p.getUUID());
                Keuze.vergeetOpen(p.getUUID());
                CompoundTag saved = GuhQuests.saved(p);
                saved.remove(SnuffelData.SLEUTEL);
                saved.remove(SnuffelKluis.SLEUTEL);
                saved.remove(SnuffelKluis.POST);
                Minigames.forget(p);
                if (!p.isRemoved()) {
                    helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
                }
            }
            for (Entity e : helper.getLevel().getEntities((Entity) null, helper.getBounds().inflate(6),
                    x -> x instanceof BoompjeEntity || x instanceof BewonerEntity || x instanceof MaatjeEntity)) {
                e.discard();
            }
            Eiland.testWeg(plaats);
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A recognisable inventory: hotbar, main, all armour, the offhand, a named stack, slot 5 selected. */
    private static void vul(ServerPlayer p) {
        Inventory inv = p.getInventory();
        inv.clearContent();
        inv.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        inv.setItem(5, new ItemStack(Items.COOKED_BEEF, 17));
        inv.setItem(8, new ItemStack(Items.TORCH, 64));
        inv.setItem(9, new ItemStack(Items.OAK_PLANKS, 33));
        inv.setItem(35, new ItemStack(Items.ENDER_PEARL, 16));
        inv.setItem(36, new ItemStack(Items.IRON_BOOTS));
        inv.setItem(37, new ItemStack(Items.IRON_LEGGINGS));
        inv.setItem(38, new ItemStack(Items.IRON_CHESTPLATE));
        inv.setItem(39, new ItemStack(Items.IRON_HELMET));
        inv.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.SHIELD));
        ItemStack naam = new ItemStack(Items.PAPER, 2);
        naam.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Njeg"));
        inv.setItem(20, naam);
        inv.setSelectedSlot(5);
    }

    private static List<ItemStack> foto(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            out.add(inv.getItem(i).copy());
        }
        return out;
    }

    /** The first slot that differs from the photo (null: every slot exactly the same). */
    private static String verschil(List<ItemStack> voor, ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!ItemStack.matches(voor.get(i), inv.getItem(i))) {
                return "slot " + i + ": " + voor.get(i) + " became " + inv.getItem(i);
            }
        }
        return null;
    }

    /** A dog's pockets: the memory card in its slot and nothing else, nothing on the cursor. */
    private static boolean alleenKaart(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            boolean kaart = inv.getItem(i).is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get());
            if (i == Hondvorm.KAART_SLOT ? !kaart : !inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return p.containerMenu.getCarried().isEmpty();
    }

    private static boolean leeg(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean heeftKaart(ServerPlayer p) {
        return p.getInventory().hasAnyMatching(s -> s.is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get()));
    }

    private static boolean hond(ServerPlayer p) {
        return Hondvorm.actief(p) && SnuffelKluis.heeft(p) && p.getBbHeight() < 1.0f && p.getEyeHeight() < 0.85f;
    }

    private static boolean mens(ServerPlayer p) {
        return !Hondvorm.actief(p) && !SnuffelKluis.heeft(p) && p.getBbHeight() > 1.4f && !heeftKaart(p);
    }

    private static String geur(String naam) {
        return "snuffeltest_" + naam + "_" + NR.incrementAndGet();
    }

    private static boolean dicht(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b) < 1.0e-6;
    }

    // =====================================================================================================================
    // the inventory safe
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelInventarisVeilig(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0);
        vul(p);
        List<ItemStack> voor = foto(p);
        p.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 7));
        helper.assertTrue(mens(p), "a player at home");
        helper.assertTrue(Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND), "sailed to the island");
        helper.assertTrue(hond(p), "a dog on the island, with a dog's size: " + p.getBbHeight() + " / " + p.getEyeHeight());
        helper.assertTrue(alleenKaart(p), "a dog's pockets hold the memory card and nothing else");
        helper.assertTrue(GuhQuests.saved(p).getCompound(SnuffelKluis.SLEUTEL).isPresent(), "the snapshot lives in the player's own data (one file with the inventory)");
        helper.assertTrue(!SnuffelKluis.bewaar(p), "a second snapshot never overwrites the first");
        helper.assertTrue(p.getInventory().getSelectedSlot() == 0, "the paw is on the first slot, not on the memory card");
        // what the island gives travels home with the post
        Snuffel.geef(p, new ItemStack(Items.EMERALD, 3));
        helper.assertTrue(alleenKaart(p) && SnuffelKluis.postAantal(p) == 1, "a gift does not land in a dog's pockets");
        // a builder's block in a dog's pockets is not lost either
        p.getInventory().setItem(3, new ItemStack(Items.STICK, 9));
        t.tick(p);
        helper.assertTrue(p.getInventory().getItem(Hondvorm.KAART_SLOT).is(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get()), "the card stays in its slot");
        helper.assertTrue(Reis.naarHuis(p), "home");
        helper.assertTrue(mens(p), "a player again, with a player's size: " + p.getBbHeight());
        // the cursor stack, the gift and the stick took free slots: everything else is exactly where it was
        int goud = 0, smaragd = 0, stok = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Items.GOLD_INGOT) || s.is(Items.EMERALD) || s.is(Items.STICK)) {
                helper.assertTrue(voor.get(i).isEmpty(), "what came on top took a free slot");
                goud += s.is(Items.GOLD_INGOT) ? s.getCount() : 0;
                smaragd += s.is(Items.EMERALD) ? s.getCount() : 0;
                stok += s.is(Items.STICK) ? s.getCount() : 0;
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        helper.assertTrue(goud == 7 && smaragd == 3 && stok == 9, "the cursor stack, the gift and the stick are all there: " + goud + " " + smaragd + " " + stok);
        String anders = verschil(voor, p);
        helper.assertTrue(anders == null, "every slot exactly as before: " + anders);
        helper.assertTrue(p.getInventory().getSelectedSlot() == 5, "the selected slot is back");
        helper.assertTrue(!SnuffelKluis.heeftPost(p) && !SnuffelKluis.herstel(p), "the safe and the post are used up");
        // pockets that are full: the post waits, nothing drops
        // a second trip works just the same
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.LAATSTE);
        helper.assertTrue(hond(p) && alleenKaart(p), "a dog again");
        Snuffel.geef(p, new ItemStack(Items.DIAMOND, 2));
        Reis.naarHuis(p);
        helper.assertTrue(mens(p) && p.getInventory().countItem(Items.DIAMOND) == 2, "a second trip's gift arrives too");
        t.klaar();
        helper.succeed();
    }

    /** Full pockets at home: what the island gave waits for a free slot and is never dropped. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelPostWacht(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0);
        Inventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        List<ItemStack> voor = foto(p);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Snuffel.geef(p, new ItemStack(SnuffelFeature.GUHSTATION_ITEM.get()));
        Reis.naarHuis(p);
        helper.assertTrue(verschil(voor, p) == null && SnuffelKluis.postAantal(p) == 1, "no room: the Guhstation waits in the post");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(6)).isEmpty(), "nothing lies on the ground");
        inv.setItem(4, ItemStack.EMPTY);
        helper.assertTrue(SnuffelKluis.geefPost(p) && inv.getItem(4).is(SnuffelFeature.GUHSTATION_ITEM.get()) && !SnuffelKluis.heeftPost(p),
                "a free slot: there it is");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // the dog form in every way in and out
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelVormBijElkeUitgang(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = t.speler(0);
        vul(p);
        List<ItemStack> voor = foto(p);

        // 1) the memory card's "Opslaan en naar huis"
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        helper.assertTrue(hond(p) && alleenKaart(p), "1: a dog");
        helper.assertTrue(GeheugenkaartItem.naarHuis(p) && mens(p) && verschil(voor, p) == null, "1: the memory card brings a player home: " + verschil(voor, p));
        helper.assertTrue(!GeheugenkaartItem.naarHuis(p), "1: the card's button does nothing for a player");

        // 2) INTO the island by a teleport (a command), OUT of it by a teleport: the one rule sees both on the next tick
        t.zet(p, t.op(6.5, 6.5));
        t.tick(p);
        helper.assertTrue(hond(p) && alleenKaart(p), "2: teleported in: a dog");
        t.zet(p, t.thuis(1));
        t.tick(p);
        helper.assertTrue(mens(p) && verschil(voor, p) == null, "2: teleported out: a player with everything: " + verschil(voor, p));

        // 3) logging out as a dog and in again on the island: still a dog, the snapshot still there, nothing doubled
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Hondvorm.zetHouding(p, Hondvorm.SNUFFELT);
        SnuffelEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
        helper.assertTrue(Hondvorm.actief(p) && SnuffelKluis.heeft(p) && alleenKaart(p) && Hondvorm.houding(p) == 0, "3: logged out: the dog and the safe stay in the player file");
        SnuffelEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        helper.assertTrue(hond(p) && alleenKaart(p), "3: logged in on the island: a dog");
        // 4) ... and logging in when the island is NOT where the player is any more (the dimension is gone, the server moved them)
        SnuffelEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
        p.snapTo(t.thuis(1).x, t.thuis(1).y + 3, t.thuis(1).z, 0f, 0f);
        SnuffelEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        helper.assertTrue(mens(p) && verschil(voor, p) == null, "4: logged in off the island: a player with everything: " + verschil(voor, p));
        helper.assertTrue(dicht(p.position(), t.thuis(1)), "4: ... and back where they left for the island from (they left from home 1 in step 2): " + p.position());

        // 5) a dog dies (/kill): nothing drops, nothing goes onto the dead body, the respawn gives everything back
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        p.getInventory().setItem(2, new ItemStack(Items.BRICKS, 5));     // (a builder's block in a dog's pockets)
        var bron = level.damageSources().genericKill();
        SnuffelEvents.onDeath(new LivingDeathEvent(p, bron));
        p.setHealth(0f);
        helper.assertTrue(leeg(p) && SnuffelKluis.heeft(p) && Hondvorm.stierfAlsHond(p), "5: dead: empty pockets, the safe shut");
        LivingDropsEvent drops = new LivingDropsEvent(p, bron, new ArrayList<>(), false);
        SnuffelEvents.onDrops(drops);
        helper.assertTrue(drops.isCanceled(), "5: a dead dog drops nothing");
        t.tick(p);
        helper.assertTrue(leeg(p) && SnuffelKluis.heeft(p), "5: the tick leaves a dead body alone");
        // 6) logging in again while still on the death screen: the safe stays shut
        SnuffelEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
        SnuffelEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        helper.assertTrue(leeg(p) && SnuffelKluis.heeft(p), "6: dead at login: nothing onto the dead body");
        // the respawn: alive, at the spawn point (off the island)
        p.setHealth(p.getMaxHealth());
        p.snapTo(t.thuis(0).x, t.thuis(0).y, t.thuis(0).z, 0f, 0f);
        SnuffelEvents.onRespawn(new PlayerEvent.PlayerRespawnEvent(p, false));
        helper.assertTrue(mens(p) && !Hondvorm.stierfAlsHond(p), "5: respawned: a player");
        int stenen = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(Items.BRICKS)) {
                stenen += p.getInventory().getItem(i).getCount();
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        helper.assertTrue(stenen == 5 && verschil(voor, p) == null, "5: everything back, the builder's bricks too: " + verschil(voor, p));

        // 7) a change of dimension that did not come through Reis (the event of another mod's teleport)
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        p.snapTo(t.thuis(1).x, t.thuis(1).y, t.thuis(1).z, 0f, 0f);
        SnuffelEvents.onDimension(new PlayerEvent.PlayerChangedDimensionEvent(p, Eiland.DIM, Level.OVERWORLD));
        helper.assertTrue(mens(p) && verschil(voor, p) == null, "7: another dimension: a player with everything: " + verschil(voor, p));

        // 8) a dog whose island disappears under it (a crash left "dog" in the file but the player elsewhere)
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Eiland.testWeg(t.plaats);
        t.tick(p);
        helper.assertTrue(mens(p) && verschil(voor, p) == null, "8: no island under a dog: a player with everything: " + verschil(voor, p));
        // 9) a memory card that got out anyway does not exist outside the island
        p.getInventory().setItem(22, new ItemStack(SnuffelFeature.SNUFFEL_GEHEUGENKAART.get()));
        Hondvorm.haalKaartWeg(p);
        helper.assertTrue(!heeftKaart(p) && verschil(voor, p) == null, "9: no memory card off the island");
        t.klaar();
        helper.succeed();
    }

    /** A spectator and a creative player are dogs on the island like everybody (their things are just as safe). */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelVormVoorIedereen(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0);
        vul(p);
        List<ItemStack> voor = foto(p);
        p.setGameMode(GameType.CREATIVE);
        t.zet(p, t.op(6.5, 6.5));
        t.tick(p);
        helper.assertTrue(hond(p), "a creative builder on the island is a dog too");
        p.setGameMode(GameType.SURVIVAL);
        t.zet(p, t.thuis(0));
        t.tick(p);
        helper.assertTrue(mens(p) && verschil(voor, p) == null, "and gets everything back: " + verschil(voor, p));
        // a player who never chose is the default dog; a chosen dog has its own size
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        helper.assertTrue(!Keuze.heeft(p) && "shiba".equals(Keuze.vanOfStandaard(p).ras()), "never chose: a red shiba");
        float shiba = p.getEyeHeight();
        helper.assertTrue(Keuze.zet(p, new Keuze("teckel", "zwart", "Worstje", "c")), "a teckel now");
        float teckel = p.getEyeHeight();
        helper.assertTrue(Keuze.zet(p, new Keuze("golden", "goud", "Zonnetje", "a")), "a golden now");
        float golden = p.getEyeHeight();
        helper.assertTrue(teckel < shiba && shiba < golden && golden < 0.85f, "the eyes are as high as the breed's: " + teckel + " " + shiba + " " + golden);
        helper.assertTrue(Hondvorm.SPEL.equals(Minigames.playing(p)) && Minigames.invulnerable(p), "a dog is in a game: one at a time, no /lobby");
        Reis.naarHuis(p);
        helper.assertTrue(Minigames.playing(p) == null, "at home no game runs");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // the rules
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelRegels(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        BlockPos vloer = BlockPos.containing(t.op(6.5, 6.5)).below();
        BreakBlockEvent breek = new BreakBlockEvent(level, vloer, level.getBlockState(vloer), p);
        SnuffelEvents.onBreak(breek);
        helper.assertTrue(breek.isCanceled(), "a dog breaks nothing");
        BreakBlockEvent thuis = new BreakBlockEvent(level, vloer, level.getBlockState(vloer), q);
        SnuffelEvents.onBreak(thuis);
        helper.assertTrue(!thuis.isCanceled(), "a player at home is not our business");
        // nothing hurts a dog (only /kill gets through); a player at home is not our business
        var pijn = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(level.damageSources().generic(), 6f));
        SnuffelEvents.onDamage(pijn);
        var verdrinkt = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(level.damageSources().drown(), 2f));
        SnuffelEvents.onDamage(verdrinkt);
        var kill = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(p,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(level.damageSources().genericKill(), 1000f));
        SnuffelEvents.onDamage(kill);
        var thuisPijn = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(q,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(level.damageSources().generic(), 6f));
        SnuffelEvents.onDamage(thuisPijn);
        helper.assertTrue(pijn.isCanceled() && verdrinkt.isCanceled() && !kill.isCanceled() && !thuisPijn.isCanceled(),
                "a dog is not hurt and does not drown; /kill gets through; a player at home is hurt as always");
        float leven = p.getHealth();
        p.hurtServer(level, level.damageSources().generic(), 6f);
        helper.assertTrue(p.getHealth() == leven, "the real damage path leaves a dog alone too: " + p.getHealth());
        // the memory card cannot be thrown away
        ItemStack kaart = p.getInventory().removeItemNoUpdate(Hondvorm.KAART_SLOT);
        ItemEntity los = new ItemEntity(level, p.getX(), p.getY(), p.getZ(), kaart);
        ItemTossEvent gooi = new ItemTossEvent(los, p);
        SnuffelEvents.onToss(gooi);
        helper.assertTrue(gooi.isCanceled() && alleenKaart(p), "a tossed memory card is back in its slot");
        // the blocks a dog may use are a tag; a chest is not in it, a door is
        helper.assertTrue(Blocks.OAK_DOOR.defaultBlockState().is(SnuffelEvents.BRUIKBAAR) && Blocks.BELL.defaultBlockState().is(SnuffelEvents.BRUIKBAAR)
                && !Blocks.CHEST.defaultBlockState().is(SnuffelEvents.BRUIKBAAR), "doors and bells can be used, chests cannot");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // the companion: only its own player
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelMaatjeAlleenVoorEigenSpeler(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer a = t.speler(0), b = t.speler(1);
        helper.assertTrue(Keuze.zet(a, new Keuze("corgi", "sable", "Bolle", "a")) && Keuze.zet(b, new Keuze("mops", "zwart", "Knor", "c")), "two choices");
        Reis.naarEiland(a, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarEiland(b, t.plaats, Reis.Aankomst.HAVEN);
        t.tick(a);
        t.tick(b);
        helper.assertTrue(Maatjes.van(a) == null && Maatjes.van(b) == null, "no companion before the story gives it");
        helper.assertTrue(Maatjes.geef(a) && !Maatjes.geef(a), "it appears once");
        t.tick(a);
        t.tick(b);
        MaatjeEntity ma = Maatjes.van(a);
        helper.assertTrue(ma != null && "a".equals(ma.soort()) && a.getUUID().equals(ma.eigenaar()), "A's companion floats, the variant A chose");
        helper.assertTrue(ma.broadcastToPlayer(a), "A is sent their own companion");
        helper.assertTrue(!ma.broadcastToPlayer(b), "B is NEVER sent A's companion");
        helper.assertTrue(Maatjes.van(b) == null, "B has none");
        Maatjes.geef(b);
        t.tick(b);
        MaatjeEntity mb = Maatjes.van(b);
        helper.assertTrue(mb != null && mb != ma && "c".equals(mb.soort()), "B's own companion, the variant B chose");
        helper.assertTrue(mb.broadcastToPlayer(b) && !mb.broadcastToPlayer(a) && !ma.broadcastToPlayer(b), "each only their own");
        helper.assertTrue(!ma.shouldBeSaved() && !ma.isPickable(), "it is never saved and cannot be clicked");
        // the naughty face, and happy again
        Maatjes.ondeugend(a, 40);
        helper.assertTrue(ma.ondeugend() && !mb.ondeugend(), "A's companion is up to something, B's is not");
        // another choice: the next tick brings the new variant
        Keuze.zet(a, new Keuze("corgi", "sable", "Bolle", "b"));
        t.tick(a);
        MaatjeEntity nieuw = Maatjes.van(a);
        helper.assertTrue(ma.isRemoved() && nieuw != null && "b".equals(nieuw.soort()), "the new choice floats now");
        // A goes home: the companion leaves the world, B's stays
        Reis.naarHuis(a);
        helper.assertTrue(nieuw.isRemoved() && Maatjes.van(a) == null && !mb.isRemoved(), "A is home: A's companion is gone, B's floats on");
        // ... and comes back with A
        Reis.naarEiland(a, t.plaats, Reis.Aankomst.LAATSTE);
        t.tick(a);
        helper.assertTrue(Maatjes.van(a) != null && Maatjes.heeft(a), "back on the island: there it is again");
        // a companion whose player logs out goes too
        MaatjeEntity laatste = Maatjes.van(a);
        SnuffelEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(a));
        helper.assertTrue(laatste.isRemoved(), "logged out: no companion left behind");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // ranks and the nose
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelRangen(GameTestHelper helper) {
        String makkelijk = geur("rang1"), moeilijk = geur("rang2");
        Geuren.registreer(makkelijk, GeurSoort.ETEN, 1, "minecraft:bone");
        Geuren.registreer(moeilijk, GeurSoort.VREEMD, 2, "minecraft:ender_pearl");
        Proef t = new Proef(helper, List.of(), List.of(new Eiland.BronPlek("b_" + makkelijk, makkelijk, new BlockPos(3, 1, 3), true, 20),
                new Eiland.BronPlek("b_" + moeilijk, moeilijk, new BlockPos(9, 1, 9), true, 20)));
        // the five ranks and their thresholds
        Rang[] r = Rang.values();
        helper.assertTrue(r.length == 5 && r[0].drempel() == 0 && r[1].drempel() == 20 && r[2].drempel() == 50 && r[3].drempel() == 100 && r[4].drempel() == 150,
                "five ranks at 0 / 20 / 50 / 100 / 150 scents");
        helper.assertTrue(Rang.van(0) == Rang.SNUFFELPUP && Rang.van(19) == Rang.SNUFFELPUP, "a Snuffelpup from the start");
        helper.assertTrue(Rang.HOOGSTE_NU == Rang.SNUFFELPUP && Rang.van(20) == Rang.SNUFFELPUP && Rang.van(500) == Rang.SNUFFELPUP,
                "only the first rank is reachable now");
        helper.assertTrue(r[0].bereikbaar() && !r[1].bereikbaar() && !r[4].bereikbaar(), "ranks 2..5 are locked");
        String regel = NlTekst.tekst(Rang.SNUFFELPUP.regel());
        helper.assertTrue("Snuffelpup (rang 1 (laagste) van 5 (hoogste))".equals(regel), "the rank line: " + regel);
        helper.assertTrue(NlTekst.tekst(r[4].regel()).startsWith("Opper-Snuffelmeester (rang 5"), "the highest rank's line: " + NlTekst.tekst(r[4].regel()));
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarEiland(q, t.plaats, Reis.Aankomst.STRAND);
        helper.assertTrue(Rang.van(p) == Rang.SNUFFELPUP && Geuren.aantal(p) == 0, "rank 1 with no scents");
        // the Guhdex shows it under the story: "Snuffelpup (rang 1 (laagste) van 5 (hoogste))" and the list of all five
        List<VerhaalStand.Beloning> rijen = SnuffelFeature.LIJN.stand(p).beloningen();
        List<String> teksten = rijen.stream().map(b -> NlTekst.tekst(b.tekst())).toList();
        helper.assertTrue(teksten.stream().anyMatch(s -> s.contains("Snuffelpup (rang 1 (laagste) van 5 (hoogste))")), "the Guhdex names the rank: " + teksten);
        for (Rang rang : r) {
            String naam = NlTekst.tekst(rang.naam());
            VerhaalStand.Beloning rij = rijen.stream().filter(b -> NlTekst.tekst(b.tekst()).startsWith(rang.nummer() + ". " + naam)).findFirst().orElse(null);
            helper.assertTrue(rij != null && rij.binnen() == (rang == Rang.SNUFFELPUP), "the Guhdex lists " + naam + ", ticked only when reached: " + teksten);
        }
        // the rank decides what the nose smells: the rank-2 scent is not in the air for a Snuffelpup
        t.zet(p, t.op(9.5, 9.5));
        Geurbronnen.Neus neus = Geurbronnen.ruik(p);
        helper.assertTrue(neus != null && neus.bron().geur().id().equals(makkelijk) && !neus.opDePlek(), "standing ON the rank-2 scent a Snuffelpup smells the rank-1 one far off");
        helper.assertTrue(Geurbronnen.graafbaarBij(p) == null, "and digs up nothing there");
        helper.assertTrue(Geuren.ruikt(p, Geuren.van(makkelijk)) && !Geuren.ruikt(p, Geuren.van(moeilijk)), "rank 1 smells rank-1 scents only");
        // learned scents are per player
        helper.assertTrue(Geuren.leer(p, makkelijk) && !Geuren.leer(p, makkelijk) && Geuren.kent(p, makkelijk) && Geuren.aantal(p) == 1, "learned once");
        helper.assertTrue(!Geuren.kent(q, makkelijk) && Geuren.aantal(q) == 0, "the other dog's snuffelboekje is still empty");
        helper.assertTrue(!Geuren.leer(p, "snuffeltest_bestaat_niet"), "an unknown scent is not learned");
        CompoundTag stand = Stand.van(p);
        helper.assertTrue(stand.getIntOr("Rang", 0) == 1 && stand.getIntOr("Aantal", 0) == 1 && stand.getListOrEmpty("Geuren").size() == 1
                && stand.getListOrEmpty("Geuren").getCompoundOrEmpty(0).getIntOr("Soort", -1) == GeurSoort.ETEN.ordinal(), "the client gets the rank and the snuffelboekje");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // sniffing and digging
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void snuffelSnuffelenEnGraven(GameTestHelper helper) {
        String bot = geur("bot"), bal = geur("bal"), lucht = geur("lucht"), dier = geur("dier");
        Geuren.registreer(bot, GeurSoort.ETEN, 1, "minecraft:bone");
        Geuren.registreer(bal, GeurSoort.VOORWERP, 1, "minecraft:slime_ball");
        Geuren.registreer(lucht, GeurSoort.VREEMD, 1, "minecraft:feather");
        Geuren.registreer(dier, GeurSoort.DIER, 1, "minecraft:egg");
        Proef t = new Proef(helper, List.of(), List.of(new Eiland.BronPlek("b_" + bot, bot, new BlockPos(2, 1, 2), true, 30),
                new Eiland.BronPlek("b_" + bal, bal, new BlockPos(10, 1, 2), true, 6), new Eiland.BronPlek("b_" + lucht, lucht, new BlockPos(10, 1, 10), false, 6)));
        Geurbronnen.voorwaarde("b_" + bal, x -> SnuffelData.van(x).getBooleanOr("TestBalMag", false));
        List<String> gevonden = new ArrayList<>();
        Geurbronnen.bijVondst("b_" + bot, x -> gevonden.add("bot:" + x.getUUID()));
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarEiland(q, t.plaats, Reis.Aankomst.STRAND);
        // the nose: stronger the closer, the kind is the scent's kind
        t.zet(p, t.op(8.5, 8.5));
        Geurbronnen.Neus ver = Geurbronnen.ruik(p);
        t.zet(p, t.op(4.5, 4.5));
        Geurbronnen.Neus dichtbij = Geurbronnen.ruik(p);
        helper.assertTrue(ver != null && dichtbij != null && ver.bron().geur().id().equals(bot) && dichtbij.sterkte() > ver.sterkte() && ver.sterkte() > 0
                && !dichtbij.opDePlek() && ver.bron().geur().soort() == GeurSoort.ETEN, "the meter swings harder closer to the bone: " + ver + " / " + dichtbij);
        // a source the story has not opened is not in the air
        t.zet(p, t.op(10.5, 2.5));
        helper.assertTrue(Geurbronnen.graafbaarBij(p) == null, "the ball is not there for a player the story has not asked");
        SnuffelData.van(p).putBoolean("TestBalMag", true);
        helper.assertTrue(Geurbronnen.graafbaarBij(p) != null && Geurbronnen.ruik(p).opDePlek(), "now it is, and the dog stands on it");
        // digging takes its time, and a dog in the air cannot dig
        p.setOnGround(false);
        helper.assertTrue(!Snuffelen.graaf(p), "no digging in the air");
        p.setOnGround(true);
        helper.assertTrue(Snuffelen.graaf(p) && Snuffelen.graaft(p) && !Snuffelen.graaf(p), "digging (once at a time)");
        helper.assertTrue(!Geuren.kent(p, bal), "not found before the hole is dug");
        Snuffelen.graafNu(p);
        helper.assertTrue(Geuren.kent(p, bal) && Geurbronnen.gevonden(p, "b_" + bal) && !Snuffelen.graaft(p), "dug up: the scent is learned");
        helper.assertTrue(Geurbronnen.graafbaarBij(p) == null, "what you found is gone for your nose");
        Geurbronnen.vergeet(p, "b_" + bal);
        helper.assertTrue(Geurbronnen.graafbaarBij(p) != null && Geuren.kent(p, bal), "until the story hides it again (the scent stays learned)");
        // digging where nothing lies finds nothing
        t.zet(p, t.op(6.5, 6.5));
        Snuffelen.graafNu(p);
        Snuffelen.vergeet(p.getUUID());
        Snuffelen.graaf(p);
        Snuffelen.graafNu(p);
        helper.assertTrue(Geuren.aantal(p) == 1, "an empty hole teaches nothing");
        // the bone: per player, with what the story hangs on it
        t.zet(p, t.op(2.5, 2.5));
        Snuffelen.vergeet(p.getUUID());
        Snuffelen.graaf(p);
        Snuffelen.graafNu(p);
        helper.assertTrue(Geuren.kent(p, bot) && gevonden.equals(List.of("bot:" + p.getUUID())), "the bone is found, the story hears it once: " + gevonden);
        t.zet(q, t.op(2.5, 2.5));
        helper.assertTrue(Geurbronnen.graafbaarBij(q) != null && !Geuren.kent(q, bot), "the other dog can still find the same bone");
        // a scent on an entity
        ArmorStand kip = EntityType.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 bij = t.op(4.5, 10.5);
        kip.snapTo(bij.x, bij.y, bij.z, 0f, 0f);
        helper.getLevel().addFreshEntity(kip);
        Geurbronnen.opEntiteit(kip, dier, null);
        t.zet(q, t.op(5.5, 10.5));
        Geurbronnen.Neus beest = Geurbronnen.ruik(q);
        helper.assertTrue(beest != null && beest.bron().entiteit() == kip && beest.opDePlek() && !beest.bron().graven(), "an entity carries a scent: " + beest);
        // a source that is not buried: sniff next to it for a second (the real tick, with the sniff pose held)
        t.zet(p, t.op(10.5, 10.5));
        Hondvorm.zetHouding(p, Hondvorm.SNUFFELT);
        Hondvorm.zetHouding(q, Hondvorm.SNUFFELT);
        helper.assertTrue(Hondvorm.snuffelt(p) && Hondvorm.houding(p) == Hondvorm.SNUFFELT, "nose to the ground");
        helper.onEachTick(() -> {
            if (!p.isRemoved()) {
                Snuffelen.tick(p);
                Snuffelen.tick(q);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(Geuren.kent(p, lucht), "a long sniff next to it teaches the scent that is not buried");
            helper.assertTrue(Geuren.kent(q, dier), "and the scent of the entity");
            helper.assertTrue(Geurbronnen.gevonden(p, "b_" + lucht) && !Geurbronnen.gevonden(q, "b_" + lucht), "found by the one who sniffed there");
            kip.discard();
            t.klaar();
        });
    }

    // =====================================================================================================================
    // the tree, good deeds, the growth scene
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 300)
    public static void snuffelBoomEnDaden(GameTestHelper helper) {
        String wol = geur("wol");
        Geuren.registreer(wol, GeurSoort.VOORWERP, 1, "minecraft:white_wool");
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarEiland(q, t.plaats, Reis.Aankomst.HAVEN);
        helper.assertTrue(Boom.stap(p) == 0 && Boom.stap(q) == 0 && Stand.van(p).getIntOr("Boom", -1) == 0, "no tree yet: only the ring of stones");
        List<String> gehoord = new ArrayList<>();
        // a good deed: once, with its scent, and the tree grows a step with the scene
        helper.assertTrue(Daden.geef(p, "snuffeltest_wol", wol, x -> gehoord.add("na1")), "the first good deed");
        helper.assertTrue(Daden.heeft(p, "snuffeltest_wol") && Daden.aantal(p) == 1 && Geuren.kent(p, wol) && Boom.stap(p) == 1, "it counts, teaches its scent, the tree is a kiem");
        helper.assertTrue(Cutscenes.bezig(p), "the growth scene plays");
        helper.assertTrue(!Daden.geef(p, "snuffeltest_wol", wol) && Daden.aantal(p) == 1 && Boom.stap(p) == 1, "the same deed never counts twice");
        helper.assertTrue(Boom.stap(q) == 0 && Daden.aantal(q) == 0 && !Cutscenes.bezig(q), "the other dog's tree did not grow and they watch nothing");
        helper.assertTrue(Boom.GROEI.duur() == 120, "the scene lasts six seconds");
        // while the scene plays a second step is kept all the same (no scene twice at once: daarna runs at once)
        helper.assertTrue(Boom.groei(p, x -> gehoord.add("na2")) && Boom.stap(p) == 2 && gehoord.contains("na2"), "a step during a scene is kept, its follow-up runs");
        Boom.zet(p, 3);
        helper.assertTrue(Daden.geef(p, "snuffeltest_bel", null) && Boom.stap(p) == 4, "the fourth step: a jong boompje");
        helper.assertTrue(Daden.geef(p, "snuffeltest_bal", null) && Boom.stap(p) == Boom.MAX && Daden.aantal(p) == 3 && !Boom.groei(p, null),
                "a deed more finds the tree full-grown and simply counts");
        CompoundTag stand = Stand.van(p);
        helper.assertTrue(stand.getIntOr("Boom", 0) == 4 && stand.getListOrEmpty("Daden").size() == 3, "the client gets the stage and the deeds");
        helper.assertTrue(Boom.geefCadeau(p) && !Boom.geefCadeau(p) && SnuffelKluis.postAantal(p) == 1, "the tree's gift, once, in the post of a dog");
        // the island keeps exactly one tree on its spot (a hasty double is taken away)
        BlockPos voet = t.plaats.boom();
        helper.onEachTick(() -> {
            if (!p.isRemoved()) {
                t.tick(p);
                Eiland.bewoon(t.plaats);
            }
        });
        helper.succeedWhen(() -> {
            List<BoompjeEntity> bomen = helper.getLevel().getEntitiesOfClass(BoompjeEntity.class, new AABB(voet).inflate(8));
            helper.assertTrue(bomen.size() == 1 && bomen.get(0).position().distanceToSqr(Vec3.atBottomCenterOf(voet)) < 0.01, "one tree, on its spot: " + bomen.size());
            helper.assertTrue(Eiland.boomEntity(t.plaats) == bomen.get(0), "the island knows its tree");
            helper.assertTrue(!Cutscenes.bezig(p) && gehoord.contains("na1"), "the scene is over and its follow-up ran: " + gehoord);
            t.klaar();
        });
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 300)
    public static void snuffelBewonersBlijvenOpHunPlek(GameTestHelper helper) {
        Proef t = new Proef(helper, List.of(new Eiland.BewonerPlek("dokter", "dokter", "", "", false, new Vec3(3.5, 1, 3.5), 90f, "zit"),
                new Eiland.BewonerPlek("buurpup", "", "corgi", "sable", true, new Vec3(9.5, 1, 4.5), 0f, "")), List.of());
        ServerPlayer p = t.speler(0);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        List<String> gepraat = new ArrayList<>();
        Bewoners.zetRol("dokter", (npc, wie) -> gepraat.add(npc.bewoner() + ":" + wie.getUUID()));
        int[] fase = {0};
        helper.onEachTick(() -> {
            if (!p.isRemoved()) {
                Eiland.bewoon(t.plaats);
            }
        });
        helper.succeedWhen(() -> {
            BewonerEntity dokter = Eiland.bewoner(t.plaats, "dokter"), pup = Eiland.bewoner(t.plaats, "buurpup");
            helper.assertTrue(dokter != null && pup != null, "both residents came");
            AABB doos = helper.getBounds().inflate(4);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(BewonerEntity.class, doos).size() == 2, "exactly one of each");
            if (fase[0] == 0) {
                helper.assertTrue("dokter".equals(dokter.bewoner()) && "teckel".equals(dokter.ras()) && !dokter.pup() && dokter.houding() == Hondvorm.ZIT
                        && dokter.hasCustomName(), "the doctor: the generator's teckel, sitting, with his name");
                helper.assertTrue(pup.bewoner().isEmpty() && "corgi".equals(pup.ras()) && "sable".equals(pup.kleur()) && pup.pup()
                        && pup.getBbHeight() < dokter.getBbHeight() + 0.2f, "any dog: breed + coat + puppy flag");
                helper.assertTrue(dicht(dokter.position(), t.plaats.wereld(new Vec3(3.5, 1, 3.5))), "on his spot");
                helper.assertTrue(dokter.isInvulnerableTo(helper.getLevel(), helper.getLevel().damageSources().generic()) && !dokter.isPushable(), "nothing hurts or pushes him");
                // a right-click goes to his role; the puppy has none and just greets
                Bewoners.klik(dokter, p);
                Bewoners.klik(pup, p);
                helper.assertTrue(gepraat.equals(List.of("dokter:" + p.getUUID())), "the role hears the click: " + gepraat);
                // one walks off the island, one gets a double: the island puts it right
                dokter.discard();
                BewonerEntity dubbel = Bewoners.plaatsHond(helper.getLevel(), pup.position().add(1, 0, 0), 0f, "corgi", "sable", true);
                Eiland.merk(dubbel, t.plaats, "buurpup");
                fase[0] = 1;
                helper.assertTrue(false, "(wait for the island to put its residents right)");
            }
            t.klaar();
        });
    }

    // =====================================================================================================================
    // exams
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelExamen(GameTestHelper helper) {
        String een = geur("ex1"), twee = geur("ex2");
        Geuren.registreer(een, GeurSoort.ETEN, 1, "minecraft:bone");
        Geuren.registreer(twee, GeurSoort.DIER, 1, "minecraft:feather");
        String examen = "snuffeltest_examen_" + NR.incrementAndGet();
        Proef t = new Proef(helper, List.of(), List.of(new Eiland.BronPlek("b_" + een, een, new BlockPos(2, 1, 2), true, 30),
                new Eiland.BronPlek("b_" + twee, twee, new BlockPos(10, 1, 10), true, 30)));
        List<String> geslaagd = new ArrayList<>();
        Examen.registreer(examen, List.of("b_" + een, "b_" + twee), x -> geslaagd.add(x.getUUID().toString()));
        ServerPlayer p = t.speler(0), q = t.speler(1);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarEiland(q, t.plaats, Reis.Aankomst.STRAND);
        helper.assertTrue(Geurbronnen.ruik(p) == null && Examen.bezig(p) == null, "the exam's scents are not in the air before the exam");
        helper.assertTrue(!Examen.start(p, "snuffeltest_bestaat_niet") && Examen.start(p, examen), "the exam begins");
        helper.assertTrue(Examen.bezig(p) != null && Examen.bezig(p).gevonden() == 0 && Geurbronnen.ruik(p) != null && Geurbronnen.ruik(q) == null,
                "now the examinee smells them, the other dog does not");
        helper.assertTrue(Stand.van(p).getCompoundOrEmpty("Examen").getIntOr("Totaal", 0) == 2, "the screen shows 0 / 2");
        t.zet(p, t.op(2.5, 2.5));
        Snuffelen.graaf(p);
        Snuffelen.graafNu(p);
        helper.assertTrue(Examen.bezig(p) != null && Examen.bezig(p).gevonden() == 1 && geslaagd.isEmpty() && !Examen.gehaald(p, examen), "one of two");
        t.zet(p, t.op(10.5, 10.5));
        Snuffelen.vergeet(p.getUUID());
        Snuffelen.graaf(p);
        Snuffelen.graafNu(p);
        helper.assertTrue(Examen.bezig(p) == null && Examen.gehaald(p, examen) && geslaagd.equals(List.of(p.getUUID().toString())), "passed, the story hears it once");
        helper.assertTrue(!Examen.gehaald(q, examen), "the other dog did not pass");
        Snuffel.geefDiploma(p);
        helper.assertTrue(Snuffel.heeftDiploma(p) && !Snuffel.heeftDiploma(q) && Stand.van(p).getBooleanOr("Diploma", false), "the diploma");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // travel: where you arrive, exactly home, the Guhstation (alone in their batch: they use "the island of this server")
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = ALLEEN)
    public static void snuffelReisPosities(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = t.speler(0);
        Vec3 thuis = t.thuis(0);
        helper.assertTrue(Eiland.plaats(level.getServer()) != null && Eiland.plaats(level.getServer()).zelfde(t.plaats), "this test's island is the server's");
        helper.assertTrue(!Reis.bezocht(p) && Reis.thuis(p) == null && Reis.laatste(p) == null, "never been there");
        helper.assertTrue(!Reis.naarHuis(p), "nothing to go home from");
        // the first crossing: washed ashore on the beach
        helper.assertTrue(Snuffel.spoelAan(p), "the dock's captain sails out");
        helper.assertTrue(dicht(p.position(), t.plaats.strand()) && Math.abs(p.getYRot() - 180f) < 0.01f && hond(p), "on the beach, looking at the island: " + p.position());
        Reis.Thuis opgeslagen = Reis.thuis(p);
        helper.assertTrue(opgeslagen != null && opgeslagen.dim() == level.dimension() && dicht(opgeslagen.plek(), thuis) && opgeslagen.yaw() == 33f
                && opgeslagen.pitch() == -12f, "home is stored exactly: " + opgeslagen);
        helper.assertTrue(Reis.weigering(p) != null && !Reis.naarEiland(p), "a dog cannot leave for the island again");
        // a walk over the island; the memory card brings you home EXACTLY, and the island spot is remembered
        Vec3 plekje = t.op(3.25, 8.75);
        t.zet(p, plekje);
        p.setYRot(77f);
        helper.assertTrue(Reis.naarHuis(p), "home");
        helper.assertTrue(dicht(p.position(), thuis) && Math.abs(p.getYRot() - 33f) < 0.01f && Math.abs(p.getXRot() + 12f) < 0.01f && mens(p),
                "exactly where the player left, looking the same way: " + p.position() + " " + p.getYRot() + " " + p.getXRot());
        Vec3 laatste = Reis.laatste(p);
        helper.assertTrue(laatste != null && dicht(t.plaats.wereld(laatste), plekje), "the last spot on the island is remembered: " + laatste);
        // the Guhstation: back to that last spot
        helper.assertTrue(Reis.naarEiland(p) && dicht(p.position(), plekje) && Math.abs(p.getYRot() - 77f) < 0.01f && hond(p), "back on the last spot: " + p.position());
        Reis.naarHuis(p);
        // a later crossing with the captain: the harbour
        helper.assertTrue(Snuffel.vaarNaarEiland(p) && dicht(p.position(), t.plaats.haven()) && Math.abs(p.getYRot() - 90f) < 0.01f, "the boat lands in the harbour");
        // home from another home: every trip stores where it started
        Reis.naarHuis(p);
        t.zet(p, t.thuis(1));
        p.setYRot(-140f);
        p.setXRot(20f);
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Reis.naarHuis(p);
        helper.assertTrue(dicht(p.position(), t.thuis(1)) && Math.abs(p.getYRot() + 140f) < 0.01f && Math.abs(p.getXRot() - 20f) < 0.01f, "the new home, exactly: " + p.position());
        // somebody built on the spot while the player was away: a little higher, never inside a block
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        BlockPos gebouwd = BlockPos.containing(t.thuis(1));
        level.setBlockAndUpdate(gebouwd, Blocks.STONE.defaultBlockState());
        Reis.naarHuis(p);
        helper.assertTrue(dicht(p.position(), t.thuis(1).add(0, 1, 0)), "on top of what was built there: " + p.position());
        level.setBlockAndUpdate(gebouwd, Blocks.AIR.defaultBlockState());
        // a last spot that is no place to stand any more: the beach
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Vec3 muur = t.op(8.5, 3.5);
        t.zet(p, muur);
        Reis.naarHuis(p);
        level.setBlockAndUpdate(BlockPos.containing(muur), Blocks.STONE.defaultBlockState());
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.LAATSTE);
        helper.assertTrue(dicht(p.position(), t.plaats.strand()), "the last spot is blocked: the beach instead: " + p.position());
        level.setBlockAndUpdate(BlockPos.containing(muur), Blocks.AIR.defaultBlockState());
        Reis.naarHuis(p);
        // a home that lies on an island is no home (the way in by a teleport ON the island stores nothing useful)
        t.klaar();
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = STATION)
    public static void snuffelGuhstation(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer p = t.speler(0);
        BlockPos kastje = BlockPos.containing(t.thuis(0)).offset(1, 0, 0);
        level.setBlockAndUpdate(kastje, SnuffelFeature.GUHSTATION.get().defaultBlockState());
        helper.assertTrue(!GuhstationBlock.open(p, kastje) && !GuhstationBlock.wachtOpStart(p), "somebody who never was on the island gets a black screen");
        helper.assertTrue(Keuze.zet(p, new Keuze("jackrussell", "driekleur", "Stuiter", "b")), "a choice");
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        Vec3 plekje = t.op(9.5, 9.5);
        t.zet(p, plekje);
        helper.assertTrue(!GuhstationBlock.open(p, kastje), "a dog has no use for a Guhstation");
        Reis.naarHuis(p);
        helper.assertTrue(!GuhstationBlock.start(p), "start without the window does nothing");
        helper.assertTrue(GuhstationBlock.open(p, kastje) && GuhstationBlock.wachtOpStart(p), "the window opens");
        helper.assertTrue(GuhstationBlock.start(p) && hond(p) && dicht(p.position(), plekje), "start: the last spot on the island, a dog: " + p.position());
        helper.assertTrue(!GuhstationBlock.start(p), "one start per window");
        Reis.naarHuis(p);
        // the window was opened, but the player walked away / the box is gone: start does nothing
        GuhstationBlock.open(p, kastje);
        t.zet(p, t.thuis(1));
        helper.assertTrue(!GuhstationBlock.start(p) && mens(p), "too far from the Guhstation");
        t.zet(p, t.thuis(0));
        GuhstationBlock.open(p, kastje);
        level.setBlockAndUpdate(kastje, Blocks.AIR.defaultBlockState());
        helper.assertTrue(!GuhstationBlock.start(p) && mens(p), "the Guhstation is gone");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // the choice, the questline's end
    // =====================================================================================================================

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelKeuze(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0);
        helper.assertTrue(!Keuze.heeft(p) && Keuze.van(p) == null, "no choice yet");
        helper.assertTrue(!Keuze.zet(p, new Keuze("speurhond", "bruin", "Neus", "a")), "the trainer's breed is not for players");
        helper.assertTrue(!Keuze.zet(p, new Keuze("shiba", "goud", "Neus", "a")), "a coat of another breed");
        helper.assertTrue(!Keuze.zet(p, new Keuze("shiba", "rood", "Neus", "d")), "there are three companions");
        helper.assertTrue(!Keuze.heeft(p), "nothing was stored");
        // a client that sends a choice by itself is ignored; after the server opened the screen one answer is taken
        List<String> gehoord = new ArrayList<>();
        Keuze.opKeuze((wie, k) -> {
            if (wie == p) {
                gehoord.add(k.ras() + "/" + k.maatje());
            }
        });
        helper.assertTrue(!Keuze.ontvang(p, new Keuze("mops", "abrikoos", "Knor", "c")) && !Keuze.heeft(p), "not asked: ignored");
        Keuze.open(p);
        helper.assertTrue(Keuze.magKiezen(p) && Keuze.ontvang(p, new Keuze("mops", "abrikoos", "  Kn§cor   de   Mops van heel ver weg  ", "c")), "asked: taken");
        Keuze k = Keuze.van(p);
        helper.assertTrue(k != null && "mops".equals(k.ras()) && "abrikoos".equals(k.kleur()) && "c".equals(k.maatje()), "stored: " + k);
        helper.assertTrue(k.naam().length() <= Keuze.NAAM_MAX && k.naam().startsWith("Kncor de Mops") && !k.naam().contains("§"), "the name is cleaned: '" + k.naam() + "'");
        helper.assertTrue(gehoord.equals(List.of("mops/c")), "whoever listens hears it: " + gehoord);
        helper.assertTrue(!Keuze.magKiezen(p) && !Keuze.ontvang(p, new Keuze("corgi", "rood", "Bolle", "a")) && "mops".equals(Keuze.van(p).ras()), "one answer per screen");
        // an empty name becomes the player's own
        Keuze.zet(p, new Keuze("corgi", "rood", "   ", "a"));
        helper.assertTrue(Keuze.van(p).naam().equals(Keuze.netjes(p.getGameProfile().name(), "x")), "no name: the player's: " + Keuze.van(p).naam());
        // everybody gets told which dog to draw (the message for the others)
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        SnuffelPayloads.Vorm v = Hondvorm.bericht(p);
        helper.assertTrue(v.actief() && v.speler().equals(p.getUUID()) && "corgi".equals(v.ras()) && "rood".equals(v.kleur()), "the look that is sent: " + v);
        Reis.naarHuis(p);
        helper.assertTrue(!Hondvorm.bericht(p).actief(), "and no dog any more at home");
        t.klaar();
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = BATCH)
    public static void snuffelVerhaallijnEnAfronden(GameTestHelper helper) {
        Proef t = new Proef(helper);
        ServerPlayer p = t.speler(0), q = t.speler(1);
        var lijn = SnuffelFeature.LIJN;
        helper.assertTrue("snuffeleiland".equals(lijn.id()) && "snuffeleiland".equals(lijn.groep()) && lijn.stappen() == SnuffelFeature.STAPPEN
                && SnuffelFeature.STAPPEN == 9, "the questline snuffeleiland: nine steps");
        helper.assertTrue(lijn.stap(p) == 0 && !lijn.klaar(p) && !Snuffel.klaar(p), "not begun");
        // the frame: a slice moves a player on from exactly the step it owns
        lijn.begin(p);
        helper.assertTrue(lijn.verder(p, SnuffelFeature.STAP_STEIGER) && !lijn.verder(p, SnuffelFeature.STAP_STEIGER) && lijn.stap(p) == SnuffelFeature.STAP_UITVAREN,
                "a step forward, once");
        lijn.zet(p, SnuffelFeature.STAP_DADEN);
        Daden.geef(p, "snuffeltest_a", null);
        VerhaalStand stand = lijn.stand(p);
        helper.assertTrue(stand.nodig().size() == 1 && stand.nodig().get(0).heb() == 1 && stand.nodig().get(0).nodig() == SnuffelFeature.DADEN_NODIG,
                "the Guhdex counts the good deeds: " + stand.nodig());
        // where "Mijn verhaal" points is what a slice registers for its steps
        net.minecraft.core.BlockPos doelPlek = BlockPos.containing(t.op(6.5, 6.5));
        Snuffel.doel(SnuffelFeature.STAP_DADEN, SnuffelFeature.STAP_DADEN,
                (wie, stap) -> wie == p ? nl.juiced.guhs.feature.verhaal.Doel.plek(helper.getLevel().dimension(), doelPlek, Component.literal("proef")) : null);
        helper.assertTrue(lijn.doel(p) != null && doelPlek.equals(lijn.doel(p).plek()), "the goal of the step");
        // the end of the first series, as a dog: the story is done, the gifts travel home
        Reis.naarEiland(p, t.plaats, Reis.Aankomst.STRAND);
        helper.assertTrue(Snuffel.rondAf(p) && lijn.klaar(p) && Snuffel.klaar(p), "the first series is finished: this is what the Guhpad asks");
        helper.assertTrue(alleenKaart(p) && SnuffelKluis.postAantal(p) == 3 && Snuffel.heeftPlaatGehad(p),
                "a dog's pockets stay empty: the Guhstation, the music disc and the twig are in the post");
        helper.assertTrue(!Snuffel.rondAf(p) && !Snuffel.geefPlaat(p) && SnuffelKluis.postAantal(p) == 3, "finishing again gives nothing more");
        Reis.naarHuis(p);
        Inventory inv = p.getInventory();
        helper.assertTrue(inv.countItem(SnuffelFeature.GUHSTATION_ITEM.get()) == 1 && inv.countItem(SnuffelFeature.SNUFFEL_BLOESEMTAKJE.get()) == 1
                && inv.countItem(SnuffelFeature.MUZIEKPLAAT.get()) == 1, "at home: one Guhstation, one music disc and the blossom twig");
        var gaven = lijn.stand(p).beloningen();
        helper.assertTrue(lijn.stand(p).klaar() && gaven.get(0).binnen() && gaven.get(1).binnen() && gaven.get(2).binnen()
                && "guhs:music_disc_snuffeleiland".equals(gaven.get(1).item()), "the Guhdex ticks the three gifts, the disc next to the Guhstation");
        helper.assertTrue(lijn.stand(q).beloningen().stream().noneMatch(b -> b.binnen()), "nothing ticked for somebody else");
        // the disc is a real music disc of the Disco-dynamo's rare kind
        ItemStack plaat = new ItemStack(SnuffelFeature.MUZIEKPLAAT.get());
        helper.assertTrue(plaat.has(net.minecraft.core.component.DataComponents.JUKEBOX_PLAYABLE)
                && plaat.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, nl.juiced.guhs.Guhs.id("techbron/zeldzame_plaat")))
                && helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.JUKEBOX_SONG)
                        .getOptional(nl.juiced.guhs.Guhs.id("snuffeleiland"))
                        .filter(s -> s.soundEvent().value() == SnuffelFeature.MUZIEK.get() && s.lengthInSeconds() >= 60f).isPresent(),
                "the disc plays the island tune in a jukebox and counts as a rare disc");
        helper.assertTrue(Snuffel.geefGuhstation(p, true) && inv.countItem(SnuffelFeature.GUHSTATION_ITEM.get()) == 2, "a lost Guhstation can be given again");
        helper.assertTrue(!lijn.klaar(q) && !Snuffel.heeftGuhstationGehad(q) && lijn.stap(q) == 0, "everything per player");
        // a player at home gets things at once
        Snuffel.geef(q, new ItemStack(Items.COOKIE, 4));
        helper.assertTrue(q.getInventory().countItem(Items.COOKIE) == 4 && !SnuffelKluis.heeftPost(q), "no post for a player");
        t.klaar();
        helper.succeed();
    }

    // =====================================================================================================================
    // the data and the jar
    // =====================================================================================================================

    private static boolean bestaat(String pad) {
        try (InputStream in = Guhs.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static JsonObject lees(String pad) throws Exception {
        try (InputStream in = Guhs.class.getResourceAsStream(pad)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Every dog, companion and tree stage that the data names has its model, its animations (all of them) and its texture. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void snuffelModellenEnTeksten(GameTestHelper helper) throws Exception {
        List<String> mis = new ArrayList<>();
        List<Honden.Ras> speelbaar = Honden.speelbaar();
        helper.assertTrue(speelbaar.size() == 6 && Honden.rassen().size() == 7 && Honden.ras("speurhond") != null && !Honden.ras("speurhond").speelbaar(),
                "six breeds to choose from, a seventh for the trainer");
        helper.assertTrue(Honden.bewoners().size() == 11 && Honden.bewoner("dokter") != null && Honden.bewoner("pup").pup(), "eleven named residents");
        List<String[]> honden = new ArrayList<>();   // {model, texture}
        for (Honden.Ras r : Honden.rassen()) {
            helper.assertTrue(r.kleuren().size() == (r.speelbaar() ? 3 : 1), r.id() + " has its coats");
            helper.assertTrue(r.oog() < r.hoogte() && r.hoogte() <= 0.9f && r.pupOog() <= r.oog(), r.id() + ": a dog's size");
            for (String kleur : r.kleuren()) {
                honden.add(new String[] {Honden.model("", r.id(), false), Honden.textuur("", r.id(), kleur, false)});
                honden.add(new String[] {Honden.model("", r.id(), true), Honden.textuur("", r.id(), kleur, true)});
                if (!NlTekst.has("gui.guhs.snuffel.kleur." + r.id() + "." + kleur)) {
                    mis.add("coat name " + r.id() + "." + kleur);
                }
            }
            if (!NlTekst.has("gui.guhs.snuffel.ras." + r.id())) {
                mis.add("breed name " + r.id());
            }
        }
        for (Honden.Bewoner b : Honden.bewoners()) {
            honden.add(new String[] {Honden.model(b.id(), b.ras(), b.pup()), Honden.textuur(b.id(), b.ras(), b.kleur(), b.pup())});
            helper.assertTrue(Honden.bestaat(b.ras(), b.kleur()), b.id() + " is a known breed and coat");
            if (!NlTekst.has("entity.guhs.snuffel_bewoner." + b.id())) {
                mis.add("resident name " + b.id());
            }
        }
        for (String[] h : honden) {
            if (!bestaat("/assets/guhs/geckolib/models/entity/" + h[0] + ".geo.json")) {
                mis.add("model " + h[0]);
            }
            if (!bestaat("/assets/guhs/textures/entity/" + h[1] + ".png")) {
                mis.add("texture " + h[1]);
            }
            String anim = "/assets/guhs/geckolib/animations/entity/" + h[0] + ".animation.json";
            if (!bestaat(anim)) {
                mis.add("animations " + h[0]);
            } else {
                JsonObject a = lees(anim).getAsJsonObject("animations");
                for (String naam : SnuffelHond.ANIMATIES) {
                    if (!a.has(naam)) {
                        mis.add(h[0] + " has no animation " + naam);
                    }
                }
            }
        }
        for (String m : Honden.MAATJES) {
            for (String gezicht : List.of("blij", "ondeugend")) {
                String naam = "snuffel_maatje_" + m + "_" + gezicht;
                if (!bestaat("/assets/guhs/geckolib/models/entity/" + naam + ".geo.json") || !bestaat("/assets/guhs/textures/entity/" + naam + ".png")) {
                    mis.add("companion " + naam);
                } else {
                    JsonObject a = lees("/assets/guhs/geckolib/animations/entity/" + naam + ".animation.json").getAsJsonObject("animations");
                    for (String an : List.of("idle", "blij", "ondeugend", "wijs")) {
                        if (!a.has(an)) {
                            mis.add(naam + " has no animation " + an);
                        }
                    }
                }
            }
            if (!NlTekst.has("gui.guhs.snuffel.maatje." + m) || !NlTekst.has("gui.guhs.snuffel.maatje." + m + ".tekst")) {
                mis.add("companion text " + m);
            }
        }
        for (int stap = 1; stap <= Boom.MAX; stap++) {
            String naam = "snuffel_boompje_" + stap;
            if (!bestaat("/assets/guhs/geckolib/models/entity/" + naam + ".geo.json") || !bestaat("/assets/guhs/textures/entity/" + naam + ".png")
                    || !lees("/assets/guhs/geckolib/animations/entity/" + naam + ".animation.json").getAsJsonObject("animations").has("groei")) {
                mis.add("tree " + naam);
            }
        }
        for (int stap = 0; stap <= Boom.MAX; stap++) {
            if (!NlTekst.has("gui.guhs.snuffel.boom.stap." + stap) || stap > 0 && !NlTekst.has("gui.guhs.snuffel.boom.groeit." + stap)) {
                mis.add("tree text " + stap);
            }
        }
        for (Rang r : Rang.values()) {
            if (!NlTekst.has("gui.guhs.snuffel.rang." + r.id())) {
                mis.add("rank " + r.id());
            }
        }
        for (GeurSoort s : GeurSoort.values()) {
            if (!NlTekst.has("gui.guhs.snuffel.soort." + s.id())) {
                mis.add("kind " + s.id());
            }
        }
        // the island of this jar: it parses, every scent it names is registered and has a name, every resident is a known dog
        Eiland.Opzet opzet = Eiland.lees(lees(Eiland.PAD));
        helper.assertTrue(opzet.versie() >= 1 && !opzet.stukken().isEmpty() && opzet.maat().getX() > 0, "the island's data");
        for (Eiland.GeurDef g : opzet.geuren()) {
            if (Geuren.van(g.id()) == null || !NlTekst.has("gui.guhs.snuffel.geur." + g.id())) {
                mis.add("scent " + g.id());
            }
        }
        for (Eiland.BronPlek b : opzet.bronnen()) {
            if (Geuren.van(b.geur()) == null) {
                mis.add("source " + b.id() + " names an unknown scent");
            }
        }
        for (Eiland.BewonerPlek b : opzet.bewoners()) {
            if (b.bewoner().isEmpty() ? !Honden.bestaat(b.ras(), b.kleur()) : Honden.bewoner(b.bewoner()) == null) {
                mis.add("resident " + b.sleutel() + " is no known dog");
            }
        }
        for (Eiland.Stuk s : opzet.stukken()) {
            if (helper.getLevel().getStructureManager().get(s.template()).isEmpty()) {
                mis.add("template " + s.template());
            }
        }
        helper.assertTrue(bestaat("/data/guhs/dimension/snuffeleiland.json") && bestaat("/data/guhs/dimension_type/snuffeleiland.json")
                && bestaat("/data/guhs/worldgen/biome/snuffeleiland.json"), "the dimension's files");
        JsonObject soort = lees("/data/guhs/dimension_type/snuffeleiland.json").getAsJsonObject("attributes");
        helper.assertTrue(soort.getAsJsonObject("minecraft:audio/background_music").size() == 0
                && lees("/data/guhs/worldgen/biome/snuffeleiland.json").getAsJsonObject("attributes").getAsJsonObject("minecraft:audio/background_music").size() == 0,
                "no music on the island (the dimension type and the biome both say so)");
        helper.assertTrue(mis.isEmpty(), "missing: " + mis);
        helper.succeed();
    }
}
