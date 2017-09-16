package mrriegel.transprot;

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
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class ContainerExI extends CommonContainer<EntityPlayer> {

	public boolean space, shift, ctrl;
	IRecipe recipe;

	public ContainerExI(InventoryPlayer invPlayer) {
		super(invPlayer, invPlayer.player, Pair.of("matrix", new InventoryBasic("matrix", false, 9)), Pair.of("result", new InventoryCraftResult()));
		invs.put("matrix", new InventoryCrafting(this, 3, 3));
		for (int i = 0; i < getMatrixList().size(); i++)
			getMatrix().setInventorySlotContents(i, getMatrixList().get(i));
		new SlotGhost(getMatrix(), 0, 0, 0) {
			@Override
			public boolean canTakeStack(EntityPlayer playerIn) {
//				ELCH
				this.putStack(playerIn.inventory.getItemStack());
				return false;
			}
		};
		addSlotToContainer(new SlotCrafting(invPlayer.player, getMatrix(), (IInventory) invs.get("result"), 0, 44, 196) {
			@Override
			public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
				if (playerIn.world.isRemote) {
					return stack;
				}
				List<ItemStack> lis = Lists.newArrayList();
				for (int i = 0; i < getMatrix().getSizeInventory(); i++)
					if (getMatrix().getStackInSlot(i) == null)
						lis.add(null);
					else
						lis.add(getMatrix().getStackInSlot(i).copy());
				super.onTake(playerIn, stack);
				detectAndSendChanges();
				for (int i = 0; i < getMatrix().getSizeInventory(); i++)
					if (getMatrix().getStackInSlot(i) == null && lis.get(i) != null) {
						ItemStack req = ExInventory.getInventory(playerIn).extractItem(new FilterItem(lis.get(i), true, false, true), 1, false);
						getMatrix().setInventorySlotContents(i, req);
					}
				detectAndSendChanges();
				return stack;
			}

			@Override
			public void onSlotChanged() {
				super.onSlotChanged();
				saveMatrix();
			}
		});
		initSlots(getMatrix(), 8, 138, 3, 3);
		initPlayerSlots(80, 138);
		//		if (!invPlayer.player.worldObj.isRemote)
		//			PacketHandler.sendTo(new MessageItemListRequest(getNetworkCore(invPlayer.player.worldObj).network.getItemstacks()), (EntityPlayerMP) invPlayer.player);
	}

	public List<ItemStack> getMatrixList() {
		return ExInventory.getInventory(getPlayer()).matrix;
	}

	protected void saveMatrix() {
		ExInventory.getInventory(getPlayer()).matrix.clear();
		for (int i = 0; i < 9; i++)
			ExInventory.getInventory(getPlayer()).matrix.add(getMatrix().getStackInSlot(i));
	}

	//	abstract public Sort getSort();

	public boolean isTopdown() {
		return true;
	}

	public boolean isJEI() {
		return true;
	}

	@Override
	protected void initSlots() {
	}

	@Override
	protected List<Area> allowedSlots(ItemStack stack, IInventory inv, int index) {
		if (inv == getMatrix())
			return Lists.newArrayList(getAreaForEntireInv(invPlayer));
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
				return null;
			} else if (inv == invs.get("result")) {
				craftShift();
				return null;
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
						ItemStack rest = ExInventory.getInventory(player).insertItem(s.getStack(), false);
						s.putStack(rest);
						if (rest != null)
							break;
					}
				}
				detectAndSendChanges();
				return null;
			}
			if (ctrl && space && getSlot(slotId) != null && getSlot(slotId).getHasStack() && getSlot(slotId).inventory instanceof InventoryPlayer && getSlot(slotId).getSlotIndex() > 8) {
				for (Slot s : getSlotsFor(player.inventory)) {
					if (s.getSlotIndex() <= 8)
						continue;
					if (s.getHasStack()) {
						ItemStack rest = ExInventory.getInventory(player).insertItem(s.getStack(), false);
						s.putStack(rest);
					}
				}
				detectAndSendChanges();
				return null;
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
		SlotCrafting sl = new SlotCrafting(save, getMatrix(), result, 0, 0, 0);
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
		if (recipe != null)
			invs.get("result").setInventorySlotContents(0, recipe.getCraftingResult(getMatrix()));
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return super.canInteractWith(playerIn);
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != invs.get("result") && super.canMergeSlot(stack, slot);
	}

	public InventoryCrafting getMatrix() {
		return (InventoryCrafting) invs.get("matrix");
	}

}
