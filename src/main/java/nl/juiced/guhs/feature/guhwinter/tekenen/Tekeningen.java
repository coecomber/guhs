package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The public API of the drawings (for the lootjes and verhaal slices too):
 * <ul>
 *   <li>{@link #openEditor(ServerPlayer, Doel, UUID)}: opens the 64x64 editor on the player's screen, empty or with an existing
 *       drawing of theirs preloaded. Saving stores it ({@link TekeningenData}) and calls the purpose's listeners
 *       ({@link Doel#opOpslaan}) and the optional per-call callback;</li>
 *   <li>{@link #maakItem(UUID)} / {@link #maakItem(UUID, String)}: the hangable 2x2 drawing item ({@code guhs:guhwinter_tekening});</li>
 *   <li>{@link #get(MinecraftServer, UUID)}: a stored drawing.</li>
 * </ul>
 * Uploads are rate limited (one save per 2 s, 20 per 10 minutes per player) and checked (size, one valid 64x64 picture);
 * fetches too (60 per 10 s per player; the client simply asks again later).
 */
public final class Tekeningen {
    private static final long OPSLAAN_PAUZE_MS = 2_000, OPSLAAN_VENSTER_MS = 10 * 60_000, VRAAG_VENSTER_MS = 10_000;
    private static final int OPSLAAN_MAX = 20, VRAAG_MAX = 60;

    private record Sessie(int token, Doel doel, @Nullable UUID bestaande, @Nullable Consumer<Tekening> klaar) {
    }

    private static final Map<UUID, Sessie> SESSIES = new HashMap<>();
    private static final Map<UUID, Deque<Long>> OPSLAGEN = new HashMap<>();
    private static final Map<UUID, long[]> VRAGEN = new HashMap<>();
    private static int volgende = 1;

    private Tekeningen() {
    }

    // =================================================================================================================
    // API
    // =================================================================================================================

    /** Opens the editor; {@code bestaande}: a drawing of this player to edit (else, or when it isn't theirs: a new one). */
    public static void openEditor(ServerPlayer speler, Doel doel, @Nullable UUID bestaande) {
        openEditor(speler, doel, bestaande, null);
    }

    /** As {@link #openEditor(ServerPlayer, Doel, UUID)}, plus a callback for this one session (called after a successful save). */
    public static void openEditor(ServerPlayer speler, Doel doel, @Nullable UUID bestaande, @Nullable Consumer<Tekening> klaar) {
        MinecraftServer server = speler.level().getServer();
        TekeningenData data = TekeningenData.get(server);
        if (doel == Doel.KERSTBOOM && bestaande == null) {
            bestaande = data.kerstboomVan(speler.getUUID());   // one kerstboom drawing per player: always edit that one
        }
        Tekening oud = bestaande == null ? null : data.get(bestaande);
        if (oud != null && !oud.maker().equals(speler.getUUID())) {
            oud = null;
        }
        int token = volgende++;
        SESSIES.put(speler.getUUID(), new Sessie(token, doel, oud == null ? null : oud.id(), klaar));
        PacketDistributor.sendToPlayer(speler, new TekenPayloads.Open(token, doel.id(), oud == null ? new byte[0] : TekenPixels.pak(oud.pixels())));
    }

    /** The hangable 2x2 drawing of this drawing (maker from the stored drawing; "?" when unknown or no server). */
    public static ItemStack maakItem(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Tekening t = server == null ? null : get(server, id);
        return maakItem(id, t == null ? "?" : t.makerNaam());
    }

    public static ItemStack maakItem(UUID id, String maker) {
        ItemStack s = new ItemStack(TekenenFeature.TEKENING.get());
        s.set(TekenenFeature.TEKENING_REF.get(), new TekeningRef(id, maker));
        return s;
    }

    @Nullable
    public static Tekening get(MinecraftServer server, UUID id) {
        return TekeningenData.get(server).get(id);
    }

    /** Gives the stack, or drops it at the player's feet when the inventory is full. */
    public static void geef(ServerPlayer speler, ItemStack stack) {
        if (!speler.getInventory().add(stack) && !stack.isEmpty()) {
            speler.drop(stack, false);
        }
    }

    // =================================================================================================================
    // the editor session (payload handlers)
    // =================================================================================================================

    static void opslaan(ServerPlayer speler, int token, byte[] pak) {
        Sessie s = SESSIES.get(speler.getUUID());
        if (s == null || s.token() != token) {
            antwoord(speler, token, false, "guhwinter.tekenen.fout.sessie");
            return;
        }
        if (!magOpslaan(speler.getUUID())) {
            antwoord(speler, token, false, "guhwinter.tekenen.fout.rustig");
            return;
        }
        int[] px = TekenPixels.pakUit(pak);
        if (px == null) {
            antwoord(speler, token, false, "guhwinter.tekenen.fout.kapot");
            return;
        }
        String weiger = s.doel().weiger(speler, s.bestaande());
        if (weiger != null) {
            antwoord(speler, token, false, weiger);
            return;
        }
        MinecraftServer server = speler.level().getServer();
        TekeningenData data = TekeningenData.get(server);
        long nu = System.currentTimeMillis();
        String naam = speler.getGameProfile().name();
        Tekening oud = s.bestaande() == null ? null : data.get(s.bestaande());
        Tekening t;
        if (oud != null && oud.maker().equals(speler.getUUID())) {
            t = oud.bewerkt(naam, px, nu);
        } else {
            t = new Tekening(UUID.randomUUID(), speler.getUUID(), naam, s.doel(), 1, nu, nu, px);
        }
        data.zet(t);
        SESSIES.remove(speler.getUUID());
        if (oud != null) {
            PacketDistributor.sendToAllPlayers(new TekenPayloads.Vergeet(t.id()));
        }
        if (t.doel() == Doel.KERSTBOOM) {
            data.zetKerstboom(speler.getUUID(), t.id());
        }
        antwoord(speler, token, true, "");
        speler.level().playSound(null, speler.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8f, 1.2f);
        t.doel().opgeslagen(speler, t);
        if (s.klaar() != null) {
            s.klaar().accept(t);
        }
    }

    static void annuleer(ServerPlayer speler, int token) {
        Sessie s = SESSIES.get(speler.getUUID());
        if (s != null && s.token() == token) {
            SESSIES.remove(speler.getUUID());
        }
    }

    static void vraag(ServerPlayer speler, UUID id) {
        long nu = System.currentTimeMillis();
        long[] v = VRAGEN.computeIfAbsent(speler.getUUID(), k -> new long[]{nu, 0});
        if (nu - v[0] > VRAAG_VENSTER_MS) {
            v[0] = nu;
            v[1] = 0;
        }
        if (++v[1] > VRAAG_MAX) {
            return;   // the client asks again in a little while
        }
        Tekening t = get(speler.level().getServer(), id);
        PacketDistributor.sendToPlayer(speler, t == null ? new TekenPayloads.Data(id, "", new byte[0])
                : new TekenPayloads.Data(id, t.makerNaam(), TekenPixels.pak(t.pixels())));
    }

    private static boolean magOpslaan(UUID speler) {
        long nu = System.currentTimeMillis();
        Deque<Long> d = OPSLAGEN.computeIfAbsent(speler, k -> new ArrayDeque<>());
        while (!d.isEmpty() && nu - d.peekFirst() > OPSLAAN_VENSTER_MS) {
            d.pollFirst();
        }
        if ((!d.isEmpty() && nu - d.peekLast() < OPSLAAN_PAUZE_MS) || d.size() >= OPSLAAN_MAX) {
            return false;
        }
        d.addLast(nu);
        return true;
    }

    private static void antwoord(ServerPlayer speler, int token, boolean ok, String sleutel) {
        PacketDistributor.sendToPlayer(speler, new TekenPayloads.Antwoord(token, ok, sleutel));
    }

    static void uitgelogd(UUID speler) {
        SESSIES.remove(speler);
        VRAGEN.remove(speler);
    }

    static void wis() {
        SESSIES.clear();
        OPSLAGEN.clear();
        VRAGEN.clear();
    }

    static Component melding(String sleutel, Object... args) {
        return Component.translatable(sleutel, args).withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
