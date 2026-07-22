# CreateLazyTick combined workspace

This branch is a maintenance workspace, not a merged multi-loader implementation.

- `common/` is intentionally code-empty until a shared API has been proven across all versions.
- `project/forge/1.19.2/` contains the complete `1.19.2-0.5.1.i-forge` branch snapshot.
- `project/forge/1.20.1-0.5.1/` contains the complete `1.20.1-0.5.1.j-forge` branch snapshot.
- `project/forge/1.20.1-6.0.x/` contains the complete `1.20.1-6.0.x-forge` branch snapshot.
- `project/neoforge/1.21.1/` contains the complete `1.21.1-6.0.x-neoforge` branch snapshot.

Each version project retains its own Gradle files and can be built in isolation. The root project only
provides discovery and the `buildAllVersions` aggregate task; it does not create cross-version code
dependencies.
