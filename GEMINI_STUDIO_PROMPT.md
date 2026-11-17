# Google AI Studio (Gemini 1.5 Pro) Master Prompt: Minecraft 1.20.1 World Generation in Rust for Amalgam

This document contains the complete, authoritative prompt package for porting the vanilla Minecraft Java Edition 1.20.1 terrain generation pipeline into high-performance, zero-allocation Rust for the **Amalgam** server core.

---

## Part 1: System Instruction (Set in Google AI Studio "System Instructions")

Copy and paste the following block into the **System Instructions** box in Google AI Studio:

```markdown
You are a Principal Systems Engineer, 3D Mathematics Specialist, and Core Rust Architect specializing in extreme-performance game server architecture.
You are tasked with porting the vanilla Minecraft Java Edition 1.20.1 world generation pipeline (Overworld noise settings, noise router, density functions, aquifers, biomes, and surface rules) into idiomatic, zero-allocation, multi-threaded Rust for the "Amalgam" server core.

### CORE ARCHITECTURE PRINCIPLES:
1. **Zero Heap Allocation in Inner Loops**:
   - The terrain generation hot path samples millions of density points per chunk.
   - NEVER allocate (`Box`, `Vec`, `String`, `Rc`, `Arc`) inside per-block or per-cell evaluation functions.
   - Use stack arrays, static lookup tables, scratch arena buffers, or compile-time monomorphized structs.
   - All scratch buffers (cell grids, density buffers, chunk sections) must be preallocated and recycled via thread-local or object-pool patterns.

2. **Bit-Exact Deterministic Noise Math**:
   - All pseudo-random number generation must bit-for-bit replicate Mojang's `Xoroshiro128PlusPlus` (and legacy LCG where applicable) and SHA-256 seed hashing.
   - Implement Mojang's `ImprovedNoise` (Perlin noise with 512-permutation table, quintic fade $6t^5 - 15t^4 + 10t^3$, and gradient dot products).
   - Implement `BlendedNoise` (octave sampling with upper/lower limits and main blending noise).
   - Implement `CubicSpline` Hermite spline evaluation matching vanilla interpolation.

3. **Coordinate & World Geometry Constraints**:
   - Vertical coordinate range: $Y \in [-64, 319]$ (Total height = 384 blocks, exactly 24 chunk sections of $16 \times 16 \times 16$).
   - Horizontal chunk size: $16 \times 16$ blocks.
   - Density sampling grid: Horizontal cell size = 4 blocks ($4 \times 4$ cells per chunk), vertical cell size = 8 blocks (48 vertical cells). Total cell sampling corners per chunk = $5 \times 49 \times 5 = 1,225$ vertices.
   - Density interpolation: Trilinear interpolation between cell corners to evaluate density at all $16 \times 384 \times 16 = 98,304$ voxel positions per chunk.

4. **Amalgam Protocol & Chunk Format (Protocol 763 / 1.20.1)**:
   - Chunk packets must adhere to `amalgam_protocol::ChunkDataPacket` (Packet ID 0x24).
   - 24 Chunk Sections:
     * `non_air_count: i16`
     * Block States: PalettedContainer (0-bit single block, 4 to 8-bit indirect palette, or 15-bit direct palette). Packed into `u64` bit arrays.
     * Biomes: PalettedContainer (usually 0-bit single biome or 1-3 bit indirect).
   - Heightmaps: Packed into 37 `u64` longs (9 bits per entry, 7 entries per long) for `MOTION_BLOCKING` and `WORLD_SURFACE`.
   - Zero-leak buffer lifecycle: Sections encoded into pre-sized `bytes::BytesMut`.

5. **Code Quality & Rust Standards**:
   - Rust 2021 edition.
   - Strict `#![forbid(unsafe_code)]` unless SIMD vectorization is explicitly isolated and proven safe.
   - Comprehensive documentation (`///`) on every struct and method.
   - Zero `unwrap()` or `expect()` in library paths; return typed `Result<T, WorldGenError>`.
   - Include standalone unit tests with known vanilla seeds and test vectors for every component.
