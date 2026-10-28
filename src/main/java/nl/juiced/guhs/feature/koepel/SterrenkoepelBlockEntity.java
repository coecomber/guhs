package nl.juiced.guhs.feature.koepel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.kamperen.KamperenFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * De Sterrenkoepel (guhs:sterrenkoepel, 3 x 2 x 2): the Vadsigheids-monument. It never grows: you feed it knabbels (a
 * knabbelblok counts as 9) and with every {@link KoepelGetallen#kostVoor} knabbels one more guh-sterrenbeeld lights up in the
 * dome (twinkling stars over it, client side), up to {@link KoepelGetallen#STERRENBEELDEN}: a whole sky of guhs. When a new
 * sterrenbeeld lights up, everybody online is "uitgerust" for a while (a small, kind bonus) and hears it. Who gave how much is
 * kept (its own scoreboard: right-click with an empty hand). The stars only shine on {@link KoepelGetallen#STERRENKOEPEL}
 * vadskracht; feeding works always.
 */
public class SterrenkoepelBlockEntity extends MachineBlockEntity implements KoepelBlock.KoepelMachine {
    private long gevoerd;
    private int niveau;
    private final Map<UUID, Long> bijdragen = new HashMap<>();
    private final Map<UUID, String> namen = new HashMap<>();

    public SterrenkoepelBlockEntity(BlockPos pos, BlockState state) {
        super(KoepelFeature.STERRENKOEPEL_BE.get(), pos, state, KoepelGetallen.STERRENKOEPEL, 0);
    }

    public int niveau() {
        return niveau;
    }

    public long gevoerd() {
        return gevoerd;
    }

    /** How many knabbels are needed for the next sterrenbeeld (0: the sky is full). */
    public long nogNodig() {
        if (niveau >= KoepelGetallen.STERRENBEELDEN) {
            return 0;
        }
        long som = 0;
        for (int n = 1; n <= niveau + 1; n++) {
            som += KoepelGetallen.kostVoor(n);
        }
        return som - gevoerd;
    }

    /** Feeds the koepel (also for tests); returns how many new sterrenbeelden lit up. */
    public int voer(ServerPlayer wie, long knabbels) {
        if (knabbels <= 0 || niveau >= KoepelGetallen.STERRENBEELDEN) {
            return 0;
        }
        gevoerd += knabbels;
        if (wie != null) {
            bijdragen.merge(wie.getUUID(), knabbels, Long::sum);
            namen.put(wie.getUUID(), wie.getGameProfile().name());
        }
        int nieuw = 0;
        while (niveau < KoepelGetallen.STERRENBEELDEN && nogNodig() <= 0) {
            niveau++;
            nieuw++;
            vier();
        }
        sync();
        return nieuw;
    }

    private void vier() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Component naam = Component.translatable("gui.guhs.koepel.koepel.beeld." + (niveau - 1));
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            p.addEffect(new MobEffectInstance(KamperenFeature.UITGERUST, KoepelGetallen.STERRENKOEPEL_BONUS_TICKS, 0, true, true));
            p.sendSystemMessage(Component.translatable("gui.guhs.koepel.koepel.nieuw", naam, niveau, KoepelGetallen.STERRENBEELDEN)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1.2f);
        sl.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 1, 40, 1.2, 0.6, 1.2, 0.05);
    }

    /** The top givers, most first: "name: knabbels". */
    public List<Map.Entry<String, Long>> scorebord() {
        List<Map.Entry<String, Long>> lijst = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : bijdragen.entrySet()) {
            lijst.add(Map.entry(namen.getOrDefault(e.getKey(), "?"), e.getValue()));
        }
        lijst.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return lijst;
    }

    @Override
    public boolean klik(ServerPlayer speler, ItemStack inHand) {
        if (!inHand.isEmpty()) {
            int waarde = MegaOvenBlockEntity.isKnabbelblok(inHand) ? 9 : MegaOvenBlockEntity.isKnabbel(inHand) ? 1 : 0;
            if (waarde == 0) {
                return false;
            }
            if (niveau >= KoepelGetallen.STERRENBEELDEN) {
                speler.sendOverlayMessage(Component.translatable("gui.guhs.koepel.koepel.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
                return true;
            }
            int n = inHand.getCount();
            inHand.shrink(n);
            voer(speler, (long) n * waarde);
            speler.sendOverlayMessage(Component.translatable("gui.guhs.koepel.koepel.gevoerd", (long) n * waarde, nogNodig())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WAX_ON, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 1, 10, 0.8, 0.4, 0.8, 0.02);
            }
            return true;
        }
        speler.sendSystemMessage(Component.translatable("gui.guhs.koepel.koepel.stand", niveau, KoepelGetallen.STERRENBEELDEN, nogNodig())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        int plek = 1;
        for (Map.Entry<String, Long> e : scorebord()) {
            if (plek > 5) {
                break;
            }
            speler.sendSystemMessage(Component.literal("  " + plek++ + ". " + e.getKey() + ": " + e.getValue()).withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    @Override
    protected boolean kanWerken() {
        return niveau > 0;
    }

    @Override
    protected boolean isVol() {
        return false;
    }

    @Override
    protected void werk() {
    }

    /** The stars of the lit sterrenbeelden twinkle over the dome (only with vadskracht: {@link #bezig}). */
    @Override
    protected void clientTick() {
        if (!bezig() || level == null || level.getGameTime() % 5 != 0) {
            return;
        }
        for (int b = 0; b < niveau; b++) {
            double hoek = b * Math.PI * 2 / KoepelGetallen.STERRENBEELDEN + level.getGameTime() * 0.002;
            for (int s = 0; s < 3; s++) {
                double r = 0.8 + s * 0.35;
                double x = worldPosition.getX() + 0.5 + Math.cos(hoek + s * 0.25) * r;
                double z = worldPosition.getZ() + 1.0 + Math.sin(hoek + s * 0.25) * r;
                double y = worldPosition.getY() + 2.1 + (s % 2) * 0.2;
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.002, 0);
            }
        }
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.koepel.koepel.uitlezing", niveau, KoepelGetallen.STERRENBEELDEN).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putLong("Gevoerd", gevoerd);
        uit.putInt("Niveau", niveau);
        var lijst = uit.list("Bijdragen", com.mojang.serialization.Codec.STRING);
        for (Map.Entry<UUID, Long> e : bijdragen.entrySet()) {     // "uuid|knabbels|name"
            lijst.add(e.getKey() + "|" + e.getValue() + "|" + namen.getOrDefault(e.getKey(), "?"));
        }
    }

    @Override
    protected void laden(ValueInput in) {
        gevoerd = in.getLongOr("Gevoerd", 0);
        niveau = in.getIntOr("Niveau", 0);
        bijdragen.clear();
        namen.clear();
        for (String regel : in.listOrEmpty("Bijdragen", com.mojang.serialization.Codec.STRING)) {
            String[] d = regel.split("[|]", 3);
            try {
                UUID wie = UUID.fromString(d[0]);
                bijdragen.put(wie, Long.parseLong(d[1]));
                namen.put(wie, d.length > 2 ? d[2] : "?");
            } catch (RuntimeException ignored) {
            }
        }
    }
}
