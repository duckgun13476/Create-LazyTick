# CreateLazyTick combined workspace

This branch is a maintenance workspace with a BO/CSC/CEC-style combined Gradle layout,
not a merged multi-loader implementation.

- `common/` owns source/resource fragments shared by version projects.  The fragments are
  injected through `gradle/clt-common.gradle`, not published as one binary dependency,
  because some shared classes depend on target-version Minecraft mappings.
- `project/forge/1.19.2/` contains the complete `1.19.2-0.5.1.i-forge` branch snapshot.
- `project/forge/1.20.1-0.5.1/` contains the complete `1.20.1-0.5.1.j-forge` branch snapshot.
- `project/forge/1.20.1-6.0.x/` contains the complete `1.20.1-6.0.x-forge` branch snapshot.
- `project/neoforge/1.21.1/` contains the complete `1.21.1-6.0.x-neoforge` branch snapshot.

Each version project retains its own Gradle files and can be built in isolation. The root wrapper provides
aggregate `compileJavaAllVersions`, `jarAllVersions`, and `buildAllVersions` tasks, plus cached client-run
entry points. It does not create cross-version binary dependencies.
