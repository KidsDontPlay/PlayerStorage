package mrriegel.playerstorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.cyclops.commoncapabilities.api.capability.itemhandler.DefaultSlotlessItemHandlerWrapper;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ISlotlessItemHandler;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import mrriegel.limelib.gui.ContainerNull;
import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.network.PacketHandler;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.playerstorage.Enums.GuiMode;
import mrriegel.playerstorage.Enums.Sort;
import mrriegel.playerstorage.gui.ContainerExI;
import mrriegel.playerstorage.registry.Registry;
import mrriegel.playerstorage.registry.TileInterface;
import mrriegel.playerstorage.registry.TileKeeper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketCollectItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

@EventBusSubscriber(modid = PlayerStorage.MODID)
public class ExInventory implements INBTSerializable<NBTTagCompound> {

	public EntityPlayer player;
	public List<StackWrapper> items = new ArrayList<>(), itemsPlusTeam = new ArrayList<>();
	public List<FluidStack> fluids = new ArrayList<>(), fluidsPlusTeam = new ArrayList<>();
	public int itemLimit = ConfigHandler.itemCapacity, fluidLimit = ConfigHandler.fluidCapacity, gridHeight = 4;
	public boolean needSync = true, defaultGUI = true, autoPickup, infiniteWater, noshift;
	public NonNullList<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);
	private List<ItemStack> itemlist = null;
	public Set<String> members = new HashSet<>();
	public Set<GlobalBlockPos> tiles = new HashSet<>();
	public Object2ObjectMap<ItemStack, Limit> itemLimits = new Object2ObjectOpenCustomHashMap<>(itemStrategy);
	public Object2ObjectMap<FluidStack, Limit> fluidLimits = new Object2ObjectOpenCustomHashMap<>(fluidStrategy);
	public boolean jeiSearch = false, topdown = true, autofocus = true, autopickupInverted = false;
	public Sort sort = Sort.NAME;
	public GuiMode mode = GuiMode.ITEM;
	public List<CraftingRecipe> recipes = new ArrayList<>();
	public ReferenceSet<StackWrapper> dirtyStacks = new ReferenceOpenHashSet<>();
	public ReferenceSet<FluidStack> dirtyFluids = new ReferenceOpenHashSet<>();
	public ObjectSet<ItemStack> highlightItems = new ObjectOpenCustomHashSet<>(itemStrategy);
	public ObjectSet<FluidStack> highlightFluids = new ObjectOpenCustomHashSet<>(fluidStrategy);

	public ExInventory() {
		itemLimits.defaultReturnValue(Limit.defaultValue);
		fluidLimits.defaultReturnValue(Limit.defaultValue);
	}

	private void update() {
		if (player.ticksExisted % 20 == 0) {
			for (CraftingRecipe r : recipes) {
				if (!insertItem(r.recipe.getRecipeOutput(), true).isEmpty())
					continue;
				List<ItemStack> lis = new ArrayList<>();
				List<Ingredient> ings = r.exact ? r.stacks.stream().map(Ingredient::fromStacks).collect(Collectors.toList()) : r.ings;
				for (Ingredient i : ings) {
					ItemStack s = extractItem(i, 1, false);
					if (!i.apply(s))
						break;
					lis.add(s);
				}
				ItemStack result = ItemStack.EMPTY;
				InventoryCrafting inv = new InventoryCrafting(new ContainerNull(), 3, 3);
				for (int i = 0; i < lis.size(); i++)
					inv.setInventorySlotContents(i, lis.get(i));
				if (lis.size() != ings.size() || !insertItem(result = r.recipe.getCraftingResult(inv), true).isEmpty()) {
					for (ItemStack s : lis)
						player.dropItem(insertItem(s, false), false);
				} else {
					player.dropItem(insertItem(result, false), false);
					for (ItemStack s : r.recipe.getRemainingItems(inv))
						player.dropItem(insertItem(s, false), false);
				}
			}
		}
		if (needSync && player.ticksExisted % 3 == 0 && player.openContainer instanceof ContainerExI) {
			itemsPlusTeam = items.stream().map(StackWrapper::copy).collect(Collectors.toList());
			fluidsPlusTeam = fluids.stream().map(FluidStack::copy).collect(Collectors.toList());
			for (ExInventory ei : getMembers()) {
				ei.items.forEach(sw -> {
					boolean merged = false;
					for (StackWrapper w : itemsPlusTeam)
						if (w.canInsert(sw.getStack())) {
							w.setSize(w.getSize() + sw.getSize());
							merged = true;
							break;
						}
					if (!merged)
						itemsPlusTeam.add(sw.copy());
				});
				ei.fluids.forEach(fs -> {
					boolean merged = false;
					for (FluidStack f : fluidsPlusTeam)
						if (f.isFluidEqual(fs)) {
							f.amount += fs.amount;
							merged = true;
							break;
						}
					if (!merged)
						fluidsPlusTeam.add(fs.copy());
				});
			}
			sync((EntityPlayerMP) player);
			needSync = false;

		}
		if (infiniteWater && player.ticksExisted % 20 == 0) {
			int water = getAmountFluid(s -> s.getFluid() == FluidRegistry.WATER);
			if (water >= 2000 && water < 10000) {
				insertFluid(new FluidStack(FluidRegistry.WATER, 1000), true, false);
				needSync = true;
			}
		}
	}

	private void init() {
		if (!player.world.isRemote) {
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.LOG)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.STICK)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.LOG, 1, 1)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.LOG, 1, 2)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.LOG, 1, 3)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.GOLD_BLOCK)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.IRON_BLOCK)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.DIAMOND_BLOCK)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.EMERALD_BLOCK)));
			//			recipes.add(CraftingRecipe.from(new ItemStack(Blocks.STONE), new ItemStack(Blocks.STONE), new ItemStack(Blocks.STONE)));

			tiles.removeIf(gb -> !(gb.getTile() instanceof TileInterface) || (((TileInterface) gb.getTile()).getPlayerName() != null && !(((TileInterface) gb.getTile()).getPlayerName().equals(player.getName()))));
		}
	}

	public List<StackWrapper> getItems() {
		List<StackWrapper> lis = new ArrayList<>(itemsPlusTeam);
		return lis;
	}

	public List<FluidStack> getFluids() {
		List<FluidStack> lis = new ArrayList<>(fluidsPlusTeam);
		return lis;
	}

	private List<ExInventory> getMembers() {
		Validate.isTrue(!player.world.isRemote);
		return members.stream().map(s -> getInventory(getPlayerByName(s, player.world))).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private ItemStack insertItem(ItemStack stack, boolean ignoreLimit, boolean simulate) {
		if (stack.isEmpty())
			return stack;
		int absLimit = (ConfigHandler.infiniteSpace || ignoreLimit ? Integer.MAX_VALUE : itemLimit);
		int limit = itemLimits.get(stack).max;
		boolean voidd = itemLimits.get(stack).voidd;
		ItemStack rest = ItemHandlerHelper.copyStackWithSize(stack, Math.max(0, Math.max((stack.getCount() + getAmountItem(s -> ItemHandlerHelper.canItemStacksStack(stack, s))) - limit, (stack.getCount() + getItemCount()) - absLimit)));
		rest.setCount(Math.min(stack.getCount(), rest.getCount()));
		if (rest.getCount() == stack.getCount())
			return voidd ? ItemStack.EMPTY : rest;
		for (StackWrapper s : items)
			if (s.canInsert(stack)) {
				if (!simulate) {
					s.insert(ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - rest.getCount()));
					//					dirtyStacks.add(s);
					markForSync();
				}
				return voidd ? ItemStack.EMPTY : rest;
			}
		if (!simulate) {
			items.add(new StackWrapper(ItemHandlerHelper.copyStackWithSize(stack, 1), stack.getCount() - rest.getCount()));
			markForSync();
		}
		return voidd ? ItemStack.EMPTY : rest;
	}

	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		return insertItem(stack, false, simulate);
	}

	private ItemStack extractItem(Predicate<ItemStack> pred, int size, boolean useMembers, boolean simulate) {
		if (size <= 0 || pred == null || pred.test(ItemStack.EMPTY))
			return ItemStack.EMPTY;
		for (int i = 0; i < items.size(); i++) {
			StackWrapper s = items.get(i);
			if (pred.test(s.getStack())) {
				size = MathHelper.clamp(s.getSize() - itemLimits.get(s.getStack()).min, 0, size);
				if (size <= 0)
					return ItemStack.EMPTY;
				if (!simulate) {
					markForSync();
					ItemStack res = s.extract(size);
					if (s.getSize() == 0)
						items.remove(i);
					else if (i > 0)
						Collections.swap(items, i, i - 1);
					return res;
				} else
					return ItemHandlerHelper.copyStackWithSize(s.getStack(), Math.min(size, s.getSize()));
			}
		}
		ItemStack ret = ItemStack.EMPTY;
		for (ExInventory ei : getMembers()) {
			if (useMembers && !(ret = ei.extractItem(pred, size, false, simulate)).isEmpty()) {
				markForSync();
				break;
			}
		}
		return ret;
	}

	public ItemStack extractItem(Predicate<ItemStack> pred, int size, boolean simulate) {
		return extractItem(pred, size, true, simulate);
	}

	public ItemStack extractItem(ItemStack stack, int size, boolean simulate) {
		return extractItem(s -> ItemHandlerHelper.canItemStacksStack(s, stack), size, simulate);
	}

	private int insertFluid(FluidStack stack, boolean ignoreLimit, boolean simulate) {
		if (stack == null)
			return 0;
		boolean voidd = fluidLimits.get(stack).voidd;
		int voidSize = stack.amount;
		int absLimit = (ConfigHandler.infiniteSpace || ignoreLimit ? Integer.MAX_VALUE : fluidLimit);
		int limit = fluidLimits.get(stack).max;
		FluidStack dummy = stack.copy();
		int canFill = Math.max(0, Math.min(limit - (/*stack.amount +*/ getAmountFluid(s -> s.isFluidEqual(dummy))), absLimit - (stack.amount + getFluidCount())));
		if (canFill == 0)
			return voidd ? voidSize : 0;
		canFill = Math.min(stack.amount, canFill);
		for (FluidStack s : fluids)
			if (s.isFluidEqual(stack)) {
				if (!simulate) {
					s.amount += canFill;
					markForSync();
				}
				return voidd ? voidSize : canFill;
			}
		if (!simulate) {
			stack = stack.copy();
			stack.amount = canFill;
			fluids.add(stack);
			markForSync();
		}
		return voidd ? voidSize : canFill;
	}

	public int insertFluid(FluidStack stack, boolean simulate) {
		return insertFluid(stack, false, simulate);
	}

	private FluidStack extractFluid(Predicate<FluidStack> pred, int size, boolean useMembers, boolean simulate) {
		if (size <= 0 || pred == null || pred.test(null))
			return null;
		for (int i = 0; i < fluids.size(); i++) {
			FluidStack s = fluids.get(i);
			if (pred.test(s)) {
				size = MathHelper.clamp(s.amount - fluidLimits.get(s).min, 0, size);
				int drain = Math.min(s.amount, size);
				if (drain <= 0)
					return null;
				if (!simulate) {
					markForSync();
					s.amount -= drain;
					FluidStack ret = s.copy();
					ret.amount = drain;
					if (s.amount == 0)
						fluids.remove(i);
					else if (i > 0)
						Collections.swap(fluids, i, i - 1);
					return ret;
				} else {
					return new FluidStack(s, drain);
				}
			}
		}
		FluidStack ret = null;
		for (ExInventory ei : getMembers()) {
			if (useMembers && (ret = ei.extractFluid(pred, size, false, simulate)) != null) {
				markForSync();
				break;
			}
		}
		return ret;
	}

	public FluidStack extractFluid(Predicate<FluidStack> pred, int size, boolean simulate) {
		return extractFluid(pred, size, true, simulate);
	}

	public FluidStack extractFluid(FluidStack stack, int size, boolean simulate) {
		return extractFluid(s -> s != null ? s.isFluidEqual(stack) : stack == null, size, simulate);
	}

	public int getItemCount() {
		return MathHelper.clamp(items.stream().mapToInt(s -> s.getSize()).sum(), 0, Integer.MAX_VALUE);
	}

	public int getFluidCount() {
		return MathHelper.clamp(fluids.stream().mapToInt(s -> s.amount).sum(), 0, Integer.MAX_VALUE);
	}

	public int getAmountItem(Predicate<ItemStack> pred) {
		return items.stream().filter(s -> pred.test(s.getStack())).mapToInt(s -> s.getSize()).findAny().orElse(0);
	}

	public int getAmountFluid(Predicate<FluidStack> pred) {
		return fluids.stream().filter(s -> pred.test(s)).mapToInt(s -> s.amount).findAny().orElse(0);
	}

	public void markForSync() {
		needSync = true;
		itemlist = null;
		if (!player.world.isRemote)
			getMembers().forEach(e -> e.needSync = true);
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
		NBTHelper.set(nbt, "autofocus", autofocus);
		NBTHelper.set(nbt, "sort", sort);
		NBTHelper.set(nbt, "mode", mode);
		NBTHelper.set(nbt, "gridHeight", gridHeight);
		NBTHelper.set(nbt, "dirty", needSync);
		NBTHelper.set(nbt, "defaultGUI", defaultGUI);
		NBTHelper.set(nbt, "autoPickup", autoPickup);
		NBTHelper.set(nbt, "infiniteWater", infiniteWater);
		NBTHelper.set(nbt, "noshift", noshift);
		NBTHelper.setList(nbt, "members", new ArrayList<>(members));
		NBTHelper.setList(nbt, "tilesInt", tiles.stream().map(g -> g.getDimension()).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "tilesPos", tiles.stream().map(g -> g.getPos()).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "itemLimitsKey", new ArrayList<>(itemLimits.keySet()));
		NBTHelper.setList(nbt, "itemLimitsValue", itemLimits.values().stream().map(l -> l.toPos()).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "fluidLimitsKey", new ArrayList<>(fluidLimits.keySet()));
		NBTHelper.setList(nbt, "fluidLimitsValue", fluidLimits.values().stream().map(l -> l.toPos()).collect(Collectors.toList()));
		NBTHelper.set(nbt, "recipesize", recipes.size());
		for (int i = 0; i < recipes.size(); i++)
			NBTHelper.set(nbt, "recipe" + i, CraftingRecipe.serialize(recipes.get(i)));
		NBTHelper.setList(nbt, "hItems", highlightItems.stream().map(s -> s.writeToNBT(new NBTTagCompound())).collect(Collectors.toList()));
		NBTHelper.setList(nbt, "hFluids", highlightFluids.stream().map(s -> s.writeToNBT(new NBTTagCompound())).collect(Collectors.toList()));
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		int size = NBTHelper.get(nbt, "itemsize", Integer.class);
		items.clear();
		for (int i = 0; i < size; i++) {
			StackWrapper sw = StackWrapper.loadStackWrapperFromNBT(NBTHelper.get(nbt, "item" + i, NBTTagCompound.class));
			if (sw != null)
				items.add(sw);
		}
		size = NBTHelper.get(nbt, "fluidsize", Integer.class);
		fluids.clear();
		for (int i = 0; i < size; i++) {
			FluidStack fs = FluidStack.loadFluidStackFromNBT(NBTHelper.get(nbt, "fluid" + i, NBTTagCompound.class));
			if (fs != null)
				fluids.add(fs);
		}
		itemLimit = NBTHelper.get(nbt, "itemLimit", Integer.class);
		fluidLimit = NBTHelper.get(nbt, "fluidLimit", Integer.class);
		List<ItemStack> tmp = NBTHelper.getList(nbt, "matrix", ItemStack.class);
		for (int i = 0; i < matrix.size(); i++)
			matrix.set(i, tmp.get(i));
		jeiSearch = NBTHelper.get(nbt, "jei", Boolean.class);
		topdown = NBTHelper.get(nbt, "top", Boolean.class);
		autofocus = NBTHelper.getSafe(nbt, "autofocus", Boolean.class).orElse(true);
		sort = NBTHelper.get(nbt, "sort", Sort.class);
		mode = NBTHelper.get(nbt, "mode", GuiMode.class);
		gridHeight = NBTHelper.get(nbt, "gridHeight", Integer.class);
		needSync = NBTHelper.get(nbt, "dirty", Boolean.class);
		defaultGUI = NBTHelper.get(nbt, "defaultGUI", Boolean.class);
		autoPickup = NBTHelper.get(nbt, "autoPickup", Boolean.class);
		infiniteWater = NBTHelper.get(nbt, "infiniteWater", Boolean.class);
		noshift = NBTHelper.get(nbt, "noshift", Boolean.class);
		members = new HashSet<>(NBTHelper.getList(nbt, "members", String.class));
		List<Integer> ints = NBTHelper.getList(nbt, "tilesInt", Integer.class);
		List<BlockPos> poss = NBTHelper.getList(nbt, "tilesPos", BlockPos.class);
		tiles.clear();
		for (int i = 0; i < ints.size(); i++)
			tiles.add(new GlobalBlockPos(poss.get(i), ints.get(i)));
		itemLimits.clear();
		List<ItemStack> lisIK = NBTHelper.getList(nbt, "itemLimitsKey", ItemStack.class);
		List<BlockPos> lisIV = NBTHelper.getList(nbt, "itemLimitsValue", BlockPos.class);
		for (int i = 0; i < lisIK.size(); i++)
			itemLimits.put(lisIK.get(i), new Limit(lisIV.get(i)));
		fluidLimits.clear();
		List<FluidStack> lisFK = NBTHelper.getList(nbt, "fluidLimitsKey", FluidStack.class);
		List<BlockPos> lisFV = NBTHelper.getList(nbt, "fluidLimitsValue", BlockPos.class);
		for (int i = 0; i < lisFK.size(); i++)
			fluidLimits.put(lisFK.get(i), new Limit(lisFV.get(i)));
		size = NBTHelper.get(nbt, "recipesize", Integer.class);
		recipes.clear();
		for (int i = 0; i < size; i++) {
			NBTTagCompound n = NBTHelper.get(nbt, "recipe" + i, NBTTagCompound.class);
			CraftingRecipe cr = CraftingRecipe.deserialize(n);
			if (cr != null)
				recipes.add(cr);
		}
		highlightItems.clear();
		highlightItems.addAll(NBTHelper.getList(nbt, "hItems", NBTTagCompound.class).stream().map(n -> new ItemStack(n)).collect(Collectors.toList()));
		highlightFluids.clear();
		highlightFluids.addAll(NBTHelper.getList(nbt, "hFluids", NBTTagCompound.class).stream().map(n -> FluidStack.loadFluidStackFromNBT(n)).collect(Collectors.toList()));
	}

	public void writeSyncOnlyNBT(NBTTagCompound nbt) {
		NBTHelper.set(nbt, "itemsize+", itemsPlusTeam.size());
		for (int i = 0; i < itemsPlusTeam.size(); i++)
			NBTHelper.set(nbt, "item+" + i, itemsPlusTeam.get(i).writeToNBT(new NBTTagCompound()));
		NBTHelper.set(nbt, "fluidsize+", fluidsPlusTeam.size());
		for (int i = 0; i < fluidsPlusTeam.size(); i++)
			NBTHelper.set(nbt, "fluid+" + i, fluidsPlusTeam.get(i).writeToNBT(new NBTTagCompound()));

	}

	public void readSyncOnlyNBT(NBTTagCompound nbt) {
		int size = NBTHelper.get(nbt, "itemsize+", Integer.class);
		itemsPlusTeam.clear();
		for (int i = 0; i < size; i++)
			itemsPlusTeam.add(StackWrapper.loadStackWrapperFromNBT(NBTHelper.get(nbt, "item+" + i, NBTTagCompound.class)));
		size = NBTHelper.get(nbt, "fluidsize+", Integer.class);
		fluidsPlusTeam.clear();
		for (int i = 0; i < size; i++)
			fluidsPlusTeam.add(FluidStack.loadFluidStackFromNBT(NBTHelper.get(nbt, "fluid+" + i, NBTTagCompound.class)));
	}

	@CapabilityInject(ExInventory.class)
	public static Capability<ExInventory> EXINVENTORY = null;
	public static final ResourceLocation LOCATION = new ResourceLocation(PlayerStorage.MODID, "inventory");
	public static Strategy<ItemStack> itemStrategy = new Strategy<ItemStack>() {

		@Override
		public int hashCode(ItemStack o) {
			return (o.getItem().getRegistryName().toString() + o.getItemDamage()).hashCode();
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			return a == null || b == null ? false : a.isItemEqual(b);
		}

	};
	public static Strategy<FluidStack> fluidStrategy = new Strategy<FluidStack>() {

		@Override
		public int hashCode(FluidStack o) {
			return o.getFluid().getName().hashCode();
		}

		@Override
		public boolean equals(FluidStack a, FluidStack b) {
			return a == null || b == null ? false : a.getFluid() == b.getFluid();
		}

	};

	public static void register() {
		CapabilityManager.INSTANCE.register(ExInventory.class, new IStorage<ExInventory>() {

			@Override
			public NBTBase writeNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side) {
				return instance.serializeNBT();
			}

			@Override
			public void readNBT(Capability<ExInventory> capability, ExInventory instance, EnumFacing side, NBTBase nbt) {
				if (nbt instanceof NBTTagCompound) {
					instance.deserializeNBT((NBTTagCompound) nbt);
					instance.init();
				}
			}

		}, ExInventory::new);
	}

	public static ExInventory getInventory(EntityPlayer player) {
		return (player == null || !player.hasCapability(EXINVENTORY, null)) ? null : player.getCapability(EXINVENTORY, null);
	}

	public static void sync(EntityPlayerMP player) {
		PacketHandler.sendTo(new MessageCapaSync(player), player);
	}

	public static EntityPlayer getPlayerByName(String name, @Nullable World world) {
		if (name == null)
			return null;
		if (world != null ? world.isRemote : FMLCommonHandler.instance().getEffectiveSide().isClient())
			return world.getPlayerEntityByName(name);
		else
			return Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().worlds).flatMap(w -> w.playerEntities.stream()).filter(p -> p.getName().equals(name)).findFirst().orElse(null);
	}

	@SubscribeEvent
	public static void tick(PlayerTickEvent event) {
		if (event.phase == Phase.END && !event.player.world.isRemote) {
			ExInventory exi;
			if ((exi = getInventory(event.player)) != null)
				exi.update();
		}
	}

	@SubscribeEvent
	public static void clone(Clone event) {
		if (!event.getEntityPlayer().world.isRemote) {
			ExInventory old, neww;
			if ((old = getInventory(event.getOriginal())) != null && (neww = getInventory(event.getEntityPlayer())) != null) {
				neww.deserializeNBT(old.serializeNBT());
				sync((EntityPlayerMP) event.getEntityPlayer());
			}
		}
	}

	@SubscribeEvent
	public static void attach(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(LOCATION, new Provider((EntityPlayer) event.getObject()));
			if (!event.getObject().world.isRemote)
				TileInterface.refresh();
		}
	}

	@SubscribeEvent
	public static void logout(PlayerLoggedOutEvent event) {
		ExInventory.getInventory(event.player).markForSync();
		if (!event.player.world.isRemote) {
			TileInterface.refresh();
			TileInterface.updateState(event.player, false);
		}
	}

	@SubscribeEvent
	public static void join(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityPlayerMP) {
			sync((EntityPlayerMP) event.getEntity());
			TileInterface.updateState((EntityPlayer) event.getEntity(), true);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void pickup(EntityItemPickupEvent event) {
		ExInventory ei = ExInventory.getInventory(event.getEntityPlayer());
		EntityItem e = event.getItem();
		if (ei.autoPickup != ei.autopickupInverted && !e.getItem().isEmpty()) {
			int count = e.getItem().getCount();
			ItemStack rest = ei.insertItem(e.getItem(), false);
			if (rest.getCount() != count) {
				EntityTracker entitytracker = ((WorldServer) event.getEntityPlayer().world).getEntityTracker();
				entitytracker.sendToTracking(e, new SPacketCollectItem(e.getEntityId(), event.getEntityPlayer().getEntityId(), count - rest.getCount()));
			}
			e.getItem().setCount(rest.getCount());
		}
	}

	@SubscribeEvent
	public static void death(LivingDeathEvent event) {
		if (ConfigHandler.keeper && event.getEntityLiving() instanceof EntityPlayerMP) {
			BlockPos p = new BlockPos(event.getEntityLiving()).down();
			World world = event.getEntityLiving().world;
			while (p.getY() < world.getActualHeight()) {
				if (world.isValid(p) && world.isAirBlock(p)) {
					boolean placed = world.setBlockState(p, Registry.keeper.getDefaultState());
					if (placed) {
						((TileKeeper) world.getTileEntity(p)).create(ExInventory.getInventory((EntityPlayer) event.getEntityLiving()));
						event.getEntityLiving().sendMessage(new TextComponentString(TextFormatting.GOLD + "You lost your entire player storage."));
					}
					break;
				}
				p = p.add(0, 1, 0);
			}
		}
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

	@Interface(iface = "org.cyclops.commoncapabilities.api.capability.itemhandler.ISlotlessItemHandler", modid = "commoncapabilities")
	public static class Handler implements IItemHandler, IFluidHandler, ISlotlessItemHandler {

		private final static Stream<ItemStack> func(StackWrapper s) {
			final int max = s.getStack().getMaxStackSize(), size = (int) Math.ceil(s.getSize() / (double) max);
			List<ItemStack> lis = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				if (i == size - 1) {
					lis.add(ItemHandlerHelper.copyStackWithSize(s.getStack(), s.getSize() - (max * (size - 1))));
				} else {
					lis.add(ItemHandlerHelper.copyStackWithSize(s.getStack(), max));
				}
			}
			return lis.stream();
		};

		ExInventory ei;
		TileInterface tile;

		public Handler(EntityPlayer player, @Nullable TileInterface tile) {
			ei = getInventory(player);
			this.tile = tile;
		}

		private boolean isPlayerOn() {
			return tile == null || tile.isOn();
		}

		private void refresh() {
			if (ei.itemlist == null) {
				ei.itemlist = ei.items.stream().flatMap(Handler::func).filter(st -> !st.isEmpty()).collect(Collectors.toList());
			}
		}

		@Override
		public int getSlots() {
			if (!isPlayerOn())
				return 0;
			if (ConfigHandler.betterInterface) {
				refresh();
				return ei.itemlist.size() + 2;
			} else
				return ei.items.size() + 2;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			if (ConfigHandler.betterInterface) {
				refresh();
				if (ei.itemlist.size() < slot || slot == 0)
					return ItemStack.EMPTY;
				return ei.itemlist.get(slot - 1);
			} else {
				if (ei.items.size() < slot || slot == 0)
					return ItemStack.EMPTY;
				StackWrapper w = ei.items.get(slot - 1);
				return ItemHandlerHelper.copyStackWithSize(w.getStack(), MathHelper.clamp(w.getSize(), 1, w.getStack().getMaxStackSize()));
			}
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isPlayerOn())
				return stack;
			return ei.insertItem(stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return ei.extractItem(getStackInSlot(slot), amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			if (!isPlayerOn())
				return new IFluidTankProperties[0];
			return ei.fluids.stream().map(s -> new FluidTankProperties(s, s.amount * 2)).toArray(IFluidTankProperties[]::new);
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (!isPlayerOn())
				return 0;
			return ei.insertFluid(resource, !doFill);
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if (!isPlayerOn())
				return null;
			return ei.extractFluid(resource, resource.amount, !doDrain);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			if (!isPlayerOn())
				return null;
			if (!ei.fluids.isEmpty())
				return ei.extractFluid(ei.fluids.get(0), maxDrain, !doDrain);
			else
				return null;
		}

		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!isPlayerOn())
				return stack;
			return ei.insertItem(stack, simulate);
		}

		@Override
		public ItemStack extractItem(int amount, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return ei.extractItem(s -> !s.isEmpty(), amount, simulate);
		}

		@Override
		public ItemStack extractItem(ItemStack matchStack, int matchFlags, boolean simulate) {
			if (!isPlayerOn())
				return ItemStack.EMPTY;
			return new DefaultSlotlessItemHandlerWrapper(this).extractItem(matchStack, matchFlags, simulate);
		}

	}

}
