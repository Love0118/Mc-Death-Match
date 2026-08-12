package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    private static final Path VARIANT_ROOT = Path.of("resource-pack-variants");
    private static final Map<Integer, String> KILL_SOUND_SHA256 = Map.of(
        1, "50de4010f88be0a3011d60af67f29cec9acb6cd43a258fcd5150dc3cb135fca8",
        2, "a40e268608cfdb06c94235771a86ff19b0a1eaf344cfca6fb181e499da649bc0",
        3, "df6804d4b1f867bb5b6068325be92bb5729ff62c99f653e51ed1eb9529bb54fe",
        4, "bf623ad43270fce046e616d259e0149f5896a08d3bff7c32e03dcaf5312cf89d",
        5, "dc32c97c143597d8b9cec7c164f9e3ada9d725f4838e58a686fae5c13243888a"
    );

    @Test
    void versionMatrixCoversEverySupportedProtocolAndPackFormat() throws IOException {
        String matrix = Files.readString(VARIANT_ROOT.resolve("matrix.json"));
        for (String version : java.util.List.of(
            "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2"
        )) {
            assertTrue(matrix.contains("\"" + version + "\""), version);
        }
        for (int protocol = 772; protocol <= 776; protocol++) {
            assertEquals(1, occurrences(matrix, "\"protocol\": " + protocol));
        }
        for (int format : java.util.List.of(64, 69, 75, 84, 88)) {
            assertEquals(1, occurrences(matrix, "\"pack_format\": [" + format + ", 0]"));
        }
        assertTrue(matrix.contains("sniper-pvp-1.21.9-1.21.10.zip"));
        assertTrue(matrix.contains("sniper-pvp-26.1.x.zip"));
    }

    @Test
    void allFiveKillEventsAndVorbisFilesExist() throws IOException {
        String sounds = Files.readString(PACK_ROOT.resolve("assets/sniperpvp/sounds.json"));
        assertEquals(5, occurrences(sounds, "\"preload\": true"));
        for (int tier = 1; tier <= 5; tier++) {
            assertTrue(sounds.contains("\"kill." + tier + "\""));
            Path audio = PACK_ROOT.resolve("assets/sniperpvp/sounds/kill/" + tier + ".ogg");
            assertTrue(Files.size(audio) > 10_000L, audio + " should contain converted audio");
            byte[] header = Files.readAllBytes(audio);
            assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, new byte[]{
                header[0], header[1], header[2], header[3]
            });
            int identification = indexOf(header, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'});
            assertTrue(identification >= 0, audio + " must contain a Vorbis identification packet");
            assertEquals(1, Byte.toUnsignedInt(header[identification + 11]), audio + " must be mono");
            int sampleRate = ByteBuffer.wrap(header, identification + 12, Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
            assertEquals(48_000, sampleRate, audio + " must use a 48 kHz sample rate");
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
        String thirdPersonRight = modelJson.substring(
            modelJson.indexOf("\"thirdperson_righthand\""),
            modelJson.indexOf("\"thirdperson_lefthand\"")
        );
        String thirdPersonLeft = modelJson.substring(
            modelJson.indexOf("\"thirdperson_lefthand\""),
            modelJson.indexOf("\"firstperson_righthand\"")
        );
        assertTrue(thirdPersonRight.contains("\"rotation\": [\n        0,\n        90,\n        0"));
        assertTrue(thirdPersonLeft.contains("\"rotation\": [\n        0,\n        -90,\n        0"));
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

        for (String relative : java.util.List.of(
            "shaders/1.21.8/rendertype_text.vsh",
            "shaders/1.21.9/rendertype_text.vsh",
            "shaders/26.1/rendertype_text.vsh",
            "shaders/26.2/text.vsh"
        )) {
            String textShader = Files.readString(VARIANT_ROOT.resolve(relative));
            assertTrue(textShader.contains("BOTTOM_HEALTH_SHADER_ID 2"), relative);
            assertTrue(textShader.contains("pos.y += ui.y - 40.0"), relative);
        }
        String legacyTextShader = Files.readString(VARIANT_ROOT.resolve(
            "shaders/1.21.8/rendertype_text.vsh"
        ));
        assertTrue(legacyTextShader.contains("#version 150"));
        String twentySixTwoTextShader = Files.readString(VARIANT_ROOT.resolve(
            "shaders/26.2/text.vsh"
        ));
        assertTrue(twentySixTwoTextShader.contains("#ifdef IS_GUI"));
        assertTrue(twentySixTwoTextShader.contains("sample_lightmap(Sampler2, UV2)"));

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

    private static int indexOf(byte[] value, byte[] needle) {
        outer:
        for (int index = 0; index <= value.length - needle.length; index++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (value[index + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return index;
        }
        return -1;
    }
}
