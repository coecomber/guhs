package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.Minigames;

/**
 * Het pepernotenwinkeltje of the winter plaza: temporary fun for pepernoten. Opened by the winkelguh (npc slice) or the
 * stall block ({@link SpelBlokken.Kraam}): {@link #open}. What it sells ({@link Waar}): snowballs, warme chocomel (a little
 * warmth: speed and a bit of regeneration), a chocoladeletter with the first letter of YOUR name, firework rockets, winter
 * skates, a kerstmuts and a Sint-mijter for on your head, and kerstlampjes for on your walls. The muts, the mijter, the
 * lampjes (and the skates) melt on 3 January (tags guhs:guhwinter_smelt / guhs:guhwinter_smelt_blok; kern does the melting).
 */
public final class GuhwinterWinkel {
    /** How long after opening and how far from where it opened you can still buy (a closed or old screen can't). */
    static final long OPEN_MS = 10 * 60_000L;
    static final double BEREIK = 12;

    public enum Waar {
        SNEEUWBALLEN("sneeuwballen", 3, naam -> new ItemStack(Items.SNOWBALL, 8)),
        CHOCOMEL("chocomel", 6, naam -> new ItemStack(SpellenFeature.CHOCOMEL.get())),
        CHOCOLADELETTER("chocoladeletter", 8, ChocoladeletterItem::maak),
        VUURWERK("vuurwerk", 6, naam -> vuurwerk()),
        SCHAATSEN("schaatsen", 30, naam -> new ItemStack(SpellenFeature.SCHAATSEN.get())),
        KERSTMUTS("kerstmuts", 40, naam -> new ItemStack(SpellenFeature.KERSTMUTS.get())),
        MIJTER("mijter", 40, naam -> new ItemStack(SpellenFeature.MIJTER.get())),
        KERSTLAMPJES("kerstlampjes", 10, naam -> new ItemStack(SpellenFeature.KERSTLAMPJES_ITEM.get(), 4));

        public final String id;
        public final int prijs;
        private final Function<String, ItemStack> maak;

        Waar(String id, int prijs, Function<String, ItemStack> maak) {
            this.id = id;
            this.prijs = prijs;
            this.maak = maak;
        }

        /** What you get (the buyer's name: the chocoladeletter's letter). */
        public ItemStack maak(String naam) {
            return maak.apply(naam);
        }

        public static Waar van(int i) {
            return i >= 0 && i < values().length ? values()[i] : null;
        }
    }

    private record Open(Vec3 waar, long ms) {
    }

    private static final Map<UUID, Open> OPEN = new ConcurrentHashMap<>();

    private GuhwinterWinkel() {
    }

    /** Three rockets with a pink-and-gold star burst. */
    static ItemStack vuurwerk() {
        ItemStack s = new ItemStack(Items.FIREWORK_ROCKET, 3);
        FireworkExplosion ster = new FireworkExplosion(FireworkExplosion.Shape.STAR, IntList.of(0xFF9EC4, 0xFFD34E), IntList.of(0xFFFFFF), true, true);
        FireworkExplosion bol = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(0xE0384C, 0x4A7BE0), IntList.of(), false, true);
        s.set(DataComponents.FIREWORKS, new Fireworks(2, List.of(ster, bol)));
        return s;
    }

    /** Opens the shop's screen for this player (the winkelguh calls this). */
    public static void open(ServerPlayer p) {
        if (!SpelBrug.open(p.level().getServer())) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.voorbij").withStyle(ChatFormatting.GRAY));
            return;
        }
        OPEN.put(p.getUUID(), new Open(p.position(), System.currentTimeMillis()));
        List<Integer> prijzen = new ArrayList<>();
        for (Waar w : Waar.values()) {
            prijzen.add(w.prijs);
        }
        PacketDistributor.sendToPlayer(p, new SpellenPayloads.WinkelOpen(SpelBrug.tel(p), prijzen));
    }

    static void vergeet(UUID id) {
        OPEN.remove(id);
    }

    /** A purchase from the screen. */
    static void koop(ServerPlayer p, int i) {
        Waar w = Waar.van(i);
        Open o = OPEN.get(p.getUUID());
        if (w == null || o == null || System.currentTimeMillis() - o.ms() > OPEN_MS || p.position().distanceTo(o.waar()) > BEREIK) {
            return;
        }
        boolean gelukt = SpelBrug.neem(p, w.prijs);
        if (gelukt) {
            Minigames.give(p, w.maak(p.getGameProfile().name()));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.4f, 1.5f);
            p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhwinter_spellen.winkel.te_weinig", w.prijs).withStyle(ChatFormatting.GRAY));
        }
        PacketDistributor.sendToPlayer(p, new SpellenPayloads.WinkelStand(SpelBrug.tel(p), i, gelukt));
    }
}
