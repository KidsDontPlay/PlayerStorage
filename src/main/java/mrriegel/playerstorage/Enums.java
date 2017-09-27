package mrriegel.playerstorage;

import mrriegel.limelib.helper.NBTHelper;
import net.minecraft.nbt.NBTTagCompound;

public class Enums {
	public static enum Sort {
		AMOUNT("\u03A3"), NAME("AZ"), MOD("M");
		private static Sort[] vals = values();
		public String shortt;

		private Sort(String shortt) {
			this.shortt = shortt;
		}

		public Sort next() {
			return vals[(this.ordinal() + 1) % vals.length];
		}
	}

	public static enum GuiMode {
		ITEM, FLUID;
	}

	public static enum MessageAction {
		SORT, //
		DIRECTION, //
		CLEAR, //
		JEI, //
		GUIMODE, //
		SLOT, //
		INCGRID, //
		DECGRID, //
		KEYUPDATE, //
		TEAMINVITE, //
		TEAMACCEPT, //
		TEAMUNINVITE, //
		JEITRANSFER, //
		DEFAULTGUI, //
		SETLIMIT, //
		PICKUP;

		public NBTTagCompound set(NBTTagCompound nbt) {
			NBTHelper.set(nbt, "action", this);
			return nbt;
		}

	}
}
