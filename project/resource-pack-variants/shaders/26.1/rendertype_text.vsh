#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

#define HEIGHT_BIT 13
#define MAX_BIT 10
#define ADD_OFFSET 4095
#define DEFAULT_OFFSET 10
#define BOTTOM_HEALTH_SHADER_ID 2

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vec3 pos = Position;
    vec2 ui = ceil(2.0 / vec2(ProjMat[0][0], -ProjMat[1][1]));
    vertexColor = Color * sample_lightmap(Sampler2, UV2);

    if (pos.y >= ui.y && ProjMat[3].x == -1.0) {
        int bit = int(pos.y) >> HEIGHT_BIT;
        if (((bit >> MAX_BIT) & 1) == 1) {
            int id = bit - (1 << MAX_BIT);
            pos.x -= 0.5 * ui.x;
            pos.y -= float((bit << HEIGHT_BIT) + ADD_OFFSET + DEFAULT_OFFSET);

            switch (id) {
                case BOTTOM_HEALTH_SHADER_ID:
                    pos.x += 0.5 * ui.x - 50.0;
                    pos.y += ui.y - 40.0;
                    break;
                default:
                    break;
            }
        }
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    texCoord0 = UV0;
}
