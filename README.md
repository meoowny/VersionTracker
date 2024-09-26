# VersionTracker

一个支持跟踪文件修改并自动记录修改历史的 IDEA 插件，将通过细粒度的跟踪保障您的代码安全，告别文件意外丢失的烦恼。

## 需要考虑的问题

- 如何查看文件内容：详见 Document 类与 VirtualDocument 类的文档，以及 IDEA 插件文档对于 [VFS](https://plugins.jetbrains.com/docs/intellij/virtual-file-system.html) 的说明，可以在 VersionTrackerListener 中找到榉简单的 DocumentListener 使用示例；
- 何时记录更改：何时记录一次版本信息，待讨论；
- 更改记录如何保存：待讨论，可以考虑直接使用 git 来保存相关信息；
- 如何查看更改：用户界面设计，鉴于我们需要实现两个基本功能点：查看修改记录和手动确认提交以将修改合并到原分支中，可以新增一个 Tool Window （见 VersionTrackerToolWindowFactory）和一个用于手动提交的 action 按钮。

## 项目结构说明

设置一个应用级监听器，用于在 IDEA 启动时初始化插件服务，具体说明见 AppOpenListener。

插件涉及的监听器等内容均在 VersionTrackerService 类中注册，action 在 plugin.xml 中注册，其余。
插件中的监听器需在 VersionTrackerService 类中注册，action 在 plugin.xml 的 actions 标签下进行注册，其余依文档依需要在 VersionTrackerService 类中或 plugin.xml 中注册。
