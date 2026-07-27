# Create: LazyTick

Create: LazyTick (CLT) is a performance addon for [Create](https://github.com/Creators-of-Create/Create).
It reduces repeated, unproductive ticking in Create automation while preserving normal processing,
event-driven wakeups, and bounded retries for machines that still need to make progress.

It is designed for Create-heavy modpacks and servers where machines are often idle, waiting for
inputs or output capacity, or repeatedly searching large recipe sets.

## What It Optimizes

- **Lazy scheduling** reduces polling for eligible idle machines and bounded retry states.
- **Recipe and state caches** avoid repeating expensive recipe and capability searches when the
  relevant machine state has not changed.
- Coverage includes belts, funnels, chutes, depots, item drains, basins, mechanical crafters,
  saws, deployers, spouts, mechanical arms, and configurable fluid scheduling.

## Benchmark Overview

The following published comparison summarizes the average running work of sample production lines
before and after CLT. Lower green bars are better; results vary by Create version, addons, factory
topology, configuration, and whether a line is idle or active.

![Create: LazyTick optimization overview](https://raw.githubusercontent.com/duckgun13476/Create-LazyTick/CLT-combined/assets/optimization-overview-en.png)

## Supported Versions

| Loader | Minecraft | Create target |
| --- | --- | --- |
| Forge | 1.19.2 | 0.5.1.i |
| Forge | 1.20.1 | 0.5.1.j |
| Forge | 1.20.1 | 6.0.x |
| NeoForge | 1.21.1 | 6.0.x |

Create is required. Install the matching CLT build on the server and on every connecting client.

## Configuration and Timing

CLT adds a **Lazy Clock** for per-machine control. Its default cycle is `0`, `25`, `50`, `75`,
and `100` percent: `0` keeps the machine at normal cadence, while higher values permit a longer
lazy interval. Dynamic mode uses adaptive/backoff behavior; forced mode applies a fixed interval.

Start with the defaults and test representative automation before raising a delay limit. Timing-
critical machines can be kept at full speed with the Lazy Clock instead of disabling CLT globally.
Some optimized paths intentionally retain normal or bounded cadence to protect short interaction
windows and eventual output progress.

## Links

- [Source and issue tracker](https://github.com/duckgun13476/Create-LazyTick)
- [Modrinth](https://modrinth.com/mod/createlazytick)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-lazytick)
- [Published measurements on MC百科](https://www.mcmod.cn/class/23850.html)

Create: LazyTick is licensed under [GNU GPLv3](https://github.com/duckgun13476/Create-LazyTick/blob/CLT-combined/LICENSE) (`GPL-3.0-only`).
