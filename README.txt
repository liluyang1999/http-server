本项目的当前教学说明、离线运行步骤、接口契约和验证结果请阅读 README.md 与 docs/。

根目录 HTTP.jar、server.p12、client.p12 与证书是历史文件，不参与当前构建。
请使用 scripts/build.ps1 -Test 从源码构建；新 HTTPS 演示使用本机 keytool 生成证书。
