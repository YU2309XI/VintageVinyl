# CI/CD Add‑Ons for Vintage Vinyl

**Badges (add these to your README.md):**

[![CI — Build & Test](https://img.shields.io/github/actions/workflow/status/OWNER/REPO/ci.yml?branch=main&label=CI%20Build%20%26%20Tests)](../../actions/workflows/ci.yml)
[![CodeQL](https://img.shields.io/github/actions/workflow/status/OWNER/REPO/codeql.yml?branch=main&label=CodeQL)](../../actions/workflows/codeql.yml)
[![Docker GHCR](https://img.shields.io/github/actions/workflow/status/OWNER/REPO/docker-ghcr.yml?label=Docker%20GHCR)](../../actions/workflows/docker-ghcr.yml)
[![Docker Image Version](https://img.shields.io/docker/v/ghcr/OWNER/REPO?label=ghcr.io%2FOWNER%2FREPO)](https://ghcr.io/OWNER/REPO)

> 替换上面的 `OWNER/REPO` 为你的 GitHub 用户名和仓库名。

## How to Use

1. 把 `.github/workflows/*.yml`、`Dockerfile` 放到你的 `vintage-vinyl` 根目录。
2. 把 `src/main/resources/application-prod.properties` 放到对应路径。
3. **重要**：从 `application.properties` 中移除明文密码，改为只在 `application-prod.properties` 使用环境变量（见上）。
4. 推送到 `main` 分支，CI 会自动运行构建和测试。
5. 想推送 Docker 镜像到 GHCR：给仓库打 tag（例如 `v1.0.0`）或手动运行该工作流。

### GHCR 登录 & 运行
运行容器时提供数据库环境变量：
```bash
docker run -p 8080:8080   -e DB_URL="jdbc:mysql://dbhost:3306/vintage_vinyl_db"   -e DB_USERNAME="youruser"   -e DB_PASSWORD="yourpass"   ghcr.io/OWNER/REPO:latest
```