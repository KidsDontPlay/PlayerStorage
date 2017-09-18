package mrriegel.playerstorage;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.AbstractMessage;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public class MessageInventory extends AbstractMessage {

	public MessageInventory() {
	}

	public MessageInventory(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		if (player.openContainer instanceof ContainerExI) {
			ContainerExI con = (ContainerExI) player.openContainer;
			ExInventory ei = con.ei;
			if (ei == null)
				return;
			NBTTagCompound slot = NBTHelper.get(nbt, "slot", NBTTagCompound.class);
			int mouse = NBTHelper.get(nbt, "mouse", Integer.class);
			boolean shift = NBTHelper.get(nbt, "shift", Boolean.class), ctrl = NBTHelper.get(nbt, "ctrl", Boolean.class);
			switch (NBTHelper.get(nbt, "action", MessageAction.class)) {
			case CLEAR:
				for (int i = 0; i < con.getMatrix().getSizeInventory(); i++)
					con.getMatrix().setInventorySlotContents(i, ei.insertItem(con.getMatrix().getStackInSlot(i), false));
				break;
			case DIRECTION:
				ei.topdown ^= true;
				break;
			case JEI:
				ei.jeiSearch ^= true;
				break;
			case SORT:
				ei.sort = ei.sort.next();
				break;
			case GUIMODE:
				ei.mode = ei.mode == GuiMode.ITEM ? GuiMode.FLUID : GuiMode.ITEM;
				if (!player.world.isRemote)
					player.openGui(PlayerStorage.instance, 0, player.world, 0, 0, 0);
				break;
			case SLOT:
				if (player.world.isRemote)
					break;
				ItemStack hand = player.inventory.getItemStack().copy();
				if (ei.mode == GuiMode.ITEM) {
					if (hand.isEmpty()) {
						if (slot == null)
							break;
						ItemStack stack = new ItemStack(slot);
						int size = ctrl ? 1 : mouse == 0 ? stack.getMaxStackSize() : Math.max(1, stack.getMaxStackSize() / 2);
						ItemStack newStack = ei.extractItem(stack, size, false);
						if (!newStack.isEmpty()) {
							if (shift) {
								player.dropItem(ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.inventory), newStack, false), false);
							} else {
								player.inventory.setItemStack(newStack);
								((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-1, 0, newStack));
							}

						}
					} else {
						//dump
						if (mouse == 0) {
							hand = ei.insertItem(hand, false);
						} else if (mouse == 1) {
							ItemStack x = ei.insertItem(ItemHandlerHelper.copyStackWithSize(hand, 1), false);
							if (x.isEmpty())
								hand.shrink(1);
						}
						player.inventory.setItemStack(hand);
						((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-1, 0, hand));
					}
					con.detectAndSendChanges();
				} else {
					int size = (shift ? 10 : 1) * Fluid.BUCKET_VOLUME;
					IFluidHandlerItem handler;
					if ((handler = FluidUtil.getFluidHandler(hand)) != null) {
						if (mouse == 0) {
							if (slot == null)
								break;
							FluidStack resource = FluidStack.loadFluidStackFromNBT(slot).copy();
							resource.amount = size;
							int filled = handler.fill(resource, false);
							FluidStack newStack = ei.extractFluid(resource, filled, false);
							handler.fill(newStack, true);
						} else if (mouse == 1) {
							FluidStack bucket = handler.drain(size, false);
							int filled = ei.insertFluid(bucket, true);
							FluidStack newStack = handler.drain(filled, true);
							ei.insertFluid(newStack, false);
						}
						hand = handler.getContainer();
						player.inventory.setItemStack(hand);
						((EntityPlayerMP) player).connection.sendPacket(new SPacketSetSlot(-1, 0, hand));
					} else if (hand.isEmpty() && slot != null) {
						FluidStack resource = FluidStack.loadFluidStackFromNBT(slot).copy();
						for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
							ItemStack s = player.inventory.getStackInSlot(i);
							IFluidHandlerItem fh;
							if ((fh = FluidUtil.getFluidHandler(s)) != null) {
								resource.amount = size;
								int filled = fh.fill(resource, false);
								FluidStack newStack = ei.extractFluid(resource, filled, false);
								if (fh.fill(newStack, true) > 0) {
									player.inventory.setInventorySlotContents(i, fh.getContainer());
									con.detectAndSendChanges();
									break;
								}
							}
						}
					}
				}
				break;
			case INCGRID:
				ei.gridHeight++;
				if (!player.world.isRemote) {
					player.openGui(PlayerStorage.instance, 0, player.world, 0, 0, 0);
				}
				break;
			case DECGRID:
				if (ei.gridHeight >= 2)
					ei.gridHeight--;
				if (!player.world.isRemote) {
					player.openGui(PlayerStorage.instance, 0, player.world, 0, 0, 0);
				}
				break;
			}
			ei.dirty = true;
		}
	}

}