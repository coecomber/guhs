package nl.juiced.guhs.feature.ringvlucht;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * 1.7.0 (ronde 37-40): two extra places for the Rookguh-vlucht, on top of the stops of the story:
 * <ul>
 *   <li><b>Het Super Guhrio-kasteel</b> ({@link #GUHRIO}): in front of its gate, on the forecourt. Shown to everyone, greyed
 *       with "Speel eerst wereld 1-1 uit!" until the player finished level 1-1 ({@link GuhrioKasteel#gehaald}).</li>
 *   <li><b>Your own Rookguh-platform</b> ({@link #PLATFORM}, the block {@link RookPlatformBlok}): one per player, in any
 *       dimension; putting it down (or clicking another one) makes that one yours, the old one stops counting. A click on
 *       your own platform calls the Rookguh: the same screen, from there to every stop of the story and the castle. The
 *       Rookguh puts you down on top of it.</li>
 * </ul>
 * The platform comes with the end of the story (the feast of chapter 6, {@link Ring#klaar}); who finished before 1.7.0 gets it
 * at their first login after the update, with a little message from the Rookguh ({@link #geschenk}, flag {@value #GEGEVEN}).
 * Saved per player: {@value #KEY} = "dimension|x|y|z".
 */
public final class RookPlatform {
    public static final String KEY = "guhs_ringvlucht_platform", GEGEVEN = "guhs_ringvlucht_platform_gegeven";
    /** The row ids of the two places on the Rookguh's screen. */
    public static final String GUHRIO = "#guhrio", PLATFORM = "#platform";
    /** The castle's gate (template coordinates of the whole castle, tools/features/guhrio_kasteel.py: x C, y G + 1, the arch at z 111), out to the south. */
    public static final BlockPos GUHRIO_POORT = new BlockPos(63, 27, 111);
    static final Haltes.Halte GUHRIO_HALTE = new Haltes.Halte("guhrio", "guhrio", GuhrioKasteel.STRUCTUUR, null,
            List.of(new Haltes.Ingang(GUHRIO_POORT, 0, 1)), 6);
    /** (tests) where the castle is, instead of a real copy. */
    public static final Map<String, Vlucht.Doelplek> TEST_DOEL = new ConcurrentHashMap<>();

    public record Plek(ResourceKey<Level> dim, BlockPos pos) {
    }

    private RookPlatform() {
    }

    static void registreer() {
        // the end of the story: the platform at once
        Ring.lijn(6).opStap((p, oud, nieuw) -> {
            if (oud < nieuw && nieuw >= Ring.lijn(6).stappen()) {
                geschenk(p, false);
            }
        });
        // who finished it before 1.7.0: at the first login after the update
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p && Ring.klaar(p)) {
                geschenk(p, true);
            }
        });
    }

    /** The platform for this player, once (true: given now). */
    public static boolean geschenk(ServerPlayer p, boolean later) {
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.getBooleanOr(GEGEVEN, false)) {
            return false;
        }
        saved.putBoolean(GEGEVEN, true);
        Minigames.give(p, new ItemStack(RingVluchtFeature.PLATFORM_ITEM.get()));
        String key = later ? "quest.guhs.ringvlucht.platform.geschenk_later" : "quest.guhs.ringvlucht.platform.geschenk";
        p.sendSystemMessage(Component.literal("<").append(Component.translatable("entity.guhs.ringvlucht_rookguh")).append("> ")
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.translatable(key).withStyle(ChatFormatting.WHITE)));
        p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.platform.geschenk.2").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** May this player use a platform (the story is done, or they were given one)? */
    public static boolean mag(ServerPlayer p) {
        return Ring.klaar(p) || GuhQuests.saved(p).getBooleanOr(GEGEVEN, false);
    }

    /** Did this player finish Super Guhrio's world 1-1 (then the castle is a destination)? */
    public static boolean guhrioOpen(ServerPlayer p) {
        return GuhrioKasteel.gehaald(p, GuhrioKasteel.LEVELS.get(0));
    }

    // =====================================================================================================================
    // the saved platform
    // =====================================================================================================================

    @Nullable
    public static Plek van(ServerPlayer p) {
        String w = GuhQuests.saved(p).getStringOr(KEY, "");
        String[] d = w.split("\\|");
        if (d.length != 4) {
            return null;
        }
        Identifier id = Identifier.tryParse(d[0]);
        try {
            return id == null ? null : new Plek(ResourceKey.create(Registries.DIMENSION, id),
                    new BlockPos(Integer.parseInt(d[1]), Integer.parseInt(d[2]), Integer.parseInt(d[3])));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** This block is the player's platform from now on (an older one stops counting). */
    public static void zet(ServerPlayer p, ResourceKey<Level> dim, BlockPos pos) {
        GuhQuests.saved(p).putString(KEY, dim.identifier() + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ());
    }

    public static void vergeet(ServerPlayer p) {
        GuhQuests.saved(p).remove(KEY);
    }

    static boolean isVan(ServerPlayer p, ServerLevel level, BlockPos pos) {
        Plek plek = van(p);
        return plek != null && plek.dim() == level.dimension() && plek.pos().equals(pos);
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    static boolean isExtra(String id) {
        return GUHRIO.equals(id) || PLATFORM.equals(id);
    }

    /** The two extra rows, after the stops (the platform's row only when it has one and does not stand at it). */
    static void extra(ServerPlayer p, ListTag haltes, boolean vanafPlatform) {
        CompoundTag g = new CompoundTag();
        g.putString("Id", GUHRIO);
        g.putString("Naam", "gui.guhs.ringvlucht.plek.guhrio");
        g.putString("Wereld", "gui.guhs.ringvlucht.wereld.barbecuether");
        if (!guhrioOpen(p)) {
            g.putBoolean("Grijs", true);
            g.putString("Waarom", "gui.guhs.ringvlucht.guhrio_eerst");
        }
        haltes.add(g);
        Plek plek = van(p);
        if (plek != null && !vanafPlatform) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", PLATFORM);
            t.putString("Naam", "gui.guhs.ringvlucht.plek.platform");
            t.putString("Wereld", wereld(plek.dim()));
            haltes.add(t);
        }
    }

    /** Is there anything to pick on this screen (a row that is not greyed)? */
    static boolean heeftKeuze(CompoundTag data) {
        for (Tag t : data.getListOrEmpty("Haltes")) {
            if (t instanceof CompoundTag c && !c.getBooleanOr("Grijs", false)) {
                return true;
            }
        }
        return false;
    }

    /** The lang key of a dimension's name on the screen. */
    static String wereld(ResourceKey<Level> dim) {
        if (dim == ModDimensions.GUHMENSION) {
            return "gui.guhs.ringvlucht.wereld.guhmensie";
        }
        if (dim == BarbecuetherFeature.BARBECUETHER) {
            return "gui.guhs.ringvlucht.wereld.barbecuether";
        }
        if (dim == GuheindeFeature.GUHEINDE) {
            return "gui.guhs.ringvlucht.wereld.guheinde";
        }
        if (dim == Level.OVERWORLD) {
            return "gui.guhs.ringvlucht.wereld.overworld";
        }
        if (dim == Level.NETHER) {
            return "gui.guhs.ringvlucht.wereld.nether";
        }
        if (dim == Level.END) {
            return "gui.guhs.ringvlucht.wereld.end";
        }
        return "gui.guhs.ringvlucht.wereld.elders";
    }

    // =====================================================================================================================
    // flying there
    // =====================================================================================================================

    /** Where an extra place is for this player (null: not known, or no such place). */
    @Nullable
    static Vlucht.Doelplek doelplek(ServerPlayer p, String id) {
        Vlucht.Doelplek test = TEST_DOEL.get(id);
        if (test != null) {
            return test;
        }
        MinecraftServer server = p.level().getServer();
        if (server == null) {
            return null;
        }
        if (GUHRIO.equals(id)) {
            ServerLevel level = Vlucht.level(server, BarbecuetherFeature.BARBECUETHER);
            StructureStart start = level == null ? null : Vlucht.kopie(p, level, GuhrioKasteel.STRUCTUUR);
            return start == null ? null : Vlucht.vanStart(level, GUHRIO_HALTE, start);
        }
        Plek plek = van(p);
        ServerLevel level = plek == null ? null : Vlucht.testLevel != null ? Vlucht.testLevel : server.getLevel(plek.dim());
        if (level == null) {
            return null;
        }
        Landing.laad(level, plek.pos(), 1);
        if (!level.getBlockState(plek.pos()).is(RingVluchtFeature.PLATFORM.get())) {
            vergeet(p);                                    // (the block is gone: no more platform)
            return null;
        }
        return new Vlucht.Doelplek(level, List.of(new Landing.Ingang(plek.pos().above(), 0, 1)), null, 0);
    }

    /**
     * Fly to an extra place (from a Reis-Rookguh, or from the own platform). Checked here: the castle only after world 1-1,
     * the platform only when there is one and not from itself.
     */
    static boolean vlieg(ServerPlayer p, String id, @Nullable Entity spreker, boolean vanafPlatform) {
        if (GUHRIO.equals(id)) {
            if (!guhrioOpen(p)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.ringvlucht.guhrio_eerst").withStyle(ChatFormatting.GRAY));
                return false;
            }
            return Vlucht.vliegNaar(p, Component.translatable("gui.guhs.ringvlucht.plek.guhrio"), doelplek(p, id), spreker);
        }
        if (PLATFORM.equals(id) && !vanafPlatform && van(p) != null) {
            Vlucht.Doelplek doel = doelplek(p, id);
            if (doel == null) {
                p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.platform.weg").withStyle(ChatFormatting.LIGHT_PURPLE));
                return false;
            }
            return Vlucht.vliegNaar(p, Component.translatable("gui.guhs.ringvlucht.plek.platform"), doel, spreker);
        }
        return false;
    }

    /** A click on your own platform: the Rookguh's screen from here (every stop the story reached, the castle). */
    static void open(ServerPlayer p) {
        String nee = Vlucht.waaromNiet(p);
        if (nee != null) {
            p.sendOverlayMessage(Component.translatable(nee).withStyle(ChatFormatting.GRAY));
            return;
        }
        CompoundTag data = Vlucht.menu(p, null, true);
        if (!heeftKeuze(data)) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.niks").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        p.level().playSound(null, p.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 0.8f);
        ModNetworking.sendTo(p, new VluchtPayloads.Open(-1, data));
    }

    /** The choice on the screen of the own platform (the server checks it all again: at their platform, the place allowed). */
    public static boolean kiesVanafPlatform(ServerPlayer p, String id) {
        Plek plek = van(p);
        if (plek == null || plek.dim() != p.level().dimension() || p.distanceToSqr(plek.pos().getCenter()) > Vlucht.REIK * Vlucht.REIK || !mag(p)
                || !p.level().getBlockState(plek.pos()).is(RingVluchtFeature.PLATFORM.get())) {
            return false;
        }
        if (isExtra(id)) {
            return vlieg(p, id, null, true);
        }
        Haltes.Halte naar = Haltes.van(id);
        return naar != null && Vlucht.bestemmingen(p, null).contains(naar) && Vlucht.vlieg(p, naar, null);
    }
}
