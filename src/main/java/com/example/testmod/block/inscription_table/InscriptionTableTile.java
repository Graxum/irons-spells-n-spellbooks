package com.example.testmod.block.inscription_table;

import com.example.testmod.TestMod;
import com.example.testmod.gui.inscription_table.InscriptionTableMenu;
import com.example.testmod.item.Scroll;
import com.example.testmod.item.SpellBook;
import com.example.testmod.registries.BlockRegistry;
import com.example.testmod.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;


public class InscriptionTableTile extends BlockEntity implements MenuProvider {
    private InscriptionTableMenu menu;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            TestMod.LOGGER.debug("InscriptionTableTile.contentsChange: {}", slot);
            if (slot != 2)
                updateMenuSlots();
            setChanged();
        }
    };

    private void updateMenuSlots() {
        if (menu != null)
            menu.onSlotsChanged();
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public InscriptionTableTile(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockRegistry.INSCRIPTION_TABLE_TILE.get(), pWorldPosition, pBlockState);
    }

    @Override
    public MutableComponent getDisplayName() {
        return Component.literal("Inscription Table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        menu = new InscriptionTableMenu(containerId, inventory, this);
        return menu;
    }

    public void doInscription(int selectedIndex) {
        // This method is called by the inscription packet
        ItemStack spellBookItemStack = menu.getSpellBookSlot().getItem();
        ItemStack scrollItemStack = menu.getScrollSlot().getItem();

        if (spellBookItemStack.getItem() instanceof SpellBook spellBook && scrollItemStack.getItem() instanceof Scroll scroll) {

            var spellBookData = spellBook.getSpellBookData(spellBookItemStack);
            var scrollData = scroll.getScrollData(scrollItemStack);

            if (spellBookData.addSpell(scrollData.getSpell(), selectedIndex))
                menu.getScrollSlot().remove(1);
        }
    }

    public void setSelectedSpell(int index) {
        this.menu.setSelectedSpell(index);
    }

//    public void removeSelectedSpell(int selectedIndex) {
//        // All data should have been validated by now
//        TestMod.LOGGER.debug("recieving request to destroy");
//        var slots = this.menu.slots;
//
//        ItemStack spellBookItemStack = slots.get(SPELLBOOK_SLOT).getItem();
//        SpellBook spellBook = (SpellBook) spellBookItemStack.getItem();
//        var spellBookData = spellBook.getSpellBookData(spellBookItemStack);
//
//        var spellId = spellBookData.getInscribedSpells()[selectedIndex].getID();
//        var spellLevel = spellBookData.getInscribedSpells()[selectedIndex].getLevel();
//        generateScroll(spellId, spellLevel);
//        spellBookData.removeSpell(selectedIndex);
//
//    }


    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }


    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

}
