# VersionTracker

一个支持跟踪文件修改并自动记录修改历史的 IDEA 插件，将通过细粒度的跟踪保障您的代码安全，告别文件意外丢失的烦恼。

## 需要考虑的问题

- 如何查看文件内容：详见 Document 类与 VirtualDocument 类的文档，以及 IDEA 插件文档对于 [VFS](https://plugins.jetbrains.com/docs/intellij/virtual-file-system.html) 的说明，可以在 VersionTrackerListener 中找到榉简单的 DocumentListener 使用示例；
- 文件更改的类型：
  - 文件修改
  - 新增文件
  - 删除文件
- 何时记录更改：何时记录一次版本信息，待讨论；
    - 保存粒度可做区分：包括增量保存与快照保存；
    - 可采用的保存策略包括：
      - 按时间保存，每隔一段时间自动保存一次；
      - 按编辑行为保存，用户保存文件 (Ctrl-S) 及退出 IDE 时保存一次；
- 更改记录如何保存：待讨论，可以考虑直接使用 git 来保存相关信息；
- 如何查看更改：用户界面设计，鉴于我们需要实现两个基本功能点：查看修改记录和手动确认提交以将修改合并到原分支中，可以新增一个 Tool Window （见 VersionTrackerToolWindowFactory）和一个用于手动提交的 action 按钮。
  - 历史记录界面可能设计方案：
    - 查看修改记录时新开一个子界面，按版本-项目结构-修改内容的形式呈现修改记录；

## 项目结构说明

设置一个应用级监听器，用于在 IDEA 启动时初始化插件服务，具体说明见 AppOpenListener。

插件涉及的监听器等内容均在 VersionTrackerService 类中注册，action 在 plugin.xml 中注册，其余。
插件中的监听器需在 VersionTrackerService 类中注册，action 在 plugin.xml 的 actions 标签下进行注册，其余依文档依需要在 VersionTrackerService 类中或 plugin.xml 中注册。

## 存储策略

- 采用版本+增量的策略，保证插件性能与缓存体积
  - 增量保存按编辑保存，每次编辑后保存一份增量信息
  - 版本当增量保存数量到一定阈值后存为一个版本，版本做全量保存，或者当特定事件触发（用户手动保存、退出 IDE）时保存为一个版本
  - 出现新版本时在新分支上提交一个 commit
- 保存格式/数据结构
  - 按版本/增量-版本号-项目结构树-文件 diff 的格式存储
  - 用户界面按照相同层级进行显示
- git 相关 API

## 项目结构说明

### 监听器

- 编辑操作监听器：PsiTreeListener
- 文件保存/删除监听器：FileListener
- IDE 退出：ProjectListener（原 AppOpenListener）
- 定时器：VersionTrackerTimer

前两者可以拿到具体的文件句柄，可以较为方便地获得修改位置与信息。后两者无法拿到具体的被修改文件，需要先通过一些方法获得被修改过的文件。
