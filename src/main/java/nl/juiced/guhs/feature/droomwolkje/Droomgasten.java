package nl.juiced.guhs.feature.droomwolkje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomSlapen;
import nl.juiced.guhs.feature.slaapzorg.Slapen;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.GuhSavedData;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.8.0 (uitbreiding U1): droomgasten. Now and then a tamed guh of another player that is asleep right now (in a slaapplek,
 * or tucked in) visits the Droomplein in its dream: a stand-in with its looks and name ("... (droomgast)"), never saved,
 * gone again after a few minutes. Give it a knuffel (empty hand) or a kaasknabbel, and its owner gets a little message
 * ("<speler> gaf <guh> een knuffel in Guhdromenland"); an offline owner gets it at the next login ({@link Post}).
 * At most {@link #MAX} guests at a time.
 */
public final class Droomgasten {
    /** Persistent data key of a guest (the real guh's UUID): a guest that isn't ours (loaded from disk) never comes in. */
    static final String GAST = "guhs_droomgast";
    public static final int MAX = 3;
    /** How long a guest stays (ticks), and how often a new one may come (ticks, per try). */
    static final int BLIJFT = 20 * 60 * 3, ELKE = 20 * 30;
    /** One knuffel or knabbel per giver per guest per this many ticks (the owner's inbox stays calm). */
    static final int RUST = 20 * 20;

    /** A living guest: whose guh, which guh, its name, when it goes. */
    record Gast(UUID eigenaar, UUID bron, Component naam, long weg) {
    }

    private static final Map<UUID, Gast> LEVEND = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAATST = new HashMap<>();

    private Droomgasten() {
    }

    public static boolean isGast(Entity e) {
        return e.getPersistentData().contains(GAST);
    }

    public static int aantal() {
        return LEVEND.size();
    }

    // =====================================================================================================================
    // coming and going
    // =====================================================================================================================

    /** Every 30 seconds while someone dreams on the plein: old guests go, and maybe a new one comes. */
    static void tick(MinecraftServer server) {
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        if (droom == null) {
            return;
        }
        long nu = droom.getGameTime();
        for (Map.Entry<UUID, Gast> e : List.copyOf(LEVEND.entrySet())) {
            Entity ent = droom.getEntity(e.getKey());
            if (ent == null || nu >= e.getValue().weg()) {
                LEVEND.remove(e.getKey());
                if (ent != null) {
                    poef(droom, ent);
                    ent.discard();
                }
            }
        }
        List<ServerPlayer> opPlein = new ArrayList<>();
        Vec3 plein = Vec3.atBottomCenterOf(DroomEilanden.midden(DroomEilanden.PLEIN));
        for (ServerPlayer p : droom.players()) {
            if (p.position().distanceTo(plein) < 48) {
                opPlein.add(p);
            }
        }
        if (opPlein.isEmpty() || LEVEND.size() >= MAX || droom.getRandom().nextInt(2) != 0) {
            return;
        }
        GuhEntity bron = kies(server, opPlein, droom.getRandom());
        if (bron != null) {
            maak(droom, bron);
        }
    }

    /** A sleeping (or tucked-in) tamed guh of someone who isn't the only dreamer on the plein, not visiting already. */
    @Nullable
    static GuhEntity kies(MinecraftServer server, List<ServerPlayer> opPlein, RandomSource rng) {
        List<GuhEntity> kandidaten = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (DromenFeature.isDroom(level)) {
                continue;
            }
            level.getEntities(EntityTypeTest.forClass(GuhEntity.class), g -> g.isTame() && g.getOwnerUUID() != null
                    && !isGast(g) && !bezoekt(g.getUUID()) && (Slapen.slaapt(g) || DroomSlapen.isIngestopt(g))
                    && opPlein.stream().anyMatch(p -> !p.getUUID().equals(g.getOwnerUUID())), kandidaten, 64);
        }
        return kandidaten.isEmpty() ? null : kandidaten.get(rng.nextInt(kandidaten.size()));
    }

    private static boolean bezoekt(UUID bron) {
        return LEVEND.values().stream().anyMatch(g -> g.bron().equals(bron));
    }

    /** A guest with this guh's looks, somewhere on the plein near the arrival spot. Null when it can't be made. */
    @Nullable
    public static GuhEntity maak(ServerLevel droom, GuhEntity bron) {
        GuhEntity g = ModEntities.GUH.get().create(droom, EntitySpawnReason.TRIGGERED);
        if (g == null) {
            return null;
        }
        CompoundTag looks = Band.looks(bron);
        g.setVariant(GuhVariant.byId(looks.getStringOr("Variant", "")));
        GuhPersonality aard = GuhPersonality.byId(looks.getStringOr("Personality", ""));
        if (aard != null) {
            g.setPersonality(aard);
        }
        CompoundTag kleren = looks.getCompoundOrEmpty("Clothes");
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            GuhClothes c = GuhClothes.byId(kleren.getStringOr(slot.name(), ""));
            if (c != null) {
                g.wear(c);
            }
        }
        if (bron.getHaarkleur() >= 0) {
            g.setHaarkleur(bron.getHaarkleur());
        }
        g.setGuhScale(Math.min(bron.getGuhScale(), 1.4f));
        if (bron.isBaby()) {
            g.setAge(-24000);
        }
        Component naam = bron.hasCustomName() ? bron.getCustomName().copy() : bron.getName().copy();
        g.setCustomName(Component.translatable("gui.guhs.droomwolkje.gast.naam", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        BlockPos plek = plekje(droom);
        g.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, droom.getRandom().nextFloat() * 360f, 0f);
        g.setHomeTo(plek, 6);
        g.setInvulnerable(true);
        g.getPersistentData().putString(GAST, bron.getUUID().toString());
        LEVEND.put(g.getUUID(), new Gast(bron.getOwnerUUID(), bron.getUUID(), naam, droom.getGameTime() + BLIJFT));   // (before it joins)
        if (!droom.addFreshEntity(g)) {
            LEVEND.remove(g.getUUID());
            return null;
        }
        droom.sendParticles(ParticleTypes.END_ROD, g.getX(), g.getY() + 0.6, g.getZ(), 14, 0.3, 0.5, 0.3, 0.03);
        droom.playSound(null, g.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.4f);
        return g;
    }

    /** A free spot with ground on the plein, a few blocks from the arrival spot (the arrival spot itself when nothing fits). */
    static BlockPos plekje(ServerLevel droom) {
        Vec3 a = DroomEilanden.wereld(DroomEilanden.PLEIN, DroomEilanden.opzet(DroomEilanden.PLEIN).aankomst().plek());
        BlockPos aankomst = BlockPos.containing(a);
        RandomSource rng = droom.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos p = aankomst.offset(rng.nextInt(13) - 6, 0, rng.nextInt(13) - 6);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos q = p.above(dy);
                if (droom.isEmptyBlock(q) && droom.isEmptyBlock(q.above()) && droom.getBlockState(q.below()).isFaceSturdy(droom, q.below(), Direction.UP)
                        && q.distSqr(aankomst) >= 4) {
                    return q;
                }
            }
        }
        return aankomst;
    }

    private static void poef(ServerLevel level, Entity e) {
        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.8, e.getZ(), 8, 0.3, 0.5, 0.3, 0.03);
    }

    /** A guest that isn't one of ours (saved by a crash, loaded from disk) never comes in. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && isGast(event.getEntity()) && !LEVEND.containsKey(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    /** The server stops: no guest is saved. */
    static void allesWeg(MinecraftServer server) {
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        for (UUID id : List.copyOf(LEVEND.keySet())) {
            Entity e = droom == null ? null : droom.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        LEVEND.clear();
        LAATST.clear();
    }

    // =====================================================================================================================
    // a knuffel or a knabbel
    // =====================================================================================================================

    /** A click on a guest: knuffel (empty hand) or knabbel (a kaasknabbel in the hand). True when it was a guest. */
    static boolean klik(Entity doel, Player speler, InteractionHand hand) {
        if (!isGast(doel)) {
            return false;
        }
        if (!(speler instanceof ServerPlayer sp) || !(doel.level() instanceof ServerLevel level)) {
            return true;
        }
        Gast gast = LEVEND.get(doel.getUUID());
        if (gast == null) {
            return true;
        }
        ItemStack stack = sp.getItemInHand(hand);
        String soort;
        if (stack.is(ModItems.KAAS_KNABBELS.get())) {
            soort = "knabbel";
        } else if (stack.isEmpty()) {
            soort = "knuffel";
        } else {
            return true;
        }
        String sleutel = sp.getUUID() + "/" + doel.getUUID() + "/" + soort;
        long nu = level.getGameTime();
        Long eerder = LAATST.get(sleutel);
        if (eerder != null && nu - eerder < RUST) {
            level.sendParticles(ParticleTypes.HEART, doel.getX(), doel.getY() + 1, doel.getZ(), 1, 0.2, 0.2, 0.2, 0);
            return true;
        }
        LAATST.put(sleutel, nu);
        if (soort.equals("knabbel") && !sp.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.sendParticles(soort.equals("knabbel") ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.HEART, doel.getX(), doel.getY() + 1,
                doel.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, doel.blockPosition(), soort.equals("knabbel") ? SoundEvents.GENERIC_EAT.value() : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 0.8f, 1.3f);
        sp.sendOverlayMessage(Component.translatable("gui.guhs.droomwolkje.gast." + soort + ".jij", gast.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!sp.getUUID().equals(gast.eigenaar())) {
            meld(level.getServer(), gast.eigenaar(), bericht(soort, sp.getDisplayName(), gast.naam()));
        }
        return true;
    }

    /** The owner's message: "<speler> gaf <guh> een knuffel in Guhdromenland 💤" (or a knabbel). */
    public static Component bericht(String soort, Component gever, Component guh) {
        return Component.translatable("gui.guhs.droomwolkje.gast.bericht." + soort, gever, guh).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /** To the owner now when online, else at their next login. */
    public static void meld(MinecraftServer server, UUID eigenaar, Component bericht) {
        ServerPlayer p = server.getPlayerList().getPlayer(eigenaar);
        if (p != null) {
            p.sendSystemMessage(bericht);
            return;
        }
        Post post = Post.van(server);
        CompoundTag t = new CompoundTag();
        Tekst.put(t, "T", bericht);
        ListTag l = post.berichten.computeIfAbsent(eigenaar, k -> new ListTag());
        if (l.size() < 20) {   // (an inbox that was never read stays small)
            l.add(t);
            post.setDirty();
        }
    }

    /** Takes this player's waiting messages (empty when none). */
    public static List<Component> bezorg(MinecraftServer server, UUID eigenaar) {
        Post post = Post.van(server);
        ListTag l = post.berichten.remove(eigenaar);
        List<Component> uit = new ArrayList<>();
        if (l != null) {
            post.setDirty();
            for (var t : l) {
                if (t instanceof CompoundTag c) {
                    uit.add(Tekst.get(c, "T"));
                }
            }
        }
        return uit;
    }

    /** The messages waiting for owners who were offline (in the overworld's saved data). */
    public static final class Post extends SavedData {
        public static final SavedDataType<Post> TYPE = GuhSavedData.tagType("droomgasten_post", Post::new, Post::load, Post::save);
        final Map<UUID, ListTag> berichten = new HashMap<>();

        public Post() {
        }

        private static Post load(CompoundTag tag) {
            Post p = new Post();
            for (String k : tag.keySet()) {
                try {
                    p.berichten.put(UUID.fromString(k), tag.getListOrEmpty(k));
                } catch (IllegalArgumentException ignored) {
                    // (a broken entry: skipped)
                }
            }
            return p;
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            berichten.forEach((id, l) -> t.put(id.toString(), l));
            return t;
        }

        static Post van(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }
    }
}
