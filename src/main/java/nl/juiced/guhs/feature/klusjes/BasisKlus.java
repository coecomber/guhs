package nl.juiced.guhs.feature.klusjes;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.feature.piep.PoepschillyEntity;
import nl.juiced.guhs.feature.piep.SchillyEntity;
import nl.juiced.guhs.registry.ModEntities;

/** The common part of the ten chores: id, icon, wait time, and who may do it (3.0: public, for the critter chores). */
public abstract class BasisKlus implements Klus {
    private final String id;
    private final Supplier<ItemStack> icoon;
    private final int wacht;

    protected BasisKlus(String id, Supplier<ItemStack> icoon, int wacht) {
        this.id = id;
        this.icoon = icoon;
        this.wacht = wacht;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public ItemStack icoon() {
        return icoon.get();
    }

    @Override
    public int wacht() {
        return wacht;
    }

    /** A real guh (not a race / parade stand-in). */
    public static boolean isGuh(Mob m) {
        return m instanceof GuhEntity && m.getType() == ModEntities.GUH.get();
    }

    public static boolean isMuisje(Mob m) {
        return m instanceof PieppiepmuisjeEntity;
    }

    /** Schilly and Poepschilly, the little turtles. */
    public static boolean isSchildpad(Mob m) {
        return m instanceof SchillyEntity || m instanceof PoepschillyEntity;
    }

    /** Mika's that belong to a game or are a character (the Mika Baas, the doolhof / circuit / beroepen Mika's): left alone. */
    public static final java.util.Set<String> SPEL_MIKAS = java.util.Set.of("mika_baas", "doolhof_mika", "circuit_mikapikker", "knabbeldief_mika");

    /** Any kind of Mika (guhs:*mika*, not the game ones): the only ones a guh may gently push away. */
    public static boolean isMika(Entity e) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return Guhs.MODID.equals(key.getNamespace()) && key.getPath().contains("mika") && !SPEL_MIKAS.contains(key.getPath());
    }

    /** A Mika of a game or a character (never a threat for the huisje). */
    public static boolean isSpelMika(Entity e) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return Guhs.MODID.equals(key.getNamespace()) && SPEL_MIKAS.contains(key.getPath());
    }
}
