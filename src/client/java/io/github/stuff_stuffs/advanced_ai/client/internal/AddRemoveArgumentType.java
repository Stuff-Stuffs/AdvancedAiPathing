package io.github.stuff_stuffs.advanced_ai.client.internal;

import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.util.StringIdentifiable;

public class AddRemoveArgumentType extends EnumArgumentType<AddRemoveArgumentType.Op> {
    public static final AddRemoveArgumentType ARGUMENT_TYPE = new AddRemoveArgumentType();

    private AddRemoveArgumentType() {
        super(StringIdentifiable.createCodec(Op::values), Op::values);
    }

    public enum Op implements StringIdentifiable {
        ADD,
        REMOVE;

        @Override
        public String asString() {
            return switch (this) {
                case ADD -> "add";
                case REMOVE -> "remove";
            };
        }
    }
}
