package nl.juiced.guhs.feature.guhriobeloning.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhriobeloning.GuhrioBeloningPayloads;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;

/**
 * Client side of bbq2 (guhrio-beloning):
 * <ul>
 *     <li>the own models of Pad-guh (a toadstool cap, a little blue vest) and Prinses Perzikguh (crown, golden locks, a pink
 *     dress): geckolib/models/entity/guh_npc_padguh / guh_npc_perzikguh from tools/features/guhrio_beloning_modellen.py;</li>
 *     <li>a Guhshi never shows the leather saddle of the guh model: his own red saddle is part of him (he is saddled so
 *     that he can be ridden);</li>
 *     <li>the little film of a green pipe ({@link GuhrioBeloningPayloads.Film}): whoever goes in is drawn shrinking down
 *     into the pipe and growing out of the other one; for yourself the screen also closes and opens like an iris and
 *     your keys do nothing for that second.</li>
 * </ul>
 * The building blocks are plain block models; their items get a line of text ({@link #lore}).
 */
public final class GuhrioBeloningClient {
    /** One half of a pipe film for one player: which half, how long, when it started (client ticks). */
    private record Film(int fase, int ticks, long start) {
    }

    private static final Map<Integer, Film> FILMS = new ConcurrentHashMap<>();
    /** After the way in the traveller stays hidden this long at most (the server sends the way out). */
    private static final int WACHT = 30;
    private static long klok;
    /** Set by the render-state modifier: how far (0..1) this player is inside a pipe. */
    private static final ContextKey<Float> IN_PIJP = new ContextKey<>(Guhs.id("guhriobeloning_in_pijp"));
    private static final ContextKey<Boolean> GEDUWD = new ContextKey<>(Guhs.id("guhriobeloning_geduwd"));

    private GuhrioBeloningClient() {
    }

    public static void init(IEventBus modBus) {
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.PADGUH, Guhs.id("entity/guh_npc_padguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.PERZIKGUH, Guhs.id("entity/guh_npc_perzikguh"));
        VariantUiterlijk.zet(GuhVariant.GUHSHI, new VariantUiterlijk.Uiterlijk() {
            @Override
            public void botten(GuhEntity guh, GuhRenderFrame frame, float partialTick) {
                frame.bones(bones -> bones.ifPresent("saddle", b -> b.skipRender(true).skipChildrenRender(true)));
            }
        });
        GuhrioBeloningPayloads.filmOntvanger = f -> FILMS.put(f.speler(), new Film(f.fase(), Math.max(1, f.ticks()), klok));
        modBus.addListener((net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) -> event.registerAvatarEntityModifier(
                new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
                    @Override
                    public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(
                            T avatar, net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                        float diep = diepte(avatar.getId(), state.partialTick);
                        state.setRenderData(IN_PIJP, diep > 0.001f ? diep : null);
                    }
                }));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("guhriobeloning_pijp"), GuhrioBeloningClient::iris));
        NeoForge.EVENT_BUS.register(Krimp.class);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(GuhrioBeloningClient::stilStaan);
        NeoForge.EVENT_BUS.addListener(GuhrioBeloningClient::lore);
    }

    /** The four building blocks get their line of text (block.guhs.guhriobeloning_*.lore). */
    private static void lore(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!Guhs.MODID.equals(id.getNamespace()) || !id.getPath().startsWith("guhriobeloning_")) {
            return;
        }
        String key = "block.guhs." + id.getPath() + ".lore";
        if (net.minecraft.client.resources.language.I18n.exists(key)) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private static void tick() {
        klok++;
        if (Minecraft.getInstance().level == null) {
            FILMS.clear();
            return;
        }
        FILMS.values().removeIf(f -> klok - f.start > f.ticks + (f.fase == GuhrioBeloningPayloads.Film.IN ? WACHT : 0));
    }

    /** How far this entity is inside a pipe now: 0 = out in the open, 1 = gone. */
    static float diepte(int entity, float partialTick) {
        Film f = FILMS.get(entity);
        if (f == null) {
            return 0f;
        }
        float t = Mth.clamp((klok - f.start + partialTick) / f.ticks, 0f, 1f);
        return f.fase == GuhrioBeloningPayloads.Film.IN ? t : 1f - t;
    }

    /** Draws a player who is in a pipe film shrinking down into the pipe (the pose is put back after the player is drawn). */
    public static final class Krimp {
        private Krimp() {
        }

        @SubscribeEvent
        public static void voor(RenderLivingEvent.Pre<?, ?, ?> event) {
            Float diep = event.getRenderState().getRenderData(IN_PIJP);
            if (diep != null) {
                event.getPoseStack().pushPose();
                event.getPoseStack().translate(0, -0.9f * diep, 0);
                event.getPoseStack().scale(1f - 0.35f * diep, Math.max(0.02f, 1f - 0.97f * diep), 1f - 0.35f * diep);
                event.getRenderState().setRenderData(GEDUWD, Boolean.TRUE);
            }
        }

        @SubscribeEvent
        public static void na(RenderLivingEvent.Post<?, ?, ?> event) {
            if (event.getRenderState().getRenderData(GEDUWD) != null) {
                event.getRenderState().setRenderData(GEDUWD, null);
                event.getRenderState().setRenderData(IN_PIJP, null);
                event.getPoseStack().popPose();
            }
        }
    }

    /** Your own screen in a pipe: black with a round hole that closes on the way in and opens on the way out. */
    private static void iris(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float diep = diepte(mc.player.getId(), delta.getGameTimeDeltaPartialTick(false));
        if (diep <= 0.001f) {
            return;
        }
        int w = g.guiWidth(), h = g.guiHeight();
        float max = (float) Math.hypot(w / 2.0, h / 2.0) + 2;
        float straal = max * (1f - diep) * (1f - diep);
        int zwart = 0xFF000000, cx = w / 2, cy = h / 2, band = 3;
        for (int y = 0; y < h; y += band) {
            float dy = y + band / 2f - cy;
            if (Math.abs(dy) >= straal) {
                g.fill(0, y, w, y + band, zwart);
                continue;
            }
            int half = (int) Math.sqrt(straal * straal - dy * dy);
            if (cx - half > 0) {
                g.fill(0, y, cx - half, y + band, zwart);
            }
            if (cx + half < w) {
                g.fill(cx + half, y, w, y + band, zwart);
            }
        }
    }

    /** In a pipe your keys do nothing (the game gets an input that never moves, and its keyboard back afterwards). */
    private static void stilStaan(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer p)) {
            return;
        }
        Film f = FILMS.get(p.getId());
        boolean vast = f != null && klok - f.start <= f.ticks;
        if (vast) {
            if (!(p.input instanceof StilInput)) {
                p.input = new StilInput();
            }
        } else if (p.input instanceof StilInput) {
            p.input = new net.minecraft.client.player.KeyboardInput(Minecraft.getInstance().options);
        }
    }

    /**
     * An input that stands still: no walking, no jumping. Only the sneak key is passed on, so the server keeps seeing that
     * you never let go of it (a pipe only takes you again after you did: you do not bounce straight back).
     */
    private static final class StilInput extends net.minecraft.client.player.ClientInput {
        @Override
        public void tick() {
            this.keyPresses = new net.minecraft.world.entity.player.Input(false, false, false, false, false,
                    Minecraft.getInstance().options.keyShift.isDown(), false);
        }
    }
}
