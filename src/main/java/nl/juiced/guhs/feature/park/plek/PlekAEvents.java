package nl.juiced.guhs.feature.park.plek;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.bio.bouwmeer.BouwMeerSlice;
import nl.juiced.guhs.feature.bio.bouwmeer.MeerpaalBlock;
import nl.juiced.guhs.feature.bio.bouwmeer.RoeibootjeEntity;
import nl.juiced.guhs.feature.park.ParkFeature;
import nl.juiced.guhs.feature.waterdiertjes.GuhEendjeEntity;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.4.2 (Het Knabbelpark, plekjes A): the game-bus side.
 * <ul>
 *   <li>Every two seconds, for a player in the Guhmensie near a park ({@link #zorg}): a roeibootje lies ready at the jetty
 *       of every eendenvijver (the roeibootje's own rules make an away boat go after a minute, so there is always one and
 *       never a pile), the eendjes, the dancers and the koi that wandered off go back home ({@link #thuis}), and the
 *       eendjes' kuikentjes stay little (a mama that lost her rijtje gets a new one).</li>
 *   <li>The jukebox of a muziektent ({@link #jukebox}): a click with no record in your hand puts on a record from the
 *       park's own box (marked, see {@link #PLAATJE}); a park record never comes out: a click stops it (or, when the song
 *       is over, puts on another). Your own records go in and out as always.</li>
 *   <li>Feeding a park eendje bread, seeds or a kaasknabbel ({@link #voerEendje}): kwak-njeg, hearts, the kuikentjes hop.
 *       No breeding: the pond stays as it is.</li>
 * </ul>
 */
public final class PlekAEvents {
    /** The custom data key that marks a record of the park's own box. */
    public static final String PLAATJE = "guhs_parkplek_plaatje";
    /** The records in the park's box. */
    public static final List<Item> PLATEN = List.of(Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI,
            Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD);
    /** How often (ticks) and how far (blocks) a player's park is looked after. */
    public static final int ZORG_STAP = 40, ZORG_BEREIK = 64;
    /** How far the inhabitants may wander from their spot before they go back: eendjes, dancers, koi. */
    public static final int EEND_THUIS = 7, DANSER_THUIS = 3, KOI_THUIS = 6;
    /** What a park eendje eats from your hand. */
    public static final List<Item> EENDENVOER = List.of(Items.BREAD, Items.WHEAT_SEEDS);

    private PlekAEvents() {
    }

    // --- upkeep -----------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.isSpectator() && (p.tickCount + p.getId()) % ZORG_STAP == 7
                && p.level().dimension() == ModDimensions.GUHMENSION) {
            Structure park = Kopieen.structuur(p.level(), ParkFeature.STRUCTUUR);
            if (park != null) {
                for (StructureStart start : Kopieen.bij(p.level(), park, p.blockPosition(), ZORG_BEREIK)) {
                    zorg(p.level(), start);
                }
            }
        }
    }

    /** Keeps the plekjes of this park in order (see the class text); returns how many things it did. */
    public static int zorg(ServerLevel level, StructureStart start) {
        int n = 0;
        BlockPos ligplaats = Kopieen.wereld(start, PlekAPlekken.EENDENVIJVER, PlekAPlekken.LIGPLAATS);
        if (ligplaats != null) {
            n += boot(level, ligplaats, Kopieen.draai(start, PlekAPlekken.EENDENVIJVER)) ? 1 : 0;
        }
        for (int i = 0; i < PlekAPlekken.EENDEN.size(); i++) {
            Mob mama = bewoner(level, start, PlekA.EEND_ID + (i + 1), PlekAPlekken.EENDENVIJVER, PlekAPlekken.EENDEN.get(i));
            if (mama instanceof GuhEendjeEntity eend) {
                n += thuis(eend, Kopieen.wereld(start, PlekAPlekken.EENDENVIJVER, PlekAPlekken.EENDEN.get(i)), EEND_THUIS) ? 1 : 0;
                n += rijtje(level, eend);
            }
        }
        for (int i = 0; i < PlekAPlekken.DANSERS.size(); i++) {
            Mob danser = bewoner(level, start, PlekA.DANSER_ID + (i + 1), PlekAPlekken.MUZIEKTUIN, PlekAPlekken.DANSERS.get(i));
            n += thuis(danser, Kopieen.wereld(start, PlekAPlekken.MUZIEKTUIN, PlekAPlekken.DANSERS.get(i)), DANSER_THUIS) ? 1 : 0;
        }
        for (int i = 0; i < PlekAPlekken.KOIEN.size(); i++) {
            Mob koi = bewoner(level, start, PlekA.KOI_ID + (i + 1), PlekAPlekken.JAPANSE_TUIN, PlekAPlekken.KOIEN.get(i));
            n += thuis(koi, Kopieen.wereld(start, PlekAPlekken.JAPANSE_TUIN, PlekAPlekken.KOIEN.get(i)), KOI_THUIS) ? 1 : 0;
        }
        return n;
    }

    /** The registered inhabitant {@code id} of this copy (by its Bezetting tag, near its spot), or null. */
    @Nullable
    static Mob bewoner(ServerLevel level, StructureStart start, String id, String stuk, BlockPos lokaal) {
        BlockPos pos = Kopieen.wereld(start, stuk, lokaal);
        if (pos == null || !level.isLoaded(pos)) {
            return null;
        }
        String tag = Bezetting.tag(id, start);
        List<Mob> l = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(40), m -> {
            String t = m.getPersistentData().getStringOr(Bezetting.TAG, "");
            return m.isAlive() && (t.equals(tag) || t.equals(id));
        });
        return l.isEmpty() ? null : l.get(0);
    }

    /** A wanderer further than {@code straal} from its spot walks (or swims) back; true when it was sent. */
    public static boolean thuis(@Nullable Mob mob, @Nullable BlockPos plek, int straal) {
        if (mob == null || plek == null || mob.isPassenger() || mob.isLeashed()) {
            return false;
        }
        Vec3 doel = Vec3.atBottomCenterOf(plek);
        if (mob.position().distanceToSqr(doel.x, mob.getY(), doel.z) <= straal * straal) {
            return false;
        }
        mob.getNavigation().moveTo(doel.x, doel.y, doel.z, 1.1);
        return true;
    }

    /** The kuikentjes of a park mama stay little; a mama with fewer than two gets her rijtje back. Returns how many came. */
    static int rijtje(ServerLevel level, GuhEendjeEntity mama) {
        List<GuhEendjeEntity> rij = mama.rijtje();
        for (GuhEendjeEntity k : rij) {
            if (k.getAge() > GuhEendjeEntity.KUIKEN_TIJD / 2) {
                k.setAge(GuhEendjeEntity.KUIKEN_TIJD);
            }
        }
        if (rij.size() >= 2 || !level.areEntitiesLoaded(ChunkPos.pack(mama.blockPosition()))) {
            return 0;
        }
        PlekA.kuikens(level, mama, PlekA.KUIKENS - rij.size());
        return PlekA.KUIKENS - rij.size();
    }

    /** A roeibootje lies in this berth whenever it is empty (never more than one empty one there). True: a new one came. */
    public static boolean boot(ServerLevel level, BlockPos ligplaats, Rotation draai) {
        if (!level.isPositionEntityTicking(ligplaats) || !level.areEntitiesLoaded(ChunkPos.pack(ligplaats))
                || !level.getFluidState(ligplaats).is(FluidTags.WATER)) {
            return false;
        }
        List<RoeibootjeEntity> liggend = MeerpaalBlock.liggend(level, ligplaats);
        for (int i = 1; i < liggend.size(); i++) {
            liggend.get(i).verdwijn(level);
        }
        if (!liggend.isEmpty()) {
            if (liggend.get(0).thuis() == null || !liggend.get(0).thuis().equals(ligplaats)) {
                liggend.get(0).zetThuis(ligplaats);
            }
            return false;
        }
        RoeibootjeEntity boot = BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (boot == null) {
            return false;
        }
        boot.zetThuis(ligplaats);
        boot.setInitialPos(ligplaats.getX() + 0.5, ligplaats.getY() + 0.9, ligplaats.getZ() + 0.5);
        boot.setYRot(draai.rotate(Direction.NORTH).toYRot());
        level.addFreshEntity(boot);
        return true;
    }

    // --- the muziektent ---------------------------------------------------------------------------------------------------

    /** Is this a record of the park's own box? */
    public static boolean isPlaatje(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(PLAATJE, false);
    }

    /** A record from the park's box (marked). */
    public static ItemStack plaatje(net.minecraft.util.RandomSource random) {
        ItemStack stack = new ItemStack(PLATEN.get(random.nextInt(PLATEN.size())));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(PLAATJE, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ServerLevel level = p.level();
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(Blocks.JUKEBOX) || !ParkFeature.STRUCTUUR.equals(Bescherming.structuurBij(level, pos))) {
            return;
        }
        if (jukebox(p, pos, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * A click on a park jukebox with this in the main hand. True: handled here (the click goes no further); false: the
     * jukebox's own (a record of your own goes in or comes out).
     */
    public static boolean jukebox(ServerPlayer p, BlockPos pos, ItemStack hand) {
        if (!(p.level().getBlockEntity(pos) instanceof JukeboxBlockEntity box)) {
            return false;
        }
        ItemStack in = box.getTheItem();
        if (in.isEmpty()) {
            if (hand.has(DataComponents.JUKEBOX_PLAYABLE)) {
                GuhAdvancements.grant(p, "parkplek_muziek");   // (their own record: the jukebox takes it)
                return false;
            }
            speel(p, box);
            return true;
        }
        if (!isPlaatje(in)) {
            return false;
        }
        if (box.getSongPlayer().isPlaying()) {
            box.setTheItem(ItemStack.EMPTY);
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.muziek.uit").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            speel(p, box);
        }
        return true;
    }

    private static void speel(ServerPlayer p, JukeboxBlockEntity box) {
        ItemStack plaat = plaatje(p.getRandom());
        box.setTheItem(plaat);
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.muziek.aan").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(p, "parkplek_muziek");
    }

    // --- the eendjes ------------------------------------------------------------------------------------------------------

    /** Is this an eendje of a park pond (a mama or one of her kuikentjes)? */
    public static boolean isParkEendje(net.minecraft.world.entity.Entity e) {
        return e instanceof GuhEendjeEntity && (e.getPersistentData().getBooleanOr(PlekA.KUIKEN, false)
                || e.getPersistentData().getStringOr(Bezetting.TAG, "").startsWith(PlekA.EEND_ID));
    }

    @SubscribeEvent
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide() && isParkEendje(event.getTarget()) && isEendenvoer(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer p) {
                voerEendje(p, (GuhEendjeEntity) event.getTarget(), event.getItemStack());
            }
        }
    }

    public static boolean isEendenvoer(ItemStack stack) {
        return stack.is(ModItems.KAAS_KNABBELS.get()) || EENDENVOER.stream().anyMatch(stack::is);
    }

    /** Kwak-njeg! One bite, hearts, and the whole rijtje hops for joy. */
    public static void voerEendje(ServerPlayer p, GuhEendjeEntity eend, ItemStack stack) {
        ServerLevel level = p.level();
        stack.consume(1, p);
        level.playSound(null, eend, WaterdiertjesFeature.KWAK.get(), SoundSource.NEUTRAL, 1f, eend.isBaby() ? 1.5f : 1.1f);
        level.sendParticles(ParticleTypes.HEART, eend.getX(), eend.getY() + 0.7, eend.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        GuhEendjeEntity mama = eend.isBaby() ? eend.mamaHier() : eend;
        if (mama != null) {
            for (GuhEendjeEntity k : mama.rijtje()) {
                k.setDeltaMovement(k.getDeltaMovement().add(0, 0.32, 0));
                k.hurtMarked = true;
            }
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkplek.eendjes." + level.getRandom().nextInt(3)).withStyle(ChatFormatting.YELLOW));
        GuhAdvancements.grant(p, "parkplek_eendjes");
    }
}
