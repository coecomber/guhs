package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.quest.GuhQuests;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: a libelkonijnguh (DESIGN_EINDE2 5): a tiny curious spirit, a bunny with the guh face and four
 * dragonfly wings. It flits about the spot it was put (within its Honk, else {@link #RONDJE} blocks); a player within
 * {@link #NIEUWSGIERIG} blocks is followed at a little distance, and whoever comes too close ({@link #TE_DICHTBIJ}), or just now
 * and then, makes it dart off ({@code schiet_weg}) and come back later. It flies around blocks, never through them.
 * <p>
 * phase B: the cosmetic pet of the side quest "Verdwaalde lantaarns" ({@link Libelkonijnguh}) is this entity with an OWNER
 * (NBT {@code Eigenaar}): it flits round its owner's shoulder instead of its spot, never darts off from its owner, is put at
 * their side when it falls more than {@link #KWIJT} blocks behind, and stays where it is while its owner told it to (sneak-
 * click). A copy that is not its owner's one pet any more takes itself away.
 */
public class LibelkonijnguhEntity extends GeestWezen {
    static final double NIEUWSGIERIG = 10.0, TE_DICHTBIJ = 1.3, RONDJE = 6.0;
    /** Blocks a tick: flitting, and darting away. */
    static final double SNEL = 0.16, WEG = 0.55;

    @Nullable
    private Vec3 thuis, doel;
    private int nieuwDoelOver, wegTot, rustTot;
    private boolean vliegt;
    @Nullable
    private UUID eigenaar;
    static final double KWIJT = 20.0;

    public LibelkonijnguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** phase B: whose pet this is (null: a wild one). */
    public void zetEigenaar(@Nullable UUID wie) {
        eigenaar = wie;
        if (wie != null) {
            zetHonk(0);
        }
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    @Nullable
    private ServerPlayer baasje() {
        return eigenaar == null || !(level().getPlayerByUUID(eigenaar) instanceof ServerPlayer p) ? null : p;
    }

    /** Is it darting off right now (server)? */
    public boolean schietWeg() {
        return wegTot > tickCount;
    }

    @Nullable
    private Player nieuwsgierigNaar() {
        Player p = level().getNearestPlayer(this, NIEUWSGIERIG);
        return p == null || p.isSpectator() ? null : p;
    }

    private void kiesDoel(@Nullable Player speler) {
        if (speler != null && rustTot <= tickCount) {
            double a = random.nextDouble() * Math.PI * 2;
            doel = speler.getEyePosition().add(Math.cos(a) * 2.2, random.nextDouble() * 0.8 - 0.2, Math.sin(a) * 2.2);
        } else {
            double straal = honk() > 0 ? honk() : RONDJE;
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * straal;
            doel = thuis.add(Math.cos(a) * r, random.nextDouble() * 2.0, Math.sin(a) * r);
        }
        nieuwDoelOver = 30 + random.nextInt(40);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (thuis == null) {
            thuis = position();
        }
        vliegt = false;
        if (stil() || isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (eigenaar != null && huisdier()) {
            return;
        }
        Player speler = nieuwsgierigNaar();
        if (!schietWeg() && speler != null && (distanceTo(speler) < TE_DICHTBIJ || random.nextInt(400) == 0)) {
            Vec3 weg = position().subtract(speler.position()).multiply(1, 0, 1);
            weg = weg.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : weg.normalize();
            doel = position().add(weg.scale(7.0)).add(0, 1.5, 0);
            wegTot = tickCount + wezen.duur("schiet_weg");
            rustTot = tickCount + 100 + random.nextInt(100);       // (it keeps its distance a while)
            nieuwDoelOver = 30;
            zetAnimatie("schiet_weg");
        }
        if (doel == null || --nieuwDoelOver <= 0 || position().distanceToSqr(doel) < 0.3) {
            if (!schietWeg()) {
                kiesDoel(speler);
            }
        }
        Vec3 stap = doel.subtract(position());
        double snel = schietWeg() ? WEG : SNEL;
        if (stap.length() > snel) {
            stap = stap.normalize().scale(snel);
        }
        stap = stap.add(0, Math.sin(tickCount * 0.3) * 0.02, 0);   // (a little flutter)
        move(MoverType.SELF, stap);
        setDeltaMovement(Vec3.ZERO);
        if (speler != null && !schietWeg() && rustTot <= tickCount) {
            kijkLangs(speler.position().subtract(position()));       // (it keeps an eye on you)
        } else {
            kijkLangs(stap);
        }
        vliegt = stap.lengthSqr() > 1e-4;
    }

    /** The pet's tick: round its owner's shoulder; true = handled. */
    private boolean huisdier() {
        ServerPlayer baas = baasje();
        if (baas == null) {
            if (eigenaar != null && level().getServer() != null && level().getServer().getPlayerList().getPlayer(eigenaar) == null) {
                setDeltaMovement(Vec3.ZERO);                        // (its owner is away: it waits here)
                return true;
            }
            return false;                                            // (owner in another world: Libelkonijnguh.controleer)
        }
        if (!Libelkonijnguh.isVan(baas, this)) {
            discard();                                               // (an old copy: there is only one)
            return true;
        }
        if (Libelkonijnguh.blijft(baas)) {
            return false;                                            // (told to stay: it flits round where it is)
        }
        if (distanceTo(baas) > KWIJT) {
            Vec3 bij = baas.getEyePosition().add(0, 0.3, 0);
            snapTo(bij.x, bij.y, bij.z, getYRot(), 0f);
            thuis = position();
        }
        if (doel == null || --nieuwDoelOver <= 0 || position().distanceToSqr(doel) < 0.3) {
            double a = random.nextDouble() * Math.PI * 2;
            doel = baas.getEyePosition().add(Math.cos(a) * 1.6, 0.1 + random.nextDouble() * 0.7, Math.sin(a) * 1.6);
            nieuwDoelOver = 20 + random.nextInt(30);
        }
        Vec3 stap = doel.subtract(position());
        double snel = distanceTo(baas) > 4 ? WEG * 0.6 : SNEL;
        if (stap.length() > snel) {
            stap = stap.normalize().scale(snel);
        }
        move(MoverType.SELF, stap.add(0, Math.sin(tickCount * 0.3) * 0.02, 0));
        setDeltaMovement(Vec3.ZERO);
        kijkLangs(random.nextInt(3) == 0 ? baas.position().subtract(position()) : stap);
        vliegt = stap.lengthSqr() > 1e-4;
        thuis = position();
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (eigenaar != null && eigenaar.equals(player.getUUID()) && hand == InteractionHand.MAIN_HAND && !level().isClientSide()
                && player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) {
                boolean blijf = Libelkonijnguh.wisselBlijf(sp);
                zetAnimatie("praat");
                GuhQuests.say(sp, this, PRAATJE + "huisdier." + (blijf ? "blijf" : "mee"));
            } else {
                zetAnimatie("schiet_weg");
                GuhQuests.say(sp, this, PRAATJE + "huisdier." + random.nextInt(2));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (eigenaar != null) {
            tag.putString("Eigenaar", eigenaar.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String e = tag.getStringOr("Eigenaar", "");
        try {
            eigenaar = e.isEmpty() ? null : UUID.fromString(e);
        } catch (IllegalArgumentException ex) {
            eigenaar = null;
        }
    }

    @Override
    protected boolean isBezig() {
        return vliegt || !stil() && !isNoAi();
    }
}
