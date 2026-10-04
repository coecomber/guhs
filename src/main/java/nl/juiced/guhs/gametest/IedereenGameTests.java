package nl.juiced.guhs.gametest;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.KoningsTroonBlock;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.entity.QuestGuhEntity;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.feature.eilanden.EilandenEvents;
import nl.juiced.guhs.feature.guheinde.GuheindeEvents;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.guheinde.MagereCellen;
import nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature;
import nl.juiced.guhs.feature.kaasmijn.Mijnguh;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasEvents;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.piep.KaasknabbelNest;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.Terugkeer;
import nl.juiced.guhs.world.VoorIedereen;
import nl.juiced.guhs.world.WildeDieren;

/**
 * 1.2.7: every quest can be done by every player on one server. The one-of-a-kind things of the world (the magere guhs,
 * the Koningguh, the Wolkguh, Big Mika, the Hungry Guh, loot that was there once) with a second player.
 */
public class IedereenGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "iedereen";

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void done(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static <T extends Entity> T spawn(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, double x, double y, double z) {
        T e = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        e.snapTo(at.x, at.y, at.z, 0f, 0f);
        helper.getLevel().addFreshEntity(e);
        return e;
    }

    private static int count(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static boolean has(ServerPlayer player, String advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(advancement));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static AABB area(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
    }

    // --- the magere guhs --------------------------------------------------------------------------------------------------

    /** Two players feed the same magere guh: both count, it stays grey; feeding it twice does nothing. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void aMagereGuhIsThereForEveryPlayer(GameTestHelper helper) {
        ServerPlayer a = player(helper), b = player(helper);
        GuhEntity guh = spawn(helper, ModEntities.GUH.get(), 3.5, 1, 3.5);
        guh.setVariant(GuhVariant.MAGER);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
        a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        helper.assertTrue(guh.isAlive() && guh.getVariant() == GuhVariant.MAGER, "the grey guh stays");
        helper.assertTrue(GuhQuests.saved(a).getIntOr(GuheindeEvents.GERED, 0) == 1 && a.getMainHandItem().getCount() == 4, "A: counted, knabbel eaten");
        a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        helper.assertTrue(GuhQuests.saved(a).getIntOr(GuheindeEvents.GERED, 0) == 1 && a.getMainHandItem().getCount() == 4,
                "A again: already fed, no credit, no knabbel gone");
        b.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        helper.assertTrue(GuhQuests.saved(b).getIntOr(GuheindeEvents.GERED, 0) == 1 && b.getMainHandItem().getCount() == 4, "B: counted too");
        GuhWorldData.PlayerData pb = GuhWorldData.get(b.level().getServer()).player(b.getUUID());
        helper.assertTrue(pb.seen.contains(GuhVariant.MAGER) && pb.tamed.contains(GuhVariant.MAGER), "B: the Guhdex page and its star");
        helper.assertTrue(has(a, "guheinde/guheinde_gered") && has(b, "guheinde/guheinde_gered"), "both: the advancement");
        List<GuhEntity> vrij = MagereCellen.vrij().stream().filter(g -> g.distanceToSqr(guh) < 100).toList();
        helper.assertTrue(vrij.size() == 2 && vrij.stream().noneMatch(g -> g.getVariant() == GuhVariant.MAGER || g.isPersistenceRequired()),
                "two colourful guhs ran off (come-and-go guhs): " + vrij.size());
        // six different guhs for the bevrijder
        for (int i = 0; i < GuheindeEvents.BEVRIJDER - 1; i++) {
            GuhEntity meer = spawn(helper, ModEntities.GUH.get(), 3.5, 1, 2.5);
            meer.setVariant(GuhVariant.MAGER);
            b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 5));
            b.interactOn(meer, InteractionHand.MAIN_HAND, meer.position());
            meer.discard();
        }
        helper.assertTrue(has(b, "guheinde/guheinde_bevrijder") && !has(a, "guheinde/guheinde_bevrijder"), "B freed six");
        MagereCellen.vrij().forEach(Entity::discard);
        guh.discard();
        done(helper, a, b);
    }

    /** A cell without its guh (a world from before 1.2.7) gets one back; a cell with one doesn't get a second. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void anEmptyCellGetsAMagereGuhBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cel = helper.absolutePos(new BlockPos(2, 1, 2));
        List<BlockPos> cellen = List.of(cel);
        Vec3 bij = Vec3.atCenterOf(cel);
        Terugkeer.zetGezien(level, MagereCellen.SOORT, cel, null);
        helper.assertTrue(MagereCellen.controleer(level, cellen, bij).isEmpty(), "seen empty once: not yet (entities may still be loading)");
        Terugkeer.zetGemist(level, MagereCellen.SOORT, cel, Terugkeer.BEVESTIG);
        List<GuhEntity> nieuw = MagereCellen.controleer(level, cellen, bij);
        helper.assertTrue(nieuw.size() == 1, "never seen with a guh: a new one (ticking " + level.isPositionEntityTicking(cel) + ", entities "
                + level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(cel)) + ")");
        helper.assertTrue(nieuw.get(0).getVariant() == GuhVariant.MAGER && nieuw.get(0).isPersistenceRequired()
                && cel.equals(MagereCellen.thuis(nieuw.get(0))), "a magere guh, at home in this cell: " + nieuw.get(0).getVariant() + " " + MagereCellen.thuis(nieuw.get(0)));
        helper.assertTrue(MagereCellen.controleer(level, cellen, bij).isEmpty(), "one per cell");
        // a guh from the template (no home yet) learns its cell, and one that strays goes back
        GuhEntity guh = nieuw.get(0);
        guh.getPersistentData().remove(MagereCellen.THUIS);
        guh.snapTo(cel.getX() + 4.5, cel.getY(), cel.getZ() + 0.5);
        MagereCellen.controleer(level, cellen, bij);
        helper.assertTrue(cel.equals(MagereCellen.thuis(guh)) && guh.distanceToSqr(Vec3.atBottomCenterOf(cel)) < 1, "knows its cell, back in it");
        // gone (somebody hit it): only after a while a new one
        guh.discard();
        Terugkeer.zetGemist(level, MagereCellen.SOORT, cel, Terugkeer.BEVESTIG);
        helper.assertTrue(MagereCellen.controleer(level, cellen, bij).isEmpty(), "just gone: not yet");
        Terugkeer.zetGezien(level, MagereCellen.SOORT, cel, level.getGameTime() - MagereCellen.BIJVUL_WACHT);
        Terugkeer.zetGemist(level, MagereCellen.SOORT, cel, Terugkeer.BEVESTIG);
        nieuw = MagereCellen.controleer(level, cellen, bij);
        helper.assertTrue(nieuw.size() == 1, "after the wait: a new one");
        nieuw.get(0).discard();
        Terugkeer.zetGezien(level, MagereCellen.SOORT, cel, null);
        helper.succeed();
    }

    // --- the Koningguh ----------------------------------------------------------------------------------------------------------

    /** A tamed king on the throne doesn't keep the next king away, still tells the story to others, and the throne can't be mined. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void aTamedKingDoesNotBlockTheCastleStory(GameTestHelper helper) {
        ServerPlayer owner = player(helper), other = player(helper);
        BlockPos pos = new BlockPos(2, 1, 2);
        BlockState troon = ModBlocks.KONINGSTROON.get().defaultBlockState().setValue(KoningsTroonBlock.ROYAL, true);
        helper.setBlock(pos, troon);
        BlockPos abs = helper.absolutePos(pos);
        GuhEntity king = spawn(helper, ModEntities.GUH.get(), 2.5, 1.3, 2.5);
        KoningsTroonBlock.makeKing(king);
        king.tame(owner);
        // somebody else's king: he still tells the story (and gives the book)
        other.interactOn(king, InteractionHand.MAIN_HAND, king.position());
        helper.assertTrue(GuhQuests.saved(other).getIntOr(GuheindeEvents.KONING, 0) == 1, "another player's tamed king tells the story");
        boolean book = false;
        for (ItemStack stack : other.getInventory().getNonEquipmentItems()) {
            book |= Guhboek.of(stack) == Guhboek.GUHEINDE;
        }
        helper.assertTrue(book, "and gives the book");
        helper.assertTrue(GuheindeEvents.heeftNieuws(owner), "the owner hasn't heard it yet either");
        owner.interactOn(king, InteractionHand.MAIN_HAND, king.position());
        helper.assertTrue(GuhQuests.saved(owner).getIntOr(GuheindeEvents.KONING, 0) == 1 && !GuheindeEvents.heeftNieuws(owner), "your own king: the story once");
        // the throne: a tamed king is not "the" king
        var throne = helper.getBlockEntity(pos, KoningsTroonBlock.Entity.class);
        throne.setLastKing(helper.getLevel().getGameTime() - KoningsTroonBlock.NEW_KING_AFTER - 1);
        throne.check(helper.getLevel(), abs, helper.getBlockState(pos));
        List<GuhEntity> kings = helper.getLevel().getEntitiesOfClass(GuhEntity.class, area(helper), g -> g.getVariant() == GuhVariant.KONING);
        helper.assertTrue(kings.size() == 2 && kings.stream().filter(g -> !g.isTame()).count() == 1, "a new wild king next to the tamed one: " + kings.size());
        throne.setLastKing(helper.getLevel().getGameTime() - KoningsTroonBlock.NEW_KING_AFTER - 1);
        throne.check(helper.getLevel(), abs, helper.getBlockState(pos));
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(GuhEntity.class, area(helper), g -> g.getVariant() == GuhVariant.KONING).size() == 2,
                "with a wild king there: no third");
        helper.assertTrue(KoningsTroonBlock.NEW_KING_AFTER <= 24000L, "the next king comes within a day");
        helper.assertTrue(troon.getDestroyProgress(other, helper.getLevel(), abs) == 0f, "the castle's throne can't be mined");
        helper.assertTrue(troon.setValue(KoningsTroonBlock.ROYAL, false).getDestroyProgress(other, helper.getLevel(), abs) > 0f, "your own throne can");
        kings.forEach(Entity::discard);
        done(helper, owner, other);
    }

    /** The king's outfit: the missing pieces, once per player (at the knighting or when taming a king). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void theKingsOutfitIsForEveryPlayer(GameTestHelper helper) {
        ServerPlayer a = player(helper), b = player(helper);
        GuhEntity koning = spawn(helper, ModEntities.GUH.get(), 3.5, 1, 3.5);
        koning.setVariant(GuhVariant.KONING);
        GuhQuests.saved(a).putInt(GuheindeEvents.KONING, 1);
        GuhQuests.saved(a).putInt(GuheindeGevecht.WINS, 1);
        a.interactOn(koning, InteractionHand.MAIN_HAND, koning.position());
        helper.assertTrue(GuhQuests.saved(a).getIntOr(GuheindeEvents.KONING, 0) == 2, "knighted");
        for (GuhClothes c : List.of(GuhClothes.KONING_KROON, GuhClothes.KONING_MANTEL, GuhClothes.KONING_KETTING)) {
            helper.assertTrue(count(a, ModItems.clothingItem(c)) == 1, "A got the " + c);
        }
        helper.assertTrue(GuheindeEvents.koningPakje(a) == 0, "once per player");
        KledingUnlocks.voegToe(b, GuhClothes.KONING_KROON);
        helper.assertTrue(GuheindeEvents.koningPakje(b) == 2 && count(b, ModItems.clothingItem(GuhClothes.KONING_KROON)) == 0
                && count(b, ModItems.clothingItem(GuhClothes.KONING_MANTEL)) == 1, "B had the crown already: the other two");
        koning.discard();
        done(helper, a, b);
    }

    // --- the Wolkguh ------------------------------------------------------------------------------------------------------------

    /** The knabbels count per player, a tamed Wolkguh makes room for a new wild one, and its outfit comes with the taming. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void theWolkguhIsThereForEveryPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = player(helper), b = player(helper);
        GuhEntity guh = spawn(helper, ModEntities.GUH.get(), 2.5, 1, 2.5);
        guh.setVariant(GuhVariant.WOLK);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        for (int i = 0; i < EilandenEvents.KNABBELS_NEEDED - 1; i++) {
            a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        }
        b.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        helper.assertTrue(EilandenEvents.knabbelsFed(guh, a.getUUID()) == EilandenEvents.KNABBELS_NEEDED - 1
                && EilandenEvents.knabbelsFed(guh, b.getUUID()) == 1 && !guh.isTame(), "everybody earns its trust themselves");
        for (int i = 0; i < 60 && !guh.isTame(); i++) {
            a.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        }
        helper.assertTrue(guh.isTame() && guh.isOwnedBy(a), "A tamed it");
        helper.assertTrue(count(a, ModItems.clothingItem(GuhClothes.WOLKENMUTS)) == 1 && count(a, ModItems.clothingItem(GuhClothes.WOLKENKRAAG)) == 1,
                "its outfit for the tamer");
        helper.assertTrue(EilandenEvents.wolkPakje(a) == 0, "once per player");
        // a new wild one for B
        BlockPos plek = helper.absolutePos(new BlockPos(3, 1, 3));
        AABB eilanden = area(helper);
        Terugkeer.zetGezien(level, EilandenEvents.SOORT, plek, level.getGameTime());
        Terugkeer.zetGemist(level, EilandenEvents.SOORT, plek, Terugkeer.BEVESTIG);
        helper.assertTrue(EilandenEvents.wolkTerug(level, plek, eilanden) == null, "just tamed: not yet");
        Terugkeer.zetGezien(level, EilandenEvents.SOORT, plek, level.getGameTime() - EilandenEvents.NIEUWE_WOLK_NA);
        Terugkeer.zetGemist(level, EilandenEvents.SOORT, plek, Terugkeer.BEVESTIG);
        GuhEntity nieuw = EilandenEvents.wolkTerug(level, plek, eilanden);
        helper.assertTrue(nieuw != null && !nieuw.isTame() && nieuw.getVariant() == GuhVariant.WOLK
                && nieuw.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.WOLKENMUTS && nieuw.getClothes(GuhClothes.Slot.NECK) == GuhClothes.WOLKENKRAAG,
                "a day later a new wild Wolkguh, in its outfit (the tamed one doesn't count)");
        Terugkeer.zetGezien(level, EilandenEvents.SOORT, plek, level.getGameTime() - EilandenEvents.NIEUWE_WOLK_NA);
        Terugkeer.zetGemist(level, EilandenEvents.SOORT, plek, Terugkeer.BEVESTIG);
        helper.assertTrue(EilandenEvents.wolkTerug(level, plek, eilanden) == null, "one wild Wolkguh at a time");
        helper.assertTrue(EilandenEvents.knabbelsFed(nieuw, b.getUUID()) == 0, "B starts again with the new one");
        nieuw.discard();
        guh.discard();
        Terugkeer.zetGezien(level, EilandenEvents.SOORT, plek, null);
        done(helper, a, b);
    }

    // --- Big Mika -----------------------------------------------------------------------------------------------------------------

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bigMikaCountsForEverybodyNearAndComesBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = player(helper), b = player(helper);
        MikaEntity mika = spawn(helper, ModEntities.MIKA.get(), 2.5, 1, 2.5);
        mika.makeBoss();
        List<ServerPlayer> helden = VoorIedereen.bigMikaVerslagen(mika, a);
        GuhWorldData data = GuhWorldData.get(level.getServer());
        helper.assertTrue(helden.contains(a) && helden.contains(b) && data.player(a.getUUID()).beatBigMika && data.player(b.getUUID()).beatBigMika,
                "the killer and the player next to him both beat Big Mika");
        helper.assertTrue(has(a, "guhmension/defeat_big_mika") && has(b, "guhmension/defeat_big_mika"), "both: the advancement");
        mika.discard();
        BlockPos plek = helper.absolutePos(new BlockPos(2, 1, 2));
        AABB hal = area(helper);
        Terugkeer.zetGezien(level, VoorIedereen.BIG_MIKA_SOORT, plek, level.getGameTime());
        Terugkeer.zetGemist(level, VoorIedereen.BIG_MIKA_SOORT, plek, Terugkeer.BEVESTIG);
        helper.assertTrue(VoorIedereen.bigMikaTerug(level, plek, hal) == null, "just beaten: his hall stays empty for now");
        Terugkeer.zetGezien(level, VoorIedereen.BIG_MIKA_SOORT, plek, level.getGameTime() - VoorIedereen.BIG_MIKA_NA);
        Terugkeer.zetGemist(level, VoorIedereen.BIG_MIKA_SOORT, plek, Terugkeer.BEVESTIG);
        MikaEntity nieuw = VoorIedereen.bigMikaTerug(level, plek, hal);
        helper.assertTrue(nieuw != null && nieuw.isBoss() && nieuw.getHealth() == MikaEntity.BOSS_HEALTH && nieuw.isPersistenceRequired(),
                "three days later Big Mika is back");
        Terugkeer.zetGezien(level, VoorIedereen.BIG_MIKA_SOORT, plek, level.getGameTime() - VoorIedereen.BIG_MIKA_NA);
        Terugkeer.zetGemist(level, VoorIedereen.BIG_MIKA_SOORT, plek, Terugkeer.BEVESTIG);
        helper.assertTrue(VoorIedereen.bigMikaTerug(level, plek, hal) == null, "one Big Mika per hall");
        nieuw.discard();
        Terugkeer.zetGezien(level, VoorIedereen.BIG_MIKA_SOORT, plek, null);
        done(helper, a, b);
    }

    // --- loot and hand-outs --------------------------------------------------------------------------------------------------------

    /** The Hungry Guh stays and serves every player once. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void theHungryGuhServesEveryPlayerOnce(GameTestHelper helper) {
        ServerPlayer a = player(helper), b = player(helper);
        QuestGuhEntity quest = spawn(helper, ModEntities.QUEST_GUH.get(), 2.5, 1, 2.5);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 30));
        b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 30));
        quest.interact(a, InteractionHand.MAIN_HAND, quest.position());
        helper.assertTrue(quest.isAlive() && count(a, ModItems.BANK_GUH.get()) == 1 && count(a, ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 20, "A: a Bank Guh");
        quest.interact(a, InteractionHand.MAIN_HAND, quest.position());
        helper.assertTrue(count(a, ModItems.BANK_GUH.get()) == 1 && count(a, ModItems.GEFRITUURDE_KAASKNABBELS.get()) == 20, "A again: one each");
        quest.interact(b, InteractionHand.MAIN_HAND, quest.position());
        helper.assertTrue(quest.isAlive() && count(b, ModItems.BANK_GUH.get()) == 1, "B: a Bank Guh too");
        // it remembers after a reload
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(quest, tag);
        QuestGuhEntity copy = ModEntities.QUEST_GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(copy, tag);
        helper.assertTrue(copy.heeftGehad(a) && copy.heeftGehad(b), "it remembers who had one");
        quest.discard();
        done(helper, a, b);
    }

    /** The lost cake again, the roze guh koek recipe for every winner, the Voorraadmika's diary, the knabbelvlotje's barrel. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void lootThatWasThereOnceIsThereForEverybody(GameTestHelper helper) {
        ServerPlayer a = player(helper), b = player(helper);
        // the Verloren Guh-taart
        GuhQuests.regiveCake(a);
        helper.assertTrue(count(a, ModItems.VERLOREN_GUH_TAART.get()) == 1 && GuhQuests.saved(a).contains(GuhQuests.TAART_OPNIEUW), "a new cake at the picnic");
        // the recipe: the chest of a first win holds one paper, everybody else gets theirs in hand
        helper.assertTrue(KaasknabbelNest.geefRecepten(List.of(a, b), true) == 1 && count(a, PiepFeature.ROZE_GUH_KOEK_RECEPT.get()) == 0
                && count(b, PiepFeature.ROZE_GUH_KOEK_RECEPT.get()) == 1, "first win: one in the chest, one handed out");
        helper.assertTrue(KaasknabbelNest.geefRecepten(List.of(a, b), false) == 1 && count(a, PiepFeature.ROZE_GUH_KOEK_RECEPT.get()) == 1
                && count(b, PiepFeature.ROZE_GUH_KOEK_RECEPT.get()) == 1, "a later win: whoever has none yet");
        nl.juiced.guhs.feature.piep.ReceptItem.leer(b);
        GuhQuests.take(b, PiepFeature.ROZE_GUH_KOEK_RECEPT.get(), 1);
        helper.assertTrue(KaasknabbelNest.geefRecepten(List.of(a, b), false) == 0, "who knows it (or holds one) gets no more");
        // the diary
        helper.assertTrue(VoorIedereen.voorraadboek(a) && VoorIedereen.voorraadboek(b) && !VoorIedereen.voorraadboek(a), "a copy each, once");
        boolean book = false;
        for (ItemStack stack : b.getInventory().getNonEquipmentItems()) {
            book |= Guhboek.of(stack) == Guhboek.VOORRAADKELDER;
        }
        helper.assertTrue(book, "B holds the diary");
        // the barrel of a knabbelvlotje: known while it has its loot, and after
        BlockPos ton = new BlockPos(2, 1, 2);
        helper.setBlock(ton, Blocks.BARREL);
        BarrelBlockEntity barrel = helper.getBlockEntity(ton, BarrelBlockEntity.class);
        helper.assertTrue(!KaasmoerasEvents.isVlotjeTon(helper.getLevel(), helper.absolutePos(ton), barrel), "any barrel isn't a vlotje");
        barrel.setLootTable(KaasmoerasEvents.VLOTJE_LOOT, 1L);
        helper.assertTrue(KaasmoerasEvents.isVlotjeTon(helper.getLevel(), helper.absolutePos(ton), barrel), "the vlotje's barrel");
        barrel.setLootTable(null, 0L);
        helper.assertTrue(KaasmoerasEvents.isVlotjeTon(helper.getLevel(), helper.absolutePos(ton), barrel), "still, after somebody took the loot");
        // the Baltoguh statue: placing it does the quest
        BlockPos beeld = new BlockPos(3, 1, 3);
        BlockState beeldje = BaltoFeature.BALTOGUH_BEELDJE.get().defaultBlockState();
        helper.setBlock(beeld, beeldje);
        helper.assertTrue(!has(b, "quest/balto_beeldje"), "not yet");
        beeldje.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(beeld), beeldje, b, new ItemStack(BaltoFeature.BALTOGUH_BEELDJE_ITEM.get()));
        helper.assertTrue(has(b, "quest/balto_beeldje"), "quest/balto_beeldje for placing the statue");
        // the portal to the Guheinde: whoever goes through an open one
        GuheindeEvents.portaalOpen(b);
        helper.assertTrue(has(b, "guheinde/guheinde_portaal"), "guheinde_portaal");
        done(helper, a, b);
    }

    /** Somebody else's dropped loaner pickaxe doesn't keep you from borrowing one. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void someoneElsesDroppedPickaxeDoesNotBlockYou(GameTestHelper helper) {
        ServerPlayer a = player(helper), b = player(helper);
        ItemEntity item = new ItemEntity(helper.getLevel(), a.getX(), a.getY(), a.getZ(), new ItemStack(KaasmijnFeature.LEENHOUWEEL.get()));
        item.setThrower(b);
        item.setPickUpDelay(1000);
        helper.getLevel().addFreshEntity(item);
        helper.assertTrue(Mijnguh.lyingAround(b), "B dropped theirs: pick it up first");
        helper.assertTrue(!Mijnguh.lyingAround(a), "A can borrow one");
        item.discard();
        done(helper, a, b);
    }

    // --- the fight in the Guheinde ---------------------------------------------------------------------------------------------------

    /** A participant who isn't there at the last hit gets the reward when they are back; an abandoned recalled fight ends. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void aParticipantWhoLeftStillWinsAndAnAbandonedFightEnds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer here = player(helper), away = player(helper);
        java.util.UUID longGone = java.util.UUID.randomUUID();
        GuheindeGevecht fight = new GuheindeGevecht();
        long now = level.getGameTime();
        fight.addParticipant(here.getUUID(), now);
        fight.addParticipant(away.getUUID(), now - 200);
        fight.addParticipant(longGone, now - GuheindeGevecht.RECENT - 1);
        fight.rememberAbsent(List.of(here), now, 123);
        helper.assertTrue(fight.pendingRewards().equals(java.util.Set.of(away.getUUID())), "only who fought along a moment ago: " + fight.pendingRewards());
        fight.givePending(level);
        helper.assertTrue(fight.pendingRewards().isEmpty() && GuhQuests.saved(away).getIntOr(GuheindeGevecht.WINS, 0) == 1
                && count(away, GuheindeFeature.KNABBELKROON.get()) == 1 && has(away, "guheinde/guheinde_winst"), "back (alive): the reward");
        level.getEntitiesOfClass(GuhEntity.class, area(helper), g -> g.getVariant() == GuhVariant.VAHOEGE_ENDER).forEach(Entity::discard);
        // an abandoned recalled fight
        fight.fightActive = true;
        fight.everWon = true;
        fight.setLastActivity(now - GuheindeGevecht.ABANDONED + 100);
        helper.assertTrue(!fight.isAbandoned(now), "somebody was there a moment ago");
        fight.setLastActivity(now - GuheindeGevecht.ABANDONED);
        helper.assertTrue(fight.isAbandoned(now), "nobody for twenty minutes: given up");
        fight.everWon = false;
        helper.assertTrue(!fight.isAbandoned(now), "the first fight stays");
        fight.everWon = true;
        fight.abandonFight(level);
        helper.assertTrue(!fight.fightActive && fight.bossId == null && !fight.isAbandoned(now + 100000), "the fight is over");
        done(helper, here, away);
    }

    // --- top-ups -------------------------------------------------------------------------------------------------------------------------

    /** A muisje comes to a building guh when there is no wild one around; the little land animals that come are come-and-go ones. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void crittersGetAGentleTopUp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = player(helper);
        level.getEntitiesOfClass(PieppiepmuisjeEntity.class, a.getBoundingBox().inflate(128)).forEach(Entity::discard);
        helper.assertTrue(VoorIedereen.muisjeBij(a, p -> true) == null, "no building guh near: no muisje");
        GuhEntity guh = spawn(helper, ModEntities.GUH.get(), 2.5, 1, 2.5);
        guh.setPersistenceRequired();
        helper.assertTrue(VoorIedereen.muisjeBij(a, p -> false) == null, "not in a lief building: no muisje");
        PieppiepmuisjeEntity muis = VoorIedereen.muisjeBij(a, p -> true);
        helper.assertTrue(muis != null && !muis.isTame() && WildeDieren.isKomEnGa(muis), "a wild come-and-go muisje next to the guh");
        helper.assertTrue(VoorIedereen.muisjeBij(a, p -> true) == null, "one wild muisje around is enough");
        muis.tame(a);
        helper.assertTrue(!WildeDieren.isKomEnGa(muis), "tamed: it stays (and is saved)");
        PieppiepmuisjeEntity tweede = VoorIedereen.muisjeBij(a, p -> true);
        helper.assertTrue(tweede != null, "a tamed one doesn't count: the next player gets one too");
        // a land animal of the top-up
        BlockPos gras = new BlockPos(3, 0, 3);
        helper.setBlock(gras, Blocks.GRASS_BLOCK);
        Landdiertje dier = VoorIedereen.diertjeOp(level, LanddiertjesFeature.GUH_KONIJNTJE.get(), helper.absolutePos(gras.above()), level.getRandom());
        if (dier != null) {     // (its own spawn rules decide: light, ground)
            helper.assertTrue(!dier.isTame() && WildeDieren.isKomEnGa(dier), "a come-and-go konijntje");
            dier.discard();
        }
        helper.assertTrue(VoorIedereen.isLanddiertje(LanddiertjesFeature.PLUISEGELTJE.get()) && !VoorIedereen.isLanddiertje(LanddiertjesFeature.SHUCKLE.get()),
                "the three little land animals (not Sjokkel: it has its own spawn rules)");
        muis.discard();
        tweede.discard();
        guh.discard();
        done(helper, a);
    }
}
