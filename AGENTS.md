# 仓库协作规范

适用于整个仓库及所有代理。统一协作入口在本文件；`CLAUDE.md` 仅引用本文件。

## 功能变更流程

**先评估影响 → 编写 OpenSpec 变更规格 → 落实代码与验证 → 同步当前规格、追加日志并归档。** 新增功能、调整、缺陷修复、UI、默认值、协议、配置及数据结构变更均适用。编码前更新的是本次 change 的目标规格，不能将尚未实现的目标写成当前行为。

维护者执行功能变更或规格文档改修时，使用公共 kit-work 工作流（`/kit-work`，流程与默认格式见全局 `openspec-development` 技能）。本仓库不再维护项目内规格技能；下列目录、功能 ID 与日志约定，以及私有规格根 `openspec/config.yaml` 中的规则，优先于全局默认格式。

- 内部规格暂仅本地保存：`SPEC.md` 导航到 `openspec/specs/000-index.md`；整个 `openspec/` 与根入口被 Git 忽略，不强制添加、不复制完整内容到公开提交或 PR。
- 本机规格已整理到私有 OpenSpec Store `darkroom`；执行前用 `pi-project context --json` 核对 `codeRoot/specRoot`。本文规格路径相对 `specRoot`，源码和 `tools/` 相对 `codeRoot`；外置规格不缺失于公开副本的判断不能仅看代码根。原代码根忽略规则保留。
- 详细规格采用 `openspec/specs/XX-大功能中文名/XXYY-中功能中文名/XXYY010-小功能中文名/spec.md`；各大、中功能有 `000-index.md`。语义功能 ID 与旧 F ID 的关系由私有索引维护。
- 修改前保存真实前态，影响评估与目标规格放在对应 change 中；完成实现和验收后同步当前规格。纯文档调整说明无运行时影响并执行文档校验。
- 仅有公开代码副本时，贡献者可以按公开规则、行为说明和测试完成代码贡献，不要求访问私有规格。维护者负责补齐内部规格评估；不得自动下载或编造缺失规格。
- CLI 入口为 `sh tools/openspec.sh`，版本固定；流程由 kit-work 衔接，不依赖额外安装的 slash commands。
- 提案与设计的用户确认、DAG 登记、集成与发布按 kit-work 节点执行，不跳过。不要因为忽略规则而声称文档已备份或已在历史中撤除。

## 仓库与跨功能约束

- 技术基线：Kotlin、Compose、Room、Hilt；JDK 17；SDK 版本以 [app/build.gradle.kts](app/build.gradle.kts) 为准。
- `core/` 放纯规则，`data/` 负责存储、网络、图像和设备，`ui/` 负责交互。源码根为 `app/src/main/java/io/github/wa_otomia/darkroom/`。
- 保留用户未提交的改动，不顺手重构无关功能。并行代理使用互不重叠的写入范围，主代理统一核对交叉规则。
- 变更导入入口时同时评估自动化、分享确认门禁、URI/文件大小验证、图库与传输状态；不得绕过用户确认触发云端 AI 或实体打印。
- 变更构图/水印时同时检查竖横方向、各版本保存、预览、AI输入、导出与打印。打印规格和状态恢复必须与 [协议资料](docs/protocol/README.md) 及可用的内部打印规格核对。
- 变更后台任务时明确快照值与执行时读取值，覆盖取消、进程死亡、结果未知、避免重复打印；不能用假成功消除不确定状态。
- 持久化变更提供迁移及旧数据策略；API key、FTP 密码不得写入源码、日志、测试夹具或 saved-instance state。公开仓库不提交 `local.properties`、签名文件与个人凭据。
- 新增或修改用户文案核对 `values`、`values-zh`、`values-ja`、`values-ko`；UI 使用现有主题/交互组件，考虑 48dp 触摸区域、字号放大、RTL、TalkBack 和系统 insets。

## 验证入口

- JVM 回归：`./gradlew :app:testDebugUnitTest`；编译打包：`./gradlew :app:assembleDebug`。根据影响选择范围，无关文档修改不要求构建。
- Pro 核心脱离 Android 验证：`sh tools/verify-pro-printer-core.sh`（需要 JDK 17 与 `kotlinc`）。协议示例按 [示例说明](docs/protocol/examples/README.md) 验证。
- 文档检查（本地规格存在时）：按 kit-work 流程运行 OpenSpec 严格校验，核对结构、概要、索引、链接、功能 ID 和日志只追加，再运行 `git diff --check`；忽略文件必须另做前态对比。公开构建和 CI 不要求私有规格访问权限。界面、蓝牙、FTP 和打印实体效果按受影响的验收案例进行设备检查；无设备时如实列为未验证。
- 不因文档整理修改产品行为，不为了显示绿灯删除失败断言或降低安全门槛。发布、签名和推送按用户任务范围执行。
