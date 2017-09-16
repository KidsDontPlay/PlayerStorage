package mrriegel.transprot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.transprot.Enums.GuiMode;
import mrriegel.transprot.Enums.Sort;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@EventBusSubscriber(modid = PlayerStorage.MODID)
public class ExInventory implements INBTSerializable<NBTTagCompound> {

	EntityPlayer player;
	List<StackWrapper> items = new LinkedList<>();
	List<FluidStack> fluids = new LinkedList<>();
	//	List<CraftingPattern> patterns = new LinkedList<>();
	//	List<CraftingTask> tasks = new LinkedList<>();
	int itemLimit = 2000, fluidLimit = 20000;
	boolean dirty = true;
	public List<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);

	public boolean jeiSearch = false, topdown = true;
	public Sort sort = Sort.NAME;
	public GuiMode mode = GuiMode.ITEM;

	private void update() {
		if (dirty && player.ticksExisted % 4 == 0) {
			sync((EntityPlayerMP) player);
			items.removeIf(s -> s.getSize() == 0);
			fluids.removeIf(s -> s == null || s.amount == 0);

			dirty = false;
		}
		//		if (!tasks.isEmpty()) {
		//			CraftingTask t = tasks.remove(0);
		//			t.step();
		//			tasks.add(t);
		//		}
	}

	public List<StackWrapper> getItems() {
		return new ArrayList<>(items);
	}

	public List<FluidStack> getFluids() {
		return new ArrayList<>(fluids);
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		int itemCount = getItemCount();
		ItemStack rest = ItemHandlerHelper.copyStackWithSize(stack, Math.max(0, ConfigHandler.infiniteSpace ? 0 : (stack.getCount() + itemCount) - itemLimit));
		if (rest.getCount() == stack.getCount())
			return rest;
		for (StackWrapper s : items)
			if (s.canInsert(stack)) {
				if (!simulate)
					s.insert(ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - rest.getCount()));
				dirty = true;
				return rest;
			}
		if (!simulate)
			items.add(new StackWrapper(stack, stack.getCount() - rest.getCount()));
		dirty = true;
		return rest;
	}

	public ItemStack extractItem(Predicate<ItemStack> pred, int size, boolean simulate) {
		if (size <= 0)
			return ItemStack.EMPTY;
		for (StackWrapper s : items)
			if (pred.test(s.getStack())) {
				dirty = true;
				if (!simulate)
					return s.extract(size);
				else
					return ItemHandlerHelper.copyStackWithSize(s.getStack(), s.getSize());
			}
		dirty = true;
		return ItemStack.EMPTY;
	}

	public ItemStack extractItem(ItemStack stack, int size, boolean simulate) {
		return extractItem(s -> ItemHandlerHelper.canItemStacksStack(s, stack), size, simulate);
	}

	public int insertFluid(FluidStack stack, boolean simulate) {
		int fluidCount = getFluidCount();
		int canFill = ConfigHandler.infiniteSpace ? stack.amount : Math.max(0, fluidLimit - (stack.amount + fluidCount));
		if (canFill == 0)
			return 0;
		for (FluidStack s : fluids)
			if (s.isFluidEqual(stack)) {
				if (!simulate)
					s.amount += stack.amount - canFill;
				dirty = true;
				return canFill;
			}
		if (!simulate) {
			stack = stack.copy();
			stack.amount = canFill;
			fluids.add(stack);
		}
		dirty = true;
		return canFill;
	}

	public FluidStack extractFluid(Predicate<FluidStack> pred, int size, boolean simulate) {
		if (size <= 0)
			return null;
		for (FluidStack s : fluids)
			if (pred.test(s)) {
				FluidStack res = null;
				int drain = Math.min(s.amount, size);
				if (drain < s.amount) {
					if (!simulate)
						s.amount -= drain;
					res = s.copy();
					res.amount = drain;
				}
				dirty = true;
				return res;
			}
		return null;
	}

	public FluidStack extractFluid(FluidStack stack, int size, boolean simulate) {
		return extractFluid(s -> stack.isFluidEqual(stack), size, simulate);
	}

	public int getItemCount() {
		return items.stream().mapToInt(StackWrapper::getSize).sum();
	}

	public int getFluidCount() {
		return fluids.stream().mapToInt(s -> s.amount).sum();
	}

	public int getAmountItem(Predicate<ItemStack> pred) {
		return items.stream().filter(s -> pred.test(s.getStack())).mapToInt(s -> s.getSize()).findAny().orElse(0);
	}

	public int getAmountFluid(Predicate<FluidStack> pred) {
		return fluids.stream().filter(s -> pred.test(s)).mapToInt(s -> s.amount).findAny().orElse(0);
	}

	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTHelper.set(nbt, "itemsize", items.size());
		for (int i = 0; i < items.size(); i++)
			NBTHelper.set(nbt, "item" + i, items.get(i).writeToNBT(new NBTTagCompound()));
		NBTHelper.set(nbt, "fluidsize", fluids.size());
		for (int i = 0; i < fluids.size(); i++)
			NBTHelper.set(nbt, "fluid" + i, fluids.get(i).writeToNBT(new NBTTagCompound()));
		NBTHelper.set(nbt, "itemLimit", itemLimit);
		NBTHelper.set(nbt, "fluidLimit", fluidLimit);
		NBTHelper.setList(nbt, "matrix", matrix);
		NBTHelper.set(nbt, "jei", jeiSearch);
		NBTHelper.set(nbt, "top", topdown);
		NBTHelper.set(nbt, "sort", sort);
		NBTHelper.set(nbt, "mode", mode);
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		int size = NBTHelper.get(nbt, "itemsize", Integer.class);
		items.clear();
		for (int i = 0; i < size; i++)
			items.add(StackWrapper.loadStackWrapperFromNBT(NBTHelper.get(nbt, "item" + i, NBTTagCompound.class)));
		size = NBTHelper.get(nbt, "fluidsize", Integer.class);
		fluids.clear();
		for (int i = 0; i < size; i++)
			fluids.add(FluidStack.loadFluidStackFromNBT(NBTHelper.get(nbt, "fluid" + i, NBTTagCompound.class)));
		itemLimit = NBTHelper.get(nbt, "itemLimit", Integer.class);
		fluidLimit = NBTHelper.get(nbt, "fluidLimit", Integer.class);
		matrix = NBTHelper.getList(nbt, "matrix", ItemStack.class);
		jeiSearch = NBTHelper.get(nbt, "jei", Boolean.class);
		topdown = NBTHelper.get(nbt, "top", Boolean.class);
		sort = NBTHelper.get(nbt, "sort", Sort.class);
		mode = NBTHelper.get(nbt, "mode", GuiMode.class);
	}

	@CapabilityInject(ExInventory.class)
	public static Capability<ExInventory> EXINVENTORY = null;
	public static final ResourceLocation LOCATION = new ResourceLocation(PlayerStorage.MODID, "inventory");

	public static void register() {
		CapabilityManager.INSTANCE.register(ExInventory.class, new IStorage<ExInventory>() {

			@Override
			public NBTBase writeNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side) {
				return instance.serializeNBT();
			}

			@Override
			public void readNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side, NBTBase nbt) {
				if (nbt instanceof NBTTagCompound)
					instance.deserializeNBT((NBTTagCompound) nbt);
			}

		}, ExInventory::new);
	}

	public static ExInventory getInventory(EntityPlayer player) {
		if (player == null)
			return null;
		return player.getCapability(EXINVENTORY, null);
	}

	@SubscribeEvent
	public static void tick(PlayerTickEvent event) {
		if (event.phase == Phase.END && !event.player.world.isRemote) {
			if (event.player.hasCapability(EXINVENTORY, null))
				event.player.getCapability(EXINVENTORY, null).update();
		}
	}

	@SubscribeEvent
	public static void clone(Clone event) {
		if (!event.getEntityPlayer().world.isRemote) {
			if (event.getOriginal().hasCapability(EXINVENTORY, null) && event.getEntityPlayer().hasCapability(EXINVENTORY, null)) {
				event.getEntityPlayer().getCapability(EXINVENTORY, null).deserializeNBT(event.getOriginal().getCapability(EXINVENTORY, null).serializeNBT());
				sync((EntityPlayerMP) event.getEntityPlayer());
			}
		}
	}

	@SubscribeEvent
	public static void logout(PlayerLoggedOutEvent event) {
		for (WorldServer w : FMLCommonHandler.instance().getMinecraftServerInstance().worlds)
			for (TileEntity t : w.loadedTileEntityList)
				if (t instanceof TileInterface)
					w.notifyBlockUpdate(t.getPos(), ((TileInterface) t).getBlockState(), ((TileInterface) t).getBlockState(), 3);
	}

	@SubscribeEvent
	public static void attach(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(LOCATION, new Provider((EntityPlayer) event.getObject()));
		}
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP) {
			sync((EntityPlayerMP) event.getEntity());
		}
	}

	public static void sync(EntityPlayerMP player) {
		PacketHandler.sendTo(new MessageCapaSync(player), (EntityPlayerMP) player);
	}

	static class Provider implements ICapabilitySerializable<NBTTagCompound> {

		ExInventory ei = EXINVENTORY.getDefaultInstance();

		public Provider(EntityPlayer player) {
			this.ei.player = player;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			return capability == EXINVENTORY;
		}

		@Override
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			return hasCapability(capability, facing) ? EXINVENTORY.cast(ei) : null;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			return (NBTTagCompound) EXINVENTORY.getStorage().writeNBT(EXINVENTORY, ei, null);
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			EXINVENTORY.getStorage().readNBT(EXINVENTORY, ei, null, nbt);
		}

	}

	public static class Handler implements IItemHandler, IFluidHandler {

		ExInventory ei;

		public Handler(EntityPlayer player) {
			ei = getInventory(player);
		}

		@Override
		public int getSlots() {
			return ei.items.size() + 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			if (ei.items.size() <= slot)
				return ItemStack.EMPTY;
			return ei.items.get(slot).getStack();
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return ei.insertItem(stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ei.extractItem(getStackInSlot(slot), amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return ei.fluids.stream().map(s -> new FluidTankProperties(s, s.amount * 2)).toArray(IFluidTankProperties[]::new);
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			return ei.insertFluid(resource, !doFill);
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return ei.extractFluid(resource, resource.amount, !doDrain);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			if (!ei.fluids.isEmpty())
				return ei.extractFluid(ei.fluids.get(0), maxDrain, !doDrain);
			else
				return null;
		}

	}

}
