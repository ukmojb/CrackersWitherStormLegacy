# Cracker's Wither Storm Legacy

将 [Cracker's Wither Storm Mod](https://www.curseforge.com/minecraft/mc-mods/wither-storm-mod)（Forge 1.20.1，版本 4.2.1）完整移植到 Minecraft 1.12.2 / CleanroomLoader 的社区移植版。

## 如何食用与 ARR 相关声明

上游模组受 **All Rights Reserved** 协议保护，本移植版**不包含**上游的任何纹理、声音、模型、结构、语言文件或数据文件。

玩家必须自行下载并安装原版上游 JAR：

1. 下载 `witherstormmod-1.20.1-4.2.1-all.jar`（原版 Forge 1.20.1 模组文件）。
2. 将文件放入 Minecraft 的 `resourcepacks` 文件夹（与存档目录同级，例如 `.minecraft/resourcepacks/`）。
3. 启动游戏。

本移植模组在运行时把该 JAR 挂载为外部资源包和数据来源：

- 纹理、声音、模型、语言文件来自该 JAR；
- 腐化配方、成就、战利品表、结构、标签等数据也直接读取该 JAR；
- 启动时会校验 JAR 的清单版本必须为 `1.20.1-4.2.1`，缺失或版本不符会拒绝加载。

请勿把该 JAR 内的资源复制进本移植模组或随模组分发。

上游 JAR 内还带一个可选的 “CWSM Programmer Art” 资源包（`resourcepacks/programmer_art`）。
出于 ARR 合规，本移植不会自动解包或复制它；需要该资源包的玩家可自行从原 JAR
中把 `resourcepacks/programmer_art` 解压到游戏的 `resourcepacks` 文件夹。

## 环境要求

- Minecraft 1.12.2
- CleanroomLoader 0.6.6-alpha及以上
- Future MC（可选）
- Crossbow（可选）
- JEI（可选）

## 部分问题

Q: 引爆恐怖炸弹卡死了是为啥?
A: 因为安装了opt,去把配置文件关闭chromaticAberration。

Q: 为什么望远镜不放大?
A: 因为安装了opt,去把光影关了。
