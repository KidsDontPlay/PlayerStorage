package mrriegel.playerstorage;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import mrriegel.limelib.gui.CommonContainer;
import mrriegel.limelib.gui.slot.SlotGhost;
import mrriegel.limelib.util.FilterItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class ContainerExI extends CommonContainer<EntityPlayer> {

	public boolean space, shift, ctrl;
	IRecipe recipe;
	ExInventory ei;

	public ContainerExI(InventoryPlayer invPlayer) {
		super(invPlayer, invPlayer.player, Pair.of("result", new InventoryCraftResult()));
		ei = ExInventory.getInventory(getPlayer());
		invs.put("matrix", new InventoryCrafting(this, 3, 3));
		for (int i = 0; i < ei.matrix.size(); i++)
			getMatrix().setInventorySlotContents(i, ei.matrix.get(i));
		addSlotToContainer(new SlotResult(invPlayer.player, getMatrix(), (IInventory) invs.get("result"), 0, 44, 88 + 18 * ei.gridHeight));
		initSlots(getMatrix(), 8, 30 + 18 * ei.gridHeight, 3, 3, 0/*, SlotIng.class, ei*/);
		initPlayerSlots(80, 30 + 18 * ei.gridHeight);
	}

	protected void saveMatrix() {
		for (int i = 0; i < 9; i++)
			ei.matrix.set(i, getMatrix().getStackInSlot(i));
	}

	@Override
	protected void initSlots() {
	}

	@Override
	protected List<Area> allowedSlots(ItemStack stack, IInventory inv, int index) {
		if (inv == getMatrix())
			return Collections.singletonList(getAreaForEntireInv(invPlayer));
		return null;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		Slot slot = inventorySlots.get(index);
		if (!playerIn.world.isRemote && slot.getHasStack()) {
			IInventory inv = slot.inventory;
			if (inv instanceof InventoryPlayer) {
				if (!ctrl && !space) {
					slot.putStack(ExInventory.getInventory(playerIn).insertItem(slot.getStack(), false));
				}
				detectAndSendChanges();
				return ItemStack.EMPTY;
			} else if (inv == invs.get("result")) {
				craftShift();
				return ItemStack.EMPTY;
			}
		}
		return super.transferStackInSlot(playerIn, index);
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		if (!player.world.isRemote) {
			if (ctrl && shift && getSlot(slotId) != null && getSlot(slotId).getHasStack() && getSlot(slotId).inventory instanceof InventoryPlayer) {
				ItemStack stack = getSlot(slotId).getStack();
				for (Slot s : getSlotsFor(player.inventory)) {
					if (s.getHasStack() && s.getStack().isItemEqual(stack)) {
						ItemStack rest = ei.insertItem(s.getStack(), false);
						s.putStack(rest);
						if (!rest.isEmpty())
							break;
					}
				}
				detectAndSendChanges();
				return ItemStack.EMPTY;
			}
			if (ctrl && space && getSlot(slotId) != null && getSlot(slotId).getHasStack() && getSlot(slotId).inventory instanceof InventoryPlayer && getSlot(slotId).getSlotIndex() > 8) {
				for (Slot s : getSlotsFor(player.inventory)) {
					if (s.getSlotIndex() <= 8)
						continue;
					if (s.getHasStack()) {
						ItemStack rest = ei.insertItem(s.getStack(), false);
						s.putStack(rest);
					}
				}
				detectAndSendChanges();
				return ItemStack.EMPTY;
			}
		}
		return super.slotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		super.onContainerClosed(playerIn);
		saveMatrix();
	}

	public void craftShift() {
		IInventory result = invs.get("result");
		SlotCrafting sl = new SlotCrafting(getPlayer(), getMatrix(), result, 0, 0, 0);
		int crafted = 0;
		List<ItemStack> lis = Lists.newArrayList();
		for (int i = 0; i < getMatrix().getSizeInventory(); i++)
			lis.add(getMatrix().getStackInSlot(i));
		ItemStack res = result.getStackInSlot(0);
		detectAndSendChanges();
		IItemHandler inv = new PlayerMainInvWrapper(invPlayer);
		while (crafted + res.getCount() <= res.getMaxStackSize()) {
			if (ItemHandlerHelper.insertItemStacked(inv, res.copy(), true) != null)
				break;
			ItemHandlerHelper.insertItemStacked(inv, res.copy(), false);
			sl.onTake(save, res);
			crafted += res.getCount();
			for (int i = 0; i < getMatrix().getSizeInventory(); i++)
				if (getMatrix().getStackInSlot(i) == null && lis.get(i) != null) {
					ItemStack req = ExInventory.getInventory(getPlayer()).extractItem(new FilterItem(lis.get(i), true, false, true), 1, false);
					getMatrix().setInventorySlotContents(i, req);
				}
			onCraftMatrixChanged(null);
			if (!ItemHandlerHelper.canItemStacksStack(res, result.getStackInSlot(0)))
				break;
			else
				res = result.getStackInSlot(0);
		}
		detectAndSendChanges();
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventoryIn) {
		super.onCraftMatrixChanged(inventoryIn);
		recipe = CraftingManager.findMatchingRecipe(getMatrix(), getPlayer().world);
		invs.get("result").setInventorySlotContents(0, recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(getMatrix()));
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return super.canInteractWith(playerIn);
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != invs.get("result") /*&& slot.inventory != getMatrix()*/ && super.canMergeSlot(stack, slot);
	}

	public InventoryCrafting getMatrix() {
		return (InventoryCrafting) invs.get("matrix");
	}

	public static class SlotIng extends SlotGhost {

		ExInventory ei;

		public SlotIng(IInventory inventoryIn, int index, int xPosition, int yPosition, ExInventory ei) {
			super(inventoryIn, index, xPosition, yPosition);
			this.ei = ei;
		}

		@Override
		public boolean canTakeStack(EntityPlayer playerIn) {
			return super.canTakeStack(playerIn);
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			if (getHasStack())
				return false;
			ItemStack copy = ItemHandlerHelper.copyStackWithSize(stack, 1);
			ItemStack rest = ei.insertItem(copy, false);
			if (rest.isEmpty())
				stack.shrink(1);
			this.putStack(copy);
			return false;
		}

	}

	public static class SlotResult extends SlotCrafting {
		ExInventory ei;

		public SlotResult(EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
			super(player, craftingInventory, inventoryIn, slotIndex, xPosition, yPosition);
			ei = ExInventory.getInventory(player);
		}

		private ContainerExI con() {
			return (ContainerExI) ei.player.openContainer;
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			//			con().saveMatrix();
		}

		public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
			if (playerIn.world.isRemote) {
				return stack;
			}
			List<ItemStack> lis = Lists.newArrayList();
			for (int i = 0; i < con().getMatrix().getSizeInventory(); i++)
				lis.add(con().getMatrix().getStackInSlot(i).copy());
			List<Ingredient> ings = con().recipe.getIngredients();
			super.onTake(playerIn, stack);
			//			this.onCrafting(stack);
			//			net.minecraftforge.common.ForgeHooks.setCraftingPlayer(playerIn);
			//			NonNullList<ItemStack> nonnulllist = CraftingManager.getRemainingItems(con().getMatrix(), playerIn.world);
			//			net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
			//
			//			for (int i = 0; i < nonnulllist.size(); ++i) {
			//				ItemStack itemstack = con().getMatrix().getStackInSlot(i);
			//				ItemStack itemstack1 = nonnulllist.get(i);
			//
			//				if (!itemstack.isEmpty()) {
			//					con().getMatrix().decrStackSize(i, 1);
			//					itemstack = con().getMatrix().getStackInSlot(i);
			//				}
			//
			//				if (!itemstack1.isEmpty()) {
			//					if (itemstack.isEmpty()) {
			//						con().getMatrix().setInventorySlotContents(i, itemstack1);
			//					} else if (ItemStack.areItemsEqual(itemstack, itemstack1) && ItemStack.areItemStackTagsEqual(itemstack, itemstack1)) {
			//						itemstack1.grow(itemstack.getCount());
			//						con().getMatrix().setInventorySlotContents(i, itemstack1);
			//					} else if (!ei.player.inventory.addItemStackToInventory(itemstack1)) {
			//						ei.player.dropItem(itemstack1, false);
			//					}
			//				}
			//			}

			con().detectAndSendChanges();
			boolean empty = con().getMatrix().isEmpty();
			for (int i = 0; i < con().getMatrix().getSizeInventory(); i++)
				if (con().getMatrix().getStackInSlot(i).isEmpty() && !lis.get(i).isEmpty()) {
					ItemStack req = ei.extractItem(lis.get(i), 1, false);
					if (req.isEmpty() && empty)
						req = ei.extractItem(getIng(ings, lis.get(i)), 1, false);
					con().getMatrix().setInventorySlotContents(i, req);
				}
			con().detectAndSendChanges();
			return stack;
		}

		private Ingredient getIng(List<Ingredient> ings, ItemStack stack) {
			return ings.stream().filter(i -> i.apply(stack)).findAny().orElse(null);
		}

	}

}
