package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2, slice vetbootje: a player's own vetbootje (one per player, never lost). The boat's page in {@link Vloot} is what
 * a player owns (looks and chest); the {@link VetbootjeEntity} in the world is only its body, and {@link #roep} brings that
 * body to the player or makes a new one when the old one is out of reach.
 * <p>
 * hook fase B: the questline gives it ({@link #geef}), the shipyard of the Leegtehaven opens {@link #openWerf}, and the
 * boating exam of juf Pufguh can put down a boat that belongs to nobody ({@link #leen}).
 */
public final class Vetbootje {
    /** The colour of a new vetbootje: bruin, as the sails of the big vetschip. */
    public static final DyeColor STANDAARD_KLEUR = DyeColor.BROWN;
    /** The longest name a player can type at the werf. */
    public static final int NAAM_MAX = 24;
    /** Hidden advancement (guhs:quest/...) of the first time a player gets the boat: something for the quests of phase B to hang on. */
    public static final String GEKREGEN = "einde2_vetbootje_gekregen";

    public static boolean heeft(ServerPlayer p) {
        return blad(p).heeft;
    }

    /**
     * Gives this player their vetbootje, once: the first time also the vetfluitje and the werfboekje. When they have one
     * already, that one comes. Never null.
     */
    public static VetbootjeEntity geef(ServerPlayer p) {
        // hook fase B: the end of the vetbootje questline (the notice on a vetschip of the Mika-vesting, built at the helling) calls this
        Vloot.Boot b = blad(p);
        if (!b.heeft) {
            b.heeft = true;
            b.eigenaarNaam = p.getGameProfile().name();
            b.bewaar();
            Minigames.give(p, new ItemStack(E2VetbootjeFeature.VETBOOTJE_FLUITJE.get()));
            Minigames.give(p, new ItemStack(E2VetbootjeFeature.VETBOOTJE_WERFBOEKJE.get()));
            GuhAdvancements.grant(p, GEKREGEN);
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_vetbootje.gekregen").withStyle(ChatFormatting.GOLD));
        }
        return roep(p, true);
    }

    /**
     * Brings the player's vetbootje to them: the loaded one glides over (its guhs stay aboard), a lost one is made again
     * with its chest and looks. Null when the player has none, or when there is no room for it here.
     */
    @Nullable
    public static VetbootjeEntity roep(ServerPlayer p) {
        return roep(p, false);
    }

    @Nullable
    private static VetbootjeEntity roep(ServerPlayer p, boolean hoeDanOok) {
        Vloot.Boot b = blad(p);
        if (!b.heeft) {
            return null;
        }
        ServerLevel level = p.level();
        Vec3 plek = zoekPlek(p);
        if (plek == null) {
            if (!hoeDanOok) {
                return null;
            }
            plek = p.position().add(0, 2.5, 0);          // (a gift always arrives: it hovers over your head)
        }
        b.eigenaarNaam = p.getGameProfile().name();
        VetbootjeEntity oud = vind(level.getServer(), b);
        if (oud != null && oud.level() == level) {
            List<Entity> mee = new ArrayList<>();
            for (Entity e : List.copyOf(oud.getPassengers())) {
                if (e instanceof GuhEntity) {
                    mee.add(e);
                }
            }
            oud.ejectPassengers();
            oud.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
            oud.stilLeggen();
            for (Entity guh : mee) {                      // the guhs glide along in their seats
                guh.teleportTo(plek.x, plek.y + 0.5, plek.z);
                guh.startRiding(oud, true, false);
            }
            aankomst(level, oud);
            return oud;
        }
        if (oud != null) {
            oud.vaarwel();                               // (in another dimension: its guhs get off there)
        }
        b.nr++;
        VetbootjeEntity nieuw = E2VetbootjeFeature.VETBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (nieuw == null) {
            return null;
        }
        nieuw.snapTo(plek.x, plek.y, plek.z, p.getYRot(), 0);
        nieuw.zetEigenaar(p.getUUID());
        nieuw.nr = b.nr;
        nieuw.neemOver(b);
        level.addFreshEntity(nieuw);
        b.entiteit = nieuw.getUUID();
        b.bewaar();
        aankomst(level, nieuw);
        return nieuw;
    }

    /** Sets the looks of the player's vetbootje: hull colour, sail and name (null = the default name again). */
    public static void stel(ServerPlayer p, DyeColor kleur, Zeil zeil, @Nullable Component naam) {
        Vloot.Boot b = blad(p);
        b.kleur = kleur;
        b.zeil = zeil;
        b.naam = naam;
        b.eigenaarNaam = p.getGameProfile().name();
        b.bewaar();
        VetbootjeEntity boot = vind(p.level().getServer(), b);
        if (boot != null) {
            boot.neemOver(b);
        }
    }

    /** Opens the customise screen (the werfboekje, later the shipyard). */
    public static void openWerf(ServerPlayer p) {
        // hook fase B: the scheepsbouwer-guh of the Leegtehaven (marker leegtehaven/scheepsbouwer) opens the same screen
        Vloot.Boot b = blad(p);
        int kleur = b.kleur.getId(), zeil = b.zeil.ordinal();
        String naam = b.naam == null ? "" : b.naam.getString();
        p.openMenu(new SimpleMenuProvider((id, inv, speler) -> new WerfMenu(id, kleur, zeil, naam),
                Component.translatable("gui.guhs.einde2_vetbootje.werf.titel")), buf -> {
                    buf.writeVarInt(kleur);
                    buf.writeVarInt(zeil);
                    buf.writeUtf(naam, 64);
                });
    }

    /**
     * hook fase B: a vetbootje that belongs to nobody (the boating exam, a bootmobiel in Bikinibodem): anybody may sail it,
     * it has no chest and no fluitje calls it. Whoever put it down takes it away again ({@code discard()}).
     */
    @Nullable
    public static VetbootjeEntity leen(ServerLevel level, Vec3 plek, float yaw, DyeColor kleur, Zeil zeil, @Nullable Component naam) {
        // hook fase B: juf Pufguh's boating exam (bikinibodem/examen_start), the bootmobielen of Bikinibodem
        VetbootjeEntity boot = E2VetbootjeFeature.VETBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (boot == null) {
            return null;
        }
        boot.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        boot.zetKleur(kleur);
        boot.zetZeil(zeil);
        boot.setCustomName(naam != null ? naam : Component.translatable("gui.guhs.einde2_vetbootje.naam.leen"));
        level.addFreshEntity(boot);
        return boot;
    }

    /** The player's loaded boat (in any dimension), or null. */
    @Nullable
    public static VetbootjeEntity geladen(ServerPlayer p) {
        return vind(p.level().getServer(), blad(p));
    }

    // --- inside -------------------------------------------------------------------------------------------------------------

    static Vloot.Boot blad(ServerPlayer p) {
        return Vloot.van(p.level().getServer()).boot(p.getUUID());
    }

    /** The body that counts, when it is loaded somewhere. */
    @Nullable
    static VetbootjeEntity vind(MinecraftServer server, Vloot.Boot b) {
        if (b.entiteit == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(b.entiteit) instanceof VetbootjeEntity boot && boot.isAlive() && boot.nr == b.nr) {
                return boot;
            }
        }
        return null;
    }

    /** A free spot for the boat in front of the player (it hovers, so it needs air, not ground), or null. */
    @Nullable
    static Vec3 zoekPlek(ServerPlayer p) {
        ServerLevel level = p.level();
        float breed = E2VetbootjeFeature.VETBOOTJE.get().getWidth(), hoog = E2VetbootjeFeature.VETBOOTJE.get().getHeight();
        for (double dy : new double[]{0.2, 1.2, 2.2, -0.8}) {
            for (double afstand : new double[]{3.0, 4.5}) {
                for (float hoek : new float[]{0, 35, -35, 70, -70, 120, -120, 180}) {
                    Vec3 dir = Vec3.directionFromRotation(0, p.getYRot() + hoek);
                    Vec3 at = p.position().add(dir.x * afstand, dy, dir.z * afstand);
                    AABB doos = new AABB(at.x - breed / 2, at.y, at.z - breed / 2, at.x + breed / 2, at.y + hoog, at.z + breed / 2);
                    if (at.y > level.getMinY() + 2 && level.noCollision(doos.inflate(0.1, 0, 0.1))) {
                        return at;
                    }
                }
            }
        }
        return null;
    }

    private static void aankomst(ServerLevel level, VetbootjeEntity boot) {
        level.sendParticles(ParticleTypes.CLOUD, boot.getX(), boot.getY() + 0.6, boot.getZ(), 14, 0.9, 0.4, 0.9, 0.01);
        level.sendParticles(ParticleTypes.DRIPPING_HONEY, boot.getX(), boot.getY() + 0.2, boot.getZ(), 8, 0.7, 0.1, 0.7, 0);
        level.playSound(null, boot.blockPosition(), E2VetbootjeFeature.AANKOMST.get(), SoundSource.NEUTRAL, 0.9f, 1f);
    }

    private Vetbootje() {
    }
}
