package mrriegel.transprot;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.AbstractMessage;
import mrriegel.transprot.Enums.GuiMode;
import mrriegel.transprot.Enums.MessageAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
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
			ExInventory ei = ExInventory.getInventory(player);
			if (ei == null)
				return;
			NBTTagCompound slot = NBTHelper.get(nbt, "slot", NBTTagCompound.class);
			int mouse = NBTHelper.get(nbt, "mouse", Integer.class);
			boolean shift = NBTHelper.get(nbt, "shift", Boolean.class), ctrl = NBTHelper.get(nbt, "ctrl", Boolean.class);
			switch (NBTHelper.get(nbt, "action", MessageAction.class)) {
			case CLEAR:
				con.getMatrix().clear();
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
				ei.mode = GuiMode.values()[(ei.mode.ordinal() + 1) % 2];
				break;
			case SLOT:
				if (player.world.isRemote || slot == null)
					break;
				ItemStack hand = player.inventory.getItemStack().copy();
				if (ei.mode == GuiMode.ITEM) {
					if (hand.isEmpty()) {
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
					} else {//dump
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
					if (hand.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
						FluidStack stack = FluidStack.loadFluidStackFromNBT(slot);
						IFluidHandlerItem handler = hand.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
						int size = (shift ? 10 : 1) * Fluid.BUCKET_VOLUME;
						FluidStack resource = stack.copy();
						if (mouse == 0) {
							resource.amount = size;
							int filled = handler.fill(resource, false);
							FluidStack newStack = ei.extractFluid(resource, filled, false);
							handler.fill(newStack, true);
						} else if (mouse == 1) {
							resource.amount = size;
							int filled = ei.insertFluid(resource, true);
							resource.amount = filled;
							FluidStack newStack = handler.drain(resource, true);
							ei.insertFluid(newStack, false);
						}

					}
				}
				break;
			}
		}
	}

}