```

---

## Part 2: Architecture Deep-Dive & Rust Translation Guide

### 1. Translating Mojang's `DensityFunction` Graph to Zero-Allocation Rust

In Java Minecraft, `DensityFunction` is an interface with dozens of recursive implementations (`Add`, `Mul`, `Min`, `Max`, `Clamp`, `Noise`, `Spline`, `WeirdScaledSampler`, etc.). Calling `compute(FunctionContext)` in Java results in polymorphic virtual dispatch and megamorphic call sites that thrash CPU branch predictors.

In Rust for Amalgam, we eliminate virtual dispatch using one of two high-performance models:

#### Model A: Flat Enum Abstract Syntax Tree (AST) with Stack Evaluator
Represent density functions as an arena-allocated or flat enum tree:
```rust
#[derive(Clone, Debug)]
pub enum DensityFunctionNode {
    Constant(f64),
    YClampedGradient { from_y: i32, to_y: i32, from_value: f64, to_value: f64 },
    Add(DensityId, DensityId),
    Mul(DensityId, DensityId),
    Min(DensityId, DensityId),
    Max(DensityId, DensityId),
    Clamp { input: DensityId, min: f64, max: f64 },
    Abs(DensityId),
    Square(DensityId),
    Cube(DensityId),
    HalfNegative(DensityId),
    QuarterNegative(DensityId),
    Squeeze(DensityId),
    Noise { noise_id: NoiseId, xz_scale: f64, y_scale: f64 },
    ShiftA(NoiseId),
    ShiftB(NoiseId),
    Shift(NoiseId),
    RangeChoice { input: DensityId, min_inclusive: f64, max_exclusive: f64, when_in_range: DensityId, when_out_of_range: DensityId },
    Spline(CubicSpline<DensityId>),
    WeirdScaledSampler { input: DensityId, noise_id: NoiseId, rarity: WeirdRarity },
    BlendAlpha,
    BlendOffset,
    CacheOnce(DensityId),
    FlatCache(DensityId),
    Cache2D(DensityId),
}
```

#### Model B: Monomorphized Pipeline / Scratch Context
During chunk generation, evaluation is executed in batch across a cell column or slice:
```rust
pub struct EvaluationContext<'a> {
    pub block_x: i32,
    pub block_y: i32,
    pub block_z: i32,
    pub noise_registry: &'a NoiseRegistry,
    pub cache: &'a mut [f64], // Pre-allocated scratch cache per node ID
}
```
Batch evaluation samples 4x8x4 cell vertices, storing results in a continuous flat buffer `[f64; 1225]`.

---

### 2. Evaluating `BlendedNoise`, `PerlinNoise`, and `CubicSpline` in Rust

#### A. Bit-Exact `ImprovedNoise` (Perlin)
Vanilla Minecraft uses Ken Perlin's improved noise with:
- Permutation table of 512 entries initialized from `Xoroshiro128PlusPlus`.
- Quintic fade: $S(t) = t^3 (t (t \times 6.0 - 15.0) + 10.0)$.
- Gradient table indexed by `perm & 15`.

```rust
#[inline(always)]
pub fn fade(t: f64) -> f64 {
    t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
}

