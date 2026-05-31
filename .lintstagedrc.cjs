module.exports = {
    "**/*.{js,jsx,ts,tsx,vue}": [
        "pnpm eslint:check",
        "pnpm format:check"
    ],
    "**/*.{css,scss,less}": [
        "pnpm stylelint:check",
        "pnpm format:check"
    ],
    "**/*.java": () => [
        "pnpm net-service-netty:check",
        "pnpm net-service-netty-client:check"
    ]
};
