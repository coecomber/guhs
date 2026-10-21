package nl.juiced.guhs.feature.knabbelvelden;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.taal.Tekst;

/**
 * The retired guhs in the Knabbelvelden: while their owner is on their own plot, every guh of theirs walks around there as
 * a see-through little ghost with a halo (the KnusVlag {@link #GEEST}; the client draws it see-through and adds the halo).
 * They are stand-ins, not the guhs themselves: a new entity built from the stored looks (fur, personality, clothes, hair,
 * size, name), never tamed, never saved (gone when the owner leaves; one that was saved anyway is removed when it loads).
 * They live calmly within {@link Knabbelvelden#STRAAL} blocks of the plot spawn: nibble, nap, roll about, dance and wave to
 * their owner. A click pets one; the owner sneak-clicks for "Mee naar huis?" (KnabbelPayloads.Vraag).
 */
public final class Geesten {
    /** KnusVlag: a Knabbelvelden ghost (bit 30). */
    public static final int GEEST = 1 << 30;
    static final String EIGENAAR = "guhs_kv_geest_eigenaar", BAND = "guhs_kv_geest_band", ZWAAI = "guhs_kv_geest_zwaai";

    /** A living ghost: whose, and which retired guh. */
    record Geest(UUID eigenaar, UUID band) {
    }

    /** Ghost entity UUID -> ghost. */
    private static final Map<UUID, Geest> LEVEND = new ConcurrentHashMap<>();
    private static final Emote[] RUSTIG = {Emote.SMAKKEN, Emote.SLAPEN, Emote.ROLLEN, Emote.DANSEN, Emote.ZWAAIEN, Emote.GAPEN, Emote.ZINGEN};

    private Geesten() {
    }

    public static boolean isGeest(Entity e) {
        return e instanceof GuhEntity g && GuhHooks.heeft(g, GEEST);
    }

    /** (server) the ghost behind this entity, or null. */
    static Geest geest(Entity e) {
        return LEVEND.get(e.getUUID());
    }

