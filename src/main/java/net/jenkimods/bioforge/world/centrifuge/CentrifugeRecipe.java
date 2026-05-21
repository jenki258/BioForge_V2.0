package net.jenkimods.bioforge.world.centrifuge;

import java.util.List;

public record CentrifugeRecipe(
        CentrifugeIngredient input,
        CentrifugeIngredient output,
        boolean copyBloodData,
        boolean copyNbt,
        List<String> copyNbtKeys,
        int processingTime
) {
}