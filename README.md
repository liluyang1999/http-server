# http-server：从请求字节到 HTTPS 的 Java 教学项目

项目保留两条学习路径：

| 入口 | 用途 | 默认地址 |
| --- | --- | --- |
| JDK `HttpServer` / `HttpsServer` | 算术 API、JSON 校验、浏览器实验、TLS 与生命周期 | http://127.0.0.1:8081 |
| `server.Server` | 亲手阅读 HTTP 请求、解析查询参数、调用 Controller | http://127.0.0.1:9999/index?username=My%20Sweetheart |

两者均只监听本机。Socket 版本实现有限的 HTTP/1.1 GET 子集。它们用于理解协议与工程边界；生产服务还需要成熟服务器、依赖治理与完整的协议测试。

## 五分钟开始

使用本机已有的 JDK 和 PowerShell 7，不下载构建器或依赖。仓库原有 `lib/gson-2.8.9.jar` 用于 JSON 解析。

本次实测环境：Windows、Oracle JDK 25.0.2。源码按照原项目的 Java 19 级别编译（`--release 19`）；**Java 19 等其他运行时尚未实测**，特别是 JDK HTTP 超时属性。

```powershell
Set-Location D:\Projects\http-server
.\scripts\build.ps1 -Test
.\scripts\run.ps1
```

浏览器打开 http://127.0.0.1:8081，可选择 GET/POST 并查看真实响应。终端按 Ctrl+C 关闭服务。

```powershell
Invoke-RestMethod http://127.0.0.1:8081/add/7/8
Invoke-RestMethod http://127.0.0.1:8081/calculate -Method Post -ContentType application/json -Body '{"operation":"divide","arguments":[9,3]}'
```

切换到 Socket 实验，在两个终端分别运行：

```powershell
.\scripts\run.ps1 -Mode socket
.\scripts\run.ps1 -Mode client
```

端口可以通过 `-Port 9000` 调整。传入 `0` 让操作系统分配空闲端口，实际地址会打印到终端。客户端自定义 URL 使用 `-Url 'http://127.0.0.1:9000/index?username=My%20Sweetheart'`。

## 学习路线

1. [一次请求如何变成响应、算术 API 契约](docs/01-http-and-api.md)
2. [Socket 实现、线程与资源所有权](docs/02-socket-and-lifecycle.md)
3. [HTTPS、启动、离线打包与排错](docs/03-tls-and-operations.md)
4. [修复证据、测试边界与练习](docs/04-review-and-exercises.md)

`src/` 是源码，`tests/` 是只依赖 JDK 的真实网络测试，`scripts/` 是离线构建与启动入口。构建产物为 `build/http-server.jar` 和 `build/lib/gson-2.8.9.jar`，分发时需同时保留这两个文件的相对位置。

## 旧文件与兼容性

根目录的 `HTTP.jar`、旧 `.p12` / `.cer` 文件保留为历史材料，**不参与新构建、不用于新服务**。旧私钥已经随仓库公开，不能作为有效的身份凭证；新 TLS 演示须本地生成短期证书。

原有四种运算及大小写兼容保留。现在 HTTP 正文不再夹带第二份状态行和响应头；POST 正文变为可直接解析的 JSON。未知路由返回 404，非法输入返回 400。HTTPS 改为显式启用，关闭后会释放监听端口与工作线程。

旧 Socket 客户端的自定义单行格式改为标准 HTTP 请求；手写 `Server` 现在实现 `AutoCloseable`，构造即开始监听。示例 DAO 仍为固定演示数据，不是登录或数据库认证服务。

后续改动请先复现问题，新增行为测试，运行 `build.ps1 -Test`，再核对相关文档。项目没有格式化器、外部静态分析器或远端 CI，本次未引入新工具。
