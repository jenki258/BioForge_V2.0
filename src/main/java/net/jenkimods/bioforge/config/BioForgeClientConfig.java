package net.jenkimods.bioforge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BioForgeClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED, HEART, SCRATCHING, PARANOIA, COUGH, SNEEZE;
    private static final ForgeConfigSpec.DoubleValue STETHOSCOPE_VOLUME;
    private static final ForgeConfigSpec.BooleanValue MACHINES,TOOLS,INTERFACES;
    private static final ForgeConfigSpec.BooleanValue SCULK_ECHO_AUDIO;
    private static final ForgeConfigSpec.BooleanValue VISUALS, PARANOIA_VISUALS, FALSE_ECHOES, ENDER_VISUALS, WITHER_VISUALS, HYPOXIA_VISUALS;
    private static final ForgeConfigSpec.DoubleValue VISUAL_INTENSITY;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("audio");
        STETHOSCOPE_VOLUME=b.comment("Stethoscope volume, independent of immersive symptom audio.").defineInRange("stethoscopeVolume",1D,0D,1D);
        MACHINES=b.define("machineSounds",true);TOOLS=b.define("toolSounds",true);INTERFACES=b.define("interfaceSounds",true);
        b.push("symptoms");
        SCULK_ECHO_AUDIO=b.define("sculkEcho",true);
        ENABLED=b.comment("Mute all BioForge symptom sounds on this client when false.").define("enabled",true);
        HEART=b.define("heartbeatInEars",true);SCRATCHING=b.define("headParasiteScratching",true);
        PARANOIA=b.define("paranoiaVoices",true);COUGH=b.define("cough",true);SNEEZE=b.define("sneeze",true);
        b.pop().pop();
        b.push("visuals");
        VISUALS=b.define("enabled",true);
        PARANOIA_VISUALS=b.define("paranoiaHallucinations",true);
        FALSE_ECHOES=b.define("sculkFalseEchoes",true);
        ENDER_VISUALS=b.define("enderDistortion",true);
        WITHER_VISUALS=b.define("witherVision",true);
        HYPOXIA_VISUALS=b.define("hypoxiaVision",true);
        VISUAL_INTENSITY=b.defineInRange("intensity",0.75D,0.0D,1.0D);
        b.pop();SPEC=b.build();
    }
    public static float visualIntensity() { return VISUALS.get() ? VISUAL_INTENSITY.get().floatValue() : 0.0F; }
    public static boolean paranoiaVisuals() { return visualIntensity() > 0 && PARANOIA_VISUALS.get(); }
    public static boolean falseEchoes() { return visualIntensity() > 0 && FALSE_ECHOES.get(); }
    public static boolean enderVisuals() { return visualIntensity() > 0 && ENDER_VISUALS.get(); }
    public static boolean witherVisuals() { return visualIntensity() > 0 && WITHER_VISUALS.get(); }
    public static boolean hypoxiaVisuals() { return visualIntensity() > 0 && HYPOXIA_VISUALS.get(); }
    public static float stethoscopeVolume() { return STETHOSCOPE_VOLUME.get().floatValue(); }
    public static boolean sculkEchoAudio() { return ENABLED.get() && SCULK_ECHO_AUDIO.get(); }
    public static boolean allows(String path) {
        if(path.startsWith("machine."))return MACHINES.get();
        if(path.startsWith("instrument."))return TOOLS.get();
        if(path.startsWith("stethoscope."))return TOOLS.get() && stethoscopeVolume()>0;
        if(path.startsWith("ui."))return INTERFACES.get();
        if (!path.startsWith("symptom.")) return true;
        if (!ENABLED.get()) return false;
        return switch(path) {
            case "symptom.ear_pulse" -> HEART.get();
            case "symptom.ear_scratching" -> SCRATCHING.get();
            case "symptom.paranoia_voice" -> PARANOIA.get();
            case "symptom.cough" -> COUGH.get();
            case "symptom.sneeze" -> SNEEZE.get();
            default -> true;
        };
    }
}
