package mrriegel.playerstorage;

import mrriegel.limelib.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

public class MessageCapaSync extends AbstractMessage {

	public MessageCapaSync() {
	}

	public MessageCapaSync(EntityPlayer player) {
		ExInventory ei = ExInventory.getInventory(player);
		nbt = ei.serializeNBT();
		ei.writeSyncOnlyNBT(nbt);
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		ExInventory ei = ExInventory.getInventory(player);
		ei.deserializeNBT(nbt);
		ei.readSyncOnlyNBT(nbt);
	}

}
