package dev.mrsocks.noteblockgui.client;

import dev.mrsocks.noteblockgui.NoteblockGui;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.Arrays;
import java.util.List;

public class NoteSelectScreen extends Screen {
	private static final String[] NAMES = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};
	private static final int NOTE_COLS = 5, NOTE_W = 40, INST_COLS = 4, INST_W = 58, BTN_H = 20, GAP = 4, SECTION_GAP = 12;

	/** Instruments the player can pick (everything except mob-head sounds). */
	private static final List<NoteBlockInstrument> INSTRUMENTS =
			Arrays.stream(NoteBlockInstrument.values()).filter(NoteblockGui::isSelectable).toList();

	private final BlockHitResult hit;
	private final BlockPos pos;
	private final int originalNote;
	private final boolean modded;
	private int selectedNote;
	private NoteBlockInstrument selectedInstrument;

	public NoteSelectScreen(BlockHitResult hit, int note, NoteBlockInstrument instrument) {
		super(Component.literal("Select Note"));
		this.hit = hit;
		this.pos = hit.getBlockPos();
		this.originalNote = note;
		this.modded = ClientPlayNetworking.canSend(NoteblockGui.SetNotePayload.TYPE);
		this.selectedNote = note;
		this.selectedInstrument = instrument;
	}

	/** Note 0 = F#3 ... 24 = F#5, matching vanilla. */
	private static String noteLabel(int note) {
		return NAMES[note % 12] + (3 + (note + 6) / 12);
	}

	private static String instrumentLabel(NoteBlockInstrument i) {
		return switch (i) {
			case BASEDRUM -> "Bass Drum";
			case HAT -> "Hi-Hat";
			case BASS -> "Bass";
			case IRON_XYLOPHONE -> "Vibraphone";
			case COW_BELL -> "Cow Bell";
			case DIDGERIDOO -> "Didgeridoo";
			case TRUMPET -> "Trumpet";
			case TRUMPET_EXPOSED -> "Trump. Exp.";
			case TRUMPET_WEATHERED -> "Trump. Wea.";
			case TRUMPET_OXIDIZED -> "Trump. Oxi.";
			default -> {
				String n = i.getSerializedName();
				yield Character.toUpperCase(n.charAt(0)) + n.substring(1);
			}
		};
	}

	private static Component text(String s, boolean selected) {
		return selected ? Component.literal(s).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD) : Component.literal(s);
	}

	@Override
	protected void init() {
		int rows = 5;
		int noteW = NOTE_COLS * NOTE_W + (NOTE_COLS - 1) * GAP;
		int instW = INST_COLS * INST_W + (INST_COLS - 1) * GAP;
		int total = noteW + SECTION_GAP + instW;
		int left = (this.width - total) / 2;
		int top = this.height / 2 - (rows * (BTN_H + GAP)) / 2 - 10;

		for (int n = 0; n < 25; n++) {
			final int note = n;
			this.addRenderableWidget(silentButton(left + (n % NOTE_COLS) * (NOTE_W + GAP), top + (n / NOTE_COLS) * (BTN_H + GAP), NOTE_W, text(noteLabel(n), n == selectedNote), () -> selectNote(note)));
		}
		int instLeft = left + noteW + SECTION_GAP;
		for (int i = 0; i < INSTRUMENTS.size(); i++) {
			NoteBlockInstrument inst = INSTRUMENTS.get(i);
			Button ib = silentButton(instLeft + (i % INST_COLS) * (INST_W + GAP), top + (i / INST_COLS) * (BTN_H + GAP), INST_W, text(instrumentLabel(inst), inst == selectedInstrument), () -> selectInstrument(inst));
			ib.active = modded;
			this.addRenderableWidget(ib);
		}

		int by = top + rows * (BTN_H + GAP) + 6;
		this.addRenderableWidget(Button.builder(Component.literal(modded ? "Apply" : "Apply (note only - server has no mod)"), b -> apply())
				.bounds(left, by, total / 2 - 2, BTN_H).build());
		this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
				.bounds(left + total / 2 + 2, by, total / 2 - 2, BTN_H).build());
	}

	/** A button without the UI click, so it doesn't mask the note preview. */
	private static class SilentButton extends Button.Plain {
		SilentButton(int x, int y, int w, Component label, Runnable action) {
			super(x, y, w, BTN_H, label, b -> action.run(), DEFAULT_NARRATION);
		}

		@Override
		public void playDownSound(net.minecraft.client.sounds.SoundManager sounds) {
		}
	}

	private static Button silentButton(int x, int y, int w, Component label, Runnable action) {
		return new SilentButton(x, y, w, label, action);
	}

	private void selectNote(int note) {
		this.selectedNote = note;
		preview();
		this.rebuildWidgets();
	}

	private void selectInstrument(NoteBlockInstrument inst) {
		this.selectedInstrument = inst;
		preview();
		this.rebuildWidgets();
	}

	private void preview() {
		float pitch = selectedInstrument.isTunable() ? (float) Math.pow(2.0, (selectedNote - 12) / 12.0) : 1.0f;
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(selectedInstrument.getSoundEvent().value(), pitch));
	}

	private void apply() {
		if (modded) {
			ClientPlayNetworking.send(new NoteblockGui.SetNotePayload(pos, selectedNote, selectedInstrument.ordinal()));
		} else {
			// Vanilla server: tune by right-clicking (+1 note per click, wrapping after 24).
			ClickTuner.start(hit, (selectedNote - originalNote + 25) % 25);
		}
		onClose();
	}
}
