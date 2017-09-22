package mrriegel.playerstorage;

import java.util.List;

import mrriegel.limelib.helper.InvHelper;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.AbstractMessage;
import mrriegel.limelib.util.FilterItem;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.MessageAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.oredict.OreDictionary;

public class Message2Server extends AbstractMessage {

	public Message2Server() {
	}

	public Message2Server(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		MessageAction ma = NBTHelper.get(nbt, "action", MessageAction.class);
		if (player.openContainer instanceof ContainerExI) {
			ContainerExI con = (ContainerExI) player.openContainer;
			ExInventory ei = con.ei;
			if (ei == null)
				return;
			NBTTagCompound slot = NBTHelper.get(nbt, "slot", NBTTagCompound.class);
			int mouse = NBTHelper.get(nbt, "mouse", Integer.class);
			boolean shift = NBTHelper.get(nbt, "shift", Boolean.class), ctrl = NBTHelper.get(nbt, "ctrl", Boolean.class), space = NBTHelper.get(nbt, "space", Boolean.class);
			switch (ma) {
			case CLEAR:
				for (int i = 0; i < con.getMatrix().getSizeInventory(); i++) {
					con.getMatrix().setInventorySlotContents(i, ei.insertItem(con.getMatrix().getStackInSlot(i), false));
					con.getMatrix().setInventorySlotContents(i, ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.inventory), con.getMatrix().getStackInSlot(i), false));
				}
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
			case KEYUPDATE:
				con.ctrl = ctrl;
				con.space = space;
				con.shift = shift;
				break;
			case JEITRANSFER:
				if (!player.world.isRemote) {
					handleMessage(player, NBTHelper.set(new NBTTagCompound(), "action", MessageAction.CLEAR), side);
					boolean isempty = true;
					for (ItemStack s : ei.matrix) {
						if (!s.isEmpty()) {
							isempty = false;
							break;
						}
					}
					if (isempty) {
						for (int i = 0; i < 9; i++) {
							boolean ore = false;
							List<ItemStack> stacks = NBTHelper.getList(nbt, i + "l", ItemStack.class);
							if (stacks.isEmpty()) {
								stacks = OreDictionary.getOres(NBTHelper.get(nbt, i + "s", String.class));
								ore = true;
							}
							ItemStack ingredient = ItemStack.EMPTY;
							for (ItemStack s : stacks) {
								ingredient = InvHelper.extractItem(new PlayerMainInvWrapper(player.inventory), new FilterItem(s, true, ore, true), 1, false);
								if (!ingredient.isEmpty())
									break;
							}
							if (ingredient.isEmpty())
								for (ItemStack s : stacks) {
									ingredient = ei.extractItem(new FilterItem(s, true, ore, true), 1, false);
									if (!ingredient.isEmpty())
										break;
								}
							if (!ingredient.isEmpty()) {
								ei.matrix.set(i, ingredient);
							}
						}
						con.inventorySlots.stream().filter(s -> s.inventory instanceof InventoryCrafting).forEach(s -> s.onSlotChanged());
						//								con.inventorySlots.get(i + 1).putStack(ingredient);
						player.openContainer.onCraftMatrixChanged(null);
					}
				}
			default:
				break;
			}
			if (ma.name().toLowerCase().startsWith("team")) {
				EntityPlayer p1 = ExInventory.getPlayerByName(NBTHelper.get(nbt, "player1", String.class), player.world);
				EntityPlayer p2 = ExInventory.getPlayerByName(NBTHelper.get(nbt, "player2", String.class), player.world);
				if (p1 == null || p2 == null || p1 == p2)
					return;
				ExInventory ei1 = ExInventory.getInventory(p1), ei2 = ExInventory.getInventory(p2);
				if (ei1 == null || ei2 == null)
					return;
				switch (ma) {
				case TEAMINVITE:
					if (ei1.members.contains(p2.getName()))
						break;
					ITextComponent text = new TextComponentString(p1.getName() + " invites you to join their PlayerStorage team.");
					ITextComponent yes = new TextComponentString("[Accept]");

					//		ITextComponent no = new TextComponentString("[Decline]");
					Style yesno = new Style();
					yesno.setColor(TextFormatting.GREEN);
					yesno.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click here")));
					yesno.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ClientProxy.TEAMCODE + p1.getName()));
					yes.setStyle(yesno);
					//		yesno = yesno.createShallowCopy();
					//		yesno.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "") {
					//			@Override
					//			public Action getAction() {
					//				System.out.println("no");
					//				return super.getAction();
					//			}
					//		});
					//		no.setStyle(yesno);
					text.appendText(" ");
					text.appendSibling(yes);
					//		text.appendText(" / ");
					//		text.appendSibling(no);
					p2.sendMessage(text);
					break;
				case TEAMUNINVITE:
					if (!ei1.members.contains(p2.getName()))
						break;
					ei1.members.remove(p2.getName());
					ei2.members.remove(p1.getName());
					ei1.markForSync();
					ei2.markForSync();
					p1.sendStatusMessage(new TextComponentString("You broke up with " + p2.getName() + "."), true);
					p2.sendStatusMessage(new TextComponentString(p1.getName() + " broke up with you."), true);
					break;
				default:
					break;
				}
			}
		}
		switch (ma) {
		case TEAMACCEPT:
			EntityPlayer p1 = ExInventory.getPlayerByName(NBTHelper.get(nbt, "player1", String.class), player.world);
			EntityPlayer p2 = ExInventory.getPlayerByName(NBTHelper.get(nbt, "player2", String.class), player.world);
			if (p1 == null || p2 == null || p1 == p2)
				return;
			ExInventory ei1 = ExInventory.getInventory(p1), ei2 = ExInventory.getInventory(p2);
			if (ei1 == null || ei2 == null)
				return;
			if (ei1.members.contains(p2.getName()))
				break;
			ei1.members.add(p2.getName());
			ei2.members.add(p1.getName());
			ei1.markForSync();
			ei2.markForSync();
			p1.sendStatusMessage(new TextComponentString("You accepted " + p2.getName() + "'s invitation."), true);
			p2.sendStatusMessage(new TextComponentString(p1.getName() + " accepted your invitation."), true);
			break;
		default:
			break;
		}
	}

}