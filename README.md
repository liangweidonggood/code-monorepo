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
```

.
