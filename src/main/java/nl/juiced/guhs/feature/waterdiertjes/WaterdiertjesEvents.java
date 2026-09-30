package nl.juiced.guhs.feature.waterdiertjes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeDeelBlock;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The game-bus side of the waterdiertjes: the Diertjes-tab advancements (the five Guhdex pages, all five, a golden
 * guhxolotl close by, a mama with her rijtje, every colour tamed, a lieveheersbeestje's growth help), ladybirds that come
 * to growing guhtuintjes, and the empty bucket back when a real emmertje moves into a guhhuisje.
 */
public final class WaterdiertjesEvents {
    /** Player saved data: the guhxolotl colours this player tamed (bits = Kleur ordinals). */
    public static final String KLEUREN = "guhs_waterdiertjes_kleuren";
    /** Every this many ticks per player: the checks below (spread by the player's id). */
    public static final int ELKE = 40;
    /** A ladybird may come to a growing tuintje near you about every this many ticks. */
    public static final int LOK_ELKE = 20 * 30;

    /** Players whose real emmertje (a water bucket) just went into a guhhuisje: the empty bucket comes back next tick. */
    private static final Map<UUID, InteractionHand> EMMER_TERUG = new ConcurrentHashMap<>();

    private WaterdiertjesEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        InteractionHand hand = EMMER_TERUG.remove(p.getUUID());
        if (hand != null && !p.getItemInHand(hand).is(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get()) && !p.hasInfiniteMaterials()) {
            if (p.getItemInHand(hand).isEmpty()) {
                p.setItemInHand(hand, new ItemStack(Items.BUCKET));
            } else if (!p.getInventory().add(new ItemStack(Items.BUCKET))) {
                p.drop(new ItemStack(Items.BUCKET), false);
            }
        }
        if ((p.tickCount + p.getId()) % ELKE != 0) {
            return;
        }
        paginas(p);
        ServerLevel level = p.level();
        for (GuhxolotlEntity x : level.getEntitiesOfClass(GuhxolotlEntity.class, p.getBoundingBox().inflate(6))) {
            if (x.kleur() == GuhxolotlEntity.Kleur.GOUD) {
                geef(p, "waterdiertjes_goud");
                break;
            }
        }
        for (GuhEendjeEntity mama : level.getEntitiesOfClass(GuhEendjeEntity.class, p.getBoundingBox().inflate(10), e -> !e.isBaby())) {
            if (mama.rijtje().size() >= 3) {
                geef(p, "waterdiertjes_rijtje");
                break;
            }
        }
        if ((p.tickCount + p.getId()) % LOK_ELKE == 0) {
            lokLieveheersbeestje(level, p);
        }
    }

    /** The visible "page seen" advancements of the Diertjes tab (the pages themselves are filled in by the GuhDex). */
    static void paginas(ServerPlayer p) {
        GuhWorldData.PlayerData data = GuhWorldData.get(p.level().getServer()).player(p.getUUID());
        int gezien = 0;
        for (GuhVariant v : WaterdiertjesFeature.PAGINAS) {
            if (data.seen.contains(v)) {
                gezien++;
                GidsFeature.grant(p, "diertjes/waterdiertjes_" + v.id());
            }
        }
        if (gezien == WaterdiertjesFeature.PAGINAS.size()) {
            GidsFeature.grant(p, "diertjes/waterdiertjes_alle");
        }
    }

    /** A code-granted achievement: the hidden quest one (FTB) and the visible one in the Diertjes tab. */
    static void geef(ServerPlayer p, String naam) {
        GuhAdvancements.grant(p, naam);
        GidsFeature.grant(p, "diertjes/" + naam);
    }

    /** (GuhxolotlEntity) this player tamed a guhxolotl: remember its colour; all five: the challenge. */
    public static void getemd(ServerPlayer p, GuhxolotlEntity x) {
        CompoundTag saved = GuhQuests.saved(p);
        int bits = saved.getIntOr(KLEUREN, 0) | (1 << x.kleur().ordinal());
        saved.putInt(KLEUREN, bits);
        if (bits == (1 << GuhxolotlEntity.Kleur.values().length) - 1) {
            geef(p, "waterdiertjes_alle_kleurtjes");
            p.sendSystemMessage(Component.translatable("gui.guhs.waterdiertjes.alle_kleurtjes").withStyle(ChatFormatting.GOLD));
        }
    }

    /** (LieveheersbeestjeEntity) a ladybird helped a tuintje grow close to this player. */
    public static void tuinhulp(ServerPlayer p) {
        geef(p, "waterdiertjes_tuinhulp");
    }

    /**
     * By day, a growing guhtuintje within 12 blocks of the player and no ladybird around: now and then one comes flying in
     * (also in the overworld, wherever you have your tuintjes).
     */
    static void lokLieveheersbeestje(ServerLevel level, ServerPlayer p) {
        if (!level.isDay() || level.getRandom().nextInt(3) != 0) {
            return;
        }
        BlockPos here = p.blockPosition();
        BlockPos tuin = null;
        for (BlockPos q : BlockPos.betweenClosed(here.offset(-12, -4, -12), here.offset(12, 4, 12))) {
            if (TuinBlock.groeit(level.getBlockState(q)) && level.canSeeSky(q.above())) {
                tuin = q.immutable();
                break;
            }
        }
        if (tuin == null || !level.getEntitiesOfClass(LieveheersbeestjeEntity.class, new AABB(tuin).inflate(24)).isEmpty()) {
            return;
        }
        LieveheersbeestjeEntity lhb = WaterdiertjesFeature.LIEVEHEERSBEESTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (lhb == null) {
            return;
        }
        Vec3 van = Vec3.atCenterOf(tuin).add(level.getRandom().nextInt(9) - 4, 3 + level.getRandom().nextInt(2), level.getRandom().nextInt(9) - 4);
        if (!level.getBlockState(BlockPos.containing(van)).isAir()) {
            return;
        }
        lhb.snapTo(van.x, van.y, van.z, level.getRandom().nextFloat() * 360, 0);
        lhb.finalizeSpawn(level, level.getCurrentDifficultyAt(tuin), EntitySpawnReason.EVENT, null);
        level.addFreshEntity(lhb);
    }

    /** A real emmertje (it was a water bucket) moving into a guhhuisje: its empty bucket comes back. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(WaterdiertjesFeature.GUHXOLOTL_EMMERTJE.get()) || !GuhxolotlEmmertje.metEmmer(stack)) {
            return;
        }
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (block instanceof HuisjeBlock || block instanceof HuisjeDeelBlock) {
            EMMER_TERUG.put(p.getUUID(), event.getHand());
        }
    }

    /** (Tests) which colours this player tamed. */
    public static List<GuhxolotlEntity.Kleur> getemdeKleuren(ServerPlayer p) {
        int bits = GuhQuests.saved(p).getIntOr(KLEUREN, 0);
        return java.util.Arrays.stream(GuhxolotlEntity.Kleur.values()).filter(k -> (bits & (1 << k.ordinal())) != 0).toList();
    }
}
