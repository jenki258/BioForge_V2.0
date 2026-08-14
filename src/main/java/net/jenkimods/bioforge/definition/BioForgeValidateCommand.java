package net.jenkimods.bioforge.definition;

import com.mojang.brigadier.CommandDispatcher;
import net.jenkimods.bioforge.api.behavior.BioForgeBehaviorRegistry;
import net.jenkimods.bioforge.mutation.MutationLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class BioForgeValidateCommand {
    private BioForgeValidateCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bioforge")
                .then(Commands.literal("validate")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> execute(context.getSource()))));
    }

    private static int execute(CommandSourceStack source) {
        List<String> issues = BioForgeDefinitionManager.validateCurrent();
        source.sendSuccess(() -> Component.literal(String.format(
                "BioForge schema v%d, generation %d: %d pathogens, %d transmissions, %d symptoms, %d mutations",
                BioForgeDefinitionManager.SCHEMA_VERSION, BioForgeDefinitionManager.generation(),
                BioForgeDefinitionManager.PATHOGENS.ids().size(),
                BioForgeDefinitionManager.TRANSMISSIONS.ids().size(),
                BioForgeDefinitionManager.SYMPTOMS.ids().size(),
                MutationLoader.INSTANCE.getAllMutations().size())), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "Addon handlers: %d transmission, %d symptom, %d mutation effect, %d Vaccine Maker operation",
                BioForgeBehaviorRegistry.transmissionIds().size(),
                BioForgeBehaviorRegistry.symptomIds().size(),
                BioForgeBehaviorRegistry.mutationEffectIds().size(),
                BioForgeBehaviorRegistry.vaccineOperationIds().size())), false);
        if (issues.isEmpty() && BioForgeDefinitionManager.lastReloadSuccessful()) {
            source.sendSuccess(() -> Component.literal(
                    "Validation passed: active snapshot is internally consistent."), false);
            return 1;
        }
        source.sendFailure(Component.literal("Validation found " + issues.size()
                + " issue(s); active gameplay snapshot was preserved."));
        issues.stream().limit(20).forEach(issue ->
                source.sendFailure(Component.literal("- " + issue)));
        if (issues.size() > 20) source.sendFailure(Component.literal(
                "- ... and " + (issues.size() - 20) + " more"));
        return 0;
    }
}
