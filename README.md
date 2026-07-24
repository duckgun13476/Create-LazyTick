<img width="496" height="149" alt="logo" src="https://github.com/user-attachments/assets/b437a026-1527-48b9-9bda-a49af8369c89" />

<p align="center">
<a href="https://modrinth.com/mod/createlazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/modrinth_vector.svg" alt="Modrinth Page"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/create-lazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/curseforge_vector.svg" alt="CurseForge Page"></a>
</p>

## 机械动力：懒惰刻 ⚙️

一个专注于 **Create 性能优化** 的模组。通过懒惰刻与缓存机制，显著减少工作元件的无效计算与静态占用。

### 🔧 核心优化

- **物流元件**：传送带、漏斗、溜槽、置物台的静态占用削减 50%–95%。
- **加工元件**：动力合成器、动力搅拌器、动力锯、分液池的配方缓存与空转优化。
- **交互元件**：机械臂、机械手的重复检测优化。
- **流体系统**：全局流体刻缩减为原版的 1/k（默认 1/5）。

### 组合开发结构

本仓库采用 BO/CSC/CEC 风格的多版本组合结构：根 wrapper 提供跨版本构建与发布编排；`common/` 是由各版本源码注入的共享片段；`project/<loader>/<version>/` 只保留对应版本的 API、Mixin、资源元数据与依赖坐标。

常用任务：

```powershell
.\gradlew.bat compileJavaAllVersions
.\gradlew.bat packageAllVersions
```

`README_en.md` 提供英文说明。部署前请测试并备份存档。
