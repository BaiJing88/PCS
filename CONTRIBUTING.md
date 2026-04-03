# 🤝 Contributing to PCS

首先，感谢您考虑为 PCS 项目做出贡献！🎉

## 📋 贡献方式

### 🐛 报告 Bug

如果您发现了 Bug，请通过 [GitHub Issues](../../issues) 提交，并包含以下信息：

- 问题的清晰描述
- 重现步骤
- 期望行为与实际行为
- 环境信息（OS, Java版本, Minecraft版本等）
- 相关日志

### 💡 建议新功能

我们欢迎新功能建议！请通过 [GitHub Issues](../../issues) 提交，并描述：

- 功能的用途
- 预期的实现方式
- 可能的替代方案

### 🔧 提交代码

#### 开发流程

1. **Fork 项目**
   ```bash
   git clone https://github.com/BaiJing88/Minecraft-Player-Credit-System.git
   cd PCS
   ```

2. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/bug-description
   ```

3. **进行更改**
   - 遵循现有的代码风格
   - 添加必要的注释
   - 更新相关文档

4. **提交更改**
   ```bash
   git add .
   git commit -m "feat: add some feature"
   ```

5. **Push 到 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **创建 Pull Request**

#### 提交信息规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

- `feat:` 新功能
- `fix:` Bug修复
- `docs:` 文档更新
- `style:` 代码格式（不影响代码运行的变动）
- `refactor:` 重构
- `perf:` 性能优化
- `test:` 测试相关
- `chore:` 构建过程或辅助工具的变动

示例：
```
feat: add mute duration configuration option
fix: resolve WebSocket reconnection issue
docs: update installation guide
```

## 📝 代码规范

### Java 代码风格

- 使用 4 空格缩进
- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 添加适当的 Javadoc 注释

示例：
```java
/**
 * 处理玩家投票请求
 * 
 * @param playerUuid 玩家UUID
 * @param action 投票操作类型
 * @return 投票会话ID
 */
public String handleVote(String playerUuid, VoteAction action) {
    // 实现代码
}
```

### 文件头版权

所有 Java 文件必须包含以下版权头：

```java
/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */
```

## ⚖️ 法律声明

### 许可证

通过贡献代码，您同意您的贡献将在 **AGPL v3** 许可证下发布。

### 版权

- 您保留对您贡献的版权
- 您授予项目维护者使用、修改和分发您贡献的权利

## 💬 沟通渠道

- 📧 QQ: 1782307393
- 🐛 [GitHub Issues](../../issues)
- 💭 [GitHub Discussions](../../discussions)

## 🙏 感谢

再次感谢您的贡献！每一份帮助都让 PCS 变得更好。

---

<div align="center">

**Happy Coding!** 🚀

</div>
