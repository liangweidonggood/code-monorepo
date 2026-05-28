# 说明

这是一个多语言单仓学习项目

java21

node22.21.1(pnpm)

go1.26.2

rust1.95.0

python3.13.7

# 项目构建

初始化pnpm

```bash
pnpm init


# package.json
{
  "name": "code-monorepo",
  "version": "1.0.0",
  "description": "这是一个多语言单仓学习项目",
  "type": "module",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "keywords": ["monorepo"],
  "author": "liang wei dong",
  "license": "ISC",
  "packageManager": "pnpm@10.32.1"
}

```

新建pnpm-workspace.yaml

```yaml
packages:
  - frontend/**
  - '!**/node_modules'
  - '!**/target'
  - '!**/dist'
  - '!**/build'
  - '!frontend/**/src-tauri'
  - '!backend/**'

onlyBuiltDependencies:
  - esbuild
```

代码提交规范

```bash
pnpm add -w -D husky

# package.json中
"scripts": {
  "prepare": "husky"
}

pnpm add -w -D @commitlint/cli @commitlint/config-conventional

pnpm prepare

# commit-msg内容如下
pnpm commitlint --edit \$1

# 增加.commitlintrc.cjs
```

提交规范，支持以下类型：

- `feat`: 增加新功能
- `fix`: 修复问题/BUG
- `style`: 代码风格相关
- `perf`: 优化/性能提升
- `refactor`: 重构
- `revert`: 撤销修改
- `test`: 测试相关
- `docs`: 文档/注释
- `chore`: 依赖更新/脚手架配置修改等
- `workflow`: 工作流改进
- `ci`: 持续集成
- `types`: 类型定义文件更改
- `wip`: 开发中

示例：

```
feat: 全局配置
```

代码校验和格式化

```bash
pnpm add -w -D lint-staged eslint prettier

pnpm add -w -D @eslint/js typescript-eslint eslint-plugin-vue eslint-config-prettier eslint-plugin-prettier
pnpm add -w -D eslint-plugin-react eslint-plugin-react-hooks eslint-plugin-react-refresh globals

# 添加eslint.config.cjs .prettierrc.cjs .prettierignore
# package.json对应修改
```

样式风格校验

```bash
pnpm add -w -D stylelint postcss-less stylelint-config-recess-order stylelint-config-standard stylelint-less stylelint-scss

# 添加 .stylelintrc.cjs .stylelintignore
# package.json对应修改
```







.
