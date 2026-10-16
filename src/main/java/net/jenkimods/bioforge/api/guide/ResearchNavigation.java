package net.jenkimods.bioforge.api.guide;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Navigation operates on translated page views; locked pages remain discoverable. */
public final class ResearchNavigation {
    public enum Section { ALL, BASICS, LABORATORY, MEDICINE, INFECTIONS, MUTATIONS, PROTECTION, ITEMS, FAVORITES;
        public String key() { return "gui.bioforge.research_journal.section." + name().toLowerCase(Locale.ROOT); }
    }
    private ResearchNavigation() {}
    public static Section section(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("mutation/") || path.equals("mutations") || path.equals("mutation_interactions")) return Section.MUTATIONS;
        if (path.startsWith("items/")) return Section.ITEMS;
        if (path.contains("vaccine") || path.contains("medicine") || path.contains("diffuser")
                || path.contains("tablet") || path.contains("pill") || path.equals("directed_treatments")) return Section.MEDICINE;
        if (path.contains("haz") || path.contains("containment") || path.equals("environmental_control") || path.equals("air_vent")) return Section.PROTECTION;
        if (path.contains("infection") || path.contains("pathogen") || path.contains("transmission")
                || path.contains("rabies") || path.contains("foodborne") || path.equals("symptom_reference")) return Section.INFECTIONS;
        if (path.equals("contents") || path.equals("field_workflow") || path.equals("diagnostics") || path.equals("patient_records")) return Section.BASICS;
        return Section.LABORATORY;
    }
    public static List<ResearchJournalPageView> filter(List<ResearchJournalPageView> pages, Section section,
                                                       String query, Set<ResourceLocation> favorites) {
        String[] words = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return pages.stream().filter(page -> section == Section.ALL
                || section == Section.FAVORITES && favorites.contains(page.id()) || section(page.id()) == section)
                .filter(page -> {
                    String text = (page.title().getString() + " " + page.id() + " " + page.body().getString()).toLowerCase(Locale.ROOT);
                    return Arrays.stream(words).allMatch(text::contains);
                }).toList();
    }
}
