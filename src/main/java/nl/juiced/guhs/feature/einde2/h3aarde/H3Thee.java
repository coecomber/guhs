package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;

/**
 * Making tea with Oom Miroh (DESIGN 5 h3: "Thee zetten (minispel)"): at the big kettle in the kitchen of De Jasmijn Draak
 * ({@code theeketel}), three moments that want the right touch, each with a gauge above the hotbar. Right-click the kettle
 * to start; right-click again when the gauge is in the green:
 * <ol>
 *   <li>the water: just before it boils (boiling water makes jasmine bitter);</li>
 *   <li>the leaves: long enough, but not too long;</li>
 *   <li>pouring: almost full, never over the edge.</li>
 * </ol>
 * Too early or too late is a word from Miroh and the same moment again; nothing is lost. The third one right: a ting, Miroh
 * beams, and his own theeset is yours (step {@link E2H3Aarde#THEE} done).
 */
public final class H3Thee {
    /** Per round: ticks the gauge takes from empty to full, and the green window (percent). */
    static final int[] DUUR = {70, 90, 60};
    static final int[][] RAAM = {{70, 85}, {45, 60}, {82, 94}};
    static final int RONDES = 3;

    /** A player at the kettle: which round, and since which tick the gauge runs. */
    private record Spel(int ronde, int begin) {
    }

    private static final Map<UUID, Spel> SPELLEN = new ConcurrentHashMap<>();

    private H3Thee() {
    }

    public static boolean bezig(ServerPlayer p) {
        return SPELLEN.containsKey(p.getUUID());
    }

    /** A right-click on (or right next to) the kettle by a player whose step is the tea. */
    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        BlockPos ketel = stad == null ? null : stad.pos("theeketel");
        if (ketel == null || ketel.distSqr(event.getPos()) > 2.5 || E2H3Aarde.stap(p) != E2H3Aarde.THEE) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        klik(p, stad);
    }

    /** The click: start, or stop the gauge of this round. */
    static void klik(ServerPlayer p, E2H3Aarde.Stad stad) {
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            SPELLEN.put(p.getUUID(), new Spel(0, p.tickCount));
            uitleg(p, 0);
            return;
        }
        int pct = procent(s, p.tickCount);
        int[] raam = RAAM[s.ronde()];
        Vec3 ketel = stad.voet("theeketel");
        if (pct >= raam[0] && pct <= raam[1]) {
            if (ketel != null) {
                Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, ketel.add(0, 1, 0), 8, 0.3, 0);
                Schijn.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, BlockPos.containing(ketel), 1f, 1.2f + s.ronde() * 0.2f);
            }
            if (s.ronde() + 1 >= RONDES) {
                SPELLEN.remove(p.getUUID());
                gelukt(p, stad);
                return;
            }
            miroh(p, stad, "thee.goed." + s.ronde());
            SPELLEN.put(p.getUUID(), new Spel(s.ronde() + 1, p.tickCount));
            uitleg(p, s.ronde() + 1);
        } else {
            if (ketel != null) {
                Schijn.deeltjes(p, ParticleTypes.CLOUD, ketel.add(0, 1.2, 0), 10, 0.3, 0.02);
                Schijn.geluid(p, SoundEvents.FIRE_EXTINGUISH, BlockPos.containing(ketel), 0.6f, 1.4f);
            }
            miroh(p, stad, "thee." + (pct < raam[0] ? "vroeg" : "laat") + "." + s.ronde());
            SPELLEN.put(p.getUUID(), new Spel(s.ronde(), p.tickCount));
        }
    }

    private static void uitleg(ServerPlayer p, int ronde) {
        p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h3_aarde.thee.ronde." + ronde).withStyle(ChatFormatting.GREEN));
    }

    static int procent(Spel s, int nu) {
        int t = Math.max(0, nu - s.begin()) % (DUUR[s.ronde()] * 2);
        int heen = DUUR[s.ronde()];
        return (int) Math.round(100.0 * (t <= heen ? t : 2 * heen - t) / heen);   // up, then down again: wait for the next swing
    }

    /** The gauge above the hotbar, every 2 ticks; walking away (more than 6 blocks) puts the game away. */
    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0) {
            return;
        }
        Spel s = SPELLEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad == null || !stad.bij(p, "theeketel", 6) || E2H3Aarde.stap(p) != E2H3Aarde.THEE) {
            SPELLEN.remove(p.getUUID());
            return;
        }
        p.sendOverlayMessage(meter(s.ronde(), procent(s, p.tickCount)));
        if (p.tickCount % 20 == 0) {
            Vec3 ketel = stad.voet("theeketel");
            if (ketel != null) {
                Schijn.deeltjes(p, ParticleTypes.CLOUD, ketel.add(0, 1.1, 0), 2 + procent(s, p.tickCount) / 25, 0.15, 0.01);
            }
        }
    }

    /** "Water: ▮▮▮▮[▮▮]▯▯ ": the window in green, past it in red, the gauge so far in white. */
    static Component meter(int ronde, int pct) {
        MutableComponent uit = Component.translatable("gui.guhs.einde2_h3_aarde.thee.meter." + ronde).append(" ");
        int[] raam = RAAM[ronde];
        for (int i = 0; i < 25; i++) {
            int v = i * 4;
            ChatFormatting kleur = v >= raam[0] && v <= raam[1] ? ChatFormatting.GREEN : v > raam[1] ? ChatFormatting.RED : ChatFormatting.GRAY;
            uit.append(Component.literal(v <= pct ? "▮" : "▯").withStyle(v <= pct && kleur == ChatFormatting.GRAY ? ChatFormatting.WHITE : kleur));
        }
        return uit;
    }

    /** All three right: the ting, Miroh's joy, his theeset. */
    static void gelukt(ServerPlayer p, E2H3Aarde.Stad stad) {
        E2Audio.stinger(p, "deuntje.thee_ting");
        miroh(p, stad, "thee.gelukt");
        Minigames.give(p, new ItemStack(E2H3AardeFeature.THEESET_ITEM.get()));
        E2H3Aarde.LIJN.verder(p, E2H3Aarde.THEE);
    }

    /** A word from Miroh (the resident behind the counter, or else the narrator). */
    static void miroh(ServerPlayer p, E2H3Aarde.Stad stad, String key) {
        Entity miroh = vind(stad, GuhNpcEntity.Kind.OOM_MIROH, "miroh");
        if (miroh != null) {
            E2H3Aarde.zeg(p, miroh, key);
        } else {
            E2H3Aarde.verteller(p, key);
        }
    }

    /** A resident (not one of our own actors) of this kind near a marker. */
    static GuhNpcEntity vind(E2H3Aarde.Stad stad, GuhNpcEntity.Kind kind, String plek) {
        Vec3 v = stad.voet(plek);
        if (v == null) {
            return null;
        }
        for (GuhNpcEntity npc : stad.level().getEntitiesOfClass(GuhNpcEntity.class, new AABB(BlockPos.containing(v)).inflate(8),
                n -> n.getKind() == kind && !n.entityTags().contains(H3Acteurs.TAG))) {
            return npc;
        }
        return null;
    }
}
