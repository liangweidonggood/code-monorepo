/** @type {import('@commitlint/types').UserConfig} */
module.exports = {
  // 继承 Conventional Commits 标准配置
  extends: ['@commitlint/config-conventional'],

  rules: {
    // 正文前必须有空行
    'body-leading-blank': [2, 'always'],

    // 页脚前建议有空行
    'footer-leading-blank': [1, 'always'],

    // 标题行最大长度 108 字符
    'header-max-length': [2, 'always', 108],

    // 主题不能为空
    'subject-empty': [2, 'never'],

    // 类型不能为空
    'type-empty': [2, 'never'],

    // 主题大小写不限制
    'subject-case': [0],

    // 允许的提交类型
    'type-enum': [
      2,
      'always',
      [
        'feat', // 新功能
        'fix', // 修复 bug
        'perf', // 性能优化
        'style', // 代码格式（不影响功能）
        'docs', // 文档更新
        'test', // 测试相关
        'refactor', // 重构
        'build', // 构建系统或依赖变更
        'ci', // CI/CD 配置变更
        'chore', // 其他杂项
        'revert', // 回退提交
        'wip', // 工作进行中
        'workflow', // 工作流相关
        'types', // 类型定义变更
        'release', // 版本发布
      ],
    ],
  },
}
