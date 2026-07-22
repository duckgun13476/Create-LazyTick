
<img width="496" height="149" alt="logo" src="https://github.com/user-attachments/assets/b437a026-1527-48b9-9bda-a49af8369c89" />

<p align="center">
<a href="https://modrinth.com/mod/createlazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/modrinth_vector.svg" alt="Modrinth Page"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/create-lazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/curseforge_vector.svg" alt="CurseForge Page"></a>
</p>


## 机械动力：懒惰刻 ⚙️

---

一个专注于 **Create 性能优化** 的模组。

通过懒惰刻与缓存机制，显著减少工作元件的无效计算与静态占用，使服务器相比原版 Create 可额外承载 **2–4 倍常态产线**，以及更高规模的半静态结构。

---

### 🔧 核心优化

#### 🧱 物流元件优化
- **传送带 / 漏斗 / 溜槽 / 置物台**
- 静态占用削减 50%–95%
- 动态情况下减少 40%–80% 额外开销
- 大规模仓储与筛选结构收益明显

---

#### 🏭 加工元件优化
- **动力合成器 / 动力搅拌器 / 动力锯 / 分液池**
- 引入配方缓存机制
- 大量配方环境下性能提升极为明显
- 工作占用最高可降低 70%–95%
- 极端情况下占用从 ~300µs 降至 ~2–10µs

---

#### 🤖 交互类元件优化
- **机械臂 / 机械手**
- 减少频繁检测与重复搜索
- 20%–60% 占用削减
- 传送带联动场景下收益更明显

---

#### 🌊 流体系统优化
- 全局流体刻缩减为原版的 1/k（默认 1/5）
- 支持配置倍率调整
- 大规模管网环境收益显著

---

### 📊 实际效果

在真实服务器测试环境下：

- 可多承载 2–3 倍完整生产线
- 半静态结构可提升至 5 倍以上规模
- 高配方环境下优化强度随数据量增长而增强

---

### ⚠️ 注意事项

- 由于降低刻频率，部分设备动画可能出现轻微延迟（可通过配置调整）
- 修改了部分逻辑特判，某些基于原版刻同步的结构可能需要使用「懒惰刻时钟」进行适配
- 建议在正式服部署前进行测试并备份存档

---
