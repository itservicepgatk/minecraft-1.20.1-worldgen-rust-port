# Minecraft 1.20.1 World Generation Package for Amalgam (Rust)

Welcome to the **Minecraft 1.20.1 World Generation Package**. This package provides everything necessary to port the full vanilla Minecraft Java Edition 1.20.1 overworld terrain generation into high-performance, zero-allocation Rust for the **Amalgam** server core using **Google AI Studio (Gemini 1.5 Pro)**.

---

## Package Directory Structure

```text
Minecraft_1.20.1_Worldgen_Package/
├── GEMINI_STUDIO_PROMPT.md   # Master prompt and phase-by-phase conversion pipeline
├── README.md                 # This manual and execution guide
├── sources/                  # Extracted Mojang 1.20.1 Java levelgen source files
│   ├── NoiseBasedChunkGenerator.java
│   ├── NoiseRouter.java
│   ├── DensityFunction.java
│   ├── DensityFunctions.java
│   ├── Aquifer.java
│   ├── SurfaceSystem.java
│   ├── SurfaceRules.java
│   ├── MultiNoiseBiomeSource.java
│   ├── Climate.java
│   ├── synth/
│   │   ├── ImprovedNoise.java
│   │   ├── PerlinNoise.java
│   │   ├── NormalNoise.java
│   │   └── BlendedNoise.java
│   └── ...
└── datapack/                 # Vanilla 1.20.1 JSON datapack files
    ├── worldgen/
    │   ├── density_function/ # All overworld density function JSON graphs
    │   ├── noise_settings/   # overworld.json with NoiseRouter & SurfaceRules
    │   ├── noise/            # NormalNoise octave parameter definitions
    │   └── biome/            # All overworld biome definitions
```

---

## Quick-Start Guide for Google AI Studio Playground

Google AI Studio allows leveraging Gemini 1.5 Pro's 2,000,000 token context window to digest the entire vanilla Java worldgen codebase and JSON datapacks in a single session.

### Step 1: Open Google AI Studio Playground
1. Navigate to [Google AI Studio](https://aistudio.google.com).
2. Click **Create new prompt** (Chat Prompt or Freeform Prompt).

### Step 2: Configure Model Parameters
In the right-hand settings panel:
- **Model**: Select `Gemini 1.5 Pro` (Recommended for complex code architecture and multi-file reasoning).
- **Temperature**: Set to `0.15` (Lower temperature enforces deterministic, precision-oriented Rust syntax and mathematical accuracy).
- **Top P**: Set to `0.95`.
- **Safety Settings**: Set all categories to `Block none` or lowest threshold (prevents false positives on source code analysis).

### Step 3: Set System Instructions
1. Open [GEMINI_STUDIO_PROMPT.md](GEMINI_STUDIO_PROMPT.md).
2. Copy the entire markdown block inside **Part 1: System Instruction**.
3. Paste it into the **System Instructions** box in Google AI Studio.

### Step 4: Upload / Attach Source Code & Datapacks
You can upload the contents of this package directly into Google AI Studio's context window:
- Click the **+** (Attach / Upload) button in AI Studio.
- Select files from the `sources/` and `datapack/` directories, or upload the zipped package `Minecraft_1.20.1_Worldgen_Package.zip`.
- Gemini 1.5 Pro will ingest the raw Mojang Java classes and JSON schema directly into its working memory.

### Step 5: Execute Phase-by-Phase Conversion
To avoid token truncation and ensure each crate compiles cleanly, execute the conversion following the 6 sequential phases defined in `GEMINI_STUDIO_PROMPT.md`:

| Phase | Target Crate / Module | Scope |
| :--- | :--- | :--- |
| **Phase 1** | `amalgam-worldgen-math` | `Xoroshiro128PlusPlus`, `ImprovedNoise`, `NormalNoise`, `BlendedNoise`, `CubicSpline` |
| **Phase 2** | `amalgam-worldgen-density` | DensityFunction AST, 22 vanilla density types, `NoiseRouter`, JSON parser |
| **Phase 3** | `amalgam-worldgen-biome` | 6D Climate sampler, KD-tree biome finder, `FuzzyVoronoi` zoomer |
| **Phase 4** | `amalgam-worldgen-terrain` | 3D Cell sampling ($4 \times 8 \times 4$), trilinear interpolation, `Aquifer` fluid solver |
| **Phase 5** | `amalgam-worldgen-surface` | Bedrock dithering ($Y \in [-64, -60]$), deepslate transition ($Y \in [-8, 0]$), surface rules |
| **Phase 6** | `amalgam-worldgen-baking` | PalettedContainer packing, heightmaps, sky light, Rayon parallel chunk baking |

---

## Key Architecture Guardrails for Amalgam

1. **Zero Inner-Loop Heap Allocations**:
   - Terrain generation evaluates millions of voxel densities per chunk.
   - All cell evaluation buffers (`[f64; 1225]`) and block section buffers (`[u16; 4096]`) must be pre-allocated or stack-allocated.
2. **Height Boundaries**:
   - $Y_{\text{min}} = -64$, $Y_{\text{max}} = 319$ (Height = 384 blocks, 24 sections of 16 blocks).
3. **Protocol 763 Chunk Serialization**:
   - Sections are directly converted into `amalgam_protocol::packets::ChunkDataPacket` (Packet ID 0x24).
   - Biome paletted containers and block state paletted containers are packed with zero memory leaks.

---

## Verification & Testing in Amalgam

Once you copy the generated Rust code into Amalgam:

1. **Workspace Compilation**:
   ```powershell
   cargo check --workspace --all-targets
   ```
2. **Unit Tests**:
   ```powershell
   cargo test -p amalgam-worldgen-math
   cargo test -p amalgam-worldgen-density
   ```
3. **Benchmark**:
   ```powershell
   cargo bench -p amalgam-worldgen-baking
   ```
4. **Live Server Verification**:
   - Run Amalgam server:
     ```powershell
     cargo run --release -p amalgam-server
     ```
   - Connect client at `127.0.0.1:25565` and verify terrain relief, caves, oceans, and seamless chunk streaming.
