<img width="496" height="149" alt="logo" src="https://github.com/user-attachments/assets/b437a026-1527-48b9-9bda-a49af8369c89" />

<p align="center">
<a href="https://modrinth.com/mod/createlazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/modrinth_vector.svg" alt="Modrinth Page"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/create-lazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/curseforge_vector.svg" alt="CurseForge Page"></a>
</p>

## Create: LazyTick ⚙️

A performance-focused optimization addon for **Create**. Lazy ticking and caching reduce unnecessary machinery work and idle overhead.

### Core optimizations

- **Logistics**: Belt, Funnel, Chute, and Depot idle work.
- **Processing**: recipe caching and idle handling for Mechanical Crafters, Mixers, Saws, and Basins.
- **Interaction**: repeated checks in Mechanical Arms and Deployers.
- **Fluids**: global fluid ticking is configurable, defaulting to one fifth of vanilla frequency.

### Combined development layout

This repository follows the BO/CSC/CEC multi-version layout. The root wrapper owns cross-version build and publishing orchestration; `common/` is source-injected into target projects; `project/<loader>/<version>/` retains only version-specific API, Mixin, metadata, and dependency code.

```powershell
.\gradlew.bat compileJavaAllVersions
.\gradlew.bat packageAllVersions
```

Test and back up worlds before production deployment.
