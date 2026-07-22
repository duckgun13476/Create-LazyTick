<img width="496" height="149" alt="logo" src="https://github.com/user-attachments/assets/b437a026-1527-48b9-9bda-a49af8369c89" />

<p align="center">
<a href="https://modrinth.com/mod/createlazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/modrinth_vector.svg" alt="Modrinth Page"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/create-lazytick"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3.2.0/assets/cozy/available/curseforge_vector.svg" alt="CurseForge Page"></a>
</p>


## Create: LazyTick ⚙️

---

A performance-focused optimization addon for **Create**.

By introducing lazy ticking and caching mechanisms, this mod significantly reduces unnecessary computation and idle overhead of Create machinery, allowing servers to handle **2–4× more active production lines** compared to vanilla Create, as well as much larger semi-idle infrastructure.

---

### 🔧 Core Optimizations

#### 🧱 Logistics Components
- **Belt / Funnel / Chute / Depot**
- 50%–95% reduction in idle overhead
- 40%–80% reduction in dynamic extra workload
- Significant gains in large-scale storage and filtering systems

---

#### 🏭 Processing Components
- **Mechanical Crafter / Mechanical Mixer / Mechanical Saw / Basin**
- Introduces recipe caching mechanisms
- Performance improvement scales with recipe count
- 70%–95% reduction in active workload
- In extreme cases, workload drops from ~300µs to ~2–10µs

---

#### 🤖 Interaction Components
- **Mechanical Arm / Deployer**
- Reduces frequent checks and repeated searches
- 20%–60% workload reduction
- Greater gains when interacting with Belts

---

#### 🌊 Fluid System Optimization
- Global fluid ticking reduced to 1/k of vanilla (default: 1/5)
- Multiplier configurable via config
- Significant improvements in large pipe networks

---

### 📊 Real-World Performance

In live server testing environments:

- Supports 2–3× more fully active production lines
- Semi-idle structures can scale beyond 5× vanilla capacity
- Optimization strength increases with larger recipe datasets

---

### ⚠️ Important Notes

- Reduced tick frequency may cause slight animation delays (configurable)
- Some logic checks have been modified; contraptions relying on strict vanilla tick synchronization may require adjustment using the **LazyTick Clock**
- Always test and back up your world before deploying to production servers

---
