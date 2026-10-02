# AGENTS.md — 湮灭协议（Annihilation Protocol）

NeoForge 1.21.1 类《明日方舟》塔防 Mod。当前状态：**MVP 已实现并通过无头功能验证，UI 尚未开始**。
原始规划文档：`Temp/初版规划.md`；已批准的 MVP 计划见仓库历史（分阶段：领域核心 → 实体/渲染 → 路径移动 → 阻挡/战斗 → LDLib2 网络 → 边界/持久化）。

## 注意事项

- 1.不同的功能模块必须解耦，尽量避免相互依赖
- 2.创建功能模块前先检查项目中是否已有相应功能
- 3.编写完代码先审查一次，确认无问题后再继续任务

## 项目结构（包级分层，改动前先看这里）

```
org.lingZero
├─ AnnihilationProtocolMod   @Mod 入口：注册表 + Config + NetworkBridge.init()
├─ Config                    COMMON 配置（gateMaxHp/defaultEnemyCount/maxOperators/deployCooldownTicks/...）
├─ battle/    纯领域层：LevelState、LevelDefinition(路径/区域+NBT)、BattleRules、SpawnQueue、
│             DeployResult、Selftest。只依赖 net.minecraft.*，禁止依赖 runtime/network/entity/渲染
├─ runtime/   服务端权威运行时：BattleSession(状态机/计数/驱动生成队列)、SessionManager(按维度持有会话)、
│             LevelRuntimeData(SavedData 持久化)、BlockingSystem、CombatSystem、DeployService、
│             Snapshot/SnapshotSink/SnapshotBroadcaster、SessionEvents(事件入口)
├─ entity/    EnemyEntity、OperatorEntity（Mob + GeckoLib GeoEntity）、Definitions/EnemyDefinition/OperatorDefinition
├─ network/   APChannels、APPackets(LDLib2 @RPCPacket 端点)、NetworkBridge —— 唯一的网络桥
├─ client/    ClientBattleState(客户端只读缓存+action bar 反馈)、ClientDeployRequest(C2S 部署请求)、
│             ClientSetup(渲染器注册)、render/*
├─ registry/  ModEntities / ModItems / ModRegistries
├─ item/      OperatorDeployerItem（右键发 C2S 部署请求，不在客户端生成实体）
└─ command/   AnnihilationCommand（/ap 命令树，别名 /annihilation）
```

**分层不变量（新增功能必须遵守）**

1. `battle` 不依赖任何其他自有包（除 MC 类）；规则/判定放这里，便于 `/ap selftest` 覆盖。
2. `runtime` **禁止 import LDLib2 或 GeckoLib**（攻击/动画演出由实体自己的 `afterAttack()` 触发）。它对外的出口只有两个接口：
   - `SnapshotSink`（由 `network.NetworkBridge` 注入 `APPackets` 实现）负责把快照广播出去；
   - `SessionEvents` 监听游戏事件驱动 tick。
3. `network` 是唯一同时接触 `runtime` 与 `client` 的包；客户端的任何行为都从 `ClientBattleState` 读取。
4. `entity` 不反向依赖 `runtime`：路径/终点由会话通过 `EnemyEntity#configure(path, gatePos)` 单向注入；
   阻挡关系由 `BlockingSystem` 调用实体的 setter 建立，实体自身不知道会话存在。
5. 客户端代码只允许出现在 `client` 包，并加 `@OnlyIn(Dist.CLIENT)` + `FMLEnvironment.dist.isClient()` 守卫，
   保证专用服务器不加载客户端类（已验证 `runServer` 可正常启动）。

**数据流**：命令/物品 RPC → `DeployService`/`BattleSession` → `ServerTickEvent.Post` 驱动
`BattleSession.tick()`（`SpawnQueue.tickPop()` → 门到达判定 → `BlockingSystem`(每 5 tick) → `CombatSystem` → 胜负判定）
→ 实体用原版 `SynchedEntityData` + GeckoLib `triggerAnim` 表现 → `SnapshotBroadcaster` → LDLib2 RPC → `ClientBattleState`。

