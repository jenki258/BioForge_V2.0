package net.jenkimods.bioforge.world.splicer;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class GeneSplicerMenu extends AbstractContainerMenu {
    private final GeneSplicerBlockEntity machine;
    public final ContainerData data;
    public GeneSplicerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (GeneSplicerBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(6));
    }
    public GeneSplicerMenu(int id, Inventory inventory, GeneSplicerBlockEntity machine, ContainerData data) {
        super(BioForge.GENE_SPLICER_MENU.get(), id); this.machine = machine; this.data = data;
        addDataSlots(data);
        for (int slot = 0; slot < 6; slot++) addSlot(new SlotItemHandler(machine.items, slot, slot == 5 ? 210 : 18 + slot * 32, 36));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 43 + col * 18, 184 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 43 + col * 18, 242));
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        return !player.level().isClientSide && stillValid(player) && !player.isSpectator() && machine.button(id, player);
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()), player, BioForge.GENE_SPLICER.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), copy = source.copy();
        if (index < 6 ? !moveItemStackTo(source, 6, slots.size(), true) : !moveItemStackTo(source, 0, 5, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source); return copy;
    }
}
