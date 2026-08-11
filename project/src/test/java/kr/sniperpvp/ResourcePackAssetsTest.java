package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ResourcePackAssetsTest {
    private static final Path PACK_ROOT = Path.of("resource-pack");
    private static final Map<Integer, String> KILL_SOUND_SHA256 = Map.of(
        1, "c4f0f1b47a27909c30abcb3a043c630a888d6fdd48ac83394d6f47d8c22b9459",
        2, "8213c98b9f27339bcb5781aa69e12c3f896459461c55899a6c5f622c20fbab43",
        3, "a0f324715bbd04e12b9b54aa24c7da314616bc099019df46fc3bed93cb6040dc",
        4, "5a3714eb8482842723ab2c821142b894aa151b5d23b2dfd6160c5e450a0da8ed",
        5, "2d21951b21e6dd4438b7db0ab6fce0072b8418b1f35c88fd08bf31168b83cbe9"
    );

    @Test
    void metadataTargetsMinecraftOneTwentyOneEight() throws IOException {
        String metadata = Files.readString(PACK_ROOT.resolve("pack.mcmeta"));
        assertTrue(metadata.contains("\"pack_format\": 64"));
        assertTrue(metadata.contains("\"supported_formats\": [64, 64]"));
    }

    @Test
    void allFiveKillEventsAndVorbisFilesExist() throws IOException {
        String sounds = Files.readString(PACK_ROOT.resolve("assets/sniperpvp/sounds.json"));
        for (int tier = 1; tier <= 5; tier++) {
            assertTrue(sounds.contains("\"kill." + tier + "\""));
            Path audio = PACK_ROOT.resolve("assets/sniperpvp/sounds/kill/" + tier + ".ogg");
            assertTrue(Files.size(audio) > 10_000L, audio + " should contain converted audio");
            byte[] header = Files.readAllBytes(audio);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, new byte[]{
                header[0], header[1], header[2], header[3]
            });
            assertEquals(KILL_SOUND_SHA256.get(tier), sha256(audio));
        }
    }

    @Test
    void suppliedRifleModelAndItemDefinitionAreEmbedded() throws IOException {
        Path item = PACK_ROOT.resolve("assets/jm/items/walnut_longline_mk2.json");
        Path model = PACK_ROOT.resolve("assets/jm/models/item/walnut_longline_mk2.json");
        Path texture = PACK_ROOT.resolve("assets/jm/textures/item/walnut_longline_mk2_palette.png");
        assertTrue(Files.readString(item).contains("jm:item/walnut_longline_mk2"));
        assertTrue(Files.size(model) > 500_000L);
        assertTrue(Files.size(texture) > 100L);
        String modelJson = Files.readString(model).replace("\r\n", "\n");
        assertEquals(2, occurrences(modelJson, "\"scale\": [\n        0.46,\n        0.46,\n        0.46"));
        assertEquals(2, occurrences(modelJson, "\"scale\": [\n        0.39,\n        0.39,\n        0.39"));
    }

    @Test
    void valorantHudScopeAndTransparentBossBarAssetsExist() throws IOException {
        String font = Files.readString(PACK_ROOT.resolve("assets/sniperpvp/font/hud.json"));
        assertTrue(font.contains("hud_card_left.png"));
        assertTrue(font.contains("hud_card_right.png"));
        assertTrue(font.contains("hud_timer.png"));
        assertTrue(font.contains("hud_kill_banner.png"));
        assertTrue(font.contains("hud_rifle_icon.png"));

        var scope = ImageIO.read(PACK_ROOT.resolve(
            "assets/minecraft/textures/misc/spyglass_scope.png"
        ).toFile());
        assertEquals(512, scope.getWidth());
        assertEquals(288, scope.getHeight());
        assertEquals(255, scope.getRGB(0, 0) >>> 24);
        assertEquals(0, scope.getRGB(256, 144) >>> 24);
        for (int y = 132; y <= 156; y++) {
            for (int x = 244; x <= 268; x++) {
                assertEquals(0, scope.getRGB(x, y) >>> 24, "scope must not draw a custom crosshair");
            }
        }

        var customScope = ImageIO.read(PACK_ROOT.resolve(
            "assets/sniperpvp/textures/misc/scope.png"
        ).toFile());
        assertEquals(512, customScope.getWidth());
        assertEquals(288, customScope.getHeight());
        assertEquals(0, customScope.getRGB(256, 144) >>> 24);

        String healthFont = Files.readString(PACK_ROOT.resolve(
            "assets/sniperpvp/font/bottom_health.json"
        ));
        assertTrue(healthFont.contains("bottom_health_bars.png"));
        assertTrue(healthFont.contains("health_digits.png"));
        var healthBars = ImageIO.read(PACK_ROOT.resolve(
            "assets/sniperpvp/textures/font/bottom_health_bars.png"
        ).toFile());
        var healthDigits = ImageIO.read(PACK_ROOT.resolve(
            "assets/sniperpvp/textures/font/health_digits.png"
        ).toFile());
        assertEquals(880, healthBars.getWidth());
        assertEquals(9, healthBars.getHeight());
        assertEquals(264, healthDigits.getWidth());
        assertEquals(32, healthDigits.getHeight());

        String textShader = Files.readString(PACK_ROOT.resolve(
            "assets/minecraft/shaders/core/rendertype_text.vsh"
        ));
        assertTrue(textShader.contains("#version 150"));
        assertTrue(textShader.contains("BOTTOM_HEALTH_SHADER_ID 2"));
        assertTrue(textShader.contains("pos.y += ui.y - 40.0"));

        var hiddenBar = ImageIO.read(PACK_ROOT.resolve(
            "assets/minecraft/textures/gui/sprites/boss_bar/white_background.png"
        ).toFile());
        assertEquals(0, hiddenBar.getRGB(0, 0) >>> 24);

        Path vanillaHud = PACK_ROOT.resolve("assets/minecraft/textures/gui/sprites/hud");
        try (var sprites = Files.walk(vanillaHud)) {
            var spriteFiles = sprites.filter(path -> path.toString().endsWith(".png")).toList();
            assertTrue(spriteFiles.size() >= 56, "all vanilla health, armor and hunger sprites must be hidden");
            for (Path sprite : spriteFiles) {
                var image = ImageIO.read(sprite.toFile());
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        assertEquals(0, image.getRGB(x, y) >>> 24, sprite + " must be transparent");
                    }
                }
            }
        }
    }

    @Test
    void rifleAndPrivateMatchResultSoundsAreVorbis() throws IOException {
        String pluginSounds = Files.readString(PACK_ROOT.resolve("assets/sniperpvp/sounds.json"));
        assertTrue(pluginSounds.contains("rifle.fire"));
        assertTrue(pluginSounds.contains("rifle.bolt"));
        assertTrue(pluginSounds.contains("rifle.reload"));
        assertTrue(pluginSounds.contains("match.victory"));
        assertTrue(pluginSounds.contains("match.defeat"));

        for (String relative : java.util.List.of(
            "assets/sniperpvp/sounds/rifle/fire.ogg",
            "assets/sniperpvp/sounds/rifle/bolt.ogg",
            "assets/sniperpvp/sounds/rifle/reload.ogg",
            "assets/sniperpvp/sounds/match/victory.ogg",
            "assets/sniperpvp/sounds/match/defeat.ogg"
        )) {
            byte[] bytes = Files.readAllBytes(PACK_ROOT.resolve(relative));
            assertTrue(bytes.length > 1_000, relative);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, new byte[]{
                bytes[0], bytes[1], bytes[2], bytes[3]
            });
        }
        assertEquals(
            "9e4ae90074f299f666bda54bacbdedea959f45b841ce588ec6b225c8cb5d9c2b",
            sha256(PACK_ROOT.resolve("assets/sniperpvp/sounds/rifle/bolt.ogg"))
        );
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int occurrences(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }
}
