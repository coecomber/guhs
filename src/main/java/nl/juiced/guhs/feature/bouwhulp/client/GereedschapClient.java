package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.client.GuhKeys;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Gereedschap;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.GereedschapPayloads;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Plaatsen;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Vormstaf;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetTabs;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.vormen.VormInstelling;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import org.lwjgl.glfw.GLFW;

import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.DONKER;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.LICHT;
import static nl.juiced.guhs.feature.bouwhulp.client.BouwkeetScreen.TEKST;

/**
 * The client of F9: the settings screens ({@link InstelScherm}), the vormstaf's radial menu ({@link RondMenu}, the
 * Vormstaf key, left Alt by default), its preview (a ghost of the shape where you look, only for you), the capture frame
 * ({@link VastlegScherm}, sparkles along the frame), the Ontwerpbureau ({@link BureauScherm}) and the keet's Vormen,
 * Daken and Blauwdrukken pages.
 */
public final class GereedschapClient {
    public static final KeyMapping MENU = new KeyMapping("key.guhs.vormstaf_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, GuhKeys.CATEGORY);
    private static final UUID VOORBEELD = UUID.nameUUIDFromBytes("guhs:vormstaf_voorbeeld".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** The last "staf" data (settings, level), null until asked. */
    private static CompoundTag staf;
    private static boolean hield, voorbeeldAan;
    private static String ontwerpSleutel = "", plekSleutel = "";
    private static Ontwerp ontwerp;
    private static int wacht;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(MENU));
        GereedschapPayloads.ontvang = d -> Minecraft.getInstance().execute(() -> ontvang(d.soort(), d.data()));
        paginas();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            staf = null;
            hield = false;
            voorbeeldAan = false;
            ontwerpSleutel = plekSleutel = "";
        });
    }

    private static void ontvang(String soort, CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        switch (soort) {
            case "instel" -> {
                if (t.getBooleanOr("Open", false)) {
                    mc.setScreen(new InstelScherm(t));
                } else if (mc.screen instanceof InstelScherm s && s.gereedschap().equals(t.getStringOr("Gereedschap", ""))) {
                    s.update(t);
                }
            }
            case "staf" -> {
                staf = t;
                if (mc.screen instanceof RondMenu r) {
                    r.update(t);
                }
            }
            case "vastleg" -> {
                if (t.getBooleanOr("Klaar", false)) {
                    if (mc.screen instanceof VastlegScherm) {
                        mc.setScreen(null);
                    }
                } else if (mc.screen instanceof VastlegScherm s) {
                    s.update(t);
                } else {
                    mc.setScreen(new VastlegScherm(t));
                }
            }
            case "bureau" -> {
                if (t.getBooleanOr("Open", false) || !(mc.screen instanceof BureauScherm)) {
                    if (t.getBooleanOr("Open", false)) {
                        mc.setScreen(new BureauScherm(t));
                    }
                } else {
                    ((BureauScherm) mc.screen).update(t);
                }
            }
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // every tick: the radial menu key, asking for the staf's settings, the preview, the capture frame
    // =====================================================================================================================

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        boolean houdt = mc.player.getMainHandItem().getItem() instanceof Vormstaf;
        if (houdt && !hield) {
            ClientPacketDistributor.sendToServer(new GereedschapPayloads.Doe("staf", "vormstaf", "", 0, ""));
        }
        hield = houdt;
        while (MENU.consumeClick()) {
            if (houdt && mc.screen == null && staf != null) {
                mc.setScreen(new RondMenu(staf));
            }
        }
        voorbeeld(mc, houdt);
        if (mc.screen instanceof VastlegScherm v && mc.level.getGameTime() % 4 == 0) {
            kader(mc, BlockPos.of(v.data().getLongOr("Min", 0L)), BlockPos.of(v.data().getLongOr("Max", 0L)));
        }
    }

    /** The shape where you look, as a ghost only you see (re-made when the settings or the spot change). */
    private static void voorbeeld(Minecraft mc, boolean houdt) {
        CompoundTag inst = staf == null ? null : staf.getCompoundOrEmpty("Instelling");
        int modus = inst == null ? 0 : Gereedschap.modus(inst);
        boolean toon = houdt && inst != null && modus != Gereedschap.VASTLEG && (mc.screen == null || mc.screen instanceof RondMenu)
                && mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK;
        if (!toon) {
            if (voorbeeldAan) {
                ClientSpoken.weg(VOORBEELD);
                voorbeeldAan = false;
                plekSleutel = "";
            }
            return;
        }
        if (wacht > 0) {
            wacht--;
            return;
        }
        BlockHitResult bh = (BlockHitResult) mc.hitResult;
        int niveau = staf.getIntOr("Niveau", 0);
        String sleutel = inst + "|" + niveau;
        if (!sleutel.equals(ontwerpSleutel)) {
            ontwerp = Vormstaf.ontwerp(VormInstelling.van(inst), BouwNiveau.van(niveau), modus);
            ontwerpSleutel = sleutel;
            plekSleutel = "";
        }
        if (ontwerp == null || ontwerp.isLeeg()) {
            return;
        }
        BlockPos oorsprong = Plaatsen.oorsprong(ontwerp, bh.getBlockPos(), bh.getDirection());
        Rotation draai = switch (mc.player.getDirection()) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        String plek = oorsprong.asLong() + "|" + draai + "|" + ontwerpSleutel;
        if (plek.equals(plekSleutel)) {
            return;
        }
        plekSleutel = plek;
        ClientSpoken.ontvang(pak(ontwerp, oorsprong, draai, mc.player.getUUID()));
        voorbeeldAan = true;
        wacht = ontwerp.aantal() > 5000 ? 5 : 1;
    }

    /** The ghost tag the client store reads (as {@code SpookPayloads.pak} writes it on the server). */
    private static CompoundTag pak(Ontwerp o, BlockPos oorsprong, Rotation draai, UUID speler) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, VOORBEELD);
        t.store("EigenaarId", UUIDUtil.CODEC, speler);
        t.putString("Naam", "");
        t.putLong("Oorsprong", oorsprong.asLong());
        t.putInt("Draai", draai.ordinal());
        t.putInt("Spiegel", 0);
        t.putInt("Status", 0);
        t.put("Helpers", new ListTag());
        Vec3i g = o.grootte();
        Map<BlockState, Integer> index = new LinkedHashMap<>();
        for (BlockState st : o.blokken().values()) {
            if (!st.isAir()) {
                index.computeIfAbsent(st, k -> index.size());
            }
        }
        boolean breed = index.size() > 254;
        byte[] raster = new byte[g.getX() * g.getY() * g.getZ() * (breed ? 2 : 1)];
        for (Map.Entry<BlockPos, BlockState> e : o.blokken().entrySet()) {
            if (e.getValue().isAir()) {
                continue;
            }
            BlockPos p = e.getKey();
            int i = (p.getX() * g.getY() + p.getY()) * g.getZ() + p.getZ();
            int v = index.get(e.getValue()) + 1;
            if (breed) {
                raster[i * 2] = (byte) (v >> 8);
                raster[i * 2 + 1] = (byte) v;
            } else {
                raster[i] = (byte) v;
            }
        }
        t.putIntArray("Palet", index.keySet().stream().mapToInt(Block::getId).toArray());
        t.putIntArray("Grootte", new int[] {g.getX(), g.getY(), g.getZ()});
        t.putBoolean("Breed", breed);
        t.putByteArray("Raster", raster);
        return t;
    }

    /** Pink sparkles along the twelve edges of the capture frame. */
    private static void kader(Minecraft mc, BlockPos a, BlockPos b) {
        DustParticleOptions stof = new DustParticleOptions(0xE07AAE, 1.0f);
        double x0 = a.getX(), y0 = a.getY(), z0 = a.getZ(), x1 = b.getX() + 1, y1 = b.getY() + 1, z1 = b.getZ() + 1;
        double stap = Math.max(1, Math.max(x1 - x0, Math.max(y1 - y0, z1 - z0)) / 24.0);
        for (double t = 0; t <= 1.0001; t += stap / Math.max(1, Math.max(x1 - x0, Math.max(y1 - y0, z1 - z0)))) {
            double x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t, z = z0 + (z1 - z0) * t;
            for (double[] p : new double[][] {{x, y0, z0}, {x, y1, z0}, {x, y0, z1}, {x, y1, z1}, {x0, y, z0}, {x1, y, z0}, {x0, y, z1}, {x1, y, z1},
                    {x0, y0, z}, {x1, y0, z}, {x0, y1, z}, {x1, y1, z}}) {
                mc.level.addParticle(stof, p[0], p[1], p[2], 0, 0, 0);
            }
        }
    }

    // =====================================================================================================================
    // the keet's pages: Vormen, Daken, Blauwdrukken
    // =====================================================================================================================

    private static OptieLijst.Houder houder(BouwkeetScreen s) {
        return new OptieLijst.Houder() {
            @Override
            public Button knop(Component tekst, int x, int y, int w, Runnable actie, boolean actief) {
                return s.knop(tekst, x, y, w, actie, actief);
            }

            @Override
            public <T extends AbstractWidget> T voegToe(T widget) {
                return s.voegToe(widget);
            }

            @Override
            public Font font() {
                return s.lettertype();
            }

            @Override
            public void opnieuw() {
                s.update(s.data());
            }
        };
    }

    private static BouwkeetScreen.TabPagina vormPagina(String id) {
        return new BouwkeetScreen.TabPagina() {
            @Override
            public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
                OptieLijst.knoppen(houder(s), tab, x, y, w, h - 22, s.magBewerken(),
                        (key, waarde, tekst) -> s.doe(id, 1, waarde, tekst.isEmpty() && !key.equals("tekst") ? key : key + "=" + tekst));
                s.knop(Component.translatable("gui.guhs.bouwhulp.plaats_spook"), x + w - 120, y + h - 20, 120, () -> s.doe(id, 2, 0, ""), s.magBewerken())
                        .setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.plaats_spook.uitleg")));
            }

            @Override
            public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
                OptieLijst.teken(g, tab, x, y, w, h - 22, TEKST, LICHT);
                int[] gr = tab.getIntArray("Grootte").orElse(new int[] {0, 0, 0});
                GidsTekst.passend(g, Component.translatable("gui.guhs.blauwdruk.maat", gr[0], gr[1], gr[2], tab.getIntOr("Aantal", 0)), x, y + h - 15,
                        w - 126, 0.75f, DONKER, false);
            }
        };
    }

    private static void paginas() {
        BouwkeetScreen.tab(BouwkeetTabs.VORMEN, vormPagina(BouwkeetTabs.VORMEN));
        BouwkeetScreen.tab(BouwkeetTabs.DAKEN, vormPagina(BouwkeetTabs.DAKEN));
        BouwkeetScreen.tab(BouwkeetTabs.BLAUWDRUKKEN, new BouwkeetScreen.TabPagina() {
            @Override
            public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
                BlauwdrukLijst.knoppen(houder(s), tab, x, y, w, h, s.magBewerken(), (a, waarde, tekst) -> s.doe(BouwkeetTabs.BLAUWDRUKKEN, a, waarde, tekst));
            }

            @Override
            public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
                BlauwdrukLijst.teken(g, tab, x, y, w, h, DONKER, TEKST, LICHT);
            }
        });
    }

    private GereedschapClient() {
    }
}
