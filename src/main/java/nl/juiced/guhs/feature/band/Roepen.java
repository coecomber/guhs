package nl.juiced.guhs.feature.band;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhkamer.Guhkamer;
import nl.juiced.guhs.feature.guhkamer.GuhkamerData;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.registry.ModSounds;

/**
 * "Roep naar mij" (1.2.5): the button on a guh's page in the Guhdex (Mijn guhs) calls that tamed guh to its owner,
 * wherever it is: in another dimension or an unloaded chunk too (its chunk is loaded with a ticket and we wait for the
 * entity to load, at most {@value #WACHT} ticks). It arrives ~2 blocks from the player on safe ground, as the very same
 * guh (vanilla teleport: hearts, clothes, name, favourites, dagboekje all stay). Where it came from is cleaned up: out of
 * its Guhhuisje ({@link Huisjes#trekUit}), checked out of the Guhkamer ({@link Guhkamer#roep}), no longer sitting or
 * wandering, off any vehicle / lead (the lead goes into your pockets). Not for a guh that is picked up (an item in a
 * chest, backpack, pockets, a Bank Guh or on the ground), running in a Guh Wheel or in the wolkjes, and only by its owner.
 */
public final class Roepen {
    /** How long we wait for an unloaded guh's entity to load. */
    static final int WACHT = 1200;   // (1.2.6: was 200; a busy server needs longer to load a far chunk and its entities)
    private static final int TICKET_STRAAL = 2;

