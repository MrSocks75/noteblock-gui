package dev.mrsocks.noteblockgui;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class NoteblockGui implements ModInitializer {
	public static final String MOD_ID = "noteblockgui";

	/** Client -> server: set the note of the note block at pos. */
	public record SetNotePayload(BlockPos pos, int note, int instrument) implements CustomPacketPayload {
		public static final Type<SetNotePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "set_note"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SetNotePayload> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, SetNotePayload::pos,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, SetNotePayload::note,
				net.minecraft.network.codec.ByteBufCodecs.VAR_INT, SetNotePayload::instrument,
				SetNotePayload::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Mob-head instruments come from a head above the block and can't be chosen. */
	public static boolean isSelectable(NoteBlockInstrument i) {
		return switch (i) {
			case ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD -> false;
			default -> true;
		};
	}

	/** Shift + right-click opens the GUI instead of tuning/playing the block. */
	public static boolean isGuiClick(net.minecraft.world.entity.player.Player player, Level level, BlockPos pos) {
		return player.isShiftKeyDown()
				&& !player.isSpectator()
				&& player.getMainHandItem().isEmpty()
				&& level.getBlockState(pos).is(Blocks.NOTE_BLOCK);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(SetNotePayload.TYPE, SetNotePayload.CODEC);

		// Runs on both sides: stops vanilla from tuning the block on the server.
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				isGuiClick(player, level, hit.getBlockPos()) ? InteractionResult.SUCCESS : InteractionResult.PASS);

		ServerPlayNetworking.registerGlobalReceiver(SetNotePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			String error = apply(player, payload);
			if (error != null) {
				org.slf4j.LoggerFactory.getLogger(MOD_ID).info("Rejected note change from {} at {}: {}", player.getName().getString(), payload.pos(), error);
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Noteblock GUI: " + error), true);
			}
		});
	}

	/** Applies the change; returns an error message, or null on success. */
	private static String apply(ServerPlayer player, SetNotePayload payload) {
		net.minecraft.server.level.ServerLevel level = player.level();
		BlockPos pos = payload.pos();
		if (payload.note() < 0 || payload.note() > 24) return "invalid note";
		if (!level.isLoaded(pos)) return "block is not loaded";
		if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64.0) return "too far away";
		if (!player.mayInteract(level, pos)) return "you can't modify blocks here (spawn protection?)";
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.NOTE_BLOCK)) return "not a note block";
		NoteBlockInstrument[] all = NoteBlockInstrument.values();
		if (payload.instrument() < 0 || payload.instrument() >= all.length) return "invalid instrument";
		NoteBlockInstrument instrument = all[payload.instrument()];
		// Keep whatever is there if a non-selectable (mob head) instrument was sent.
		if (!isSelectable(instrument)) instrument = state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT);
		level.setBlock(pos, state.setValue(BlockStateProperties.NOTE, payload.note())
				.setValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT, instrument), 3);
		return null;
	}
}
