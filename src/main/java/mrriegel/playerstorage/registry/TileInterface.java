package mrriegel.playerstorage.registry;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.cyclops.commoncapabilities.capability.itemhandler.SlotlessItemHandlerConfig;

import mrriegel.limelib.helper.NBTHelper;
import mrriegel.limelib.tile.CommonTile;
import mrriegel.limelib.tile.IHUDProvider;
import mrriegel.limelib.util.GlobalBlockPos;
import mrriegel.playerstorage.ExInventory;
import mrriegel.playerstorage.PlayerStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileInterface extends CommonTile implements IHUDProvider {

	private EntityPlayer player;
	private String playerName;
	private boolean refreshPlayer = true, on;

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (on && (getPlayer() != null && (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || (PlayerStorage.commonCaps && capability == SlotlessItemHandlerConfig.CAPABILITY)))) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		EntityPlayer p = getPlayer();
		if (p != null && on && (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || (PlayerStorage.commonCaps && capability == SlotlessItemHandlerConfig.CAPABILITY)))
			return (T) new ExInventory.Handler(p, this);
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
		markForSync();
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setOn(boolean on) {
		this.on = on;
		world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
		markForSync();
	}

	public boolean isOn() {
		return on;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		playerName = NBTHelper.get(compound, "player", String.class);
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			on = NBTHelper.get(compound, "on", Boolean.class);
		}
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTHelper.set(compound, "player", playerName);
		NBTHelper.set(compound, "on", on);
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
		for (World world : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
			for (int i = 0; i < world.loadedTileEntityList.size(); i++) {
				TileEntity t = world.loadedTileEntityList.get(i);
				if (t instanceof TileInterface)
					((TileInterface) t).refreshPlayer = true;
			}
		}
	}

	public static void updateState(EntityPlayer player, boolean online) {
		ExInventory.getInventory(player).tiles.stream().map(gp -> (TileInterface) gp.getTile()).//
				forEach(t -> t.setOn(online));
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (getPlayer() != null) {
			ExInventory.getInventory(getPlayer()).tiles.remove(GlobalBlockPos.fromTile(this));
		}
	}

}
