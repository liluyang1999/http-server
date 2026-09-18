# 实验三：HTTPS、离线构建与排错

## 构建做了什么

`scripts/build.ps1 -Test` 调用已有 javac/java/jar，使用仓库内 Gson。没有 Maven/Gradle 依赖解析，也没有下载命令。

编译使用 `--release 19 -encoding UTF-8 -Xlint:all -Werror`。测试通过后打包源码、网页与 web.properties，并复制运行依赖。编译临时目录在 finally 中清理，成品保留在 build/。

Java 测试失败会非零退出并停止打包；JAR 先在临时目录生成，打包成功后才替换成品，再检查启动脚本。构建失败后旧成品或本次待验成品可能仍存在，因此只能在当前构建明确成功后使用。打包固定 ZIP 时间戳；可连续构建后用 Get-FileHash 比较同环境产物。

端口必须为 0 到 65535；未传入时采用各入口默认值，显式 0 表示自动分配。HTTPS 参数只适用于 jdk 模式，Url 只适用于 client 模式，client 不接受 Port。不适用的参数会报错，避免误以为配置已经生效。

## 本地 HTTPS 演示

只使用已安装的 JDK keytool。旧仓库私钥不再读取。以下 PowerShell 7 命令生成仅当前进程环境持有的随机演示口令，不输出口令值：

```powershell
$env:HTTP_TLS_PASSWORD = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
.\scripts\new-demo-keystore.ps1
.\scripts\run.ps1 -Port 8081 -HttpsPort 8443 -KeyStore "$PWD\.demo-tls\server.p12"
```

证书有效期 7 天，SAN 包括 localhost 与 127.0.0.1，仅启用 TLS 1.2/1.3。脚本拒绝覆盖已有密钥，文件限于项目 .demo-tls/，不会把证书安装到系统信任库。

服务停止后可用 `Remove-Item Env:\HTTP_TLS_PASSWORD` 清除本终端的环境变量。随机口令若丢失，原演示密钥就不能再次使用；保留旧文件时可给脚本指定 .demo-tls/ 下的新文件名。

浏览器默认不信任自签名证书，这是预期行为。测试使用只信任当次临时证书的客户端，保留主机名校验；不使用“信任所有证书”或全局禁用 TLS 校验。它证明加密连接与身份匹配流程，不等于取得受公众信任的证书。

## JDK 配置边界

当前实测 JDK 25 中，maxReqTime/maxRspTime 单位为毫秒，默认设置各 10000，定时器检查有粒度。应用启动前设置这些属性，且不覆盖已有 -D 参数；其他 JDK 需另行验证。参考 [JDK 25 jdk.httpserver 模块文档](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/module-summary.html)。

Windows 本机的 JDK 曾在内部 Selector 建立连接时报告：

```text
Unable to establish loopback connection
Invalid argument: connect
```

查阅本机 JDK 源码后，项目脚本通过 `-Djdk.net.unixdomain.tmpdir=<项目的 build 目录>` 为 AF_UNIX 临时端点指定较短目录，实际请求恢复正常。它是启动进程级设置，没有修改系统环境或安装替代 JDK。直接运行 JAR 时也应带这个参数：

```powershell
java "-Djdk.net.unixdomain.tmpdir=$PWD\build" -jar .\build\http-server.jar --port 8081
```

## 常见故障

| 表现 | 检查与处理 |
| --- | --- |
| 找不到 java/javac/jar/keytool | 确认本机已有 JDK 的 bin 在当前终端 PATH；不要自动下载安装 |
| Address already in use | 使用 -Port 0 或另选端口，不要结束未知进程 |
| 找不到 Gson / web.properties | 重新成功构建；保留 JAR 和 lib/ 相对位置 |
| 证书口令错误或缺失 | 在当前终端设置 HTTP_TLS_PASSWORD，核对指定的是本地新证书 |
| 非法输入得到 400 | 看 docs/01-http-and-api.md，不应通过放宽 JSON 解析规避错误 |
| 源码改动后页面仍旧 | 停止旧实例，成功构建，再启动新 JAR |

启动失败时服务会关闭已创建的监听器与线程池。本机实测还覆盖了“HTTP 已启动、HTTPS 端口冲突”的路径。Ctrl+C 会触发关闭钩子；不要依赖强制结束进程来证明关闭逻辑正确。