**计数语义（P1-1 的教训，改动前必读）**：`LevelRuntimeData.remaining` 是"本局尚未结算的敌人总数"，
`start()` 时就预置为整局总数、`queueSpawn()` 追加时同步增加，`spawnEnemy()` **只增 `alive`**。
若改成"生成时才累加 remaining"，带间隔的生成队列会在中途被判胜（`/ap start 3 200` 即可复现）。

## 框架使用约定

### GeckoLib（实体模型/动画）

- 实体实现 `GeoEntity`：`registerControllers(AnimatableManager.ControllerRegistrar)` + `getAnimatableInstanceCache()`
  （缓存用 `GeckoLibUtil.createInstanceCache(this)`）。
- 动画控制器：`new AnimationController<>(this, "main", 5, handler)`，handler 内
  `state.getController().setAnimation(RawAnimation.begin().thenLoop("move"))` 并返回 `PlayState.CONTINUE`；
  一次性动作（攻击）用 `.triggerableAnim("attack", RawAnimation.begin().thenPlay("attack"))` + 服务端 `triggerAnim("main","attack")`。
- 渲染器直接 `new GeoEntityRenderer<>(ctx, ENTITY_TYPE.get())`，**不要**再写自定义 GeoModel：资产路径由实体注册名自动推导为
  `assets/annihilation_protocol/geo/entity/<name>.geo.json`、`animations/entity/<name>.animation.json`、`textures/entity/<name>.png`。
- 渲染器注册用 NeoForge `EntityRenderersEvent.RegisterRenderers`（见 `client/ClientSetup`）。
- 现有资产是手写占位模型（3~5 个 cube + 纯色 64×64 贴图 + idle/move/attack），替换为 Blockbench 导出时**保持同名同路径**即可。
- 动画名固定 `idle`/`move`/`attack`，控制器名固定 `main`。

### LDLib2（本版只用网络层，UI 见下节）

- 自定义包一律用 `@com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket("id")` 标注**静态方法**；
  `RPCPacketDistributor.rpcToServer/rpcToPlayer/rpcToAllPlayers/rpcToTracking` 发送。
  LDLib2 在它自己的 `CommonProxy` 里用 ASM 扫描全部 mod 的 `ModFileScanData`，我们的端点会自动注册，不需要手写 payload。
- **参数类型限制**：只支持能解析成 `IDirectAccessor` 的类型（基本类型、String、Enum、UUID、BlockPos、
  ResourceLocation、Component、ItemStack 等）。**Collection / Map / 数组不能作为参数**（会抛
  `Accessor for type ... is not a ManagedAccessor`）。需要传列表时用字符串编码或分多次发送。
- 处理器在主线程执行（NeoForge `PayloadRegistrar` 默认 `HandlerThread.MAIN`），可直接操作世界与实体。
- 用 `RPCSender#isServer()` 判断来源，`asPlayer()` 拿发信玩家（客户端→服务端时非空）。
- 客户端发请求的唯一出口是 `client/ClientDeployRequest`（不要在 `network` 包放客户端类）。
- 现有 3 个端点（见 `network/APChannels`）：`battle_snapshot(state,gateHp,gateMaxHp,remaining,alive)`（S2C，**按维度**只发给 `level.players()`）、`deploy_request(pos,operatorId)`（C2S）、`deploy_result(result)`（S2C）。
- **不要**用 LDLib2 的 `@DescSynced/@Persisted/@RPCMethod`：那套声明式同步仅对 BlockEntity 提供 holder
  （`syncdata/holder/blockentity/*`，只发给 tracking chunk 的玩家）。关卡状态是全球性的，因此走
  `SavedData`(持久化) + RPC 广播(同步)，这也是当前架构的既有决定，改动前先讨论。

### 下一步做 UI 时（尚未实现）

- HUD：`ModularHudLayer` 经 `RegisterGuiLayersEvent`(MOD bus, client) 注册，必须 `Suppliers.memoize` 延迟构建；
  数据源直接绑定 `client.ClientBattleState`（`SupplierDataSource.of(() -> ...)`，它实现 `ITickable`，会自动按 tick 轮询）。
