package net.jenkimods.bioforge.world.medicine;

import net.jenkimods.bioforge.BioForge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class FieldMedicineMenu extends AbstractContainerMenu {
    public final FieldMedicineBlockEntity machine;
    public final ContainerData data;
    private final int machineSlots;
    public FieldMedicineMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (FieldMedicineBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()), new SimpleContainerData(FieldMedicineBlockEntity.DATA_COUNT));
    }
    public FieldMedicineMenu(int id, Inventory inventory, FieldMedicineBlockEntity machine, ContainerData data) {
        super(BioForge.FIELD_MEDICINE_MENU.get(), id); this.machine = machine; this.data = data;
        addDataSlots(data);
        for (int slot = 0; slot < 4; slot++) {
            if (machine.isDiffuser() && slot == 2) continue;
            int x = machine.isDiffuser() ? (slot == 3 ? 118 : 34 + slot * 32) : (slot == 3 ? 142 : 24 + slot * 32);
            addSlot(new SlotItemHandler(machine.items, slot, x, 43) {
                @Override public void onTake(Player player, ItemStack stack) {
                    if (getSlotIndex() == 3 && !machine.isDiffuser() && net.jenkimods.bioforge.vaccine.VaccineProfile.read(stack) != null)
                        net.jenkimods.bioforge.progression.MedicineMilestones.award(player, "field_vaccine", "produced");
                    super.onTake(player, stack);
                }
            });
        }
        machineSlots = slots.size();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 164));
    }
    @Override public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos()), player, machine.getBlockState().getBlock());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), copy = source.copy();
        if (index < machineSlots ? !moveItemStackTo(source, machineSlots, slots.size(), true)
                : !moveItemStackTo(source, 0, machineSlots - 1, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, copy); return copy;
    }
}
