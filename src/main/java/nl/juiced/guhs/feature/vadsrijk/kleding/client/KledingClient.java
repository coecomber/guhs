package nl.juiced.guhs.feature.vadsrijk.kleding.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.kleding.KledingPayloads;
import nl.juiced.guhs.feature.vadsrijk.kleding.KledingRegistry;
import nl.juiced.guhs.feature.vadsrijk.kleding.Zweven;

/**
 * T-KLEDING, client: the wardrobe key {@link #KAST} (default O, category Vadsrijk, works in BOTH worlds: cosmetics go home), the
 * screen, the collection tab, the render layer on every player ({@link KledingLaag}), the effects ({@link Effecten}) and the glide.
 * <p><b>Gliding</b> (the zone-3 Pluimguh glide on foot, {@code Z3SystemenClient}): in the Vadsrijk, with gliding unlocked and wings
 * on, where the server allows it (not in arenas/dungeons): hold space in the air while falling and you sink at {@link Zweven#VAL}
 * (the "longer" wings {@link Zweven#VAL_LANG}); holding forward keeps about walking speed ({@link Zweven#VOORUIT}; the "faster"
 * wings {@link Zweven#VOORUIT_SNEL}). Never up. The wing tricks: tap space again in the air: double jump or a small dash (once per
 * flight).
 */
public final class KledingClient {
    public static final String TAB = "kledingkast";
    public static final KeyMapping KAST = new KeyMapping("key.guhs.vr_kledingkast", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KernClient.CATEGORIE);

    private static boolean zweeft, sprongWas, trucGebruikt;
    private static int inLucht;

    private KledingClient() {
    }

    /** Is the local player gliding right now (the wing pose)? */
    public static boolean zweeftNu() {
        return zweeft;
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KAST));
        modBus.addListener(KledingClient::lagen);
        modBus.addListener(KledingClient::renderStates);
        KledingPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof KledingScherm k) {
                k.update(s.data(), java.util.List.of());
            } else {
                mc.setScreen(new KledingScherm(s.data()));
            }
        });
        KledingPayloads.uiterlijkOntvanger = u -> Minecraft.getInstance().execute(() -> {
            int oud = KledingCache.naamkleur(u.speler());
            KledingCache.zet(u.speler(), u.data());
            Minecraft mc = Minecraft.getInstance();
            if (oud != KledingCache.naamkleur(u.speler()) && mc.level != null) {
                Player p = mc.level.getPlayerByUUID(u.speler());
                if (p != null) {
                    p.refreshDisplayName();
                }
            }
        });
        KledingPayloads.staatOntvanger = s -> Minecraft.getInstance().execute(() -> KledingCache.staat = s);
        KledingPayloads.plofOntvanger = p -> Minecraft.getInstance().execute(() -> Effecten.plof(p));
        KledingRegistry.KLIENT_NAAMKLEUR = KledingCache::naamkleur;
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> KledingCache.wis());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (KAST.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    open();
                }
            }
            zweef(mc);
            Effecten.tick();
        });
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 40, () -> Component.translatable("vadsrijk.kleding.tab"), KledingClient::open);
    }

    public static void open() {
        ClientPacketDistributor.sendToServer(new KledingPayloads.Open(""));
    }

    private static void lagen(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> r = event.getPlayerRenderer(skin);
            if (r != null) {
                r.addLayer(new KledingLaag(r));
            }
        }
    }

    private static void renderStates(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar instanceof Player p) {
                    KledingLaag.extract(p, state);
                }
            }
        });
    }

    // ---- gliding ----------------------------------------------------------------------------------------------------------------

    private static void zweef(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            zweeft = false;
            return;
        }
        KledingPayloads.Staat st = KledingCache.staat();
        Cosmetica.Kracht kracht = st.kracht().isEmpty() ? null : Cosmetica.Kracht.valueOf(st.kracht());
        boolean lucht = !p.onGround() && !p.isPassenger() && !p.isInWater() && !p.isInLava() && !p.getAbilities().flying && !p.isFallFlying()
                && !p.onClimbable() && !p.isSpectator();
        boolean mag = Vadsrijk.in(p) && st.hier() && kracht != null;
        boolean sprong = p.input.keyPresses.jump();
        inLucht = lucht ? inLucht + 1 : 0;
        if (p.onGround()) {
            trucGebruikt = false;
        }
        Vec3 dm = p.getDeltaMovement();
        // the trick: a NEW press of space in the air
        if (mag && lucht && sprong && !sprongWas && inLucht > 3 && !trucGebruikt
                && (kracht == Cosmetica.Kracht.DUBBELE_SPRONG || kracht == Cosmetica.Kracht.LUCHTDASH)) {
            trucGebruikt = true;
            if (kracht == Cosmetica.Kracht.DUBBELE_SPRONG) {
                p.setDeltaMovement(dm.x, Zweven.SPRONG, dm.z);
            } else {
                Vec3 kijk = Vec3.directionFromRotation(0, p.getYRot());
                p.setDeltaMovement(kijk.x * Zweven.DASH, Math.max(-0.05, Math.min(dm.y, 0.05)), kijk.z * Zweven.DASH);
            }
            ClientPacketDistributor.sendToServer(new KledingPayloads.Zweef(2));
            for (int i = 0; i < 8; i++) {
                mc.level.addParticle(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), (p.getRandom().nextDouble() - 0.5) * 0.2, 0.02,
                        (p.getRandom().nextDouble() - 0.5) * 0.2);
            }
        }
        sprongWas = sprong;
        dm = p.getDeltaMovement();
        double val = kracht == Cosmetica.Kracht.LANGER_ZWEVEN ? Zweven.VAL_LANG : Zweven.VAL;
        boolean nu = mag && st.zweven() && lucht && sprong && dm.y < val + 0.001;
        if (nu) {
            double vooruit = kracht == Cosmetica.Kracht.SNELLER_ZWEVEN ? Zweven.VOORUIT_SNEL : Zweven.VOORUIT;
            Vec3 kijk = Vec3.directionFromRotation(0, p.getYRot());
            double h = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
            Vec3 hor = p.input.keyPresses.forward() && h < vooruit ? new Vec3(kijk.x * vooruit, 0, kijk.z * vooruit) : new Vec3(dm.x, 0, dm.z);
            p.setDeltaMovement(hor.x, val, hor.z);
            p.fallDistance = 0;
            if (p.tickCount % 4 == 0) {
                mc.level.addParticle(kracht == Cosmetica.Kracht.ZACHTE_LANDING ? ParticleTypes.WHITE_ASH : ParticleTypes.CHERRY_LEAVES, p.getX(),
                        p.getY() + 0.8, p.getZ(), 0, -0.05, 0);
            }
        }
        if (nu != zweeft) {
            ClientPacketDistributor.sendToServer(new KledingPayloads.Zweef(nu ? 1 : 0));
        }
        zweeft = nu;
    }
}