- 部署界面：`ModularUIScreen`（纯客户端）+ 点击格子发 `deploy_request`，服务端校验后回 `deploy_result`。
  若要让界面灰掉非法格子，需要一个"查询可用格"的端点——注意 RPC 不能传集合，用字符串编码。
- 写任何 LDLib2 UI/同步代码前必须加载 `/lowdragmc-reference` skill（见下节）。

### 命令入口（当前唯一的正式交互方式）

`/ap status`、`/ap start [count] [interval]`、`/ap reset`、`/ap spawn <count>`、
`/ap deploy <operatorId> [pos]`、`/ap config setspawn|setgate|setarea`、`/ap path add|clear|list`、`/ap selftest`。
权限等级 2；`/ap deploy` 与 `config`/`path` 均可从控制台执行（`DeployService` 的 player 参数可为 null，
为 null 时跳过距离/冷却校验）。`/ap spawn` 是按当前间隔入队（不是全部立即出现）。
所有玩家可见文案走 lang 键 `msg.annihilation_protocol.*`（含血条名字牌 `hp_tag`），禁止在 Java 里硬编码中文用户文案；
**例外**：`battle/Selftest` 的失败原因是开发者诊断输出，允许保留中文字面量。

## 编码约定

- 标识符英文、注释中文；用户可见文本全部用 `Component.translatable` + `assets/annihilation_protocol/lang/{en_us,zh_cn}.json`。
- 数值调整位置：玩法参数 → `Config`（含 `maxDeployDistance`）；单位数值（血量/攻击/间隔/阻挡占用） → `entity/Definitions`。
- `Config` 只监听 `ModConfigEvent.Loading`/`Reloading`（不要退回基类 `ModConfigEvent`，它含 Unloading，`get()` 会抛异常）。
- `build.gradle` 里已加 `options.encoding = 'UTF-8'`：源码含中文，**不要删掉**，否则 Windows 默认 GBK 会乱码。
- 修改关卡状态结构时，同步更新 `LevelRuntimeData` 的 NBT 读写与 `LevelDefinition`；
  `BattleSession.onLoaded()` 负责"重启后 RUNNING→IDLE + 清理残留实体"（残留清理有 200 tick 的窗口，
  因为实体随区块陆续载入；只清理不在追踪表里的己方实体）。
- `LevelRuntimeData` **只持久化** definition / state / gateMaxHp；gateHp、remaining、alive 每局由
  `start()`/`reset()` 重算、`onLoaded()` 归零，不要重新写盘（纯写放大）。
- `SessionManager.ensureSession()` 首次调用有副作用（载入持久化数据 + 执行 `onLoaded()`），只读命令也依赖它。

## 构建与运行

Java 21 必需（本机：`C:\Program Files\Zulu\zulu-21`；环境变量 `JAVA_HOME` 默认指向 jre7，**不能直接用**）。
依赖已缓存在 `G:\.gradle`，按用户要求 gradle user home 指向该目录：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-21'
./gradlew.bat -g G:\.gradle build        # 构建
./gradlew.bat -g G:\.gradle runClient    # 客户端（必须由用户手动跑）
./gradlew.bat -g G:\.gradle runServer
./gradlew.bat -g G:\.gradle runData
./gradlew.bat -g G:\.gradle runGameTestServer
```

沙箱限制 `G:\.gradle` 写入时（agent 环境可能遇到），可改用只读依赖缓存 + 工作区内可写 home，
无需联网（`--offline`）：

```powershell
$env:GRADLE_RO_DEP_CACHE='G:\.gradle\caches'
& 'G:\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat' `
  --offline -g "$PWD\.gradle-home" --console=plain build
```

你无法直接操作游戏内容测试：任务完成后先提醒用户，用户确认后再给出具体测试清单并让用户 `runClient`。

## 验证手段（无玩家也能测的部分）

