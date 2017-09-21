package mrriegel.playerstorage;

import mrriegel.limelib.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

public class MessageCapaSync extends AbstractMessage {

	public MessageCapaSync() {
	}

	public MessageCapaSync(EntityPlayer player) {
		ExInventory ei = player.getCapability(ExInventory.EXINVENTORY, null);
		nbt = ei.serializeNBT();
		ei.writeSyncOnlyNBT(nbt);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		ExInventory ei = player.getCapability(ExInventory.EXINVENTORY, null);
		ei.deserializeNBT(nbt);
		ei.readSyncOnlyNBT(nbt);
	}

}
