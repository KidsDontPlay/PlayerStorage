package mrriegel.playerstorage;

import mrriegel.limelib.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

public class MessageCapaSync extends AbstractMessage {

	public MessageCapaSync() {
	}

	public MessageCapaSync(EntityPlayer player) {
		nbt = player.getCapability(ExInventory.EXINVENTORY, null).serializeNBT();
	}

	@Override
	public void handleMessage(EntityPlayer player, NBTTagCompound nbt, Side side) {
		player.getCapability(ExInventory.EXINVENTORY, null).deserializeNBT(nbt);
	}

}