- **纯逻辑**：`/ap selftest` 会跑 `battle.Selftest` 的断言并输出 `n/N passed`（当前 25/25，含 SpawnQueue 与"未结算敌人不得判胜"）。
- **端到端（无头）**：`run/eula.txt` + `run/server.properties` 里开 RCON
  （`enable-rcon=true`、`rcon.port=25575`、`rcon.password=...`）后，可用 PowerShell 的 TCP/RCON
  发命令验证：配置 → `/ap start` → `/ap status` / `/data get entity @e[type=annihilation_protocol:...] ...`
  观察移动、阻挡、血量、胜负、重置。RCON 协议：长度(int LE)+id+type+payload+空终止符，type 3=登录、2=命令。
- **必测的胜负回归用例**：`/ap deploy operator_guard <路径旁> ; /ap start 3 200` → 首杀后必须仍是
  `Running + Remaining 2`，全部结算后才 `Victory`（这是 P1-1 的复现/回归用例）。
- **关键坑**：无玩家在线时区块不一定 entity-ticking，**实体完全不 tick**（表现为敌人/僵尸原地不动）。
  测试前必须 `/forceload add <from> [to]`，测完 `/forceload remove all`。
- **测不了、必须用户在客户端验证**：GeckoLib 模型/动画/贴图、action bar 反馈、物品右键→RPC 部署往返、多人可见性。

## 尚未实现 / 已知限制（不要当成 bug）

- 无 UI（无 HUD、无部署界面）；LDLib2 仅作网络层。
- 每个维度只有一个关卡会话；重启会把 RUNNING 降级为 IDLE 并清理实体，不恢复进行中的战斗。
- 敌人与干员无费用/波次/技能/远程/医疗；战斗仅近战 1:1，远程与格子范围未做。
- 到达蓝门的敌人不参与清除战斗；失败时不清场残留敌人（`/ap reset` 清理）。
- **路径被方块堵住时敌人不会绕路也不会结束本局**（固定路径优先），只有 `debugLogging=true` 下的
  "已卡住 N tick" 日志；需要人工清路或 `/ap reset`。
- 同一干员被多个敌人抢占时，规则是**离蓝门更近的敌人优先**（`BlockingSystem.prioritized`）；改名/改规则要同步这里。
- 相机以外：快照按维度广播，玩家不会看到别的维度的关卡状态（`SnapshotSink.broadcast(ServerLevel, ...)`）。
- 已在 MVP 提前落地（规划列为可延后）：带间隔的生成队列、GeckoLib 手写占位资产（代价见 P1-1 与占位模型说明）。
- 关卡配置为逻辑坐标（无专属方块/方块实体）；`isOnPath` 只按 XZ 距离判定 0.5 格。
- GeckoLib 资产是占位模型；占位贴图为纯色。
- 敌人的"逻辑状态"目前由表现枚举 `EnemyAnimState` 兼任（`serverTick` 用早退表达"被阻挡"），
  规划 §6.3 的"到达蓝门/死亡"只存在于会话侧；**加技能/远程前建议先收拢成显式状态机**。
- `BattleSession` 仍同时管追踪表/胜负/快照节流/残留清理（生成队列已抽成 `battle/SpawnQueue`）；
  写 UI 之前建议继续拆分追踪表。

## LDLib2 开发参考

编写任何 LDLib2 相关代码（UI、节点图、Sync 注解同步、Configurable、编辑器框架）时，**必须先加载 `/lowdragmc-reference` skill**，它基于 LDLib2 官方文档与源码校对，包含：

- **UI 系统** — ModularUI/UIElement 生命周期、Taffy 布局、LSS 样式、数据绑定（SyncStrategy）、事件传播（捕获/目标/冒泡）、30 个组件、13 种 IGuiTexture
- **Sync 同步** — `@Persisted`/`@DescSynced`/`@ReadOnlyManaged` 等注解、三种脏检测 Ref 策略、RPC 双向通信
- **Configurable** — `@Configurable` 注解族、ConfiguratorAccessors 查找机制、Inspector + History
- **节点图工具包** — Graph/GraphModel/Node/TypeHandle 三层架构、变量黑板、子图、Context/Block 节点
- **编辑器框架** — Editor/View/Resource/Project、SplitView 布局树、IResourcePath Codec 版本

不要在未加载该 skill 的情况下直接生成 LDLib2 代码，尤其是 UI XML 布局、节点图注册和同步注解类。
