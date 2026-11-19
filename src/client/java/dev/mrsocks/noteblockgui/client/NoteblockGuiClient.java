package dev.mrsocks.noteblockgui.client;

import dev.mrsocks.noteblockgui.NoteblockGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class NoteblockGuiClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Must run before the shared callback (which returns SUCCESS and would short-circuit this one).
		net.minecraft.resources.Identifier early = net.minecraft.resources.Identifier.fromNamespaceAndPath(NoteblockGui.MOD_ID, "open_gui");
		UseBlockCallback.EVENT.addPhaseOrdering(early, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
		UseBlockCallback.EVENT.register(early, (player, level, hand, hit) -> {
			if (level.isClientSide() && NoteblockGui.isGuiClick(player, level, hit.getBlockPos())) {
				var state = level.getBlockState(hit.getBlockPos());
				Minecraft.getInstance().setScreenAndShow(new NoteSelectScreen(hit,
						state.getValue(BlockStateProperties.NOTE), state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT)));
			}
			return InteractionResult.PASS;
		});
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ClickTuner::tick);
	}
}
