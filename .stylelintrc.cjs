/** @type {import('stylelint').Config} */
module.exports = {
  // 继承标准配置和属性顺序配置
  extends: ['stylelint-config-standard', 'stylelint-config-recess-order'],

  // 插件
  plugins: ['stylelint-scss', 'stylelint-less'],

  rules: {
    // 允许空源文件
    'no-empty-source': null,

    // 不限制选择器中 id 的数量
    'selector-max-id': null,

    // 允许未知的属性（空的 ignoreProperties）
    'property-no-unknown': [true, { ignoreProperties: [] }],

    // 允许未知的 @规则
    'at-rule-no-unknown': null,

    // SCSS 特定的 @规则检查
    'scss/at-rule-no-unknown': true,

    // 属性顺序为空（使用默认）
    'order/properties-order': null,

    // 规则前不需要空行
    'rule-empty-line-before': null,

    // 声明前不需要空行
    'declaration-empty-line-before': null,

    // 颜色函数别名 notation
    'color-function-alias-notation': null,

    // 颜色函数 notation
    'color-function-notation': null,

    // Alpha 值 notation
    'alpha-value-notation': null,

    // 颜色十六进制长度
    'color-hex-length': null,

    // 值关键字大小写
    'value-keyword-case': null,
  },

  // 文件类型特定配置
  overrides: [
    // Less 文件
    {
      files: ['**/*.less'],
      customSyntax: 'postcss-less',
    },
    // SCSS 文件
    {
      files: ['**/*.scss'],
      customSyntax: 'postcss-scss',
    },
  ],

  // 忽略的文件
  ignoreFiles: [
    'node_modules/**/*',
    'dist/**/*',
    'build/**/*',
    'out/**/*',
    'backend/**/*',
    '*.png',
    '*.jpg',
    '*.jpeg',
    '*.gif',
    '*.ico',
    'assets/**/*',
  ],
}
