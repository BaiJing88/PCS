# PCS 官网与管理系统

基于风格.mhtml风格设计的PCS（Player Credit System）官网和后台管理系统。

## 项目结构

```
website/
├── public/           # 前台官网
│   └── index.html    # 官网主页（Vue3 + Tailwind CSS）
├── admin/            # 后台管理系统
│   └── index.html    # 管理后台（Vue3 + Chart.js）
├── src/              # 源代码（可扩展）
│   ├── css/
│   └── js/
├── api/              # API接口文档
└── assets/           # 静态资源
    └── images/
```

## 功能特性

### 前台官网
- ✅ 响应式设计，支持移动端
- ✅ 黑白主题切换（自动保存偏好）
- ✅ 毛玻璃效果（Glassmorphism）
- ✅ 渐变背景和动画效果
- ✅ 完整的下载站功能
- ✅ 平滑滚动导航
- ✅ 系统架构展示

### 后台管理系统
- ✅ 仪表盘（实时数据统计）
- ✅ 在线玩家趋势图表（Chart.js）
- ✅ 服务器负载可视化
- ✅ 服务器管理（添加、查看、命令）
- ✅ 玩家管理（搜索、筛选、操作）
- ✅ 信用分进度条显示
- ✅ 批量操作功能
- ✅ 通知系统
- ✅ 黑白主题切换
- ✅ 响应式侧边栏

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **CSS框架**: Tailwind CSS
- **图标库**: Lucide Icons
- **图表库**: Chart.js
- **字体**: Inter + JetBrains Mono

## 使用说明

### 本地预览

由于使用了ES Modules和CORS限制，建议使用本地服务器打开：

```bash
# 使用 Python 3
python -m http.server 8080

# 或使用 Node.js
npx serve .

# 或使用 PHP
php -S localhost:8080
```

然后访问:
- 官网: http://localhost:8080/public/
- 后台: http://localhost:8080/admin/

### 部署

将 `website` 目录部署到任意静态网站托管服务：
- GitHub Pages
- Vercel
- Netlify
- 腾讯云 COS
- 阿里云 OSS

## 自定义配置

### 修改版本号
在 `public/index.html` 和 `admin/index.html` 中修改:
```javascript
const latestVersion = ref('1.0.1');
```

### 添加下载文件
在 `downloads` 数组中添加:
```javascript
{
    name: 'PCS-YourPlatform',
    description: '描述',
    version: '1.0.1',
    size: 'XXX KB',
    icon: 'icon-name',
    bgColor: 'bg-color-500',
    category: 'plugin',
    url: '/path/to/file.jar'
}
```

### 修改主题色
在 Tailwind 配置中修改:
```javascript
colors: {
    primary: {
        500: '#6366f1',  // 修改为主色调
        600: '#4f46e5',
    }
}
```

## 浏览器支持

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## 开源协议

AGPL-3.0 License
