// Forge 1.20.1: instant potion effects call MobEffect directly instead of addEffect.
function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
    var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
    function gate(method, entityLocal) {
        var instructions = new InsnList();
        var proceed = new LabelNode();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, entityLocal));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            'net/jenkimods/bioforge/mutation/TreatmentRejection', 'blocksEffect',
            '(Lnet/minecraft/world/effect/MobEffect;Lnet/minecraft/world/entity/LivingEntity;)Z', false));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
        instructions.add(new InsnNode(Opcodes.RETURN));
        instructions.add(proceed);
        method.instructions.insert(instructions);
        return method;
    }
    return {
        'bioforge_effect_tick': {
            target: {type: 'METHOD', class: 'net.minecraft.world.effect.MobEffect',
                methodName: ASMAPI.mapMethod('m_6742_'), methodDesc: '(Lnet/minecraft/world/entity/LivingEntity;I)V'},
            transformer: function(method) { return gate(method, 1); }
        },
        'bioforge_effect_instant': {
            target: {type: 'METHOD', class: 'net.minecraft.world.effect.MobEffect',
                methodName: ASMAPI.mapMethod('m_19461_'), methodDesc: '(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;ID)V'},
            transformer: function(method) { return gate(method, 3); }
        }
    };
}
