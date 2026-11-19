package dev.mrsocks.noteblockgui.client;

import dev.mrsocks.noteblockgui.NoteblockGui;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fallback for servers without the mod: vanilla raises a note block's note by one per right-click
 * (wrapping 24 -> 0), so we send that many ordinary right-clicks, one per tick. Notes only.
 */
final class ClickTuner {
	private static BlockHitResult target;
	private static final int TICKS_BETWEEN_CLICKS = 4;

	private static int remaining;
	private static int cooldown;

	private ClickTuner() {
	}

	static void start(BlockHitResult hit, int clicks) {
		target = hit;
		remaining = clicks;
		cooldown = 0;
	}

	static void tick(Minecraft mc) {
		if (remaining <= 0 || target == null) return;
		if (mc.player == null || mc.level == null || mc.gameMode == null
				|| !mc.level.getBlockState(target.getBlockPos()).is(Blocks.NOTE_BLOCK)
				|| mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target.getBlockPos())) > 36.0) {
			remaining = 0;
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		NoteblockGui.suppressGuiClick = true;
		try {
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, target);
		} finally {
			NoteblockGui.suppressGuiClick = false;
		}
		remaining--;
		cooldown = TICKS_BETWEEN_CLICKS - 1;
	}
}
