# 前端模块

- `user-frontend`：用户端，默认开发端口 `25173`。
- `admin-frontend`：管理端，默认开发端口 `25174`。

两个模块拥有各自的 `package.json`、依赖锁文件和环境配置，可以独立安装、构建和部署。

```bash
cd user-frontend
npm install
npm run dev
```

```bash
cd admin-frontend
npm install
npm run dev
```

也可以在当前目录使用聚合脚本：

```bash
npm run dev:user
npm run dev:admin
npm run build:uat:user
npm run build:uat:admin
```
