module.exports = {
    "**/*.{js,jsx,ts,tsx,vue}": [
        "pnpm eslint:check",
        "pnpm format:check"
    ],
    "**/*.{css,scss,less}": [
        "pnpm stylelint:check",
        "pnpm format:check"
    ],
    "backend/net-service/net-service-netty/**/*.java": () => [
        "pnpm net-service-netty:check"
    ],
    "backend/net-service/net-service-netty-client/**/*.java": () => [
        "pnpm net-service-netty-client:check"
    ]
};
