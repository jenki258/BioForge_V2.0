package net.jenkimods.bioforge.infection.naming;

import net.minecraft.client.Minecraft;

public final class StrainNameClientHandler {
    private StrainNameClientHandler() {}

    public static void open(String fingerprint) {
        Minecraft.getInstance().setScreen(new StrainNamingScreen(fingerprint));
    }
}
