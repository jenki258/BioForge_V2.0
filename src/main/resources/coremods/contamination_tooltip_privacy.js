// Apply to the tooltip receiver only. No inventory, save or network data is modified.
function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    return {
        'bioforge_contamination_tooltip_privacy': {
            target: {type: 'METHOD', class: 'net.minecraft.world.item.ItemStack',
                methodName: ASMAPI.mapMethod('m_41651_'),
                methodDesc: '(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;'},
            transformer: function(method) {
                var instructions = new InsnList();
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'net/jenkimods/bioforge/compat/ContaminationTooltipPrivacy', 'forTooltip',
                    '(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;', false));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
                method.instructions.insert(instructions);
                return method;
            }
        }
    };
}
