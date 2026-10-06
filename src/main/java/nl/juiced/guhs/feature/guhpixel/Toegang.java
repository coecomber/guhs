package nl.juiced.guhs.feature.guhpixel;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Getting into Guhpixel and out again.
 * <ul>
 *   <li>Unlocked per player ({@link #heeft}): false for everyone until they walked through the portal of the
 *   Guh-internetcafé ({@link #ontgrendel}).</li>
 *   <li>{@link #naarLobby}: remembers where the player came from (dimension, position, yaw, pitch in
 *   {@code GuhQuests.saved(p)["guhs_guhpixel_terug"]}; never overwritten while they are already in guhpixel), makes sure the
 *   lobby stands and puts them on the spawn point.</li>
 *   <li>{@link #naarHuis}: back to exactly that spot; without one (or when that dimension is gone) their own respawn
 *   point, else the world spawn.</li>
 * </ul>
 * Entering is refused ({@link #weigering}) when Guhpixel is not unlocked (the café portal unlocks), while the player is in
 * another minigame, rides something or carries passengers. Leads are dropped. Only players use the portals.
 */
public final class Toegang {
    public static final String TERUG = "guhs_guhpixel_terug";
    private static final String ONTGRENDELD = "Ontgrendeld", KABEL_GEHAD = "KabelGehad";

    public static boolean heeft(ServerPlayer p) {
        return Muntjes.data(p).getBooleanOr(ONTGRENDELD, false);
    }

    /** True the first time: the hidden advancement quest/guhpixel_ontgrendeld, a title, a sound. */
    public static boolean ontgrendel(ServerPlayer p) {
        if (heeft(p)) {
            return false;
        }
        Muntjes.data(p).putBoolean(ONTGRENDELD, true);
        PxData.vuil(p.level().getServer());
        GuhAdvancements.grant(p, "guhpixel_ontgrendeld");
        PxGeluid.titel(p, Component.translatable("gui.guhs.guhpixel.ontgrendeld.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.guhpixel.ontgrendeld.onder").withStyle(ChatFormatting.GOLD), 70);
        p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.ontgrendeld.chat").withStyle(ChatFormatting.LIGHT_PURPLE));
        PxGeluid.speel(p, GuhpixelFeature.ONTGRENDELD.get(), SoundSource.PLAYERS, 1f, 1f);
        Rangen.ververs(p);
        GuhpixelPayloads.hud(p);
        return true;
    }

    /** (Dev) locks Guhpixel again for this player. */
    public static void vergrendel(ServerPlayer p) {
        Muntjes.data(p).putBoolean(ONTGRENDELD, false);
        PxData.vuil(p.level().getServer());
        Rangen.ververs(p);
        GuhpixelPayloads.hud(p);
    }

    /** Why this player may not enter now (null: they may). {@code viaCafe}: through the café portal, which unlocks. */
    @Nullable
    public static Component weigering(ServerPlayer p, boolean viaCafe) {
        if (!viaCafe && !heeft(p)) {
            return Component.translatable("commands.guhs.guhpixel.lobby.op_slot");
        }
        String spel = Minigames.playing(p);
        if (spel != null && !Sessies.SPEL_ID.equals(spel)) {
            return Component.translatable("gui.guhs.guhpixel.binnen.spel_bezig");
        }
        if (p.isPassenger() || p.isVehicle()) {
            return Component.translatable("gui.guhs.guhpixel.binnen.afstappen");
        }
        if (p.isSleeping()) {
            return Component.translatable("gui.guhs.guhpixel.binnen.wakker");
        }
        return null;
    }

    /** To the lobby spawn. False when refused (the reason is shown) or when the dimension is missing. */
    public static boolean naarLobby(ServerPlayer p) {
        TeleportTransition t = lobbyTransitie(p, false);
        if (t == null) {
            return false;
        }
        p.teleport(t);
        return true;
    }

    /** The way in (null: refused, the reason was shown in the overlay). Remembers the return point. */
    @Nullable
    public static TeleportTransition lobbyTransitie(ServerPlayer p, boolean viaCafe) {
        Component nee = weigering(p, viaCafe);
        if (nee != null) {
            p.sendOverlayMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            return null;
        }
        MinecraftServer server = p.level().getServer();
        ServerLevel level = Guhpixel.level(server);
        if (level == null || !Lobby.zorg(level)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.binnen.storing").withStyle(ChatFormatting.RED));
            return null;
        }
        if (Sessies.van(p) != null) {
            Sessies.verlaat(p, Vertrek.VERLATEN);
        }
        onthoudTerug(p);
        for (Leashable dier : Leashable.leashableLeashedTo(p)) {
            dier.dropLeash();
        }
        Vec3 spawn = LobbyPlek.SPAWN.pos();
        return new TeleportTransition(level, spawn, Vec3.ZERO, LobbyPlek.SPAWN.yaw(), 0f, e -> {
            if (e instanceof ServerPlayer sp) {
                PxGeluid.speel(sp, GuhpixelFeature.PORTAAL_GELUID.get(), SoundSource.PLAYERS, 0.9f, 1f);
                GuhpixelPayloads.hud(sp);
            }
        });
    }

    /** Remembers where the player is, unless they are in guhpixel already. */
    static void onthoudTerug(ServerPlayer p) {
        if (Guhpixel.echt(p.level())) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putDouble("X", p.getX());
        t.putDouble("Y", p.getY());
        t.putDouble("Z", p.getZ());
        t.putFloat("Yaw", p.getYRot());
        t.putFloat("Pitch", p.getXRot());
        GuhQuests.saved(p).put(TERUG, t);
    }

    /** Back to where the player came from. False when they are not in guhpixel. */
    public static boolean naarHuis(ServerPlayer p) {
        if (!Guhpixel.echt(p.level())) {
            return false;
        }
        if (Sessies.van(p) != null) {
            Sessies.verlaat(p, Vertrek.VERLATEN);
        }
        p.stopRiding();
        // the stored spot can lie INSIDE the café's screen (they walked in there): no new trip until they step out of it
        p.setPortalCooldown();
        p.teleport(huisTransitie(p));
        return true;
    }

    /** The way home: the stored spot, else the own respawn point, else the world spawn. */
    public static TeleportTransition huisTransitie(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        CompoundTag t = GuhQuests.saved(p).getCompound(TERUG).orElse(null);
        if (t != null) {
            Identifier id = Identifier.tryParse(t.getStringOr("Dim", ""));
            ResourceKey<Level> dim = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
            ServerLevel level = dim == null || dim == Guhpixel.DIM ? null : server.getLevel(dim);
            if (level != null) {
                Vec3 pos = new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 80), t.getDoubleOr("Z", 0));
                return new TeleportTransition(level, pos, Vec3.ZERO, t.getFloatOr("Yaw", 0f), t.getFloatOr("Pitch", 0f), Toegang::thuis);
            }
        }
        return p.findRespawnPositionAndUseSpawnBlock(false, Toegang::thuis);
    }

    private static void thuis(net.minecraft.world.entity.Entity e) {
        if (e instanceof ServerPlayer sp) {
            PxGeluid.speel(sp, GuhpixelFeature.PORTAAL_GELUID.get(), SoundSource.PLAYERS, 0.9f, 0.8f);
        }
    }

    /** The player left guhpixel (any way): the return point is used up. */
    static void verlaten(ServerPlayer p) {
        GuhQuests.saved(p).remove(TERUG);
        Minigames.forget(p);
        GuhpixelPayloads.hud(p);
    }

    // --- the Netwerkkabeltje ---------------------------------------------------------------------------------------------

    public static ItemStack kabeltje() {
        return new ItemStack(GuhpixelFeature.NETWERKKABELTJE.get());
    }

    /** Has the greeter ever given this player a cable? */
    public static boolean kabeltjeGehad(ServerPlayer p) {
        return Muntjes.data(p).getBooleanOr(KABEL_GEHAD, false);
    }

    /** Gives a Netwerkkabeltje; false (and a hint) when the player still carries one. */
    public static boolean geefKabeltje(ServerPlayer p) {
        if (p.getInventory().hasAnyMatching(s -> s.is(GuhpixelFeature.NETWERKKABELTJE.get()))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.kabeltje.heb_je_al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        Minigames.give(p, kabeltje());
        Muntjes.data(p).putBoolean(KABEL_GEHAD, true);
        PxData.vuil(p.level().getServer());
        return true;
    }

    private Toegang() {
    }
}
