package nl.juiced.guhs.feature.kermiskraam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * A round at a game stall. Every player plays on their own little screen (client.KraamSpelScherm), so a whole server can stand
 * at one stall at once and nobody waits for a turn. The server hands out a round (one per player), keeps the waiting time between
 * rounds per stall, and pays out: half of the top score = 1 kermisbon, the top score = 2, less = a troostknabbel.
 * The score itself comes from the client: this is cosy kermis fun, the waiting time is what keeps the bonnen in check.
 */
public final class KraamSpel {
    /** Waiting time per stall, from the start of a round, and a round's shortest and longest possible life (ticks). */
    public static final int WACHT = 45 * 20, KORTST = 3 * 20, LANGST = 3 * 60 * 20;
    static final double BIJ = 12;
    static final String PERFECT = "guhs_kermiskraam_perfect";
    private static final String T = "gui.guhs.kermiskraam.";

    public enum Start { OK, WACHT }

    public record Prijs(int bonnen, int knabbels) {
    }

    record Ronde(Kraam kraam, BlockPos blok, long seed, long begin) {
    }

    private static final Map<UUID, Ronde> RONDES = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Kraam, Long>> VOLGENDE = new ConcurrentHashMap<>();

    private KraamSpel() {
    }

    /** What this score is worth at this stall. */
    public static Prijs prijs(Kraam kraam, int score) {
        if (score < 0) {
            return new Prijs(0, 0);
        }
        if (score >= kraam.hoogste) {
            return new Prijs(2, 0);
        }
        return score * 2 >= kraam.hoogste ? new Prijs(1, 0) : new Prijs(0, 1);
    }

    public static Start start(ServerPlayer p, Kraam kraam, BlockPos blok) {
        long nu = p.level().getGameTime();
        Map<Kraam, Long> tijden = VOLGENDE.computeIfAbsent(p.getUUID(), u -> new ConcurrentHashMap<>());
        long vanaf = tijden.getOrDefault(kraam, 0L);
        if (vanaf > nu) {
            p.sendOverlayMessage(Component.translatable(T + "wacht", (vanaf - nu + 19) / 20));
            return Start.WACHT;
        }
        tijden.put(kraam, nu + WACHT);
        long seed = p.getRandom().nextLong();
        RONDES.put(p.getUUID(), new Ronde(kraam, blok.immutable(), seed, nu));
        PacketDistributor.sendToPlayer(p, new KraamPayloads.Open(kraam.id, seed));
        return Start.OK;
    }

    /** (Tests) forgets the waiting times and the running round of this player. */
    static void vergeet(ServerPlayer p) {
        RONDES.remove(p.getUUID());
        VOLGENDE.remove(p.getUUID());
    }

    /** The seed of the running round (tests). */
    static long seed(ServerPlayer p) {
        Ronde r = RONDES.get(p.getUUID());
        return r == null ? 0 : r.seed();
    }

    /** (Tests) lets a running round start earlier, as if it has been played for a while. */
    static void spoel(ServerPlayer p, int ticks) {
        RONDES.computeIfPresent(p.getUUID(), (u, r) -> new Ronde(r.kraam(), r.blok(), r.seed(), r.begin() - ticks));
    }

    /** The client finished (score -1: closed the screen). Returns what was paid: null when this was no valid round. */
    public static Prijs klaar(ServerPlayer p, long seed, int score) {
        Ronde r = RONDES.get(p.getUUID());
        if (r == null || r.seed() != seed) {
            return null;
        }
        RONDES.remove(p.getUUID());
        long duur = p.level().getGameTime() - r.begin();
        if (duur < KORTST || duur > LANGST || !p.blockPosition().closerThan(r.blok(), BIJ)) {
            return null;
        }
        int s = Math.min(score, r.kraam().hoogste);
        Prijs prijs = prijs(r.kraam(), s);
        if (s < 0) {
            return prijs;
        }
        ItemStack uit = prijs.bonnen() > 0 ? new ItemStack(ModItems.KERMISBON.get(), prijs.bonnen()) : new ItemStack(ModItems.KAAS_KNABBELS.get(), prijs.knabbels());
        p.getInventory().placeItemBackInInventory(uit);
        boolean top = s >= r.kraam().hoogste;
        p.sendOverlayMessage(Component.translatable(T + (top ? "prijs.perfect" : prijs.bonnen() > 0 ? "prijs.bon" : "prijs.troost")));
        advancement(p, "kermiskraam_gespeeld");
        if (top) {
            perfect(p, r.kraam());
        }
        return prijs;
    }

    private static void perfect(ServerPlayer p, Kraam kraam) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag g = saved.getCompoundOrEmpty(PERFECT);
        g.putBoolean(kraam.id, true);
        saved.put(PERFECT, g);
        boolean alle = true;
        for (Kraam k : Kraam.values()) {
            alle &= !k.isSpel() || g.getBooleanOr(k.id, false);
        }
        if (alle) {
            advancement(p, "kermiskraam_alle_perfect");
        }
    }

    static void advancement(ServerPlayer p, String naam) {
        AdvancementHolder holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        if (holder != null) {
            AdvancementProgress progress = p.getAdvancements().getOrStartProgress(holder);
            for (String criterium : progress.getRemainingCriteria()) {
                p.getAdvancements().award(holder, criterium);
            }
        }
    }
}
