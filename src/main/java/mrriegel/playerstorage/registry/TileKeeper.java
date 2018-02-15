package mrriegel.playerstorage.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.tile.IHUDProvider;
import mrriegel.limelib.util.StackWrapper;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.server.SPacketCollectItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;

public class TileKeeper extends CommonTile implements IHUDProvider {

	private List<StackWrapper> items = new ArrayList<>();
	private List<FluidStack> fluids = new ArrayList<>();
	private String name;

	public void create(ExInventory exi) {
		for (StackWrapper sw : exi.items)
			items.add(sw);
		exi.items.clear();
		for (FluidStack fs : exi.fluids)
			fluids.add(fs);
		exi.fluids.clear();
		exi.markForSync();
		name = exi.player.getName();
		if (items.isEmpty() && fluids.isEmpty())
			world.destroyBlock(pos, false);
		else {
			ItemStack paper = new ItemStack(Items.PAPER);
			NBTTagCompound nbt = new NBTTagCompound();
			NBTTagCompound dis = new NBTTagCompound();
			NBTHelper.set(dis, "Name", TextFormatting.RESET + "" + TextFormatting.BOLD + "You died!");
			NBTTagList l = new NBTTagList();
			for (String s : new String[] { name, //
					TextFormatting.GOLD + "Dimension: " + TextFormatting.GRAY + DimensionManager.getProviderType(world.provider.getDimension()).getName() + " (" + world.provider.getDimension() + ")", //
					TextFormatting.GOLD + "Position:" + TextFormatting.GRAY + " x:" + TextFormatting.AQUA + getX() + TextFormatting.GRAY + " y:" + TextFormatting.AQUA + getY() + TextFormatting.GRAY + " z:" + TextFormatting.AQUA + getZ(), //
					TextFormatting.ITALIC + "You should retrieve your items and fluids there." })
				l.appendTag(new NBTTagString(TextFormatting.RESET + "" + TextFormatting.GRAY + s));
			dis.setTag("Lore", l);
			NBTHelper.set(nbt, "display", dis);
			paper.setTagCompound(nbt);
			exi.insertItem(paper, false);
		}
		markForSync();
	}

	public void destroy(EntityPlayerMP player) {
		if (player.getName().equals(name)) {
			ExInventory exi = ExInventory.getInventory(player);
			for (StackWrapper sw : items) {
				for (ItemStack s : StackWrapper.toStackList(sw)) {
					ItemStack rest = exi.insertItem(s, false);
					if (rest.isEmpty()) {
						EntityTracker entitytracker = ((WorldServer) world).getEntityTracker();
						Vec3d vec = player.getLookVec();
						vec = vec.normalize();
						Vec3d vecP = new Vec3d(player.posX, player.posY, player.posZ);
						EntityItem ei = new EntityItem(world, vecP.add(vecP).x, player.posX + .3, vecP.add(vecP).z, s);
						entitytracker.sendToTracking(ei, new SPacketCollectItem(ei.getEntityId(), player.getEntityId(), 1));
					}
					Block.spawnAsEntity(world, pos.up(), rest);
				}
			}
			for (FluidStack fs : fluids) {
				exi.insertFluid(fs, false);
			}
			world.destroyBlock(pos, false);
		}
	}

	@Override
	public boolean openGUI(EntityPlayerMP player) {
		destroy(player);
		return super.openGUI(player);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		int size = NBTHelper.get(compound, "itemsize", Integer.class);
		items.clear();
		for (int i = 0; i < size; i++) {
			StackWrapper sw = StackWrapper.loadStackWrapperFromNBT(NBTHelper.get(compound, "item" + i, NBTTagCompound.class));
			if (sw != null)
				items.add(sw);
		}
		size = NBTHelper.get(compound, "fluidsize", Integer.class);
		fluids.clear();
		for (int i = 0; i < size; i++) {
			FluidStack fs = FluidStack.loadFluidStackFromNBT(NBTHelper.get(compound, "fluid" + i, NBTTagCompound.class));
			if (fs != null)
				fluids.add(fs);
		}
		name = NBTHelper.get(compound, "name", String.class);
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.set(compound, "itemsize", items.size());
		for (int i = 0; i < items.size(); i++)
			NBTHelper.set(compound, "item" + i, items.get(i).writeToNBT(new NBTTagCompound()));
		NBTHelper.set(compound, "fluidsize", fluids.size());
		for (int i = 0; i < fluids.size(); i++)
			NBTHelper.set(compound, "fluid" + i, fluids.get(i).writeToNBT(new NBTTagCompound()));
		NBTHelper.set(compound, "name", name);
		return super.writeToNBT(compound);
	}

	@Override
	public List<String> getData(boolean sneak, EnumFacing facing) {
		return Collections.singletonList(TextFormatting.GOLD + "Owner: " + (name == null ? TextFormatting.RED : TextFormatting.GREEN) + name);
	}

	@Override
	public double scale(boolean sneak, EnumFacing facing) {
		return 1.;
	}

	@Override
	public boolean lineBreak(boolean sneak, EnumFacing facing) {
		return false;
	}

}
