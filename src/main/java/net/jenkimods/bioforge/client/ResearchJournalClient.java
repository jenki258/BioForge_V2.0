package net.jenkimods.bioforge.client;

import net.jenkimods.bioforge.api.guide.ResearchJournalPageView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ResearchJournalClient {
    private ResearchJournalClient() {}

    public static void open(List<ResearchJournalPageView> pages) {
        Minecraft.getInstance().setScreen(new ResearchTabletScreen(pages));
    }

    public static void showUnlock(Component title, boolean activated) {
        Component heading = Component.translatable(activated
                ? "toast.bioforge.research_journal.activated"
                : "toast.bioforge.research_journal.unlocked");
        SystemToast.add(Minecraft.getInstance().getToasts(),
                SystemToast.SystemToastIds.TUTORIAL_HINT, heading, title);
    }
}
