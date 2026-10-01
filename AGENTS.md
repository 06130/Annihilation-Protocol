## 注意事项
- 1.不同的功能模块必须解耦，尽量避免相互依赖
- 2.创建功能模块前先检查项目中是否已有相应功能
- 3.编写完代码先审查一次，确认无问题后再继续任务


## 构建与运行

你无法直接操作游戏内容测试，任务完成后，提醒用户，用户确认后告诉用户需要测试哪些内容并runClient,gradle路径指向 G:\.gradle 而不是默认路径。

- **Build**: `./gradlew build`
- **Run client**: `./gradlew runClient`
- **Run server**: `./gradlew runServer`
- **Run data generator**: `./gradlew runData`
- **Run gametests**: `./gradlew runGameTestServer`

## LDLib2 开发参考

编写任何 LDLib2 相关代码（UI、节点图、Sync 注解同步、Configurable、编辑器框架）时，**必须先加载 `/lowdragmc-reference` skill**，它基于 LDLib2 官方文档与源码校对，包含：

- **UI 系统** — ModularUI/UIElement 生命周期、Taffy 布局、LSS 样式、数据绑定（SyncStrategy）、事件传播（捕获/目标/冒泡）、30 个组件、13 种 IGuiTexture
- **Sync 同步** — `@Persisted`/`@DescSynced`/`@ReadOnlyManaged` 等注解、三种脏检测 Ref 策略、RPC 双向通信
- **Configurable** — `@Configurable` 注解族、ConfiguratorAccessors 查找机制、Inspector + History
- **节点图工具包** — Graph/GraphModel/Node/TypeHandle 三层架构、变量黑板、子图、Context/Block 节点
- **编辑器框架** — Editor/View/Resource/Project、SplitView 布局树、IResourcePath Codec 版本

不要在未加载该 skill 的情况下直接生成 LDLib2 代码，尤其是 UI XML 布局、节点图注册和同步注解类。
