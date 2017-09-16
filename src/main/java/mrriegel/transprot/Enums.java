package mrriegel.transprot;

public class Enums {
	public static enum Sort {
		AMOUNT, NAME, MOD;
		private static Sort[] vals = values();

		public Sort next() {
			return vals[(this.ordinal() + 1) % vals.length];
		}
	}

	public static enum GuiMode {
		ITEM, FLUID;
	}
	public static enum MessageAction{
		SORT,DIRECTION,CLEAR,JEI,GUIMODE,SLOT;
	}
}