    /** Every second: ghosts for the owners on their plot, none for the others. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        if (s.getTickCount() % 20 != 0) {
            return;
        }
        ServerLevel level = Knabbelvelden.level(s);
        if (level == null) {
            return;
        }
        Set<UUID> eigenaars = new HashSet<>(KnabbelveldenData.get(s).eigenaars());
        for (Geest g : LEVEND.values()) {
            eigenaars.add(g.eigenaar());
        }
        for (UUID e : eigenaars) {
            bijwerken(level, e);
        }
    }

    /** Is this owner on their own plot right now? */
    static boolean opPlot(ServerLevel level, UUID eigenaar) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(eigenaar);
        if (p == null || p.level() != level || p.isSpectator()) {
            return false;
        }
        BlockPos s = Knabbelvelden.spawn(KnabbelveldenData.get(level.getServer()).plot(eigenaar));
        return p.position().distanceToSqr(s.getX() + 0.5, p.getY(), s.getZ() + 0.5) < 160 * 160;
    }

    /** One owner's ghosts as they should be: one per retired guh while they are on their plot, none otherwise. */
    static void bijwerken(ServerLevel level, UUID eigenaar) {
        MinecraftServer s = level.getServer();
        boolean daar = opPlot(level, eigenaar);
        List<KnabbelveldenData.Rust> rust = KnabbelveldenData.get(s).van(eigenaar);
        Set<UUID> gewenst = new HashSet<>();
        if (daar) {
            for (KnabbelveldenData.Rust r : rust) {
                gewenst.add(r.id());
            }
        }
        Set<UUID> er = new HashSet<>();
        for (Map.Entry<UUID, Geest> e : LEVEND.entrySet()) {
            Geest g = e.getValue();
            if (!g.eigenaar().equals(eigenaar)) {
                continue;
            }
            Entity ent = level.getEntity(e.getKey());
            if (ent == null || !gewenst.contains(g.band())) {
                LEVEND.remove(e.getKey());
                if (ent != null) {
                    poef(level, ent);
                    ent.discard();
                }
            } else {
                er.add(g.band());
            }
        }
        if (!daar) {
            return;
        }
        BlockPos spawn = Knabbelvelden.spawn(KnabbelveldenData.get(s).plot(eigenaar));
        for (KnabbelveldenData.Rust r : rust) {
            if (!er.contains(r.id())) {
                maak(level, eigenaar, r, spawn);
            }
        }
    }

    /** A ghost of this retired guh, somewhere around the plot spawn. */
    static GuhEntity maak(ServerLevel level, UUID eigenaar, KnabbelveldenData.Rust r, BlockPos spawn) {
        GuhEntity g = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (g == null) {
            return null;
        }
        CompoundTag looks = r.opslag().getCompoundOrEmpty("Looks");
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
        if (looks.contains("Haar")) {
            g.setHaarkleur(looks.getIntOr("Haar", -1));
        }
        if (looks.contains("Scale")) {
            g.setGuhScale(looks.getFloatOr("Scale", 1f));
        }
        if (looks.getBooleanOr("Baby", false)) {
            g.setAge(-24000);
        }
        g.setCustomName(GuhOpslag.naam(r.opslag()).copy());
        RandomSource rng = level.getRandom();
        double hoek = rng.nextDouble() * Math.PI * 2, afstand = 5 + rng.nextDouble() * 14;
        double x = spawn.getX() + 0.5 + Math.cos(hoek) * afstand, z = spawn.getZ() + 0.5 + Math.sin(hoek) * afstand;
        g.snapTo(x, Knabbelvelden.VLOER, z, rng.nextFloat() * 360f, 0f);
        g.setHomeTo(spawn, Knabbelvelden.STRAAL);
        g.setPersistenceRequired();
        g.setInvulnerable(true);
        g.getPersistentData().putString(EIGENAAR, eigenaar.toString());
        g.getPersistentData().putString(BAND, r.id().toString());
        GuhHooks.zet(g, GEEST, true);
        LEVEND.put(g.getUUID(), new Geest(eigenaar, r.id()));   // (before it joins: the join check knows it)
        if (!level.addFreshEntity(g)) {
            LEVEND.remove(g.getUUID());
            return null;
        }
        level.sendParticles(ParticleTypes.END_ROD, x, Knabbelvelden.VLOER + 0.6, z, 8, 0.3, 0.4, 0.3, 0.03);
        return g;
    }

    /** A retired guh came home: its ghost goes. */
    static void weg(MinecraftServer s, UUID band) {
        ServerLevel level = Knabbelvelden.level(s);
        for (Map.Entry<UUID, Geest> e : LEVEND.entrySet()) {
            if (e.getValue().band().equals(band)) {
                LEVEND.remove(e.getKey());
                Entity ent = level == null ? null : level.getEntity(e.getKey());
                if (ent != null) {
                    poef(level, ent);
                    ent.discard();
                }
            }
        }
    }

    /** The server stops: no ghost is saved. */
    static void allesWeg(MinecraftServer s) {
        ServerLevel level = Knabbelvelden.level(s);
        for (UUID id : LEVEND.keySet()) {
            Entity ent = level == null ? null : level.getEntity(id);
            if (ent != null) {
                ent.discard();
            }
        }
        LEVEND.clear();
    }

    private static void poef(ServerLevel level, Entity e) {
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 0.5, e.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
    }

    /**
     * A ghost that is not one of ours (saved by a crash, loaded from disk) never comes in; nor does a real guh whose UUID
     * is in the Knabbelvelden (never two of the same guh).
     */
    static void opJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof GuhEntity g)) {
            return;
        }
        if (g.getPersistentData().contains(BAND)) {
            if (!LEVEND.containsKey(g.getUUID())) {
                event.setCanceled(true);
            }
            return;
        }
        if (KnabbelveldenData.get(((ServerLevel) event.getLevel()).getServer()).bevat(g.getUUID())) {
            com.mojang.logging.LogUtils.getLogger().warn("Knabbelvelden: guh {} is retired; a copy tried to join {}, removed", g.getUUID(),
                    event.getLevel().dimension().identifier());
            event.setCanceled(true);
        }
    }

    /** (GuhHooks.tick, server) a ghost's calm life: now and then a little something, a wave when its owner comes close. */
    static void leef(GuhEntity g) {
        if (!GuhHooks.heeft(g, GEEST)) {
            return;
        }
        Geest geest = LEVEND.get(g.getUUID());
        if (geest == null) {
            g.discard();   // (a ghost nobody keeps: gone)
            return;
        }
        if (g.fallDistance > 0 && g.getY() < Knabbelvelden.VLOER - 3) {
            BlockPos s = Knabbelvelden.spawn(KnabbelveldenData.get(g.level().getServer()).plot(geest.eigenaar()));
            g.teleportTo(s.getX() + 0.5, Knabbelvelden.VLOER, s.getZ() + 0.5);
        }
        if (g.tickCount % 20 != 0 || g.emotes.current() != null) {
            return;
        }
        RandomSource rng = g.getRandom();
        Player eigenaar = g.level().getPlayerByUUID(geest.eigenaar());
        long nu = g.level().getGameTime();
        if (eigenaar != null && eigenaar.distanceTo(g) < 6 && nu - g.getPersistentData().getLongOr(ZWAAI, -10000L) > 1200
                && GuhEmotes.canStart(g)) {
            g.getPersistentData().putLong(ZWAAI, nu);
            g.getLookControl().setLookAt(eigenaar);
            g.emotes.start(Emote.ZWAAIEN, false, GuhEmotes.Source.SELF);
            return;
        }
        if (rng.nextInt(9) == 0 && GuhEmotes.canStart(g)) {
            g.emotes.start(RUSTIG[rng.nextInt(RUSTIG.length)], false, GuhEmotes.Source.SELF);
        }
    }

    /** (GuhHooks.klik) a click on a ghost: a pet, or (the owner, sneaking) "Mee naar huis?". */
    static InteractionResult klik(GuhEntity g, Player player, InteractionHand hand) {
        if (!GuhHooks.heeft(g, GEEST)) {
            return InteractionResult.PASS;
        }
        if (g.level().isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        Geest geest = LEVEND.get(g.getUUID());
        if (geest == null) {
            return InteractionResult.SUCCESS;
        }
        boolean eigen = geest.eigenaar().equals(sp.getUUID());
        if (eigen && sp.isShiftKeyDown()) {
            CompoundTag t = new CompoundTag();
            t.putInt("Entity", g.getId());
            Tekst.put(t, "Naam", g.getName().copy());
            ModNetworking.sendTo(sp, new KnabbelPayloads.Vraag(t));
            return InteractionResult.SUCCESS;
        }
        if (GuhEmotes.canStart(g)) {
            g.emotes.start(Emote.AAIEN, false, GuhEmotes.Source.OWNER);
        }
        ServerLevel level = (ServerLevel) g.level();
        level.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + g.getBbHeight() + 0.2, g.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, g.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.7f, 1.2f);
        int n = 1 + g.getRandom().nextInt(5);
        Component regel = Component.translatable("gui.guhs.knabbelvelden.aai." + n, g.getName());
        if (eigen && g.getRandom().nextInt(3) == 0) {
            regel = regel.copy().append(" ").append(Component.translatable("gui.guhs.knabbelvelden.aai.tip").withStyle(ChatFormatting.GRAY));
        }
        sp.sendOverlayMessage(regel.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.SUCCESS;
    }

    /** (KnabbelPayloads.MeeNaarHuis) the owner said yes: the retired guh comes home as an item (dropped at the plot portal when full). */
    static void meeNaarHuis(ServerPlayer p, int entityId) {
        if (!Knabbelvelden.echt(p.level())) {
            return;
        }
        Entity e = p.level().getEntity(entityId);
        Geest geest = e == null ? null : LEVEND.get(e.getUUID());
        if (geest == null || !geest.eigenaar().equals(p.getUUID()) || e.distanceTo(p) > 10) {
            return;
        }
        int plot = KnabbelveldenData.get(p.level().getServer()).plot(p.getUUID());
        BlockPos portaal = Knabbelvelden.portaal(plot);
        Component naam = e.getName().copy();
        if (!Pensioen.terug(p, geest.band(), net.minecraft.world.phys.Vec3.atCenterOf(portaal).add(0.5, 0, 0)).isEmpty()) {
            p.sendSystemMessage(Component.translatable("gui.guhs.knabbelvelden.mee", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** (tests) how many ghosts this owner has right now. */
    public static int aantal(UUID eigenaar) {
        int n = 0;
        for (Geest g : LEVEND.values()) {
            if (g.eigenaar().equals(eigenaar)) {
                n++;
            }
        }
        return n;
    }
}
