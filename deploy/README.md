# Docker Compose 一键部署

在 Linux 服务器上用 Docker 编排 `mysql` + `SportBackend` + `webManageProject`（nginx 托管静态文件并反代 API）三个容器。

## 一、服务器准备

1. 安装 Docker Engine + Docker Compose 插件（Docker 20.10+）：

```bash
curl -fsSL https://get.docker.com | sh
sudo systemctl enable --now docker
```

2. 克隆项目代码：

```bash
git clone <你的仓库地址> sportProject
cd sportProject/deploy
```

## 二、配置密钥

```bash
cp .env.example .env
vim .env
```

`.env` 中的 `MYSQL_ROOT_PASSWORD` 必填（后端也用这个密码连库）；`WECHAT_APPID` / `WECHAT_SECRET` 保持为空则回退到后端 `application.yml` 里的值，一般建议填上正式小程序凭证。

> `.env` 已被 `deploy/.gitignore` 忽略，不会提交到仓库。

## 三、启动

```bash
docker compose up -d --build
```

首次启动会自动拉取基础镜像、编译前后端、初始化数据库（执行 `SportBackend/sql/` 下 18 个建表脚本）。查看状态与日志：

```bash
docker compose ps
docker compose logs -f backend
```

## 四、访问

| 服务 | 地址 |
| --- | --- |
| Web 管理端 | `http://<服务器IP>/` |
| 后端 API | `http://<服务器IP>:8080/api/...` |
| MySQL | `服务器IP:3306`（`sport_db`，root / 你设置的密码） |

## 五、日常操作

```bash
# 查看日志
docker compose logs -f web
# 停止
docker compose down
# 停止并删除数据卷（数据库数据会清空，慎用）
docker compose down -v
# 更新代码后重新构建启动
git pull && docker compose up -d --build
```

## 六、注意事项

- **MySQL 数据持久化**：存在命名卷 `mysql-data` 中，`docker compose down` 不会丢失；`down -v` 才会清空。
- **建表脚本只在首次初始化执行**：若已初始化过又想重建表，需 `docker compose down -v` 后重新 `up`，或手动 `docker compose exec mysql mysql -uroot -p sport_db < xxx.sql`。
- **改 `.env` 生效**：修改后执行 `docker compose up -d` 即可让受影响容器重启生效。
- **小程序端**：`sportdocu/utils/auth.js` 中 `BASE_URL` 硬编码为 `http://localhost:8080`，部署后需改为公网域名，且微信小程序要求 **HTTPS + 备案域名**，需在服务器上配置 nginx 的 443/SSL（当前 nginx 仅监听 80）。
- **图片上传**：nginx 已允许 20M 请求体（`client_max_body_size 20m`）；图片存华为云 OBS，凭证硬编码在 `OBSUtil.java`，容器内直接可用。
