package mrriegel.playerstorage;

import java.util.Collections;
import java.util.List;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.tile.IHUDProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileInterface extends CommonTile implements IHUDProvider {

	private EntityPlayer player;

	private EntityPlayer getPlayer() {
		return player;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if (getPlayer() != null)
			return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		EntityPlayer p = getPlayer();
		if (p != null && (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY))
			return (T) new ExInventory.Handler(p);
		return super.getCapability(capability, facing);
	}

	public void setPlayer(EntityPlayer player) {
		this.player = player;
		markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		player = ExInventory.getPlayerByName(NBTHelper.get(compound, "player", String.class), world);
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.set(compound, "player", player.getName());
		return super.writeToNBT(compound);
	}

	@Override
	public List<String> getData(boolean sneak, EnumFacing facing) {
		return Collections.singletonList(TextFormatting.GOLD + "Owner: " + player.getName());
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