#[inline(always)]
pub fn grad(hash: i32, x: f64, y: f64, z: f64) -> f64 {
    let h = hash & 15;
    let u = if h < 8 { x } else { y };
    let v = if h < 4 { y } else if h == 12 || h == 14 { x } else { z };
    (if (h & 1) == 0 { u } else { -u }) + (if (h & 2) == 0 { v } else { -v })
}
```

#### B. `BlendedNoise` (Octave Blending)
`BlendedNoise` samples three octave noise sets (lower, upper, main):
- `main_noise`: Determines blend factor $\alpha \in [0, 1]$ using clamped normalized output.
- If $\alpha \le 0.0$: returns `lower_noise` sample.
- If $\alpha \ge 1.0$: returns `upper_noise` sample.
- Otherwise: linear interpolation $\text{lerp}(\alpha, \text{lower}, \text{upper})$.

#### C. `CubicSpline` Evaluation
`CubicSpline` in 1.20.1 represents continuous piecewise curves mapping parameters (continentalness, erosion, ridges, depth) to target values:
- `locations: Box<[f32]>`
- `values: Box<[CubicSpline]>` or constant numbers
- `derivatives: Box<[f32]>`

Evaluation:
1. Binary search `locations.partition_point(|&loc| loc < input)`.
2. Compute Hermite cubic interpolation between interval $[i - 1, i]$:
   $$t = \frac{\text{input} - x_{i-1}}{x_i - x_{i-1}}$$
   $$h_{00}(t) = 2t^3 - 3t^2 + 1, \quad h_{10}(t) = t^3 - 2t^2 + t$$
   $$h_{01}(t) = -2t^3 + 3t^2, \quad h_{11}(t) = t^3 - t^2$$
   $$\text{value} = h_{00}(t) y_{i-1} + h_{10}(t) \Delta x \cdot d_{i-1} + h_{01}(t) y_i + h_{11}(t) \Delta x \cdot d_i$$

---

### 3. Populating Chunk Sections and Palettes Efficiently Without Leaks

Each chunk spans $Y \in [-64, 319]$ (24 sections). Each section contains $16 \times 16 \times 16 = 4096$ block states:

1. **Pre-allocated Column Scratchpad**:
   Instead of allocating per section, allocate a single `[u16; 4096]` block buffer on the thread stack.
2. **Palette Selection**:
   - Single-valued (all air, all stone): `bits_per_entry = 0`, palette length = 1, data array length = 0.
   - Indirect palette ($N \le 16$ unique blocks): `bits_per_entry = 4`, palette contains list of global block IDs, data array packed into 256 `u64` longs ($4096 \times 4 / 64 = 256$).
   - Indirect palette ($17 \le N \le 256$ unique blocks): `bits_per_entry = 8`, 512 `u64` longs.
   - Direct palette ($N > 256$): `bits_per_entry = 15`, direct global block IDs.
3. **Heightmap Updates**:
   Update `MOTION_BLOCKING` (highest non-air, non-fluid block + 1) and `WORLD_SURFACE` (highest non-air block + 1) in a single pass into a fixed `[u64; 37]` array per chunk.
4. **Light Bitsets**:
   Compute sky light masks and propagate ambient sky light arrays without allocating dynamic vectors.

---

## Part 3: Phase-by-Phase AI Generation Pipeline (Overnight Execution)

Execute these prompts in sequence in Google AI Studio. Each phase builds upon the previous phase without breaking changes.

---

### Phase 1: Math, PRNG, and Noise Primitives (`amalgam-worldgen-math`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 1: PRNG AND NOISE PRIMITIVES

Context: You are building crate `amalgam-worldgen-math` for the Amalgam Minecraft 1.20.1 server.
Reference sources:
- `net.minecraft.world.level.levelgen.synth.NormalNoise`
- `net.minecraft.world.level.levelgen.synth.BlendedNoise`
- `net.minecraft.world.level.levelgen.synth.ImprovedNoise`
- `net.minecraft.world.level.levelgen.synth.PerlinNoise`
- `net.minecraft.util.CubicSpline`
- `net.minecraft.world.level.levelgen.XoroshiroRandomSource`

Requirements:
1. Implement `Xoroshiro128PlusPlus`:
   - Exact 128-bit state `(s0, s1)` with rotl(s0 + s1, 17) + s0.
   - Forking, next_long, next_int, next_double, and seed hashing from 64-bit seed using MD5/SHA-256 matching vanilla Mojang `RandomSupport`.
2. Implement `ImprovedNoise`:
   - 512-entry permutation table initialized from Xoroshiro.
   - `sample(x, y, z, y_amp, min_y)` with quintic fade and gradient table.
3. Implement `PerlinNoise` and `NormalNoise`:
   - Multi-octave Perlin noise with first_octave and amplitude list.
   - `NormalNoise` value normalization factor ($1 / (\frac{1}{6} + \frac{1}{3} \times \text{sum_amplitudes})$).
4. Implement `BlendedNoise`:
   - Lower, upper, and main octave samplers with xz_scale, y_scale, xz_factor, y_factor.
5. Implement `CubicSpline<C>`:
   - Piecewise cubic Hermite spline with generic coordinate extractor.
   - Efficient binary search and Hermite interpolation.
6. Provide comprehensive unit tests verifying deterministic outputs against standard seed 0.

Output complete, compilable Rust code in separate module blocks (`prng.rs`, `improved_noise.rs`, `normal_noise.rs`, `blended_noise.rs`, `spline.rs`, `lib.rs`). Do not use placeholders or TODOs.
```

