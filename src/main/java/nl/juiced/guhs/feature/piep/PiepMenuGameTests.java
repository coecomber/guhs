package nl.juiced.guhs.feature.piep;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the piep-maatje menus (piepmenu, 2.9): picking up and putting down keeps everything (name, owner, health, age,
 * the menu settings) for all three; only the owner can pick up or use the menu; rondvadsen uit keeps them in place (and
 * aan lets them follow again); renaming works like a guh; the click rules; the muisje's own settings (saved too); and
 * Poepschilly's poetsbeurt on your OWN tamed guh (the click that the guh's tap/hold handler used to swallow), from waddling
 * to popping out again, robust against the guh disappearing and a save/load in the middle. (Template piep_test_wei: grass
 * at helper y 1.)
 */
public class PiepMenuGameTests {
    private static final String WEI = "piep_test_wei";
    /** Our own batch (the nest test counts the players around it). */
    private static final String BATCH = "piepmenu";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
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

    private static ItemStack vind(ServerPlayer p, Item item) {
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static <T extends Entity> List<T> alle(GameTestHelper helper, Class<T> type) {
        return helper.getLevel().getEntitiesOfClass(type, helper.getBounds().inflate(1), e -> e.isAlive());
    }

    /** Puts the item down on the grass by using it (like a player's right-click on the block) and returns what came out. */
    private static <T extends TamableAnimal> T zetNeer(GameTestHelper helper, ServerPlayer p, ItemStack stack, BlockPos grond, Class<T> type) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(grond);
        List<T> voor = alle(helper, type);
        InteractionResult r = p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        helper.assertTrue(r.consumesAction(), "putting it down works");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the item is used up (no copies)");
        List<T> na = alle(helper, type);
        na.removeAll(voor);
        helper.assertTrue(na.size() == 1, "exactly one came out: " + na.size());
        return na.get(0);
    }

