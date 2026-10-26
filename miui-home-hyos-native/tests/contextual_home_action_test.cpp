#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstring>

extern "C" {
uint32_t g_contextual_home_preference = 1, g_contextual_home_action_ready = 1;
uint32_t g_contextual_home_action_retiring = 0, g_contextual_home_action_active_count = 0;
uint32_t g_contextual_home_action_count = 0;
uint64_t g_contextual_home_action_owner_epoch = 7, g_dart_state_owner_epoch = 7;
intptr_t g_contextual_home_context_frame_offset = -144;
intptr_t g_contextual_home_result_frame_offset = -152;
uintptr_t g_contextual_home_cts_pool_offset = 128, g_contextual_home_action_resume = 0;
void TestDartAction(uintptr_t, uintptr_t, uintptr_t, uintptr_t, uintptr_t*);
void TestDartActionResume();
}

int main() {
    alignas(16) uint8_t source[64]{}, original[64]{}, cts[64]{}, context[64]{};
    alignas(16) uint8_t frame[512]{}, pool[256]{};
    auto string = [](uint8_t* bytes, const char* text) {
        const uint64_t tag = 94u << 12u;
        const uint32_t length = static_cast<uint32_t>(std::strlen(text)) * 2u;
        std::memcpy(bytes, &tag, 8); std::memcpy(bytes + 8, &length, 4);
        std::memcpy(bytes + 16, text, std::strlen(text));
        return reinterpret_cast<uintptr_t>(bytes) + 1;
    };
    const auto source_ptr = string(source, "NavLongPress");
    const auto original_ptr = string(original, "launch_voice_assistant");
    const auto cts_ptr = string(cts, "circle_to_search");
    const auto context_ptr = reinterpret_cast<uintptr_t>(context) + 1;
    const auto fp = reinterpret_cast<uintptr_t>(frame + 256);
    const uint32_t compressed_source = static_cast<uint32_t>(source_ptr);
    std::memcpy(context + 16, &compressed_source, 4);
    std::memcpy(frame + 256 - 144, &context_ptr, 8);
    std::memcpy(pool + 128, &cts_ptr, 8);
    g_contextual_home_action_resume = reinterpret_cast<uintptr_t>(TestDartActionResume);
    auto run = [&](bool changed) {
        uintptr_t result[9] = {original_ptr};
        const auto before = g_contextual_home_action_count;
        TestDartAction(fp, source_ptr >> 32, reinterpret_cast<uintptr_t>(pool), 1, result);
        assert(result[0] == 94 && result[1] == (changed ? cts_ptr : original_ptr));
        uintptr_t stored = 0; std::memcpy(&stored, frame + 256 - 152, 8);
        assert(stored == result[1]);
        assert(result[2] == 0x88 && result[3] == 0x1212 && result[4] == 0x1313);
        assert(result[5] == 0x1414 && result[6] == 0x1515 && result[7] == 0x1717);
        assert(result[8] == 0x60000000 && g_contextual_home_action_active_count == 0);
        assert(g_contextual_home_action_count == before + (changed ? 1u : 0u));
    };
    run(true);
    g_contextual_home_preference = 0; run(false); g_contextual_home_preference = 1;
    g_contextual_home_action_ready = 0; run(false); g_contextual_home_action_ready = 1;
    g_contextual_home_action_retiring = 1; run(false); g_contextual_home_action_retiring = 0;
    ++g_dart_state_owner_epoch; run(false); --g_dart_state_owner_epoch;
    source[16] = 'X'; run(false); source[16] = 'N';
    source[27] = 'X'; run(false); source[27] = 's';
    source[8] = 20; run(false); source[8] = 24;
    source[1] ^= 0x10; run(false); source[1] ^= 0x10;
    cts[1] ^= 0x10; run(false); cts[1] ^= 0x10;
    run(true);
    std::puts("Home action source, preference, owner, retirement, frame and register checks passed.");
}