---

### Phase 2: Density Function Compiler & Noise Router (`amalgam-worldgen-density`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 2: DENSITY FUNCTION ENGINE & NOISE ROUTER

Context: You are building crate `amalgam-worldgen-density` for Amalgam.
Reference sources:
- `net.minecraft.world.level.levelgen.DensityFunction`
- `net.minecraft.world.level.levelgen.DensityFunctions`
- `net.minecraft.world.level.levelgen.NoiseRouter`
- `net.minecraft.world.level.levelgen.NoiseSettings`
- Datapack JSON: `data/minecraft/worldgen/density_function/*.json` and `data/minecraft/worldgen/noise_settings/overworld.json`

Requirements:
1. Define the DensityFunction AST and evaluation engine:
   - Zero heap allocation during `evaluate(ctx)`: use a flat arena or enum graph with stack memory.
   - Implement all 22 vanilla density function types: Constant, YClampedGradient, Add, Mul, Min, Max, Clamp, Abs, Square, Cube, HalfNegative, QuarterNegative, Squeeze, Noise, ShiftA, ShiftB, Shift, RangeChoice, Spline, WeirdScaledSampler, BlendAlpha, BlendOffset.
2. Implement caching contexts:
   - `CacheOnce`, `FlatCache`, `Cache2D` to avoid redundant evaluations across the 4x8x4 cell grid.
3. Implement `NoiseRouter`:
   - Struct containing the 15 primary density functions:
     * `barrier`
     * `fluid_level_floodedness`
     * `fluid_level_spread`
     * `continents`
     * `erosion`
     * `depth`
     * `ridges`
     * `initial_density_without_jaggedness`
     * `final_density`
     * `vein_toggle`
     * `vein_ridged`
     * `vein_gap`
4. Implement JSON Datapack Parser:
   - Parse `worldgen/density_function/*.json` into the Rust density function graph.
   - Resolve cyclic/nested references by name.
5. Provide unit tests evaluating standard density values at $(x=0, y=64, z=0)$ with Overworld settings.

