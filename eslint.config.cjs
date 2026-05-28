const js = require('@eslint/js');
const tslint = require('typescript-eslint');
const pluginVue = require('eslint-plugin-vue');
const configPrettier = require('eslint-config-prettier');
const pluginPrettier = require('eslint-plugin-prettier');

// 🔌 引入全新的 React 插件
const pluginReact = require('eslint-plugin-react');
const pluginReactHooks = require('eslint-plugin-react-hooks');
const pluginReactRefresh = require('eslint-plugin-react-refresh');
const globals = require('globals');

/** @type {import('eslint').Linter.Config[]} */
module.exports = [
    // 1. 全局忽略
    {
        ignores: [
            '**/node_modules/**',
            '**/dist/**',
            '**/backend/**',
            '**/.gradle/**',
            '**/build/**',
        ],
    },

    // 2. 基础 JS 规范
    js.configs.recommended,

    // 3. TypeScript 规范
    ...tslint.config(
        tslint.configs.recommended,
        {
            files: ['**/*.{ts,tsx}'],
            languageOptions: {
                parser: tslint.parser,
                parserOptions: {
                    ecmaVersion: 'latest',
                    sourceType: 'module',
                },
            },
            rules: {
                '@typescript-eslint/no-explicit-any': 'warn',
                '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
            },
        }
    ),

    // ==========================================
    // ⚛️ 新增配置 4：React 专属规范控制区
    // ==========================================
    {
        // 精准管辖大仓库下所有包含 React 组件的文件
        files: ['**/*.{js,jsx,ts,tsx}'],
        plugins: {
            'react': pluginReact,
            'react-hooks': pluginReactHooks,
            'react-refresh': pluginReactRefresh,
        },
        languageOptions: {
            parserOptions: {
                ecmaFeatures: {
                    jsx: true, // 开启 JSX 语法解析
                },
            },
            globals: {
                ...globals.browser, // 注入浏览器全局变量（如 window, document）
            },
        },
        rules: {
            // 自动引入常用的 React 推荐规则
            ...pluginReact.configs.recommended.rules,
            ...pluginReactHooks.configs.recommended.rules,

            // 现代 React (v17+) 不需要你在每个文件顶部手动 import React from 'react' 了，关掉这个老旧限制
            'react/react-in-jsx-scope': 'off',

            // 允许在开发阶段组件热更新
            'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
        },
        settings: {
            react: {
                version: 'detect', // 💡 灵魂配置：让 ESLint 自动去子项目里探测 React 的具体版本（v18 还是 v19）
            },
        },
    },

    // 5. Vue 规范（依然保留，实现 React 和 Vue 在同一个大仓库下和谐共存！）
    ...pluginVue.configs['flat/recommended'],
    {
        files: ['**/*.vue'],
        languageOptions: {
            parserOptions: {
                parser: tslint.parser,
                sourceType: 'module',
            },
        },
        rules: {
            'vue/multi-word-component-names': 'off',
        },
    },

    // 6. Prettier 冲突消除与整合（必须放在最后）
    {
        plugins: {
            prettier: pluginPrettier,
        },
        rules: {
            'prettier/prettier': 'error',
        },
    },
    configPrettier,
];
