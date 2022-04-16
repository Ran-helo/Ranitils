package io.github.ran.ranitils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import com.sun.jna.Function;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class ColorUtils {
    private static final Map<Color, String> colorToMinecraftResourceLocation = new HashMap<>();

    /**
     * Gets the average color of the image.
     * @param inputStream The input stream of the image.
     * @return The average color of the image.
     * @throws IOException If the image fails to be read.
     */
    public static int getAverageColorImage(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int pixel : pixels) {
            red += (pixel >> 16) & 0xff;
            green += (pixel >> 8) & 0xff;
            blue += (pixel) & 0xff;
        }
        return new Color(red / pixels.length, green / pixels.length, blue / pixels.length).getRGB();
    }

    /**
     * This used for generating maps that's used for mapping colors to minecraft resource names.
     * @param resourceLocationStream The stream of the resource location you can get this by using
     *                               <br>
     *                               Minecraft.getMinecraft().getResourceManager().getResource((new ResourceLocation(resourceLocation))).getInputStream();
     * @param nameOfBlockOrItemWithNamespace The name of the block or item with namespace. For example: minecraft:stone
     *                                       <br>
     *                                       It's basically the resource location of the block or item.
     * @return The average color of the block or item.
     * @throws IOException If it fails to read the image.
     */
    public static int putMinecraftBlockItemColor(InputStream resourceLocationStream, String nameOfBlockOrItemWithNamespace) throws IOException {
        int color = getAverageColorImage(resourceLocationStream);
        if (!colorToMinecraftResourceLocation.containsKey(new Color(color))) {
            colorToMinecraftResourceLocation.put(new Color(color), nameOfBlockOrItemWithNamespace);
        }
        return color;
    }

    /**
     * Gets the closest
     * @param color The color to get the closest minecraft resource location for.
     * @return The resource location of the closest minecraft block or item.
     */
    public static String closestColorToMinecraftBlockItem(Color color) {
        int closestColor = Integer.MAX_VALUE;
        String closestResourceLocation = "";
        for (Map.Entry<Color, String> entry : colorToMinecraftResourceLocation.entrySet()) {
            int distance = color.getRGB() - entry.getKey().getRGB();
            if (distance < closestColor) {
                closestColor = distance;
                closestResourceLocation = entry.getValue();
            }
        }
        return closestResourceLocation;
    }

    /**
     * Clears the map that's used for mapping colors to minecraft resource names.
     */
    public static void clearMinecraftColorTable() {
        colorToMinecraftResourceLocation.clear();
    }

    /**
     * Gets the chroma color for a given coordinates.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param offsetScale The offset scale.
     * @return Rainbow (if you call it multiple times)
     */
    public static Color getChromaColor(double x, double y, double offsetScale) {
        float v = 2000.0f;
        return new Color(Color.HSBtoRGB((float)((System.currentTimeMillis() - x * 10.0 * offsetScale - y * 10.0 * offsetScale) % v) / v, 0.8f, 0.8f));
    }

    public static class AnsiColorUtils {
        /**
         * Enables ansi codes on windows.
         */
        public static void enableAnsiOnWindows() {
            Function GetStdHandleFunc = Function.getFunction("kernel32", "GetStdHandle");
            WinDef.DWORD STD_OUTPUT_HANDLE = new WinDef.DWORD(-11);
            WinNT.HANDLE hOut = (WinNT.HANDLE) GetStdHandleFunc.invoke(WinNT.HANDLE.class, new Object[]{STD_OUTPUT_HANDLE});

            WinDef.DWORDByReference p_dwMode = new WinDef.DWORDByReference(new WinDef.DWORD(0));
            Function GetConsoleModeFunc = Function.getFunction("kernel32", "GetConsoleMode");
            GetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, p_dwMode});

            int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 4;
            WinDef.DWORD dwMode = p_dwMode.getValue();
            dwMode.setValue(dwMode.intValue() | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
            Function SetConsoleModeFunc = Function.getFunction("kernel32", "SetConsoleMode");
            SetConsoleModeFunc.invoke(WinDef.BOOL.class, new Object[]{hOut, dwMode});
        }

        /**
         * Generates an ansi color for a given color.
         * @param color The color to generate the ansi color for.
         * @return The ansi color.
         */
        public static Attribute generateAnsiTextColorAttribute(Color color) {
            return Attribute.TEXT_COLOR(color.getRed(), color.getGreen(), color.getBlue());
        }

        /**
         * Generates an ansi color for a given color.
         * @param color The color to generate the ansi color for.
         * @param text The text to color.
         * @return The ansi color.
         */
        public static String generateAnsiTextColor(Color color, String text) {
            return Ansi.colorize(text, Attribute.TEXT_COLOR(color.getRed(), color.getGreen(), color.getBlue()));
        }

        /**
         * Replaces minecraft color codes with ansi codes.
         * @param mcText The text to color.
         * @return String that's colored with ansi codes.
         */
        public static String minecraftColorToAnsi(String mcText) {
            return Colors.colorize(mcText);
        }

        /**
         * Replaces minecraft color codes with ansi codes.
         * @param mcText The text to color.
         * @param legacy Set this to true if you're using a version of Minecraft that doesn't support RGB color codes.
         * @return String that's colored with ansi codes.
         */
        public static String minecraftColorToAnsi(String mcText, boolean legacy) {
            return legacy ? Colors.colorizeLegacy(mcText) : Colors.colorize(mcText);
        }

        /**
         * Check if the text contains color codes.
         * @param mcText The text to check for color codes.
         */
        public static boolean containsMinecraftColorCodes(String mcText) {
            return Pattern.compile("(?i)" + '\u00A7' + "[0-9A-FK-ORX]").matcher(mcText).results().findAny().isPresent();
        }

        private enum Colors {
            BLACK('\u00A7' + "0", Attribute.BLACK_TEXT()),
            DARK_GREEN('\u00A7' + "2", Attribute.GREEN_TEXT()),
            DARK_RED('\u00A7' + "4", Attribute.RED_TEXT()),
            GOLD('\u00A7' + "6", Attribute.YELLOW_TEXT()),
            DARK_GREY('\u00A7' + "8", Attribute.BRIGHT_BLACK_TEXT()),
            GREEN('\u00A7' + "a", Attribute.BRIGHT_GREEN_TEXT()),
            RED('\u00A7' + "c", Attribute.BRIGHT_RED_TEXT()),
            YELLOW('\u00A7' + "e", Attribute.BRIGHT_YELLOW_TEXT()),
            DARK_BLUE('\u00A7' + "1", Attribute.BLUE_TEXT()),
            DARK_AQUA('\u00A7' + "3", Attribute.CYAN_TEXT()),
            DARK_PURPLE('\u00A7' + "5", Attribute.MAGENTA_TEXT()),
            GREY('\u00A7' + "7", Attribute.BRIGHT_BLACK_TEXT()),
            BLUE('\u00A7' + "9", Attribute.BRIGHT_BLUE_TEXT()),
            AQUA('\u00A7' + "b", Attribute.BRIGHT_CYAN_TEXT()),
            LIGHT_PURPLE('\u00A7' + "d", Attribute.MAGENTA_TEXT()),
            WHITE('\u00A7' + "f", Attribute.WHITE_TEXT()),
            STRIKETHROUGH('\u00A7' + "m", Attribute.STRIKETHROUGH()),
            ITALIC('\u00A7' + "o", Attribute.ITALIC()),
            BOLD('\u00A7' + "l", Attribute.BOLD()),
            UNDERLINE('\u00A7' + "n", Attribute.UNDERLINE()),
            RESET('\u00A7' + "r", Attribute.CLEAR()),
            OBFUSCATE('\u00A7' + "k", Attribute.REVERSE()); // I don't know how to do this

            private final String colorCode;
            private final Attribute attribute;

            Colors(String colorCode, Attribute attribute) {
                this.colorCode = colorCode;
                this.attribute = attribute;
            }

            public String getColorCode() {
                return colorCode;
            }

            public Attribute getAttribute() {
                return attribute;
            }
            public static Colors getColor(String colorChar) {
                return switch (colorChar) {
                    case '\u00A7' + "0" -> Colors.BLACK;
                    case '\u00A7' + "1" -> Colors.DARK_BLUE;
                    case '\u00A7' + "2" -> Colors.DARK_GREEN;
                    case '\u00A7' + "3" -> Colors.DARK_AQUA;
                    case '\u00A7' + "4" -> Colors.DARK_RED;
                    case '\u00A7' + "5" -> Colors.DARK_PURPLE;
                    case '\u00A7' + "6" -> Colors.GOLD;
                    case '\u00A7' + "7" -> Colors.GREY;
                    case '\u00A7' + "8" -> Colors.DARK_GREY;
                    case '\u00A7' + "9" -> Colors.BLUE;
                    case '\u00A7' + "a" -> Colors.GREEN;
                    case '\u00A7' + "b" -> Colors.AQUA;
                    case '\u00A7' + "c" -> Colors.RED;
                    case '\u00A7' + "d" -> Colors.LIGHT_PURPLE;
                    case '\u00A7' + "e" -> Colors.YELLOW;
                    case '\u00A7' + "f" -> Colors.WHITE;
                    case '\u00A7' + "k" -> Colors.OBFUSCATE;
                    case '\u00A7' + "l" -> Colors.BOLD;
                    case '\u00A7' + "m" -> Colors.STRIKETHROUGH;
                    case '\u00A7' + "n" -> Colors.UNDERLINE;
                    case '\u00A7' + "o" -> Colors.ITALIC;
                    default -> Colors.RESET;
                };
            }

            public static String colorize(String mcText) {
                boolean tryAgain = false;
                try {
                    mcText = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build().serialize(MiniMessage.builder().build().deserialize(mcText));
                } catch (ParsingException e) {
                    tryAgain = true;
                }

                String[] magicCodes = Pattern.compile("(?i)" + '\u00A7' +"x[A-F0-9" + '\u00A7' +"]{12}").matcher(mcText).results().map(MatchResult::group).toArray(String[]::new);
                for (String magicCode : magicCodes) {
                    Color color = Color.decode("#" + magicCode.substring(2).replaceAll("\u00A7", ""));
                    mcText = mcText.replaceAll(Pattern.quote(magicCode), Matcher.quoteReplacement(Ansi.generateCode(Attribute.TEXT_COLOR(color.getRed(), color.getGreen(), color.getBlue()))));
                }

                String[] matches = Pattern.compile("(?i)" + '\u00A7' + "[0-9A-FK-ORX]").matcher(mcText).results().map(MatchResult::group).toArray(String[]::new);
                for (String match : matches) {
                    mcText = mcText.replaceAll(Pattern.quote(match), Matcher.quoteReplacement(Ansi.generateCode(Colors.getColor(match).getAttribute())));
                }
                return (tryAgain ? colorize(mcText) : mcText) + Ansi.generateCode(Attribute.CLEAR());
            }

            public static String colorizeLegacy(String mcText) {
                boolean tryAgain = false;
                try {
                    mcText = LegacyComponentSerializer.builder().build().serialize(MiniMessage.builder().build().deserialize(mcText));
                } catch (ParsingException e) {
                    tryAgain = true;
                }

                String[] magicCodes = Pattern.compile("(?i)" + '\u00A7' +"x[A-F0-9" + '\u00A7' +"]{12}").matcher(mcText).results().map(MatchResult::group).toArray(String[]::new);
                for (String magicCode : magicCodes) {
                    Color color = Color.decode("#" + magicCode.substring(2).replaceAll("\u00A7", ""));
                    mcText = mcText.replaceAll(Pattern.quote(magicCode), Matcher.quoteReplacement(Ansi.generateCode(Attribute.TEXT_COLOR(color.getRed(), color.getGreen(), color.getBlue()))));
                }

                String[] matches = Pattern.compile("(?i)" + '\u00A7' + "[0-9A-FK-ORX]").matcher(mcText).results().map(MatchResult::group).toArray(String[]::new);
                for (String match : matches) {
                    mcText = mcText.replaceAll(Pattern.quote(match), Matcher.quoteReplacement(Ansi.generateCode(Colors.getColor(match).getAttribute())));
                }
                return (tryAgain ? colorizeLegacy(mcText) : mcText) + Ansi.generateCode(Attribute.CLEAR());
            }
        }
    }
}
