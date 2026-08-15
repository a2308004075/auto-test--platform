# postman-platform 开发环境准备 SOP

> 版本：v1.1  
> 适用对象：编程初学者 / 项目新成员  
> 操作系统：Windows 11  
> 开发工具：IntelliJ IDEA（前端 + 后端共用）

---

## 目录

- [一、总览：你需要准备什么](#一总览你需要准备什么)
- [二、基础软件安装](#二基础软件安装)
  - [2.1 JDK 1.8 验证](#21-jdk-18-验证)
  - [2.2 Node.js 安装](#22-nodejs-安装)
  - [2.3 Git 安装与配置](#23-git-安装与配置)
  - [2.4 IntelliJ IDEA 安装与配置](#24-intellij-idea-安装与配置)
- [三、中间件安装](#三中间件安装)
  - [选择安装方式](#选择安装方式)
  - [方式 A：Docker 容器化安装（需管理员权限）](#方式-adocker-容器化安装需管理员权限)
    - [A.1 Docker Desktop 安装](#a1-docker-desktop-安装)
    - [A.2 MySQL 8.0 启动](#a2-mysql-80-启动)
    - [A.3 Redis 7.x 启动](#a3-redis-7x-启动)
    - [A.4 RabbitMQ 启动](#a4-rabbitmq-启动)
    - [A.5 Nacos 启动](#a5-nacos-启动)
    - [A.6 XXL-Job 启动（可选，Phase 3 再用）](#a6-xxl-job-启动可选phase-3-再用)
    - [A.7 一键启动所有中间件](#a7-一键启动所有中间件)
  - [方式 B：原生 Windows 安装（无需管理员权限）](#方式-b原生-windows-安装无需管理员权限)
    - [B.1 MySQL 8.0 安装](#b1-mysql-80-安装)
    - [B.2 Redis 安装](#b2-redis-安装)
    - [B.3 RabbitMQ 安装](#b3-rabbitmq-安装)
    - [B.4 Nacos 安装](#b4-nacos-安装)
    - [B.5 各中间件访问信息汇总](#b5-各中间件访问信息汇总)
- [四、后端项目工程搭建](#四后端项目工程搭建)
  - [4.1 Maven 多模块项目结构](#41-maven-多模块项目结构)
  - [4.2 父 POM 配置](#42-父-pom-配置)
  - [4.3 子模块创建顺序](#43-子模块创建顺序)
  - [4.4 IDEA 后端项目导入](#44-idea-后端项目导入)
  - [4.5 验证后端启动](#45-验证后端启动)
- [五、前端项目工程搭建](#五前端项目工程搭建)
  - [5.1 使用 Vite 创建 Vue 3 项目](#51-使用-vite-创建-vue-3-项目)
  - [5.2 安装核心依赖](#52-安装核心依赖)
  - [5.3 前端项目目录结构](#53-前端项目目录结构)
  - [5.4 IDEA 前端项目导入](#54-idea-前端项目导入)
  - [5.5 验证前端启动](#55-验证前端启动)
- [六、开发规范与编码配置](#六开发规范与编码配置)
- [七、环境验证清单](#七环境验证清单)
- [八、常见问题排查](#八常见问题排查)

---

## 一、总览：你需要准备什么

本项目采用**前后端分离**架构，开发环境分为两大部分：

| 类别 | 技术栈 | 说明 |
|------|--------|------|
| **前端** | Vue 3 + TypeScript + Vite + Ant Design Vue 4.x | 独立的 SPA 工程，用 IDEA 打开 |
| **后端** | Java 1.8 + Spring Boot 2.7 + Spring Cloud 2021.x 微服务 | Maven 多模块工程，用 IDEA 打开 |
| **中间件** | MySQL 8.0 + Redis 7.x + RabbitMQ + Nacos | Docker 容器 或 原生 Windows 安装（二选一） |

**你需要依次完成以下步骤：**

```
① 安装基础软件（JDK、Node.js、Git、IDEA）
② 安装中间件（Docker 方式 或 原生 Windows 方式，二选一）
③ 搭建后端 Maven 多模块工程
④ 搭建前端 Vue 3 工程
⑤ 配置开发规范（编码、格式化等）
⑥ 验证全部环境就绪
```

---

## 二、基础软件安装

### 2.1 JDK 1.8 验证

后端使用 Java 1.8 版本，你的系统中已安装 JDK 1.8.0_451（`C:\Program Files\Java\jdk-1.8`）。

**步骤：**

1. **验证安装**：打开 PowerShell，执行：
   ```powershell
   java -version
   javac -version
   ```
   应显示 `java version "1.8.0_451"` 或类似版本号。

2. **确认 JAVA_HOME 环境变量**：
   ```powershell
   echo $env:JAVA_HOME
   ```
   应显示 `C:\Program Files\Java\jdk-1.8`。如果为空，需要手动配置：
   - 右键「此电脑」→「属性」→「高级系统设置」→「环境变量」
   - 新建系统变量：
     ```
     变量名：JAVA_HOME
     变量值：C:\Program Files\Java\jdk-1.8
     ```
   - 编辑 `Path` 变量，添加：
     ```
     %JAVA_HOME%\bin
     ```

---

### 2.2 Node.js 安装

前端使用 Vue 3 + Vite，需要 Node.js 运行环境。

**步骤：**

1. 访问 Node.js 官网下载 **LTS 版本**（推荐 20.x 或更新）：
   ```
   https://nodejs.org/
   ```
   选择 `Windows Installer (.msi)` 下载并安装。

2. 安装时勾选「Automatically install necessary tools」选项。

3. **验证安装**：打开 PowerShell，执行：
   ```powershell
   node -v
   npm -v
   ```
   应分别显示 `v20.x.x` 和 `10.x.x` 或更高版本号。

4. **配置 npm 国内镜像**（加速依赖下载）：
   ```powershell
   npm config set registry https://registry.npmmirror.com
   ```

5. **安装 pnpm**（推荐的包管理器，速度更快）：
   ```powershell
   npm install -g pnpm
   pnpm -v
   ```

---

### 2.3 Git 安装与配置

你的系统中已经安装了 Git（`C:\Program Files\Git\cmd\git.exe`），只需确认配置即可。

**步骤：**

1. **验证安装**：
   ```powershell
   git --version
   ```

2. **配置用户信息**（如未配置过）：
   ```powershell
   git config --global user.name "你的姓名"
   git config --global user.email "你的邮箱@example.com"
   ```

3. **配置换行符**（Windows 开发必备）：
   ```powershell
   git config --global core.autocrlf true
   ```

---

### 2.4 IntelliJ IDEA 安装与配置

你的系统中已安装 IDEA 2025.2.1，以下是项目所需的配置调整。

#### 2.4.1 安装必要插件

打开 IDEA → `File` → `Settings` → `Plugins`，搜索并安装以下插件：

| 插件名 | 用途 | 必要性 |
|--------|------|--------|
| **Vue.js** | Vue 3 前端开发支持（语法高亮、代码补全） | 必装 |
| **Lombok** | 简化 Java 实体类代码 | 必装 |
| **MyBatisX** | MyBatis Mapper 跳转与代码补全 | 必装 |
| **Rainbow Brackets** | 彩色括号匹配，提升代码可读性 | 推荐 |
| **.env files support** | 环境变量文件语法高亮 | 推荐 |

#### 2.4.2 配置 JDK 1.8

1. 打开 IDEA → `File` → `Project Structure` → `SDKs`
2. 点击 `+` → `Add JDK`，选择 JDK 1.8 安装目录（`C:\Program Files\Java\jdk-1.8`）
3. 在 `Project` 选项卡中，将 `SDK` 设置为 JDK 1.8，`Language level` 选择 `8`

#### 2.4.3 配置文件编码（重要！）

1. 打开 IDEA → `File` → `Settings` → `Editor` → `File Encodings`
2. 将以下三项全部设置为 **UTF-8**：
   - `Global Encoding`：UTF-8
   - `Project Encoding`：UTF-8
   - `Default encoding for properties files`：UTF-8
3. 勾选 `Transparent native-to-ascii conversion`

#### 2.4.4 配置 Maven

IDEA 自带 Maven，但建议使用外部 Maven 以获得更好的控制：

1. 下载 Maven：https://maven.apache.org/download.cgi （选择 `Binary zip archive`）
2. 解压到 `D:\tools\apache-maven-3.9.x`（路径不要有中文和空格）
3. 配置环境变量：
   ```
   变量名：MAVEN_HOME
   变量值：D:\tools\apache-maven-3.9.x
   
   Path 中添加：
   %MAVEN_HOME%\bin
   ```
4. 编辑 Maven 配置文件 `D:\tools\apache-maven-3.9.x\conf\settings.xml`，添加阿里云镜像（加速依赖下载）：
   ```xml
   <mirrors>
     <mirror>
       <id>aliyun</id>
       <name>Aliyun Maven Mirror</name>
       <url>https://maven.aliyun.com/repository/public</url>
       <mirrorOf>central</mirrorOf>
     </mirror>
   </mirrors>
   ```
5. 在 IDEA 中配置：`File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
   - `Maven home path`：`D:\tools\apache-maven-3.9.x`
   - `User settings file`：`D:\tools\apache-maven-3.9.x\conf\settings.xml`（勾选 Override）
   - `Local repository`：`D:\tools\maven-repo`（勾选 Override，自定义仓库路径）

6. **验证 Maven**：
   ```powershell
   mvn -version
   ```
   应显示 Maven 版本号和 Java 1.8 信息。

---

## 三、中间件安装

本项目依赖 MySQL、Redis、RabbitMQ、Nacos 等中间件。提供两种安装方式，请根据自身情况选择其一：

### 选择安装方式

| | 方式 A：Docker 容器化 | 方式 B：原生 Windows 安装 |
|------|----------------------|-------------------------|
| **管理员权限** | ✅ 需要 | ❌ 不需要 |
| **WSL 2** | ✅ 需要 | ❌ 不需要 |
| **安装难度** | 低（一条命令搞定） | 中（需逐个下载安装） |
| **管理便利性** | 高（统一启停、易重置） | 一般（各自独立运行） |
| **推荐场景** | 有管理员权限、长期开发 | 无管理员权限、受限环境 |

> **如果你没有管理员权限或无法开启 WSL，请直接跳到 [方式 B：原生 Windows 安装](#方式-b原生-windows-安装无需管理员权限)。**

---

## 方式 A：Docker 容器化安装（需管理员权限）

以下使用 **Docker** 统一管理中间件，避免在本机逐一安装，干净且易于重置。

### A.1 Docker Desktop 安装

**步骤：**

1. 访问 Docker 官网下载 Docker Desktop for Windows：
   ```
   https://www.docker.com/products/docker-desktop/
   ```

2. 安装前确保：
   - Windows 11 已启用 **WSL 2**（Windows Subsystem for Linux 2）
   - 如未启用，在 PowerShell（管理员）执行：
     ```powershell
     wsl --install
     ```
   - 安装完成后**重启电脑**。

3. 运行 Docker Desktop 安装程序，按提示完成安装。

4. 启动 Docker Desktop，等待左下角显示绿色 `Engine running` 状态。

5. **验证安装**：
   ```powershell
   docker --version
   docker compose version
   ```

6. **配置 Docker 国内镜像加速**（推荐）：
   - 打开 Docker Desktop → `Settings` → `Docker Engine`
   - 在 JSON 配置中添加：
     ```json
     {
       "registry-mirrors": [
         "https://mirror.ccs.tencentyun.com",
         "https://docker.mirrors.ustc.edu.cn"
       ]
     }
     ```
   - 点击 `Apply & Restart`。

---

### A.2 MySQL 8.0 启动

在项目根目录 `D:\develop\postman-platform` 下创建 `docker` 文件夹，用于存放 Docker 配置。

先单独启动 MySQL 验证：

```powershell
docker run -d `
  --name pp-mysql `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=pp2024 `
  -e MYSQL_DATABASE=postman_platform `
  -v pp-mysql-data:/var/lib/mysql `
  mysql:8.0
```

**连接验证**：
```powershell
docker exec -it pp-mysql mysql -uroot -ppp2024 -e "SELECT VERSION();"
```

应显示 MySQL 8.0.x 版本信息。

> **说明**：`pp2024` 是开发环境密码，仅用于本地开发，请勿用于生产环境。

---

### A.3 Redis 7.x 启动

```powershell
docker run -d `
  --name pp-redis `
  -p 6379:6379 `
  -v pp-redis-data:/data `
  redis:7-alpine
```

**连接验证**：
```powershell
docker exec -it pp-redis redis-cli PING
```

应返回 `PONG`。

---

### A.4 RabbitMQ 启动

```powershell
docker run -d `
  --name pp-rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=admin `
  -e RABBITMQ_DEFAULT_PASS=admin123 `
  -v pp-rabbitmq-data:/var/lib/rabbitmq `
  rabbitmq:3-management-alpine
```

**验证**：打开浏览器访问 `http://localhost:15672`，使用 `admin / admin123` 登录管理界面。

> 端口说明：`5672` 是 AMQP 协议端口（程序连接用），`15672` 是 Web 管理界面端口。

---

### A.5 Nacos 启动

Nacos 是微服务的注册中心和配置中心，所有后端服务都依赖它。

> **重要**：Nacos 需要连接 MySQL 存储配置数据。首次启动前，需要先初始化 Nacos 数据库：
> 1. 下载 Nacos SQL 脚本：https://github.com/alibaba/nacos/blob/2.2.3/distribution/conf/mysql-schema.sql
> 2. 在 MySQL 中创建 `nacos_config` 数据库并执行该 SQL 脚本：
>    ```powershell
>    docker exec -it pp-mysql mysql -uroot -ppp2024 -e "CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4;"
>    ```
> 3. 将下载的 `mysql-schema.sql` 复制到容器中并执行：
>    ```powershell
>    docker cp mysql-schema.sql pp-mysql:/tmp/
>    docker exec -it pp-mysql mysql -uroot -ppp2024 nacos_config -e "source /tmp/mysql-schema.sql;"
>    ```

初始化完成后，启动 Nacos：

```powershell
docker run -d `
  --name pp-nacos `
  -p 8848:8848 `
  -p 9848:9848 `
  -e MODE=standalone `
  -e MYSQL_SERVICE_HOST=host.docker.internal `
  -e MYSQL_SERVICE_PORT=3306 `
  -e MYSQL_SERVICE_USER=root `
  -e MYSQL_SERVICE_PASSWORD=pp2024 `
  -e MYSQL_SERVICE_DB_NAME=nacos_config `
  nacos/nacos-server:v2.2.3
```

**验证**：打开浏览器访问 `http://localhost:8848/nacos`，使用 `nacos / nacos` 登录。

---

### A.6 XXL-Job 启动（可选，Phase 3 再用）

XXL-Job 用于分布式定时调度（测试计划定时执行），在项目 Phase 3 阶段才需要。现阶段可以先不部署。

```powershell
# Phase 3 时再执行
docker run -d `
  --name pp-xxl-job `
  -p 8090:8080 `
  -e PARAMS="--spring.datasource.url=jdbc:mysql://host.docker.internal:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai --spring.datasource.username=root --spring.datasource.password=pp2024" `
  xuxueli/xxl-job-admin:2.4.1
```

---

### A.7 一键启动所有中间件

为了方便日常开发，在项目中创建统一的 Docker Compose 文件，一键启停所有中间件。

在项目根目录下创建 `docker/docker-compose.yml`：

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: pp-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: pp2024
      MYSQL_DATABASE: postman_platform
      TZ: Asia/Shanghai
    volumes:
      - pp-mysql-data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: pp-redis
    ports:
      - "6379:6379"
    volumes:
      - pp-redis-data:/data

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: pp-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
    volumes:
      - pp-rabbitmq-data:/var/lib/rabbitmq

  nacos:
    image: nacos/nacos-server:v2.2.3
    container_name: pp-nacos
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      MYSQL_SERVICE_HOST: host.docker.internal
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: pp2024
      MYSQL_SERVICE_DB_NAME: nacos_config
    depends_on:
      - mysql

volumes:
  pp-mysql-data:
  pp-redis-data:
  pp-rabbitmq-data:
```

**一键启动**：
```powershell
cd D:\develop\postman-platform\docker
docker compose up -d
```

**一键停止**：
```powershell
cd D:\develop\postman-platform\docker
docker compose down
```

**查看运行状态**：
```powershell
docker compose ps
```

**各中间件访问信息汇总**：

| 中间件 | 地址 | 端口 | 账号 / 密码 |
|--------|------|------|-------------|
| MySQL | localhost | 3306 | root / pp2024 |
| Redis | localhost | 6379 | 无密码 |
| RabbitMQ 管理界面 | http://localhost:15672 | 15672 | admin / admin123 |
| Nacos 控制台 | http://localhost:8848/nacos | 8848 | nacos / nacos |

---

## 方式 B：原生 Windows 安装（无需管理员权限）

以下所有中间件均采用**下载 ZIP + 解压运行**的方式安装，**无需管理员权限**，无需 Docker / WSL。所有工具统一安装到 `D:\tools\` 目录下（路径中不要有中文和空格）。

> **约定**：MySQL root 密码统一为 `pp2024`，与方式 A 保持一致，便于后端配置文件共用。

---

### B.1 MySQL 8.0 安装

#### 第 1 步：下载

访问 MySQL Community Server 下载页面：

```
https://dev.mysql.com/downloads/mysql/
```

- 选择 `Windows (x86, 64-bit), ZIP Archive`
- 下载 8.0.x 版本（如 `mysql-8.0.xx-winx64.zip`）
- 文件大小约 200MB+

> **免登录提示**：页面底部有 `No thanks, just start my download.` 链接，点击即可跳过 Oracle 账号登录。

#### 第 2 步：解压

将 ZIP 解压到 `D:\tools\mysql-8.0`。解压后目录结构大致如下：

```
D:\tools\mysql-8.0\
├── bin\
├── lib\
├── share\
├── LICENSE
├── README
└── ...
```

#### 第 3 步：创建配置文件

在 `D:\tools\mysql-8.0\` 下新建 `my.ini` 文件，写入以下内容：

```ini
[mysqld]
port=3306
basedir=D:/tools/mysql-8.0
datadir=D:/tools/mysql-8.0/data
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-authentication-plugin=mysql_native_password

[client]
port=3306
default-character-set=utf8mb4
```

> **注意**：路径中使用正斜杠 `/`，不要用反斜杠 `\`。

#### 第 4 步：初始化数据库

打开 PowerShell，执行：

```powershell
D:\tools\mysql-8.0\bin\mysqld --initialize-insecure --console
```

该命令会：
- 自动创建 `data` 目录
- 初始化系统数据库
- 创建 root 用户，**初始密码为空**

> 终端输出的最后一行会显示临时信息，例如 `root@localhost` 并提示密码为空。这是正常的。

#### 第 5 步：设置 root 密码

先启动 MySQL（见第 6 步），然后在另一个 PowerShell 窗口中执行：

```powershell
D:\tools\mysql-8.0\bin\mysql -uroot --skip-password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'pp2024';"
```

#### 第 6 步：启动 MySQL

```powershell
D:\tools\mysql-8.0\bin\mysqld --console
```

> **说明**：此命令在前台运行 MySQL，窗口关闭则 MySQL 停止。建议开一个独立的 PowerShell 窗口运行。
>
> **停止 MySQL**：在另一个 PowerShell 窗口执行：
> ```powershell
> D:\tools\mysql-8.0\bin\mysqladmin -uroot -ppp2024 shutdown
> ```

#### 第 7 步：验证连接

打开另一个 PowerShell 窗口（MySQL 保持运行），执行：

```powershell
D:\tools\mysql-8.0\bin\mysql -uroot -ppp2024 -e "SELECT VERSION();"
```

应显示 `8.0.x` 版本信息。

#### 创建项目数据库

```powershell
D:\tools\mysql-8.0\bin\mysql -uroot -ppp2024 -e "CREATE DATABASE IF NOT EXISTS postman_platform DEFAULT CHARACTER SET utf8mb4;"
```

---

### B.2 Redis 安装

Redis 官方不提供 Windows 原生版本。推荐使用社区维护的 Windows 移植版。

#### 第 1 步：下载

从以下地址下载 Redis for Windows（社区移植版）：

```
https://github.com/tporadowski/redis/releases
```

- 选择最新稳定版（如 5.0.14.1 或更新）
- 下载 `Redis-x64-x.x.x.zip`（ZIP 格式，非 MSI）

> **备选方案**：如果上述链接不可用，也可以使用 **Memurai**（Redis 兼容的 Windows 原生服务器）：
> ```
> https://www.memurai.com/get-memurai
> ```
> Memurai Developer 版免费，API 完全兼容 Redis，可无缝替代。

#### 第 2 步：解压

将 ZIP 解压到 `D:\tools\redis`。

#### 第 3 步：启动 Redis

```powershell
D:\tools\redis\redis-server.exe
```

> Redis 默认监听 `localhost:6379`，无需额外配置。如需修改端口，可编辑同目录下的 `redis.windows.conf` 文件。
>
> 同样建议开一个独立 PowerShell 窗口运行。

#### 第 4 步：验证连接

在另一个 PowerShell 窗口执行：

```powershell
D:\tools\redis\redis-cli.exe PING
```

应返回 `PONG`。

---

### B.3 RabbitMQ 安装

RabbitMQ 依赖 **Erlang 运行时环境**，需要先安装 Erlang，再安装 RabbitMQ。两者都使用 ZIP 解压方式，无需管理员权限。

#### 第 1 步：下载 Erlang

```
https://www.erlang.org/downloads
```

- 选择 `Windows 64-bit Binary File`（如 `OTP-26.x.x_win64.exe`）
- 这是一个自解压安装包，运行后选择安装到 `D:\tools\erlang`

> 如果 .exe 安装器要求管理员权限，可以尝试从 https://github.com/erlang/otp/releases 下载 `otp_win64_x.x.zip` 手动解压到 `D:\tools\erlang`。

#### 第 2 步：下载 RabbitMQ

```
https://github.com/rabbitmq/rabbitmq-server/releases
```

- 选择最新 3.x 版本
- 下载 `rabbitmq-server-windows-x86_64-x.x.x.zip`（注意是 ZIP 格式，不是 exe）

#### 第 3 步：解压

将 ZIP 解压到 `D:\tools\rabbitmq`。解压后关键目录为 `D:\tools\rabbitmq\sbin\`。

#### 第 4 步：启用管理插件

在 PowerShell 中执行（需要先设置 Erlang 路径）：

```powershell
$env:ERLANG_HOME = "D:\tools\erlang"
D:\tools\rabbitmq\sbin\rabbitmq-plugins.bat enable rabbitmq_management
```

#### 第 5 步：启动 RabbitMQ

```powershell
$env:ERLANG_HOME = "D:\tools\erlang"
D:\tools\rabbitmq\sbin\rabbitmq-server.bat
```

> **提示**：由于每次启动都需要设置 `ERLANG_HOME`，建议在 `D:\tools\` 下创建启动脚本 `start-rabbitmq.bat`：
> ```bat
> @echo off
> set ERLANG_HOME=D:\tools\erlang
> D:\tools\rabbitmq\sbin\rabbitmq-server.bat
> ```
> 以后双击此脚本即可启动 RabbitMQ。

#### 第 6 步：验证

打开浏览器访问 `http://localhost:15672`，使用 `admin / admin123` 登录管理界面。

> **账号说明**：RabbitMQ 默认创建 `guest / guest` 账号，但仅限 localhost 访问。如需自定义账号，可通过管理界面的 Admin 选项卡创建 `admin / admin123` 用户并赋予 Administrator 角色，或通过命令行：
> ```powershell
> D:\tools\rabbitmq\sbin\rabbitmqctl.bat add_user admin admin123
> D:\tools\rabbitmq\sbin\rabbitmqctl.bat set_user_tags admin administrator
> D:\tools\rabbitmq\sbin\rabbitmqctl.bat set_permissions -p / admin ".*" ".*" ".*"
> ```

> **端口说明**：`5672` 是 AMQP 协议端口（程序连接用），`15672` 是 Web 管理界面端口。

---

### B.4 Nacos 安装

Nacos 是 Java 应用，官方提供可直接运行的 Windows 启动脚本，无需 Docker。

#### 第 1 步：下载

```
https://github.com/alibaba/nacos/releases
```

- 选择 **2.2.3** 版本（与 Docker 方式保持一致）
- 下载 `nacos-server-2.2.3.zip`（约 150MB）

#### 第 2 步：解压

将 ZIP 解压到 `D:\tools\nacos`。解压后关键目录：

```
D:\tools\nacos\
├── bin\
│   ├── startup.cmd       ← Windows 启动脚本
│   └── shutdown.cmd      ← Windows 停止脚本
├── conf\
├── logs\
└── ...
```

#### 第 3 步：选择存储模式

**开发环境推荐：使用内嵌 Derby 数据库（单机模式，无需 MySQL）**

此方式最简单，Nacos 使用内嵌数据库存储数据，无需额外配置 MySQL。

> **注意**：如果需要使用 MySQL 存储（与生产环境一致），请先确保 B.1 中 MySQL 已启动，然后：
> 1. 创建数据库：`CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4;`
> 2. 执行初始化 SQL：https://github.com/alibaba/nacos/blob/2.2.3/distribution/conf/mysql-schema.sql
> 3. 编辑 `D:\tools\nacos\conf\application.properties`，取消 MySQL 相关配置的注释并填入连接信息

#### 第 4 步：启动 Nacos（单机模式 + 内嵌数据库）

```powershell
D:\tools\nacos\bin\startup.cmd -m standalone
```

> 启动过程需要 30 秒左右，终端会显示 Nacos 启动日志。看到 `Nacos started successfully in stand alone mode` 即表示启动成功。
>
> **前提**：系统已安装 JDK 1.8（§2.1）且 `JAVA_HOME` 环境变量已配置。

#### 第 5 步：验证

打开浏览器访问 `http://localhost:8848/nacos`，使用 `nacos / nacos` 登录。

#### 停止 Nacos

```powershell
D:\tools\nacos\bin\shutdown.cmd
```

---

### B.5 各中间件访问信息汇总

无论使用方式 A 还是方式 B，各中间件的访问信息完全一致：

| 中间件 | 地址 | 端口 | 账号 / 密码 |
|--------|------|------|-------------|
| MySQL | localhost | 3306 | root / pp2024 |
| Redis | localhost | 6379 | 无密码 |
| RabbitMQ 管理界面 | http://localhost:15672 | 15672 | admin / admin123 |
| Nacos 控制台 | http://localhost:8848/nacos | 8848 | nacos / nacos |

**日常启停顺序**（方式 B）：

```
启动顺序：① MySQL → ② Redis → ③ RabbitMQ → ④ Nacos
停止顺序：④ Nacos → ③ RabbitMQ → ② Redis → ① MySQL
```

> 每个中间件建议在独立的 PowerShell 窗口中运行，方便查看日志和随时停止。

---

## 四、后端项目工程搭建

### 4.1 Maven 多模块项目结构

后端采用 Maven 多模块结构，一个父 POM 管理所有子模块。目标目录结构如下：

```
postman-platform/                  ← 项目根目录（已有）
├── backend/                       ← 后端工程根目录（新建）
│   ├── pom.xml                    ← 父 POM（依赖版本统一管理）
│   ├── pp-common/                 ← 公共模块（实体、工具类、通用配置）
│   │   ├── pom.xml
│   │   └── src/
│   ├── pp-gateway/                ← API 网关（端口 8080）
│   │   ├── pom.xml
│   │   └── src/
│   ├── pp-auth/                   ← 认证服务（端口 8081）
│   │   ├── pom.xml
│   │   └── src/
│   ├── pp-core/                   ← 核心服务（端口 8082）
│   │   ├── pom.xml
│   │   └── src/
│   ├── pp-keyword/                ← 关键字服务（端口 8083）
│   │   ├── pom.xml
│   │   └── src/
│   └── pp-execution/              ← 执行服务（端口 8084）
│       ├── pom.xml
│       └── src/
├── frontend/                      ← 前端工程根目录（新建）
│   ├── package.json
│   └── src/
├── docker/                        ← Docker 配置（已创建）
└── docs/                          ← 文档（已有）
```

---

### 4.2 父 POM 配置

以下是 `backend/pom.xml` 父 POM 的核心配置，统一管理所有子模块的依赖版本：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.postman</groupId>
    <artifactId>postman-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>postman-platform</name>
    <description>关键字驱动测试管理平台</description>

    <!-- 子模块 -->
    <modules>
        <module>pp-common</module>
        <module>pp-gateway</module>
        <module>pp-auth</module>
        <module>pp-core</module>
        <module>pp-keyword</module>
        <module>pp-execution</module>
    </modules>

    <!-- 版本统一管理 -->
    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <spring-boot.version>2.7.18</spring-boot.version>
        <spring-cloud.version>2021.0.9</spring-cloud.version>
        <spring-cloud-alibaba.version>2021.0.6.0</spring-cloud-alibaba.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <flyway.version>8.5.13</flyway.version>
        <mysql.version>8.0.33</mysql.version>
        <sa-token.version>1.39.0</sa-token.version>
        <jjwt.version>0.11.5</jjwt.version>
        <hutool.version>5.8.34</hutool.version>
        <okhttp.version>4.12.0</okhttp.version>
    </properties>

    <!-- Spring Boot 父 POM -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <!-- 依赖版本管理（子模块按需引入，不在此处直接依赖） -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Cloud Alibaba -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <!-- JJWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <!-- OkHttp（HTTP 客户端） -->
            <dependency>
                <groupId>com.squareup.okhttp3</groupId>
                <artifactId>okhttp</artifactId>
                <version>${okhttp.version}</version>
            </dependency>
            <!-- Hutool 工具库 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- pp-common 模块 -->
            <dependency>
                <groupId>com.postman</groupId>
                <artifactId>pp-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 所有子模块公共依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 4.3 子模块创建顺序

请按以下顺序在 IDEA 中创建子模块（后面开发时也会按此顺序逐步推进）：

| 顺序 | 模块名 | 类型 | 说明 |
|------|--------|------|------|
| 1 | pp-common | jar | 公共模块，无 Spring Boot 启动类 |
| 2 | pp-gateway | jar | Spring Cloud Gateway 网关 |
| 3 | pp-auth | jar | 认证服务 |
| 4 | pp-core | jar | 核心业务服务 |
| 5 | pp-keyword | jar | 关键字管理服务 |
| 6 | pp-execution | jar | 执行与报告服务 |

**在 IDEA 中创建子模块的步骤**：

1. 右键点击 `backend` 文件夹 → `New` → `Module`
2. 选择 `Maven`，填写：
   - `Name`：模块名（如 `pp-common`）
   - `GroupId`：`com.postman`
   - `ArtifactId`：模块名
3. 点击 `Create`

> 详细的子模块 pom.xml 配置、启动类代码、配置文件等，将在正式开发阶段（Phase 0）逐步生成。

---

### 4.4 IDEA 后端项目导入

1. 打开 IDEA → `File` → `Open`
2. 选择 `D:\develop\postman-platform\backend` 目录
3. 选择 `Open as Project`
4. 等待 IDEA 自动下载 Maven 依赖（右下角进度条，首次可能需要 5-10 分钟）
5. 确认 `Project Structure` → `Project SDK` 设置为 JDK 1.8

---

### 4.5 验证后端启动

在 Phase 0 开发完成后，你应该能够：

1. 启动 Nacos，确认控制台可访问：`http://localhost:8848/nacos`
2. 在 IDEA 中运行 `pp-gateway` 的启动类
3. 访问 `http://localhost:8080/actuator/health` 确认网关健康

---

## 五、前端项目工程搭建

### 5.1 使用 Vite 创建 Vue 3 项目

打开 PowerShell，执行：

```powershell
cd D:\develop\postman-platform
npm create vite@latest frontend -- --template vue-ts
```

选择 `Vue` 框架和 `TypeScript` 模板。

---

### 5.2 安装核心依赖

```powershell
cd D:\develop\postman-platform\frontend

# 安装基础依赖
pnpm install

# 安装 UI 组件库
pnpm add ant-design-vue@4

# 安装状态管理
pnpm add pinia

# 安装路由
pnpm add vue-router@4

# 安装 HTTP 客户端
pnpm add axios

# 安装图表库
pnpm add echarts

# 安装代码编辑器
pnpm add monaco-editor

# 安装流程画布引擎
pnpm add @antv/x6 @antv/x6-plugin-selection @antv/x6-plugin-snapline

# 安装开发工具
pnpm add -D @types/node unplugin-auto-import unplugin-vue-components
```

---

### 5.3 前端项目目录结构

```
frontend/
├── public/                    ← 静态资源
├── src/
│   ├── api/                   ← 后端 API 接口封装
│   │   ├── auth.ts
│   │   ├── project.ts
│   │   └── request.ts         ← Axios 实例与拦截器
│   ├── assets/                ← 图片、图标等资源
│   ├── components/            ← 公共组件
│   ├── composables/           ← 组合式函数（hooks）
│   ├── layouts/               ← 页面布局（侧边栏、顶栏）
│   ├── router/                ← 路由配置
│   │   └── index.ts
│   ├── stores/                ← Pinia 状态管理
│   │   ├── user.ts
│   │   └── project.ts
│   ├── styles/                ← 全局样式
│   │   └── global.less
│   ├── utils/                 ← 工具函数
│   ├── views/                 ← 页面组件
│   │   ├── auth/              ← 登录页
│   │   ├── project/           ← 项目相关页面
│   │   ├── api/               ← 接口管理页面
│   │   ├── keywords/          ← 关键字管理页面
│   │   ├── cases/             ← 测试用例页面
│   │   ├── execution/         ← 执行管理页面
│   │   ├── environment/       ← 环境配置页面
│   │   └── settings/          ← 系统设置页面
│   ├── App.vue                ← 根组件
│   └── main.ts                ← 入口文件
├── index.html
├── package.json
├── pnpm-lock.yaml
├── tsconfig.json
├── vite.config.ts
└── env.d.ts
```

---

### 5.4 IDEA 前端项目导入

1. 打开 IDEA → `File` → `Open`
2. 选择 `D:\develop\postman-platform\frontend` 目录
3. 选择 `Open as Project`
4. IDEA 会自动识别为 Vue 项目（需已安装 Vue.js 插件）
5. 打开内置终端（`Alt + F12`）或外部 PowerShell，运行：
   ```powershell
   cd D:\develop\postman-platform\frontend
   pnpm dev
   ```

> **提示**：你也可以在一个 IDEA 窗口中同时管理前后端项目。方法是打开后端项目后，通过 `File` → `New` → `Module from Existing Sources` 导入前端目录。但建议初期分开两个 IDEA 窗口，避免混淆。

---

### 5.5 验证前端启动

在 PowerShell 中执行：
```powershell
cd D:\develop\postman-platform\frontend
pnpm dev
```

浏览器应自动打开 `http://localhost:5173`，显示 Vite + Vue 的默认欢迎页面。

---

## 六、开发规范与编码配置

### 6.1 文件编码

**所有文件统一使用 UTF-8 编码**，这是项目强制规范。

- IDEA 已配置（§2.4.3）
- 前端 `vite.config.ts` 中无需额外配置（Vite 默认 UTF-8）
- Git 已配置（§2.3）

### 6.2 后端代码规范

| 规范项 | 要求 |
|--------|------|
| 缩进 | 4 个空格（IDEA 默认） |
| 类命名 | 大驼峰：`UserController`、`ApiEndpointService` |
| 方法/变量 | 小驼峰：`getUserById`、`projectName` |
| 常量 | 全大写下划线：`MAX_RETRY_COUNT` |
| 包名 | 全小写：`com.postman.auth.controller` |
| RESTful 路径 | 全小写短横线：`/api/v1/test-suites` |
| 日志框架 | SLF4J + Lombok `@Slf4j` 注解 |

### 6.3 前端代码规范

| 规范项 | 要求 |
|--------|------|
| 缩进 | 2 个空格 |
| 组件文件名 | 大驼峰：`UserManagement.vue` |
| 组合式函数 | `use` 前缀：`useAuth.ts` |
| CSS 类名 | 短横线：`.sidebar-menu` |
| 变量/函数 | 小驼峰：`userInfo`、`fetchProjects` |
| 常量 | 全大写：`API_BASE_URL` |

---

## 七、环境验证清单

完成所有步骤后，逐项检查以下清单。全部打 ✓ 即表示开发环境准备就绪：

### 基础软件

- [ ] `java -version` → 显示 Java 1.8.x
- [ ] `node -v` → 显示 v20.x 或更高
- [ ] `pnpm -v` → 显示版本号
- [ ] `git --version` → 显示版本号
- [ ] `mvn -version` → 显示 Maven 3.9.x + Java 1.8
- [ ] IDEA 已安装 Vue.js、Lombok、MyBatisX 插件
- [ ] IDEA 文件编码已设置为 UTF-8

### 中间件（方式 A：Docker 容器）

- [ ] `docker compose ps` → 所有容器状态为 `Up`
- [ ] MySQL 连接测试：`docker exec -it pp-mysql mysql -uroot -ppp2024 -e "SELECT 1;"`
- [ ] Redis 连接测试：`docker exec -it pp-redis redis-cli PING` → 返回 `PONG`
- [ ] RabbitMQ 管理界面：http://localhost:15672 可访问
- [ ] Nacos 控制台：http://localhost:8848/nacos 可访问

### 中间件（方式 B：原生 Windows 安装）

- [ ] MySQL 连接测试：`D:\tools\mysql-8.0\bin\mysql -uroot -ppp2024 -e "SELECT VERSION();"` → 显示 8.0.x
- [ ] Redis 连接测试：`D:\tools\redis\redis-cli.exe PING` → 返回 `PONG`
- [ ] RabbitMQ 管理界面：http://localhost:15672 可访问
- [ ] Nacos 控制台：http://localhost:8848/nacos 可访问

### 后端工程

- [ ] `backend/` 目录已创建，包含父 `pom.xml`
- [ ] 6 个子模块目录已创建（pp-common / pp-gateway / pp-auth / pp-core / pp-keyword / pp-execution）
- [ ] IDEA 中 Maven 依赖下载完成（无红色错误标记）

### 前端工程

- [ ] `frontend/` 目录已创建
- [ ] `pnpm dev` 能正常启动开发服务器
- [ ] 浏览器访问 `http://localhost:5173` 正常显示

---

## 八、常见问题排查

### Q1：Docker Desktop 启动失败，提示 WSL 2 未安装

**解决**：以管理员身份打开 PowerShell，执行：
```powershell
wsl --install
```
安装完成后重启电脑。

> **如果没有管理员权限**，无法启用 WSL，请改用 [方式 B：原生 Windows 安装](#方式-b原生-windows-安装无需管理员权限)，完全不依赖 Docker 和 WSL。

### Q2：Maven 下载依赖很慢或超时

**解决**：确认已配置阿里云镜像（§2.4.4 第 4 步）。如果还是慢，尝试更换镜像地址：
```xml
<url>https://maven.aliyun.com/repository/public</url>
```

### Q3：IDEA 提示 JDK 版本不匹配

**解决**：`File` → `Project Structure` → `Project` → 确认 SDK 为 JDK 1.8，Language level 为 8。同时检查 `Modules` 选项卡中每个子模块的 Language level 也是 8。

### Q4：MySQL 启动失败

**方式 A（Docker）**：查看日志定位原因：
```powershell
docker logs pp-mysql
```
常见原因是端口 3306 被占用，可以先停掉本机 MySQL 服务：
```powershell
net stop mysql
```

**方式 B（原生安装）**：常见原因及解决方法：
- **端口被占用**：检查 3306 端口是否已被其他 MySQL 实例使用：
  ```powershell
  netstat -ano | findstr :3306
  ```
  如有其他 MySQL 服务占用，可停止它或修改 `my.ini` 中的端口号。
- **data 目录已存在**：如果之前初始化过，重新初始化前需要先删除 `data` 目录：
  ```powershell
  Remove-Item -Recurse -Force D:\tools\mysql-8.0\data
  D:\tools\mysql-8.0\bin\mysqld --initialize-insecure --console
  ```
- **启动后立即闪退**：检查 `my.ini` 中的路径是否正确（使用正斜杠 `/`），以及 `basedir` 和 `datadir` 目录是否存在。

### Q5：Nacos 启动失败

**方式 A（Docker）**：
1. 确认 MySQL 容器已先启动并运行正常
2. 确认已创建 `nacos_config` 数据库并执行了初始化 SQL 脚本
3. 查看 Nacos 日志：
   ```powershell
   docker logs pp-nacos
   ```

**方式 B（原生安装）**：
- **`JAVA_HOME` 未配置**：`startup.cmd` 依赖 `JAVA_HOME` 环境变量。确认 JDK 1.8 已安装且环境变量正确：
  ```powershell
  echo $env:JAVA_HOME
  java -version
  ```
- **端口被占用**：检查 8848 端口：
  ```powershell
  netstat -ano | findstr :8848
  ```
- **单机模式未指定**：务必使用 `startup.cmd -m standalone` 启动，不带 `-m standalone` 参数会尝试以集群模式启动并失败。
- **启动缓慢**：Nacos 是 Java 应用，首次启动可能需要 30-60 秒。查看 `D:\tools\nacos\logs\start.out` 确认启动状态。

### Q6：前端 `pnpm dev` 报端口占用

**解决**：在 `vite.config.ts` 中修改端口：
```typescript
export default defineConfig({
  server: {
    port: 5174  // 改为其他未被占用的端口
  }
})
```

### Q7：PowerShell 执行 docker run 多行命令报错

**解决**：PowerShell 中多行命令使用反引号 `` ` `` 作为换行符（不是 `\`）。确保反引号后面没有多余的空格。也可以将所有参数写在一行中。

### Q8：RabbitMQ 启动失败（方式 B）

**常见原因及解决方法**：
- **找不到 Erlang**：确认 `$env:ERLANG_HOME` 已正确设置，且路径下有 `bin\erl.exe`：
  ```powershell
  Test-Path "$env:ERLANG_HOME\bin\erl.exe"
  ```
  如返回 `True` 则路径正确。
- **管理界面无法访问**：确认已启用管理插件：
  ```powershell
  D:\tools\rabbitmq\sbin\rabbitmq-plugins.bat enable rabbitmq_management
  ```
- **端口被占用**：检查 5672 和 15672 端口：
  ```powershell
  netstat -ano | findstr ":5672 :15672"
  ```

### Q9：没有管理员权限，无法设置系统环境变量

**解决**：本文档中方式 B 的所有操作均**不需要系统级环境变量**。对于 RabbitMQ 和 Nacos 等需要环境变量的程序，使用 PowerShell 会话级变量即可：

```powershell
# 仅在当前 PowerShell 窗口有效
$env:ERLANG_HOME = "D:\tools\erlang"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-1.8"
```

如果需要永久生效（非管理员），可以通过用户级环境变量设置（不需要管理员权限）：

```powershell
[Environment]::SetEnvironmentVariable("ERLANG_HOME", "D:\tools\erlang", "User")
```

### Q10：Redis 在 Windows 上没有官方版本怎么办

**解决**：推荐使用以下替代方案（按优先级排列）：
1. **tporadowski 社区版**（https://github.com/tporadowski/redis/releases）—— 基于 Redis 5.0 移植，ZIP 解压即用，适合开发环境
2. **Memurai**（https://www.memurai.com/get-memurai）—— Redis 协议的 Windows 原生实现，Developer 版免费，性能更好且持续维护

两者都与 Redis 协议兼容，后端代码无需做任何修改。

---

> **下一步**：环境准备完成后，我们将按照 HLD 文档中的 Phase 0（微服务基础设施搭建）开始正式开发。
