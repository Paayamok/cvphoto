# 浮生册框架交接

交付日期：2026-10-02。仓库：<https://github.com/Paayamok/cvphoto>。

## 接手顺序

1. 克隆仓库，使用 Android Studio 打开 `(PY)FuShengCe` 目录。
2. 配置 JDK 17、Android SDK 35；使用工程内 Gradle Wrapper 8.11.1。
3. 阅读 `README.md` 的模块职责、依赖方向和扩展入口，再阅读 `AGENTS.md` 的数据与视觉限制。
4. 执行 `./gradlew verifyFoundation`，发布前另执行 `./gradlew checkLicense generateLicenseReport`。

## 已交付框架

- `app` 负责启动、权限和顶层页面组装。
- 五个基础模块：`media`、`database`、`background`、`metadata`、`ui`。与六个 feature 和 app 合计 12 个模块。
- 六个业务模块：`home`、`gallery`、`viewer`、`search`、`albums`、`tasks`。
- 事项、搜索与卷册已拆出；共用收藏/题记/卷册存储和缩略图已下沉。
- 模块依赖守卫阻止反向依赖和循环；构建保留冻结资产与许可证校验。
- 查看器读取有效照片 GPS 后开放导航；没有坐标或未获必要权限时按钮禁用。

## 数据兼容要求

应用 ID 保持 `com.fushengce.app`。Room 数据库 `fushengce.db` 版本 3，保留 1→2→3 迁移。收藏、题记、卷册仍使用 `local-favorites`、`local-captions`、`local-albums`；不要改名或清库。原照片不因本机元数据整理而被移动、覆盖或删除。

## 继续开发的位置

改功能进入对应 `feature`；共享数据访问进入 `core`；顶层入口在 `app/FuShengCeApp`。实际页面切换使用状态与回调，历史 `AppDestination` 不是当前 NavHost。具体类名、调用关系与新增模块方式见 `README.md`。

产品规划见 `docs/design/v1/architecture/SCREEN_MAP.md`，按钮流程仍为草案。正式 3D、人物/事件索引、OCR 等后续业务尚未交付。地图、语音、提醒、历史数据升级与完整真机使用仍须专项验收。

## 交付范围

只包含运行源码、构建配置、必要资源、许可证与上述接手文档。测试代码/脚本/照片/报告、历史聊天与阶段记录、本机 SDK/模拟器/缓存、APK 均未上传。首页直接引用的内置占位图片保留，避免破坏现有页面。

旧仓库、旧 PR 与旧自动开发任务不参与本次交接。朋友在本仓库继续开发即可。