    // --- picking up and putting down --------------------------------------------------------------------------------------------

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuOppakkenHoudtAllesSchildpadjes(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        record Geval(EntityType<? extends PoepschillyEntity> type, Item item, String naam, PiepInstelling eigen) {
        }
        for (Geval g : List.of(new Geval(PiepFeature.POEPSCHILLY.get(), PiepFeature.POEPSCHILLY_ITEM.get(), "Blubbie", PiepInstelling.ZWEMMEN),
                new Geval(PiepFeature.SCHILLY.get(), PiepFeature.SCHILLY_ITEM.get(), "Schillekop", PiepInstelling.BESTIES))) {
            PoepschillyEntity s = helper.spawn(g.type(), new BlockPos(4, 2, 4));
            s.tame(p);
            s.setCustomName(Component.literal(g.naam()));
            s.setHealth(9f);
            s.setAge(-6000);
            PiepMenu.doe(p, s, PiepMenu.Actie.WISSEL, PiepInstelling.RONDVADSEN.ordinal(), "");
            PiepMenu.doe(p, s, PiepMenu.Actie.WISSEL, PiepInstelling.VOLGEN.ordinal(), "");
            PiepMenu.doe(p, s, PiepMenu.Actie.WISSEL, g.eigen().ordinal(), "");
            helper.assertTrue(!s.aan(PiepInstelling.RONDVADSEN) && !s.aan(PiepInstelling.VOLGEN) && !s.aan(g.eigen()), "the settings are off");
            helper.assertTrue(PiepMenu.doe(p, s, PiepMenu.Actie.OPPAKKEN, 0, ""), "the owner picks it up (menu: Oppakken)");
            helper.assertTrue(s.isRemoved(), "it left the world");
            ItemStack stack = vind(p, g.item());
            helper.assertTrue(!stack.isEmpty() && stack.getHoverName().getString().equals(g.naam()), "it is in the pockets, with its name");
            PoepschillyEntity terug = zetNeer(helper, p, stack.copy(), new BlockPos(6, 1, 6), PoepschillyEntity.class);
            stack.setCount(0);
            helper.assertTrue(terug.getType() == g.type(), "the same kind of turtle");
            helper.assertTrue(terug.isTame() && terug.isOwnedBy(p), "still tame and still yours");
            helper.assertTrue(terug.getCustomName() != null && g.naam().equals(terug.getCustomName().getString()), "its name came along");
            helper.assertTrue(terug.getHealth() == 9f && terug.getAge() < 0, "its health and age came along: " + terug.getHealth() + " " + terug.getAge());
            helper.assertTrue(!terug.aan(PiepInstelling.RONDVADSEN) && terug.isOrderedToSit() && !terug.aan(PiepInstelling.VOLGEN)
                    && !terug.aan(g.eigen()), "its settings came along");
            BlockPos at = terug.blockPosition();
            helper.assertTrue(at.equals(helper.absolutePos(new BlockPos(6, 2, 6))), "it stands on the clicked block: " + at);
            terug.discard();
        }
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuOppakkenMuisjeHoudtAlles(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(4, 2, 4));
        muis.tame(p);
        muis.setCustomName(Component.literal("Piepje"));
        muis.setHealth(6f);
        PiepMenu.doe(p, muis, PiepMenu.Actie.WISSEL, PiepInstelling.PIEPJES.ordinal(), "");
        PiepMenu.doe(p, muis, PiepMenu.Actie.WISSEL, PiepInstelling.VERSTOPPEN.ordinal(), "");
        // sneak + empty hand: picked up straight away
        p.setShiftKeyDown(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        muis.mobInteract(p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        helper.assertTrue(muis.isRemoved(), "sneak + click: picked up");
        ItemStack stack = vind(p, PiepFeature.PIEPPIEPMUISJE_ITEM.get());
        helper.assertTrue(!stack.isEmpty(), "in the pockets");
        PieppiepmuisjeEntity terug = zetNeer(helper, p, stack.copy(), new BlockPos(6, 1, 6), PieppiepmuisjeEntity.class);
        helper.assertTrue(terug.isOwnedBy(p) && "Piepje".equals(terug.getCustomName().getString()) && terug.getHealth() == 6f, "the same muisje");
        helper.assertTrue(!terug.aan(PiepInstelling.PIEPJES) && !terug.aan(PiepInstelling.VERSTOPPEN) && terug.aan(PiepInstelling.VOLGEN),
                "its settings came along");
        terug.discard();
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuAlleenHetBaasje(GameTestHelper helper) {
        ServerPlayer baas = player(helper, new BlockPos(1, 2, 1));
        ServerPlayer ander = player(helper, new BlockPos(2, 2, 1));
        PoepschillyEntity s = helper.spawn(PiepFeature.POEPSCHILLY.get(), new BlockPos(4, 2, 4));
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(5, 2, 4));
        // wild: nobody can pick it up
        helper.assertTrue(!PiepMenu.doe(baas, s, PiepMenu.Actie.OPPAKKEN, 0, "") && !s.isRemoved(), "a wild turtle can't be picked up");
        s.tame(baas);
        muis.tame(baas);
        for (PiepMaatje m : List.of((PiepMaatje) s, muis)) {
            TamableAnimal d = m.dier();
            helper.assertTrue(!PiepMenu.doe(ander, m, PiepMenu.Actie.OPPAKKEN, 0, "") && !d.isRemoved(), "someone else can't pick it up");
            helper.assertTrue(!PiepMenu.doe(ander, m, PiepMenu.Actie.NAAM, 0, "Gestolen") && d.getCustomName() == null, "or rename it");
            helper.assertTrue(!PiepMenu.doe(ander, m, PiepMenu.Actie.WISSEL, PiepInstelling.RONDVADSEN.ordinal(), "") && m.aan(PiepInstelling.RONDVADSEN),
                    "or change its settings");
            helper.assertTrue(!PiepDierItem.pakOp(m, ander), "not even directly");
            ander.setShiftKeyDown(true);
            ander.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            d.interact(ander, InteractionHand.MAIN_HAND, d.position());
            ander.setShiftKeyDown(false);
            helper.assertTrue(!d.isRemoved(), "sneak-clicking someone else's maatje doesn't pick it up");
        }
        helper.assertTrue(PiepMenu.doe(baas, s, PiepMenu.Actie.OPPAKKEN, 0, "") && s.isRemoved(), "the owner can");
        helper.assertTrue(PiepMenu.doe(baas, muis, PiepMenu.Actie.OPPAKKEN, 0, "") && muis.isRemoved(), "also the muisje");
        leave(helper, baas, ander);
        helper.succeed();
    }

    // --- rondvadsen, renaming, the click rules ------------------------------------------------------------------------------------

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 400)
    public static void piepmenuRondvadsenUitBlijftStaan(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(7, 2, 7));
        PoepschillyEntity s = helper.spawn(PiepFeature.POEPSCHILLY.get(), new BlockPos(7, 2, 4));
        SchillyEntity schilly = helper.spawn(PiepFeature.SCHILLY.get(), new BlockPos(4, 2, 7));
        List<PiepMaatje> maatjes = List.of(muis, s, schilly);
        for (PiepMaatje m : maatjes) {
            m.dier().tame(p);
            helper.assertTrue(PiepMenu.doe(p, m, PiepMenu.Actie.WISSEL, PiepInstelling.RONDVADSEN.ordinal(), ""), "the owner switches it");
            helper.assertTrue(!m.aan(PiepInstelling.RONDVADSEN) && m.dier().isOrderedToSit() && m.dier().isInSittingPose(), "rondvadsen uit: it sits");
        }
        Vec3[] plek = maatjes.stream().map(m -> m.dier().position()).toArray(Vec3[]::new);
        helper.runAfterDelay(120, () -> {
            for (int i = 0; i < maatjes.size(); i++) {
                Vec3 nu = maatjes.get(i).dier().position();
                double dx = nu.x - plek[i].x, dz = nu.z - plek[i].z;
                helper.assertTrue(dx * dx + dz * dz < 0.3 * 0.3, maatjes.get(i).soort() + " stayed put (owner 8 blocks away): moved "
                        + Math.sqrt(dx * dx + dz * dz));
            }
            // and on again: the muisje and Poepschilly come to their owner
            for (PiepMaatje m : maatjes) {
                PiepMenu.doe(p, m, PiepMenu.Actie.WISSEL, PiepInstelling.RONDVADSEN.ordinal(), "");
                helper.assertTrue(m.aan(PiepInstelling.RONDVADSEN) && !m.dier().isOrderedToSit(), "rondvadsen aan again");
            }
            double muisVoor = muis.distanceTo(p), schillyVoor = s.distanceTo(p);
            helper.succeedWhen(() -> {
                helper.assertTrue(muis.distanceTo(p) < muisVoor - 2, "the muisje follows again: " + muis.distanceTo(p));
                helper.assertTrue(s.distanceTo(p) < schillyVoor - 2, "Poepschilly follows again: " + s.distanceTo(p));
                for (PiepMaatje m : maatjes) {
                    m.dier().discard();
                }
                leave(helper, p);
            });
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuNaamVeranderen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        for (EntityType<? extends TamableAnimal> type : List.<EntityType<? extends TamableAnimal>>of(PiepFeature.PIEPPIEPMUISJE.get(),
                PiepFeature.POEPSCHILLY.get(), PiepFeature.SCHILLY.get())) {
            TamableAnimal d = helper.spawn(type, new BlockPos(4, 2, 4));
            d.tame(p);
            PiepMaatje m = (PiepMaatje) d;
            helper.assertTrue(PiepMenu.doe(p, m, PiepMenu.Actie.NAAM, 0, "  Knabbeltje  "), "renamed");
            helper.assertTrue(d.getCustomName() != null && "Knabbeltje".equals(d.getCustomName().getString()), "trimmed, like a guh: " + d.getCustomName());
            PiepMenu.hernoem(d, "x".repeat(50));
            helper.assertTrue(d.getCustomName().getString().length() == PiepMenu.MAX_NAAM, "at most " + PiepMenu.MAX_NAAM + " letters");
            PiepMenu.doe(p, m, PiepMenu.Actie.NAAM, 0, "   ");
            helper.assertTrue(d.getCustomName() == null, "empty: no name any more");
            d.discard();
        }
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuKlikRegels(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        PoepschillyEntity s = helper.spawn(PiepFeature.POEPSCHILLY.get(), new BlockPos(3, 2, 3));
        s.tame(p);
        s.setHealth(5f);
        // empty hand (owner): the menu, nothing else happens to it
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(s.interact(p, InteractionHand.MAIN_HAND, s.position()).consumesAction() && !s.isRemoved() && !s.klaarVoor(p),
                "a click opens the menu (not picked up, not yet ready for a guh)");
        // food (owner, even sneaking): still feeding
        p.setShiftKeyDown(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.KELP, 3));
        s.interact(p, InteractionHand.MAIN_HAND, s.position());
        helper.assertTrue(!s.isRemoved() && s.getHealth() > 5f && p.getMainHandItem().getCount() == 2, "kelp still feeds (sneaking too)");
        // sneak + empty hand: picked up
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        s.interact(p, InteractionHand.MAIN_HAND, s.position());
        p.setShiftKeyDown(false);
        helper.assertTrue(s.isRemoved() && !vind(p, PiepFeature.POEPSCHILLY_ITEM.get()).isEmpty(), "sneak + empty hand picks it up");
        // a wild muisje is still petted by anyone (aaien counts)
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(5, 2, 5));
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);     // (the picked-up turtle landed in the hand)
        muis.interact(p, InteractionHand.MAIN_HAND, muis.position());
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.GEAAID) == 1 && !muis.isRemoved(), "petting still works");
        muis.discard();
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuMuisjeInstellingen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        PieppiepmuisjeEntity muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(4, 2, 4));
        muis.tame(p);
        helper.assertTrue(muis.getAmbientSound() != null, "it peeps");
        PiepMenu.doe(p, muis, PiepMenu.Actie.WISSEL, PiepInstelling.PIEPJES.ordinal(), "");
        helper.assertTrue(muis.getAmbientSound() == null, "piepjes uit: quiet");
        PiepMenu.doe(p, muis, PiepMenu.Actie.WISSEL, PiepInstelling.VERSTOPPEN.ordinal(), "");
        muis.verstop().forceer = true;
        helper.assertTrue(!muis.verstop().goal().canUse(), "verstoppertje uit: it doesn't hide by itself");
        muis.verstop().forceer = false;
        helper.assertTrue(!PiepMenu.doe(p, muis, PiepMenu.Actie.WISSEL, PiepInstelling.ZWEMMEN.ordinal(), ""), "a muisje has no zwemmen setting");
        // saved and loaded
        CompoundTag tag = new CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(muis, tag);
        PieppiepmuisjeEntity kopie = PiepFeature.PIEPPIEPMUISJE.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(kopie, tag);
        helper.assertTrue(!kopie.aan(PiepInstelling.PIEPJES) && !kopie.aan(PiepInstelling.VERSTOPPEN) && kopie.aan(PiepInstelling.VOLGEN),
                "the settings are saved");
        // the big button: onto the shoulder
        PiepMenu.doe(p, muis, PiepMenu.Actie.SPECIAAL, 0, "");
        helper.assertTrue(muis.isRemoved() && Schouder.heeft(p), "Op mijn schouder!");
        Schouder.eraf(p, helper.absoluteVec(new Vec3(3.5, 2, 3.5))).dier().discard();
        leave(helper, p);
        helper.succeed();
    }

    // --- Poepschilly's poetsbeurt on your own guh ---------------------------------------------------------------------------------

    private static GuhEntity eigenGuh(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(p);
        guh.setNoAi(true);
        return guh;
    }

    private static PoepschillyEntity klaarSchilly(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        PoepschillyEntity s = helper.spawn(PiepFeature.POEPSCHILLY.get(), at);
        s.tame(p);
        helper.assertTrue(PiepMenu.doe(p, s, PiepMenu.Actie.SPECIAAL, 0, "") && s.klaarVoor(p), "Kontje poetsen!: ready");
        return s;
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 500)
    public static void piepmenuPoetsbeurtOpEigenTammeGuh(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        GuhEntity guh = eigenGuh(helper, p, new BlockPos(5, 2, 5));
        PoepschillyEntity s = klaarSchilly(helper, p, new BlockPos(1, 2, 7));
        // the click the client now sends for your own tamed guh (guhs:piep_op_guh)
        helper.assertTrue(PoepschillyEntity.opGuhGeklikt(guh, p), "clicking my own tamed guh starts the poetsbeurt");
        helper.assertTrue(s.fase() == PoepschillyEntity.Fase.LOPEN && !s.klaarVoor(p), "it waddles over first");
        boolean[] gezien = new boolean[4];
        helper.onEachTick(() -> {
            if (s.fase() != null) {
                gezien[s.fase().ordinal()] = true;
            }
            if (s.fase() == PoepschillyEntity.Fase.KRUIPEN) {
                helper.assertTrue(s.krimp() <= 1f, "it shrinks into the kontje");
            }
            if (s.isBinnen()) {
                helper.assertTrue(s.isInvisible() && s.fase() == PoepschillyEntity.Fase.BINNEN, "inside: hidden");
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(s.fase() == null, "done (now " + s.fase() + ")");
            helper.assertTrue(gezien[0] && gezien[1] && gezien[2] && gezien[3], "waddled, crawled in, cleaned, popped out");
            helper.assertTrue(!s.isBinnen() && !s.isInvisible() && !s.isNoGravity() && s.krimp() == 1f, "back out, visible and its own size");
            helper.assertTrue(s.isAlive() && s.distanceTo(guh) < 3.5, "right next to the guh: " + s.distanceTo(guh));
            helper.assertTrue(helper.getLevel().noCollision(s, s.getBoundingBox()), "not stuck in anything");
            helper.assertTrue(alle(helper, PoepschillyEntity.class).size() == 1, "still exactly one Poepschilly");
            helper.assertTrue(guh.hasEffect(PiepFeature.FRIS_VAN_BINNEN), "the guh is fris van binnen");
            helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.POETSBEURTEN) == 1 && advancement(p, "piep_poetsbeurt"),
                    "the poetsbeurt counts (piep_poetsen milestone, quest)");
            PiepMenu.doe(p, s, PiepMenu.Actie.SPECIAAL, 0, "");
            helper.assertTrue(s.rustSeconden() > 0 && !s.klaarVoor(p), "then it rests");
            s.discard();
            leave(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 60)
    public static void piepmenuPoetsbeurtWildeGuhWeigert(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        wild.setNoAi(true);
        PoepschillyEntity s = klaarSchilly(helper, p, new BlockPos(3, 2, 5));
        // a wild guh: the vanilla click reaches the server (GuhHooks.klik): only tamed guhs
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(PoepschillyEntity.klikOpGuh(wild, p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "the click is taken");
        helper.assertTrue(!s.isAanHetPoetsen() && !wild.hasEffect(PiepFeature.FRIS_VAN_BINNEN) && !s.klaarVoor(p), "refused: only tamed guhs");
        helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.POETSBEURTEN) == 0, "nothing counted");
        s.discard();
        leave(helper, p);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 400)
    public static void piepmenuPoetsbeurtGuhWegPoepschillyKomtEruit(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        GuhEntity guh = eigenGuh(helper, p, new BlockPos(5, 2, 5));
        PoepschillyEntity s = klaarSchilly(helper, p, new BlockPos(3, 2, 5));
        PoepschillyEntity.opGuhGeklikt(guh, p);
        boolean[] weg = {false};
        helper.onEachTick(() -> {
            if (!weg[0] && s.isBinnen()) {
                weg[0] = true;
                guh.discard();                                   // the guh is gone while Poepschilly is inside
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(weg[0], "it got inside first");
            helper.assertTrue(s.fase() == null && !s.isBinnen() && !s.isInvisible() && !s.isNoGravity(), "it popped out anyway");
            helper.assertTrue(s.isAlive() && helper.getLevel().noCollision(s, s.getBoundingBox()), "safe and sound");
            helper.assertTrue(KnusVoortgang.teller(p, PiepVoortgang.POETSBEURTEN) == 0, "no poetsbeurt counted");
            s.discard();
            leave(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 600)
    public static void piepmenuPoetsbeurtOverleeftOpslaanEnLaden(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        ServerLevel level = helper.getLevel();
        GuhEntity guh = eigenGuh(helper, p, new BlockPos(5, 2, 5));
        PoepschillyEntity s = klaarSchilly(helper, p, new BlockPos(3, 2, 5));
        PoepschillyEntity.opGuhGeklikt(guh, p);
        PoepschillyEntity[] geladen = {null};
        helper.onEachTick(() -> {
            if (geladen[0] == null && s.isBinnen()) {
                // a chunk unload / server restart in the middle: saved, gone, loaded again
                CompoundTag tag = new CompoundTag();
                helper.assertTrue(nl.juiced.guhs.storage.Nbt.save(s, tag), "saved");
                s.discard();
                Entity e = EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), tag), level, EntitySpawnReason.LOAD).orElse(null);
                helper.assertTrue(e instanceof PoepschillyEntity, "loaded");
                geladen[0] = (PoepschillyEntity) e;
                helper.assertTrue(geladen[0].isBinnen() && geladen[0].isInvisible() && geladen[0].isAanHetPoetsen(), "still inside after loading");
                level.addFreshEntity(e);
            }
        });
        helper.succeedWhen(() -> {
            PoepschillyEntity t = geladen[0];
            helper.assertTrue(t != null && t.fase() == null, "loaded and finished");
            helper.assertTrue(!t.isBinnen() && !t.isInvisible() && t.krimp() == 1f && t.distanceTo(guh) < 3.5, "out again next to the guh");
            helper.assertTrue(alle(helper, PoepschillyEntity.class).size() == 1, "exactly one Poepschilly (nothing lost, nothing doubled)");
            helper.assertTrue(guh.hasEffect(PiepFeature.FRIS_VAN_BINNEN) && KnusVoortgang.teller(p, PiepVoortgang.POETSBEURTEN) == 1,
                    "and the poetsbeurt still counts");
            t.discard();
            leave(helper, p);
        });
    }

    @GuhTest(template = WEI, batch = BATCH, timeoutTicks = 400)
    public static void piepmenuPoetsbeurtGeladenZonderGuh(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(1, 2, 1));
        ServerLevel level = helper.getLevel();
        GuhEntity guh = eigenGuh(helper, p, new BlockPos(5, 2, 5));
        PoepschillyEntity s = klaarSchilly(helper, p, new BlockPos(3, 2, 5));
        PoepschillyEntity.opGuhGeklikt(guh, p);
        PoepschillyEntity[] geladen = {null};
        helper.onEachTick(() -> {
            if (geladen[0] == null && s.isBinnen()) {
                CompoundTag tag = new CompoundTag();
                nl.juiced.guhs.storage.Nbt.save(s, tag);
                s.discard();
                guh.discard();                                   // (the guh never comes back)
                geladen[0] = (PoepschillyEntity) EntityType.create(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), tag), level, EntitySpawnReason.LOAD).orElseThrow();
                level.addFreshEntity(geladen[0]);
            }
        });
        helper.succeedWhen(() -> {
            PoepschillyEntity t = geladen[0];
            helper.assertTrue(t != null && t.fase() == null && !t.isBinnen() && !t.isInvisible() && !t.isNoGravity(),
                    "without its guh it pops out by itself");
            helper.assertTrue(t.isAlive() && level.noCollision(t, t.getBoundingBox()) && alle(helper, PoepschillyEntity.class).size() == 1,
                    "safe, and just one");
            t.discard();
            leave(helper, p);
        });
    }
}
