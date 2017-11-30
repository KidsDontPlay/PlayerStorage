package mrriegel.playerstorage.registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.tile.IHUDProvider;
import mrriegel.playerstorage.ExInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileInterface extends CommonTile implements IHUDProvider {

	private EntityPlayer player;
	private String playerName;
	public boolean refreshPlayer = true;

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return ((capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) && getPlayer() != null) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		EntityPlayer p = getPlayer();
		if (p != null && (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY))
			return (T) new ExInventory.Handler(p);
		return super.getCapability(capability, facing);
	}

	public EntityPlayer getPlayer() {
		if (player == null || refreshPlayer) {
			refreshPlayer = false;
			return player = ExInventory.getPlayerByName(playerName, world);
		}
		return player;
	}

	public void setPlayer(@Nonnull EntityPlayer player) {
		this.player = player;
		playerName = player.getName();
		markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		playerName = NBTHelper.get(compound, "player", String.class);
		player = ExInventory.getPlayerByName(playerName, world);
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.set(compound, "player", playerName);
		return super.writeToNBT(compound);
	}

	@Override
	public List<String> getData(boolean sneak, EnumFacing facing) {
		return Collections.singletonList(TextFormatting.GOLD + "Owner: " + (player == null ? TextFormatting.RED : TextFormatting.GREEN) + playerName);
	}

	@Override
	public double scale(boolean sneak, EnumFacing facing) {
		return 1.;
	}

	@Override
	public boolean lineBreak(boolean sneak, EnumFacing facing) {
		return false;
	}

	public static void refresh() {
		Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().worlds).forEach(w -> w.loadedTileEntityList.stream().filter(t -> t instanceof TileInterface).forEach(t -> ((TileInterface) t).refreshPlayer = true));
	}

}
