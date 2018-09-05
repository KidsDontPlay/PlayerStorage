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
		for (int i = 0; i < ei.items.size(); i++)
			nbt.removeTag("item" + i);
		for (int i = 0; i < ei.fluids.size(); i++)
			nbt.removeTag("fluid" + i);
		nbt.removeTag("itemsize");
		nbt.removeTag("fluidsize");
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		ExInventory ei = ExInventory.getInventory(player);
		ei.deserializeNBT(nbt);
		ei.readSyncOnlyNBT(nbt);
	}

}
