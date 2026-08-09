# CreateLazyTick NeoForge 流体源端 capability 失效恢复

## 现象与最小复现

主服 core 的 Create Diesel Generators 发酵罐控制器位于 `3824,64,3327`，经
`3823,64,3327 -> 3823,63,3327 -> 3822,63,3327 -> 3821,63,3327 -> 3820,63,3327`
连接到 Create 工作盆。现场连续三次采样中，发酵罐保持
`createdieselgenerators:ethanol 2685 mB`，工作盆两个输入槽分别保持 ethanol 与
biodiesel `215 mB`，输出为空；中间泵为 `86 RPM`。

所有管线段的 NBT 均显示 ethanol，且没有 `Flow.Progress`，即 Create 的 Layer II flow
已经 complete。删除方块或重启会立即恢复，故复现必须在不删除管线、不重启服务器的条件下：
先建立稳定的发酵罐 -> 泵 -> 工作盆传输，再触发发酵罐多方块 controller 重组或 capability
刷新，观察是否持续 drain/fill。

## 源码与运行时证据

- 已核对运行包 `E:\MC\test\齿轮の松饼小镇\.minecraft\mods\create-1.21.1-6.0.10.jar`：
  `ICapabilityProvider.BlockCapabilityCacheProvider#getCapability()` 在缓存失效时返回 `null`。
- Create `FluidNetwork` 仅在 `source == null` 时重取 `sourceSupplier`：
  `C:\F\MC_Project\Create-latest\src\main\java\com\simibubi\create\content\fluids\FluidNetwork.java:163`；
  但在 source capability 为 null 时直接返回：同文件 `:192-194`。真正 drain/fill 分别位于
  `:204` 与 `:251`，与现场两端数值均不变化一致。
- Create `PipeConnection#manageFlows` 保留既有 `FluidNetwork`：
  `C:\F\MC_Project\Create-latest\src\main\java\com\simibubi\create\content\fluids\PipeConnection.java:96-97,146-150`。
- Bulk Fermenter 的 `refreshCapability()` 更新 handler 后调用 `invalidateCapabilities()`：
  `E:\upgrade\_tmp\createdieselgenerators-cfr-20260707\com\jesz\createdieselgenerators\content\bulk_fermenter\BulkFermenterBlockEntity.java:372-374`。
- 修复前 CLT 只在 `IllegalStateException` 包含 `invalid cache` 时清空 source：
  `C:\F\MC_Project_Combined\CreateLazyTick\project\neoforge\1.21.1\src\main\java\net\pinkcats\createlazytick\mixin\OptElement\fluid\FluidNetworkTransferSpeedMixin.java:56-73`。

## 根因

Bulk Fermenter 刷新 capability 后，Create `FlowSource` 会取得新的 provider，但已存在的
`FluidNetwork.source` 仍持有旧 provider。旧 provider 正常返回 null；Create 于是直接退出
Layer III，既不调用源端 `drain`，也不调用目标 `fill`，并且从不把 source 置空以重取新 provider。

这不是泵、压力或工作盆配方问题。泵和 Layer II flow 仍正常，正是既有网络对象持续被保留，
使失效 source 引用永久存在。CLT 的五 tick 流体节流只使重建时机延后，不能单独造成永久停滞；
现有异常 catch 也不覆盖返回 null 的正常失效路径。

## 修复方案

仅修改 NeoForge 1.21.1 的
`FluidNetworkTransferSpeedMixin#createLazyTick$safeGetCapability`。在既有 invocation 返回
null 时，只有当该 provider 与 `this.source` 相同，才执行 `this.source = null; this.reset()`。
下一次网络 tick 将从 `sourceSupplier` 取得 FlowSource 当前 provider。

目标端 provider 返回 null 不进入此分支，继续由 Create 的 target 重新发现逻辑处理；不使用
Bulk Fermenter、Basin 或泵的模组 ID 特判，也不关闭普通流体管线的五 tick 优化。

## 构建与产物

- 构建命令：`gradlew.bat :neoforge-1.21.1:jar --console=plain`
- 编译前置验证：`gradlew.bat :neoforge-1.21.1:compileJava --console=plain` 已通过。
- 本地 jar：`C:\F\MC_Project_Combined\CreateLazyTick\project\neoforge\1.21.1\build\libs\CreateLazyTick-2.5.15-6.0.x-neoforge-1.21.1.jar`
- SHA-256：`3586E089B0B8E0FB36D0C0A1D4CC99C53BDEC00209ADC57232A0A0FDC278E0E8`
- 更新队列编号在聚合后由 `Add-UpdatePackage.ps1` 写入。
- 运行时回归待服主在不删除方块、不重启的前提下执行上述多方块 capability 刷新测试。
