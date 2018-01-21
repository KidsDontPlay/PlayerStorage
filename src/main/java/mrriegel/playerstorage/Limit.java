package mrriegel.playerstorage;

import net.minecraft.util.math.BlockPos;

public class Limit {
	public static final Limit defaultValue = new Limit(0, 33333333, false);

	public final int min, max;
	public final boolean voidd;

	public Limit(int min, int max, boolean voidd) {
		this.min = min;
		this.max = max;
		this.voidd = voidd;
	}

	public Limit(BlockPos pos) {
		this(pos.getX(), pos.getZ(), pos.getY() != 0);
	}

	public BlockPos toPos() {
		return new BlockPos(min, voidd ? 1 : 0, max);
	}
}
