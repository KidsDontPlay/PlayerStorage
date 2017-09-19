package mrriegel.playerstorage;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import mrriegel.limelib.gui.CommonContainer;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.ExInventory.Handler;
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
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
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
		if (ei.mode == GuiMode.ITEM) {
			invs.put("matrix", new InventoryCrafting(this, 3, 3));
			ReflectionHelper.setPrivateValue(InventoryCrafting.class, getMatrix(), ei.matrix, 0);
			addSlotToContainer(new SlotResult(invPlayer.player, getMatrix(), (IInventory) invs.get("result"), 0, 44, 88 + 18 * ei.gridHeight));
			initSlots(getMatrix(), 8, 30 + 18 * ei.gridHeight, 3, 3, 0/*, SlotIng.class, ei*/);
		}
		initPlayerSlots(80, 30 + 18 * ei.gridHeight);
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
					if (ei.mode == GuiMode.ITEM)
						slot.putStack(ExInventory.getInventory(playerIn).insertItem(slot.getStack(), false));
					else {
						FluidActionResult far = FluidUtil.tryEmptyContainer(slot.getStack(), new Handler(playerIn), 10 * Fluid.BUCKET_VOLUME, playerIn, true);
						if (far.success)
							slot.putStack(far.result);
					}
				}
				detectAndSendChanges();
			} else if (inv == invs.get("result")) {
				craftShift();
			}
			return ItemStack.EMPTY;
		}
		return super.transferStackInSlot(playerIn, index);
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		if (!player.world.isRemote) {
			if (ei.mode == GuiMode.ITEM && clickTypeIn == ClickType.PICKUP &&slotId>=0&& slotId<inventorySlots.size()&&getSlot(slotId) != null && getSlot(slotId).getHasStack() && getSlot(slotId).inventory instanceof InventoryPlayer) {
				ItemStack stack = getSlot(slotId).getStack();
				boolean apply = false;
				for (Slot s : getSlotsFor(player.inventory)) {
					if (ctrl && shift) {
						apply = true;
						if (s.getHasStack() && s.getStack().isItemEqual(stack)) {
							ItemStack rest = ei.insertItem(s.getStack(), false);
							s.putStack(rest);
							if (!rest.isEmpty())
								break;
						}
					} else if (ctrl && space && getSlot(slotId).getSlotIndex() > 8) {
						apply = true;
						if (s.getSlotIndex() <= 8)
							continue;
						if (s.getHasStack()) {
							ItemStack rest = ei.insertItem(s.getStack(), false);
							s.putStack(rest);
						}
					}
				}
				if (apply) {
					detectAndSendChanges();
					return ItemStack.EMPTY;
				}
			}
		}
		return super.slotClick(slotId, dragType, clickTypeIn, player);
	}

	public void craftShift() {
		IInventory result = invs.get("result");
		SlotResult sl = (SlotResult) inventorySlots.stream().filter(s -> s instanceof SlotResult).findFirst().get();
		int crafted = 0;
		ItemStack res = result.getStackInSlot(0);
		detectAndSendChanges();
		IItemHandler inv = new PlayerMainInvWrapper(invPlayer);
		while (crafted + res.getCount() <= res.getMaxStackSize()) {
			if (!ItemHandlerHelper.insertItemStacked(inv, res.copy(), true).isEmpty())
				break;
			sl.onTake(getPlayer(), res);
			crafted += res.getCount();
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
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != invs.get("result") /*&& slot.inventory != getMatrix()*/ && super.canMergeSlot(stack, slot);
	}

	public InventoryCrafting getMatrix() {
		return (InventoryCrafting) invs.get("matrix");
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

		public ItemStack onTake(EntityPlayer playerIn, ItemStack stack) {
			if (playerIn.world.isRemote) {
				return stack;
			}
			List<ItemStack> lis = Lists.newArrayList();
			for (int i = 0; i < con().getMatrix().getSizeInventory(); i++)
				lis.add(con().getMatrix().getStackInSlot(i).copy());
			List<Ingredient> ings = con().recipe.getIngredients();
			super.onTake(playerIn, stack);
//			con().detectAndSendChanges();
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
