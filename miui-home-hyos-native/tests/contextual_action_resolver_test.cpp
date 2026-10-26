#include "../dart_runtime_resolver.h"
#include <elf.h>
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <iterator>
#include <vector>

using miui_home_dart_profile::ContextualActionSite;
using miui_home_dart_profile::ResolveContextualActionSite;

static uint32_t word(const std::vector<uint8_t>& image, uintptr_t offset) {
    uint32_t value; std::memcpy(&value, image.data() + offset, 4); return value;
}
static void put(std::vector<uint8_t>& image, uintptr_t offset, uint32_t value) {
    std::memcpy(image.data() + offset, &value, 4);
}
static uintptr_t action_branch(uintptr_t address, uint32_t code) {
    int64_t delta = (code >> 5) & 0x3fff;
    if (delta & 0x2000) delta -= 0x4000;
    return address + delta * 4;
}
static void relocate_call(std::vector<uint8_t>& image, uintptr_t old, uintptr_t moved) {
    const auto instruction = word(image, old);
    assert((instruction & 0xfc000000) == 0x94000000);
    int64_t immediate = instruction & 0x03ffffff;
    if (immediate & 0x02000000) immediate -= 0x04000000;
    const int64_t delta = static_cast<int64_t>(old) + immediate * 4 - moved;
    assert((delta & 3) == 0 && delta >= -0x8000000 && delta < 0x8000000);
    put(image, moved, 0x94000000 | (static_cast<uint32_t>(delta / 4) & 0x03ffffff));
}

int main(int argc, char** argv) {
    assert(argc == 4); // Captured ELF, independently observed site and caller entry.
    std::ifstream input(argv[1], std::ios::binary);
    const std::vector<char> file((std::istreambuf_iterator<char>(input)), {});
    assert(file.size() >= sizeof(Elf64_Ehdr));
    Elf64_Ehdr header{}; std::memcpy(&header, file.data(), sizeof(header));
    std::vector<uint8_t> image(0x4000000);
    uintptr_t end = 0;
    for (uint16_t i = 0; i < header.e_phnum; ++i) {
        Elf64_Phdr p{};
        std::memcpy(&p, file.data() + header.e_phoff + i * header.e_phentsize, sizeof(p));
        if (p.p_type != PT_LOAD) continue;
        assert(p.p_vaddr + p.p_memsz <= image.size() && p.p_offset + p.p_filesz <= file.size());
        std::memcpy(image.data() + p.p_vaddr, file.data() + p.p_offset, p.p_filesz);
        if (p.p_vaddr + p.p_memsz > end) end = p.p_vaddr + p.p_memsz;
    }
    const uintptr_t expected = std::strtoull(argv[2], nullptr, 0);
    const uintptr_t entry = std::strtoull(argv[3], nullptr, 0);
    ContextualActionSite site{};
    assert(ResolveContextualActionSite(image.data(), &site) && site.offset == expected);
    assert(site.context_frame_offset < 0 && site.result_frame_offset < 0);
    assert(site.cts_pool_offset != 0 && site.context_frame_offset != site.result_frame_offset);
    const auto reject = [&] {
        assert(!ResolveContextualActionSite(image.data(), &site));
        assert(site.offset == 0 && site.cts_pool_offset == 0);
    };
    const auto context_load = word(image, expected - 28);
    put(image, expected - 28, context_load ^ 1); reject();
    put(image, expected - 28, context_load);
    const auto second = action_branch(expected + 40, word(image, expected + 40));
    const auto cts_call = word(image, second + 52);
    put(image, second + 52, cts_call ^ 1); reject();
    put(image, second + 52, cts_call);
    const auto pool_add = word(image, second + 12), pool_load = word(image, second + 16);
    put(image, second + 12, word(image, expected + 16));
    put(image, second + 16, word(image, expected + 20)); reject();
    put(image, second + 12, pool_add); put(image, second + 16, pool_load);
    assert(ResolveContextualActionSite(image.data(), &site));

    // A second valid caller must reject the whole optional extension. Copy
    // only the proven caller region and relocate its validated direct calls.
    const uintptr_t duplicate = (end + 0xffff) & ~uintptr_t{0xffff};
    const uintptr_t length = expected - entry + 0x200;
    assert(entry < expected && duplicate + length < image.size());
    std::memcpy(image.data() + duplicate, image.data() + entry, length);
    for (auto call : {entry + 88, expected - 16, expected - 4, second + 52, second + 64})
        relocate_call(image, call, duplicate + call - entry);
    Elf64_Phdr added{};
    added.p_type = PT_LOAD; added.p_flags = PF_R | PF_X;
    added.p_vaddr = duplicate; added.p_memsz = added.p_filesz = length;
    assert(header.e_phoff + (header.e_phnum + 1) * sizeof(added) < 0x1000);
    std::memcpy(image.data() + header.e_phoff + header.e_phnum * sizeof(added), &added, sizeof(added));
    ++header.e_phnum; std::memcpy(image.data(), &header, sizeof(header));
    reject();
    std::puts("Dart action graph, frame mismatch, CTS edge, pool alias and ambiguity checks passed.");
}