    public static final DeferredRegister<TicketType> TICKET_TYPES = DeferredRegister.create(Registries.TICKET_TYPE, Guhs.MODID);
    /** Loads + ticks the chunk of a guh being called (and keeps its dimension running); no timeout (removed when it's found or we give up), not saved. */
    private static final DeferredHolder<TicketType, TicketType> TICKET = TICKET_TYPES.register("roep",
            () -> new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    public enum Uitkomst { OK, BEZIG, NIET_JOUW, OPGEPAKT, GUHWIEL, DOOD, KWIJT }

    /** A guh being fetched from an unloaded chunk (or a Guhkamer guest that is still asleep in its room). */
    private static final class Klus {
        final ServerPlayer speler;
        final UUID id;
        @Nullable
        final ServerLevel level;
        @Nullable
        final ChunkPos chunk;
        final boolean kamer;
        int ticks;

        Klus(ServerPlayer speler, UUID id, @Nullable ServerLevel level, @Nullable ChunkPos chunk, boolean kamer) {
            this.speler = speler;
            this.id = id;
            this.level = level;
            this.chunk = chunk;
            this.kamer = kamer;
        }
    }

    /** Per band id. */
    private static final Map<UUID, Klus> KLUSSEN = new ConcurrentHashMap<>();

    private Roepen() {
    }

    static void register(IEventBus modBus) {
        TICKET_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Roepen::tick);
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> stopAlles());
    }

    /** Why the button is grey for a guh at this kind of place (the client greys it out too), or null when it may be called. */
    @Nullable
    public static Uitkomst nietRoepbaar(PlekSoort soort, boolean dood) {
        if (dood || soort == PlekSoort.IN_DE_WOLKJES) {
            return Uitkomst.DOOD;
        }
        return switch (soort) {
            case ITEM_SPELER, ITEM_KIST, ITEM_RUGZAK, ITEM_BANK, ITEM_GROND -> Uitkomst.OPGEPAKT;
            case GUHWIEL -> Uitkomst.GUHWIEL;
            default -> null;
        };
    }

    /** Is this guh being fetched right now (tests)? */
    public static boolean bezig(UUID id) {
        return KLUSSEN.containsKey(id);
    }

    /**
     * The player calls their guh (band id = its UUID). OK: it is here now; BEZIG: its chunk is loading, it comes in a moment
     * (or {@link #WACHT} ticks later we say it can't be found). Tells the player (overlay) and refreshes their Guhdex.
     */
    public static Uitkomst roep(ServerPlayer speler, UUID id) {
        Uitkomst u = probeer(speler, id);
        Component naam = naam(speler.level().getServer(), speler.getUUID(), id);
        switch (u) {
            case BEZIG -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.zoeken", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            case NIET_JOUW -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.niet_jouw").withStyle(ChatFormatting.RED));
            case OPGEPAKT -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.opgepakt", naam).withStyle(ChatFormatting.GOLD));
            case GUHWIEL -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.guhwiel", naam).withStyle(ChatFormatting.GOLD));
            case DOOD -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.dood", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            case KWIJT -> speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.kwijt", naam).withStyle(ChatFormatting.GOLD));
            case OK -> {
            }
        }
        if (u != Uitkomst.BEZIG) {
            MijnGuhs.stuur(speler, null);
        }
        return u;
    }

    private static Uitkomst probeer(ServerPlayer speler, UUID id) {
        MinecraftServer s = speler.level().getServer();
        if (KLUSSEN.containsKey(id)) {
            Klus k = KLUSSEN.get(id);
            return k.speler.getUUID().equals(speler.getUUID()) ? Uitkomst.BEZIG : Uitkomst.NIET_JOUW;
        }
        BandData.Rec r = BandData.get(s).vind(speler.getUUID(), id);
        if (r != null && r.dood) {
            return Uitkomst.DOOD;
        }
        Entity geladen = Band.zoekGeladen(s, id);
        if (geladen != null) {
            if (!(geladen instanceof GuhEntity guh) || !Band.isBandGuh(guh) || !speler.getUUID().equals(guh.getOwnerUUID())) {
                return Uitkomst.NIET_JOUW;
            }
        } else if (r == null || !r.guh) {
            return Uitkomst.NIET_JOUW;
        }
        // a guest of the Guhkamer: checked out the Guhkamer's own way (kept as data, or a real guh in the room)
        GuhkamerData.Kamer kamer = GuhkamerData.get(s).vanGast(id);
        if (kamer != null && kamer.eigenaar.equals(speler.getUUID())) {
            Entity e = Guhkamer.roep(speler, id);
            if (e instanceof GuhEntity guh) {
                aankomst(speler, guh, null);
                return Uitkomst.OK;
            }
            KLUSSEN.put(id, new Klus(speler, id, null, null, true));   // (still asleep in its unloaded room: it's being loaded)
            return Uitkomst.BEZIG;
        }
        if (geladen instanceof GuhEntity guh) {
            haal(speler, guh);
            return Uitkomst.OK;
        }
        Uitkomst niet = nietRoepbaar(r.plek.soort(), r.dood);
        if (niet != null) {
            return niet;
        }
        if (r.plek.soort() == PlekSoort.ONBEKEND) {
            return Uitkomst.KWIJT;
        }
        ServerLevel level = s.getLevel(r.plek.dim());
        if (level == null) {
            return Uitkomst.KWIJT;
        }
        ChunkPos chunk = ChunkPos.containing(r.plek.pos());
        level.getChunkSource().addTicketWithRadius(TICKET.get(), chunk, TICKET_STRAAL);
        KLUSSEN.put(id, new Klus(speler, id, level, chunk, false));
        return Uitkomst.BEZIG;
    }

    /** Every tick: is a guh we are fetching loaded yet? */
    private static void tick(ServerTickEvent.Post event) {
        if (KLUSSEN.isEmpty()) {
            return;
        }
        MinecraftServer s = event.getServer();
        for (Iterator<Klus> it = KLUSSEN.values().iterator(); it.hasNext(); ) {
            Klus k = it.next();
            k.ticks++;
            if (k.speler.isRemoved() || k.speler.hasDisconnected()) {
                it.remove();
                losLaten(k);
                continue;
            }
            if (k.kamer) {
                if (k.ticks % 20 == 0) {
                    GuhkamerData.Kamer kamer = GuhkamerData.get(s).vanGast(k.id);
                    Entity e = kamer == null ? Band.zoekGeladen(s, k.id) : Guhkamer.roep(k.speler, k.id);
                    if (e instanceof GuhEntity guh) {
                        it.remove();
                        if (kamer == null) {
                            haal(k.speler, guh);
                        } else {
                            aankomst(k.speler, guh, null);
                        }
                        MijnGuhs.stuur(k.speler, null);
                        continue;
                    }
                }
            } else {
                Entity e = k.level.getEntity(k.id);
                if (e instanceof GuhEntity guh && Band.isBandGuh(guh) && k.speler.getUUID().equals(guh.getOwnerUUID())) {
                    it.remove();
                    haal(k.speler, guh);
                    losLaten(k);
                    MijnGuhs.stuur(k.speler, null);
                    continue;
                }
            }
            if (k.ticks >= WACHT) {
                it.remove();
                losLaten(k);
                k.speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.kwijt", naam(s, k.speler.getUUID(), k.id))
                        .withStyle(ChatFormatting.GOLD));
                MijnGuhs.stuur(k.speler, null);
            }
        }
    }

    private static void losLaten(Klus k) {
        if (k.level != null && k.chunk != null) {
            k.level.getChunkSource().removeTicketWithRadius(TICKET.get(), k.chunk, TICKET_STRAAL);
        }
    }

    private static void stopAlles() {
        for (Klus k : KLUSSEN.values()) {
            losLaten(k);
        }
        KLUSSEN.clear();
    }

    /** A loaded guh of the player: out of where it was, then (across dimensions if needed) next to the player. */
    static GuhEntity haal(ServerPlayer speler, GuhEntity guh) {
        ServerLevel van = (ServerLevel) guh.level();
        van.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        van.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + 1, guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
        if (Huisjes.isBewoner(guh)) {
            Huisjes.trekUit(guh);   // (frees its place and its chores; out of the door if it was asleep inside)
        }
        if (Guhkamer.isGast(guh)) {   // (a guest mark without a room: just take it off)
            Guhkamer.markeer(guh, false);
        }
        guh.stopRiding();
        guh.ejectPassengers();
        if (guh instanceof Leashable l && l.isLeashed()) {
            l.removeLeash();
            ItemStack lijn = new ItemStack(Items.LEAD);
            if (!speler.getInventory().add(lijn)) {
                speler.drop(lijn, false, false);
            }
        }
        rechtOp(guh);
        ServerLevel naar = speler.level();
        Vec3 plek = veiligePlek(naar, speler, guh.getDimensions(guh.getPose()));
        float yaw = (float) (Mth.atan2(speler.getZ() - plek.z, speler.getX() - plek.x) * Mth.RAD_TO_DEG) - 90f;
        Entity nieuw = guh.teleport(new TeleportTransition(naar, plek, Vec3.ZERO, yaw, 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
        GuhEntity aan = nieuw instanceof GuhEntity g ? g : guh;
        aankomst(speler, aan, plek);
        return aan;
    }

    /** Stands up, stops wandering about: it follows its owner again. */
    private static void rechtOp(GuhEntity guh) {
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        guh.setWandering(false);
        guh.fallDistance = 0;
        guh.getNavigation().stop();
    }

    /** It's here: on safe ground next to the player (plek null: find one), poof + hearts + a happy guh, "bij jou". */
    private static void aankomst(ServerPlayer speler, GuhEntity guh, @Nullable Vec3 plek) {
        ServerLevel naar = speler.level();
        rechtOp(guh);
        if (plek == null && guh.level() == naar) {
            Vec3 p = veiligePlek(naar, speler, guh.getDimensions(guh.getPose()));
            guh.teleportTo(p.x, p.y, p.z);
        }
        guh.setDeltaMovement(Vec3.ZERO);
        naar.sendParticles(ParticleTypes.POOF, guh.getX(), guh.getY() + 0.4, guh.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        naar.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + 1, guh.getZ(), 6, 0.3, 0.2, 0.3, 0);
        naar.playSound(null, guh.blockPosition(), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1f);
        Band.bijwerken(guh);
        GuhVolger.zet(guh, PlekSoort.BIJ_JOU, speler.getGameProfile().name());
        speler.sendOverlayMessage(Component.translatable("gui.guhs.mijnguhs.roep.komt", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // a safe spot
    // =====================================================================================================================

    /** About 2 blocks from the player (in front first), on solid ground, not in a wall, liquid, fire or anything else that hurts. */
    public static Vec3 veiligePlek(ServerLevel level, ServerPlayer speler, EntityDimensions maat) {
        Vec3 kijk = speler.getLookAngle();
        double hoek = Math.atan2(kijk.z, kijk.x);
        int y0 = Mth.floor(speler.getY() + 0.01);
        for (int straal : new int[]{2, 3, 1}) {
            for (int i = 0; i < 8; i++) {
                double a = hoek + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * (Math.PI / 4);
                int bx = Mth.floor(speler.getX() + Math.cos(a) * straal), bz = Mth.floor(speler.getZ() + Math.sin(a) * straal);
                if (bx == Mth.floor(speler.getX()) && bz == Mth.floor(speler.getZ())) {
                    continue;
                }
                for (int dy : new int[]{0, 1, -1, 2, -2, -3, -4}) {
                    Vec3 v = staan(level, new BlockPos(bx, y0 + dy, bz), maat);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return speler.position();
    }

    /** Where an entity of this size stands with its feet in this block, or null when that's not a safe spot. */
    @Nullable
    static Vec3 staan(ServerLevel level, BlockPos voet, EntityDimensions maat) {
        BlockPos onder = voet.below();
        BlockState o = level.getBlockState(onder);
        VoxelShape vorm = o.getCollisionShape(level, onder);
        if (vorm.isEmpty() || vorm.max(Direction.Axis.Y) < 0.5 || vorm.max(Direction.Axis.Y) > 1.0 || gevaarlijkOnder(o)) {
            return null;
        }
        double y = onder.getY() + vorm.max(Direction.Axis.Y);
        Vec3 v = new Vec3(voet.getX() + 0.5, y, voet.getZ() + 0.5);
        AABB box = maat.makeBoundingBox(v);
        if (!level.noCollision(box) || level.containsAnyLiquid(box) || !level.getWorldBorder().isWithinBounds(box)) {
            return null;
        }
        for (BlockPos b : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            if (gevaarlijk(level.getBlockState(b))) {
                return null;
            }
        }
        return v;
    }

    private static boolean gevaarlijkOnder(BlockState s) {
        return s.is(Blocks.MAGMA_BLOCK) || s.is(BlockTags.CAMPFIRES) || s.is(BlockTags.LEAVES) || s.is(Blocks.CACTUS)
                || s.is(Blocks.POINTED_DRIPSTONE) || s.is(Blocks.LAVA) || gevaarlijk(s);
    }

    private static boolean gevaarlijk(BlockState s) {
        return s.is(BlockTags.FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.SWEET_BERRY_BUSH)
                || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.COBWEB) || s.is(Blocks.CACTUS) || s.is(Blocks.NETHER_PORTAL)
                || s.is(Blocks.END_PORTAL) || s.is(Blocks.END_GATEWAY);
    }

    private static Component naam(MinecraftServer s, UUID eigenaar, UUID id) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, id);
        return r == null ? Component.literal("Guh") : r.weergave();
    }
}
