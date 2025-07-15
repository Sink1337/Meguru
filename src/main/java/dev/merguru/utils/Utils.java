package dev.merguru.utils;

import dev.merguru.utils.font.CustomFont;
import dev.merguru.utils.font.FontUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IFontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

public interface Utils {
    Minecraft mc = Minecraft.getMinecraft();
    IFontRenderer fr = mc.fontRendererObj;

    Tessellator tessellator = Tessellator.getInstance();
    WorldRenderer worldrenderer = tessellator.getWorldRenderer();

    FontUtil.FontType tenacityFont = FontUtil.FontType.TENACITY,
            iconFont = FontUtil.FontType.ICON,
            neverloseFont = FontUtil.FontType.NEVERLOSE,
            tahomaFont = FontUtil.FontType.TAHOMA,
            rubikFont = FontUtil.FontType.RUBIK,
            interFont = FontUtil.FontType.INTER,
            robotoFont = FontUtil.FontType.ROBOTO,
            idkFont = FontUtil.FontType.IDK;


    //Regular Fonts
    CustomFont tenacityFont12 = tenacityFont.size(12),
            tenacityFont14 = tenacityFont.size(14),
            tenacityFont16 = tenacityFont.size(16),
            tenacityFont18 = tenacityFont.size(18),
            tenacityFont20 = tenacityFont.size(20),
            tenacityFont22 = tenacityFont.size(22),
            tenacityFont24 = tenacityFont.size(24),
            tenacityFont26 = tenacityFont.size(26),
            tenacityFont28 = tenacityFont.size(28),
            tenacityFont32 = tenacityFont.size(32),
            tenacityFont40 = tenacityFont.size(40),
            tenacityFont80 = tenacityFont.size(80),
            tenacityFont100 = tenacityFont.size(100);;


    //Bold Fonts
    CustomFont tenacityBoldFont12 = tenacityFont12.getBoldFont(),
            tenacityBoldFont14 = tenacityFont14.getBoldFont(),
            tenacityBoldFont16 = tenacityFont16.getBoldFont(),
            tenacityBoldFont18 = tenacityFont18.getBoldFont(),
            tenacityBoldFont20 = tenacityFont20.getBoldFont(),
            tenacityBoldFont22 = tenacityFont22.getBoldFont(),
            tenacityBoldFont24 = tenacityFont24.getBoldFont(),
            tenacityBoldFont26 = tenacityFont26.getBoldFont(),
            tenacityBoldFont28 = tenacityFont28.getBoldFont(),
            tenacityBoldFont32 = tenacityFont32.getBoldFont(),
            tenacityBoldFont40 = tenacityFont40.getBoldFont(),
            tenacityBoldFont80 = tenacityFont80.getBoldFont(),
            tenacityBoldFont100 = tenacityFont100.getBoldFont();

    //Icon Fontsor i
    CustomFont iconFont16 = iconFont.size(16),
            iconFont20 = iconFont.size(20),
            iconFont26 = iconFont.size(26),
            iconFont35 = iconFont.size(35),
            iconFont40 = iconFont.size(40);

    //Enchant Font
    CustomFont enchantFont14 = tahomaFont.size(14),
            enchantFont12 = tahomaFont.size(12);

    //INTER Regular Font
    CustomFont idkFont12 = idkFont.size(12),
            idkFont14 = idkFont.size(14),
            idkFont16 = idkFont.size(16),
            idkFont18 = idkFont.size(18),
            idkFont20 = idkFont.size(20),
            idkFont24 = idkFont.size(24),
            idkFont28 = idkFont.size(28);

    //INTER Bold Font
    CustomFont idkBoldFont16 = idkFont16.getBoldFont(),
            idkBoldFont18 = idkFont18.getBoldFont(),
            idkBoldFont20 = idkFont20.getBoldFont(),
            idkBoldFont24 = idkFont24.getBoldFont(),
            idkBoldFont28 = idkFont28.getBoldFont();

    CustomFont interMedium20 = interFont.size(20),
            interBold20 = interMedium20.getBoldFont(),
            interMedium14 = interFont.size(14);


    //Regular Fonts
    CustomFont robotoFont12 = robotoFont.size(12),
            robotoFont14 = robotoFont.size(14),
            robotoFont16 = robotoFont.size(16),
            robotoFont18 = robotoFont.size(18),
            robotoFont20 = robotoFont.size(20),
            robotoFont22 = robotoFont.size(22),
            robotoFont24 = robotoFont.size(24),
            robotoFont26 = robotoFont.size(26),
            robotoFont28 = robotoFont.size(28),
            robotoFont32 = robotoFont.size(32),
            robotoFont40 = robotoFont.size(40),
            robotoFont80 = robotoFont.size(80),
            robotoFont100 = robotoFont.size(100);;


    //Bold Fonts
    CustomFont robotoBoldFont12 = robotoFont12.getBoldFont(),
            robotoBoldFont14 = robotoFont14.getBoldFont(),
            robotoBoldFont16 = robotoFont16.getBoldFont(),
            robotoBoldFont18 = robotoFont18.getBoldFont(),
            robotoBoldFont20 = robotoFont20.getBoldFont(),
            robotoBoldFont22 = robotoFont22.getBoldFont(),
            robotoBoldFont24 = robotoFont24.getBoldFont(),
            robotoBoldFont26 = robotoFont26.getBoldFont(),
            robotoBoldFont28 = robotoFont28.getBoldFont(),
            robotoBoldFont32 = robotoFont32.getBoldFont(),
            robotoBoldFont40 = robotoFont40.getBoldFont(),
            robotoBoldFont80 = robotoFont80.getBoldFont(),
            robotoBoldFont100 = robotoFont100.getBoldFont();

}