Output complete, compilable Rust code (`ast.rs`, `eval.rs`, `cache.rs`, `router.rs`, `json_loader.rs`, `lib.rs`).
```

---

### Phase 3: Climate & Multi-Noise Biome Source (`amalgam-worldgen-biome`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 3: MULTI-NOISE BIOME SOURCE & CLIMATE SAMPLER

Context: You are building crate `amalgam-worldgen-biome` for Amalgam.
Reference sources:
- `net.minecraft.world.level.biome.MultiNoiseBiomeSource`
- `net.minecraft.world.level.biome.Climate`
- `net.minecraft.world.level.biome.Biome`
- Datapack JSON: `data/minecraft/worldgen/biome/*.json` and overworld biome presets.

Requirements:
1. Climate Point & Parameters:
   - 6D climate coordinates: `temperature`, `humidity`, `continentalness`, `erosion`, `depth`, `weirdness` (each scaled as fixed-point or `f32`/`f64`).
   - `ClimateTargetKey`: target values and min/max ranges for each parameter.
2. Biome Search Tree / Index:
   - Fast nearest-neighbor search for the vanilla overworld biomes (~60 biomes).
   - Weighted Euclidean distance:
     $$d^2 = \Delta T^2 + \Delta H^2 + \Delta C^2 + \Delta E^2 + \Delta D^2 + \Delta W^2 + \text{offset}$$
   - Implement spatial partitioning (e.g., R-tree, KD-tree, or quantized table) to perform biome lookups in $< 15$ nanoseconds.
3. Quart Coordinate & Voronoi Zoomer:
   - Biomes are sampled at quart resolution $(x/4, y/4, z/4)$.
   - Implement Mojang's `FuzzyVoronoi` zoomer to jitter and blend biome boundaries smoothly at block level.
4. Unit tests:
   - Verify that coordinate $(0, 64, 0)$ correctly resolves to its vanilla 1.20.1 biome given standard overworld parameters.

Output complete, compilable Rust code (`climate.rs`, `search_tree.rs`, `voronoi.rs`, `biome_registry.rs`, `lib.rs`).
```

---

### Phase 4: Aquifers and 3D Cell Sampler (`amalgam-worldgen-terrain`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 4: 3D CELL SAMPLING, TRILINEAR INTERPOLATION & AQUIFERS

Context: You are building crate `amalgam-worldgen-terrain` for Amalgam.
Reference sources:
- `net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator`
- `net.minecraft.world.level.levelgen.Aquifer`
- `net.minecraft.world.level.levelgen.NoiseChunk`

Requirements:
1. Cell Grid Sampling:
   - Chunk dimensions: $16 \times 384 \times 16$ blocks ($Y \in [-64, 319]$).
   - Cell size: $4 \times 8 \times 4$ blocks ($4 \times 48 \times 4$ cells).
   - Sample `NoiseRouter::final_density` at cell vertices $(5 \times 49 \times 5 = 1,225$ points) into a contiguous stack buffer.
2. 3D Trilinear Interpolation:
   - For each cell, perform trilinear interpolation between the 8 corner densities to determine the density at every block position $(x, y, z)$.
   - Optimize interpolation loop using SIMD-friendly unrolled multiplication and fused multiply-add.
3. Aquifer System:
   - Global sea level = 63 (water block if $y \le 62$ and density $\le 0$).
   - Lava level = -54 (lava block in deep caverns if $y \le -54$).
   - Underground floodedness and barrier noise sampling for localized flooded caves.
4. Output Voxel Grid:
   - Generate an intermediate `RawChunkData` representation storing block classification (`Solid`, `Fluid(Water)`, `Fluid(Lava)`, `Air`) for all 98,304 voxels.

Output complete, compilable Rust code (`cell_sampler.rs`, `interpolator.rs`, `aquifer.rs`, `terrain_generator.rs`, `lib.rs`).
```

---

### Phase 5: Surface System & Surface Rules (`amalgam-worldgen-surface`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 5: SURFACE RULES & MATERIAL APPLICATION

Context: You are building crate `amalgam-worldgen-surface` for Amalgam.
Reference sources:
- `net.minecraft.world.level.levelgen.SurfaceSystem`
- `net.minecraft.world.level.levelgen.SurfaceRules`
- Datapack JSON: `data/minecraft/worldgen/noise_settings/overworld.json` (`surface_rule` section).

Requirements:
1. Surface Rule Condition AST:
   - Implement rule conditions: `Biome`, `VerticalAnchor`, `StoneDepthFloor`, `StoneDepthCeiling`, `Water`, `AbovePreliminarySurface`, `Hole`, `Temperature`.
2. Rule Actions:
   - `Block(BlockStateId)`
   - `Sequence(Vec<SurfaceRule>)`
   - `Condition { if_true: Condition, then_run: SurfaceRule }`
   - `Bandlands`, `TerracottaBands`
3. Surface System Material Application:
   - Bedrock generation: Bottom layer at $Y = -64$, with triangular random dithering from $Y = -64$ to $Y = -60$.
   - Deepslate transition zone: Linear dithering from Stone to Deepslate between $Y = 0$ and $Y = -8$.
   - Topsoil application: Apply Grass Block on surface, Dirt 3 blocks below, Sand/Sandstone in deserts and beaches, Gravel in ocean floors.
   - Water and ice surface handling.
4. Fast Rule Evaluator:
   - Compile the JSON surface rule AST into an optimized flat decision tree or Rust match statements.

Output complete, compilable Rust code (`rules_ast.rs`, `evaluator.rs`, `surface_system.rs`, `bands.rs`, `lib.rs`).
```

---

### Phase 6: PalettedContainer Baking & Amalgam Integration (`amalgam-worldgen-baking`)

**Copy-paste this prompt into AI Studio:**

```text
PROMPT PHASE 6: PALETTED CONTAINER BAKING & AMALGAM INTEGRATION

Context: You are integrating world generation with the Amalgam server core.
Reference sources:
- `amalgam_protocol::packets::ChunkDataPacket` (Packet 0x24)
- `amalgam_core::components::ChunkViewer`
- `amalgam_core::systems::chunk_streaming_system`

Requirements:
1. Chunk Section PalettedContainer Encoder:
   - Take the generated 98,304 block IDs and biomes.
   - Split into 24 sections ($Y = -64$ to $319$).
   - For each section:
     * Count non-air blocks.
     * Build compact palette:
       - If palette.len() == 1: 0 bits/entry single value.
       - If palette.len() <= 16: 4 bits/entry packed into 256 u64 longs.
       - If palette.len() <= 256: 8 bits/entry packed into 512 u64 longs.
     * Encode biomes container (single biome or indirect).
2. Heightmap Generation:
   - Calculate `MOTION_BLOCKING` (highest non-air/water block) and `WORLD_SURFACE`.
   - Pack into 37 longs using 9 bits per coordinate column ($16 \times 16 = 256$ columns).
3. Sky Light & Block Light:
   - Calculate sky light mask (26 bits) and populate daylight arrays (`[0xFF; 2048]`).
4. End-to-End Generator Service:
   - `WorldGenerator::generate_chunk(chunk_x: i32, chunk_z: i32) -> ChunkDataPacket`
   - Multi-threaded chunk generation using Rayon (`par_iter`).
   - Zero heap allocation in inner loops; recycle serialization buffers.

Output complete, compilable Rust code (`paletted_container.rs`, `heightmap.rs`, `chunk_baker.rs`, `generator.rs`, `lib.rs`).
```

---

## Part 4: Verification & Quality Assurance Checklist

After generating code from each phase, run this verification protocol:

1. **Compilation Check**:
   ```powershell
   cargo check --workspace --all-targets
   cargo clippy --workspace --all-targets -- -D warnings
   ```
2. **Deterministic Seed Verification**:
   - Run unit test with seed `0` at coordinates `(0, 64, 0)`.
   - Verify that density, biome, and surface block match vanilla Java 1.20.1 values exactly.
3. **Benchmark & Memory Leak Check**:
   ```powershell
   cargo bench --bench worldgen_benchmark
   ```
   - Target generation speed: $> 250$ chunks per second on an 8-core CPU.
   - Heap allocations: Exactly zero heap re-allocations during chunk baking.
4. **Amalgam Server In-Game Test**:
   - Run Amalgam server with the new generator.
   - Connect client (Protocol 763 / 1.20.1).
   - Fly across chunk boundaries at high speed; verify seamless terrain, caves, mountains, oceans, and zero rubberbanding.
