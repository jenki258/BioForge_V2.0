package net.jenkimods.bioforge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BioForgeClientConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED, HEART, SCRATCHING, PARANOIA, COUGH, SNEEZE;
    private static final ForgeConfigSpec.DoubleValue STETHOSCOPE_VOLUME;
    private static final ForgeConfigSpec.BooleanValue MACHINES,TOOLS,INTERFACES;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("audio");
        STETHOSCOPE_VOLUME=b.comment("Stethoscope volume, independent of immersive symptom audio.").defineInRange("stethoscopeVolume",1D,0D,1D);
        MACHINES=b.define("machineSounds",true);TOOLS=b.define("toolSounds",true);INTERFACES=b.define("interfaceSounds",true);
        b.push("symptoms");
        ENABLED=b.comment("Mute all BioForge symptom sounds on this client when false.").define("enabled",true);
        HEART=b.define("heartbeatInEars",true);SCRATCHING=b.define("headParasiteScratching",true);
        PARANOIA=b.define("paranoiaVoices",true);COUGH=b.define("cough",true);SNEEZE=b.define("sneeze",true);
        b.pop().pop();SPEC=b.build();
    }
    public static float stethoscopeVolume() { return STETHOSCOPE_VOLUME.get().floatValue(); }
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
