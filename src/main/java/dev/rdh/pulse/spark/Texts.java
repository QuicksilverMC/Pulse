package dev.rdh.pulse.spark;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;

import net.minecraft.text.Text;

public final class Texts {
	private static final GsonComponentSerializer SERIALIZER = GsonComponentSerializer.builder()
			.options(JSONOptions.byDataVersion().at(0))
			.build();

	private Texts() {}

	public static Text of(Component component) {
		return Text.Serializer.fromJson(SERIALIZER.serialize(component));
	}
}
