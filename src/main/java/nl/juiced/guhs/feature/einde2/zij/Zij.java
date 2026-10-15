package nl.juiced.guhs.feature.einde2.zij;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 zij: the small helpers every side quest of this slice uses (texts only as lang keys: {@link #Q}, {@link #GUI},
 * written by tools/features/einde2_zij_tekst.py).
 */
public final class Zij {
    /** The prefixes of this slice's texts. */
    public static final String Q = "quest.guhs.einde2_zij.", GUI = "gui.guhs.einde2_zij.";

    private Zij() {
    }

    /** "&lt;Name&gt; text" in the chat, only for this player, without an entity (a voice, a resident far away). */
    public static void stem(ServerPlayer p, String naamKey, String key, Object... args) {
        MutableComponent line = Component.literal("<").append(Component.translatable(naamKey)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, args).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(line);
    }

    /** A line of a character that stands here (its own name). */
    public static void zeg(ServerPlayer p, Entity spreker, String key, Object... args) {
        GuhQuests.say(p, spreker, key, args);
    }

    /** "Volgende stap: ..." in gold. */
    public static void hint(ServerPlayer p, String key) {
        GuhQuests.hint(p, key);
    }

    /** A short line above the hotbar. */
    public static void melding(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A sound only this player hears, where they stand. */
    public static void geluid(ServerPlayer p, SoundEvent s, float volume, float pitch) {
        Schijn.geluid(p, s, p.blockPosition(), volume, pitch);
    }

    /** A big title (and subtitle) for this player only. */
    public static void titel(ServerPlayer p, Component titel, @Nullable Component sub, int in, int blijf, int uit) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(in, blijf, uit));
        if (sub != null) {
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        }
        p.connection.send(new ClientboundSetTitleTextPacket(titel));
    }

    /**
     * The element gate of a side line (CONTRACT_E2_B 2.1, DESIGN 4: "Kom terug als je kunt ...sturen, njeg"): true when
     * the line asks no element or the player has learnt it; else the resident says so and false.
     */
    public static boolean element(ServerPlayer p, String lijn, @Nullable Entity spreker) {
        Einde2Ids.Element e = ZijLijnen.element(lijn);
        if (e == null || ZijKrachten.geleerd(p, e)) {
            return true;
        }
        String key = Q + "element." + e.name().toLowerCase(Locale.ROOT);
        if (spreker != null) {
            zeg(p, spreker, key);
        } else {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
        return false;
    }

    /** Into the pockets (or at the feet when they are full). */
    public static void geef(ServerPlayer p, ItemStack... stacks) {
        for (ItemStack s : stacks) {
            Minigames.give(p, s.copy());
        }
    }

    /** These rewards once per player and name: true the first time. */
    public static boolean eenmalig(Verhaallijn lijn, ServerPlayer p, String naam, ItemStack... stacks) {
        if (!lijn.eenmalig(p, naam)) {
            return false;
        }
        geef(p, stacks);
        return true;
    }

    /** The spot name {@code <structuur>/<plek>} a resident of {@link Bewoners} carries ("": none). */
    public static String plek(Entity e) {
        if (e instanceof GuhNpcEntity npc) {
            return npc.roleData.getStringOr(NpcRollen.PLEK, "");
        }
        return e.getPersistentData().getStringOr(Bewoners.PLEK, "");
    }

    /** The nearest resident of this kind within {@code straal} blocks (null: none). */
    @Nullable
    public static GuhNpcEntity npc(ServerLevel level, Vec3 bij, GuhNpcEntity.Kind kind, double straal) {
        GuhNpcEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (GuhNpcEntity n : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(bij, bij).inflate(straal), n -> n.isAlive() && n.getKind() == kind)) {
            double d = n.distanceToSqr(bij);
            if (d < bestD) {
                bestD = d;
                best = n;
            }
        }
        return best;
    }

    /** The nearest creature of the cast with this id ({@code wie()}) within {@code straal} blocks (null: none). */
    @Nullable
    public static CastWezen wezen(ServerLevel level, Vec3 bij, String wie, double straal) {
        CastWezen best = null;
        double bestD = Double.MAX_VALUE;
        for (CastWezen w : level.getEntitiesOfClass(CastWezen.class, new AABB(bij, bij).inflate(straal), w -> w.isAlive() && w.wie().equals(wie))) {
            double d = w.distanceToSqr(bij);
            if (d < bestD) {
                bestD = d;
                best = w;
            }
        }
        return best;
    }
}
