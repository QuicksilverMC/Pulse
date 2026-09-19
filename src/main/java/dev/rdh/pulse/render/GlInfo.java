package dev.rdh.pulse.render;

import org.lwjgl.opengl.GL11C;

public record GlInfo(String vendor, String renderer, String version) {
	private static volatile GlInfo current;

	static void capture() {
		if (current == null) {
			current = new GlInfo(GL11C.glGetString(GL11C.GL_VENDOR), GL11C.glGetString(GL11C.GL_RENDERER), GL11C.glGetString(GL11C.GL_VERSION));
		}
	}

	public static GlInfo get() {
		return current;
	}
}
