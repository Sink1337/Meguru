package dev.meguru.module.impl.misc;

import lombok.Getter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

@Getter
public class EnumFacingUtils {
    private final Vec3 offset;
    public EnumFacing enumFacing;

    public EnumFacingUtils(final EnumFacing enumFacing, final Vec3 offset) {
        this.enumFacing = enumFacing;
        this.offset = offset;
    }
}
