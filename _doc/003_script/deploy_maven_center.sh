#!/usr/bin/env bash
#
# deploy_maven_center.sh — z-qa 一键发布到 Maven Central
#
# 子命令：
#   publish    实际 mvn deploy -Pcentral（默认）
#   verify     验证 Maven Central 上能否取到 io.github.yuku123:z-qa*
#   gpg-init   首次发布前生成 GPG 密钥并写 .env
#   readme     打印发布指引摘要
#   help       显示此帮助
#
# 用法：
#   bash _doc/003_script/deploy_maven_center.sh                   # 等同 publish
#   bash _doc/003_script/deploy_maven_center.sh gpg-init          # 首次必须先跑
#   bash _doc/003_script/deploy_maven_center.sh publish
#   bash _doc/003_script/deploy_maven_center.sh verify
#
# 设计原则（沿用 z-msg / z-boot / z-schedule 的口径）：
#   - 凭证不在本仓：按 ./.env → ../z-boot/.env → ../z-schedule/.env 顺序找，
#     GNUPGHOME 同样指向上游密钥环（副本越少越好）。本仓真建了 .env/.gnupg 也已被 .gitignore 排除
#   - 默认不做任何事情，必须显式给子命令
#
# ⚠️ 关键约束（都是实测踩过的坑，见 z-msg/deploy_maven_center.sh 头注）：
#   - 必须联网：`mvn -o deploy -Pcentral` 会把 central-publishing 的 publish 目标整场静默跳过
#     而照样 BUILD SUCCESS。判"发出去了"只认 repo1 的 HEAD 状态码
#   - 已发布的版本号永久占位，不可覆盖、不可删除（z-qa 1.0.0 是本仓第一个版本，发出去就是它）
#   - 发布那遍不要加 -DskipTests：让上传出去的字节就是跑绿的那批
#
set -eo pipefail

chmod +x "$0" 2>/dev/null || true

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { printf "${GREEN}[deploy]${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}[deploy]${NC} %s\n" "$*"; }
err()  { printf "${RED}[deploy]${NC} %s\n" "$*" >&2; }
die()  { err "$*"; exit 1; }

# 脚本在 _doc/003_script/ 下，根定位须上溯两级（../z-boot 等兄弟仓凭证源也依赖 cwd 为仓根）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"
[[ -f pom.xml ]] || die "请在 z-qa 仓库根目录运行此脚本"

VERSION=$(grep -m1 '<revision>' pom.xml | sed 's/.*<revision>\(.*\)<\/revision>.*/\1/')
ARTIFACTS=(z-qa z-qa-core z-qa-web)

# 凭证唯一源在 z-boot / z-schedule / z-config 三个仓（台账 z-opc-foundation-lead/004_重要秘钥）。
# 本仓不复制一份 .env / .gnupg 出来 —— 明文凭据与私钥环每多一处副本就多一处泄露面。
# 仓库根若真有一份 ./.env（本机调试用），它优先，方便不发上游凭证。
find_env_file() {
    if [[ -f ./.env ]]; then echo "./.env"; return; fi
    for up in ../z-boot ../z-schedule ../z-config; do
        [[ -f "$up/.env" ]] && { echo "$up/.env"; return; }
    done
    echo ""
}

find_gnupg_home() {
    if [[ -d ./.gnupg ]]; then echo "$PWD/.gnupg"; return; fi
    for up in ../z-boot ../z-schedule ../z-config; do
        if [[ -d "$up/.gnupg" ]]; then
            (cd "$up/.gnupg" && pwd)
            return
        fi
    done
    echo ""
}

load_env() {
    local env_file; env_file=$(find_env_file)
    [[ -n "$env_file" ]] || die "找不到 .env（本仓 ./.env 或 ../z-boot/.env）。台账见 z-opc-foundation-lead/004_重要秘钥"
    log "凭证来源: $env_file"
    # shellcheck disable=SC1091
    set -a; source "$env_file"; set +a

    [[ -n "${CENTRAL_USERNAME:-}"        ]] || die "$env_file 缺 CENTRAL_USERNAME"
    [[ -n "${CENTRAL_TOKEN:-}"           ]] || die "$env_file 缺 CENTRAL_TOKEN"
    [[ -n "${CENTRAL_GPG_PASSPHRASE:-}"  ]] || die "$env_file 缺 CENTRAL_GPG_PASSPHRASE"

    export CENTRAL_USERNAME CENTRAL_TOKEN CENTRAL_GPG_PASSPHRASE
}

check_deps() {
    command -v mvn >/dev/null 2>&1 || die "mvn 未安装"
    command -v gpg >/dev/null 2>&1 || die "gpg 未安装（brew install gnupg）"

    [[ -f ~/.m2/settings.xml ]] || die "~/.m2/settings.xml 不存在"
    grep -q '<id>central</id>' ~/.m2/settings.xml || die "~/.m2/settings.xml 缺 <server id=\"central\">"

    local gpg_home; gpg_home=$(find_gnupg_home)
    [[ -n "$gpg_home" ]] || die "找不到 .gnupg 密钥环（本仓 ./.gnupg 或 ../z-boot/.gnupg）"
    log "GPG 密钥环: $gpg_home"
    export GNUPGHOME="$gpg_home"
}

cmd_gpg_init() {
    command -v gpg >/dev/null 2>&1 || die "gpg 未安装（brew install gnupg）"

    if [[ ! -f .env ]]; then
        cat > .env <<'EOF'
# Maven Central (Sonatype Central Portal) — 从 z-opc-foundation-lead/004_重要秘钥 同步
CENTRAL_USERNAME=
CENTRAL_TOKEN=

# GPG 私钥 passphrase（gpg-init 自动写入）
CENTRAL_GPG_PASSPHRASE=
GPG_KEY_ID=
# GPG_USER_NAME 必须带引号：值里的 < 会被 source 当成重定向，裸写时 load_env 直接
# 报 "syntax error near unexpected token `newline'"，publish 在第一步就死（实测）。
GPG_USER_NAME='yuku123 <1340947819@qq.com>'
EOF
        die ".env 模板已创建。请填入 CENTRAL_USERNAME 和 CENTRAL_TOKEN 后重跑 gpg-init"
    fi

    load_env 2>/dev/null || warn ".env 中央凭证未填，但 gpg-init 仍可继续"

    log "生成 GPG RSA 4096 密钥..."
    mkdir -p .gnupg && chmod 700 .gnupg
    printf 'pinentry-mode loopback\n' > .gnupg/gpg.conf
    chmod 600 .gnupg/gpg.conf

    export GNUPGHOME="$PWD/.gnupg"
    export GPG_PASSPHRASE="$CENTRAL_GPG_PASSPHRASE"

    gpg --batch --pinentry-mode loopback --passphrase "$GPG_PASSPHRASE" \
        --quick-generate-key "$GPG_USER_NAME" default default 4096

    KEY_ID=$(gpg --list-secret-keys --keyid-format LONG "$GPG_USER_NAME" 2>/dev/null \
             | grep -oE '[A-F0-9]{16}' | head -1)
    [[ -n "$KEY_ID" ]] || die "无法解析 KEY_ID"

    sed -i.bak \
        -e "s|^GPG_KEY_ID=.*|GPG_KEY_ID=$KEY_ID|" \
        -e "s|^GPG_USER_NAME=.*|GPG_USER_NAME='$GPG_USER_NAME'|" \
        .env && rm -f .env.bak

    log "GPG 密钥生成成功：KEY_ID = $KEY_ID"
    log "上传公钥到 keys.openpgp.org（Central Portal 从这里拉公钥校验签名）"
    gpg --keyserver hkps://keys.openpgp.org --send-keys "$KEY_ID" 2>&1 || \
        warn "keyserver 上传失败，可手动跑：gpg --keyserver hkps://keys.openpgp.org --send-keys $KEY_ID"
    log "下一步：bash _doc/003_script/deploy_maven_center.sh publish"
}

cmd_publish() {
    load_env
    check_deps

    log "═══════════════════════════════════════════════════════════════"
    log " 即将把 z-qa 上传到 Maven Central"
    log "  groupId : io.github.yuku123"
    log "  version : $VERSION"
    log "  构件    : ${ARTIFACTS[*]}"
    log "  GPG KEY : ${GPG_KEY_ID:-?}"
    log "═══════════════════════════════════════════════════════════════"

    rm -rf target/central-staging target/central-publishing target/central-deferred 2>/dev/null

    # 日志不放 /tmp：这台机器上会有别的会话清 /tmp，出问题时唯一证据就没了。
    local deploy_log="$HOME/.cache/z-qa-deploy.log"
    # -U + legacyLocalRepo: ~/.m2 里的 io.github.yuku123 构件多半是从 mvn.seenew.info
    # (settings.xml 的 active profile "public") 拉的，那个仓会 404 掉一部分 yuku123 发布，
    # 不带这两个参数会拿"缓存的解析失败"直接死在依赖图上。
    mvn -B deploy \
        -Pcentral \
        -U -Dmaven.legacyLocalRepo=true \
        -Dgpg.passphrase="$CENTRAL_GPG_PASSPHRASE" \
        2>&1 | tee "$deploy_log" | tail -100

    grep -q "BUILD SUCCESS" "$deploy_log" || die "BUILD FAILURE，请看 $deploy_log"
    log ""
    log "✅ BUILD SUCCESS"
    if grep -q "Uploaded bundle successfully" "$deploy_log"; then
        log "✅ Bundle uploaded — https://central.sonatype.com/publishing/deployments"
        log "验证以 repo1 的 HEAD 状态码为准（Portal 状态接口不可信），约 30 分钟后："
        for a in "${ARTIFACTS[@]}"; do
            log "  curl -sI https://repo1.maven.org/maven2/io/github/yuku123/$a/$VERSION/$a-$VERSION.pom | head -1"
        done
    else
        warn "BUILD SUCCESS 但 upload 未确认。请看 $deploy_log 最后 30 行"
    fi
}

cmd_verify() {
    log "逐个 HEAD 检查 repo1（版本 $VERSION）："
    for a in "${ARTIFACTS[@]}"; do
        code=$(curl -s -o /dev/null -w "%{http_code}" \
            "https://repo1.maven.org/maven2/io/github/yuku123/$a/$VERSION/$a-$VERSION.pom")
        [[ "$code" == "200" ]] && log "  ✅ $a:$VERSION pom -> $code" || warn "  ⚠️  $a:$VERSION pom -> $code（还没同步，再等等）"
    done
    log "签名与附属文件清单："
    for a in "${ARTIFACTS[@]}"; do
        log "  $(curl -s "https://repo1.maven.org/maven2/io/github/yuku123/$a/$VERSION/" \
              | grep -oE 'href="[^"]+"' | sed 's/href="//; s/"//' | grep -v '^/' | tr '\n' ' ')"
    done
    log "中央部署状态：https://central.sonatype.com/publishing/deployments"
}

cmd_readme() {
    cat <<'EOF'
═══════════════════════════════════════════════════════════════
  z-qa 发布到 Maven Central — 指引摘要
═══════════════════════════════════════════════════════════════

【前置（人工，一次性）】
  1. 凭证不在本仓。唯一源是 ../z-boot / ../z-schedule / ../z-config 的 .env + .gnupg
     （台账 z-opc-foundation-lead/004_重要秘钥）。脚本会按 ./.env → ../z-boot/.env 的顺序找，
     找不到再考虑在本仓建 .env —— 每多一处明文副本就多一处泄露面。
  2. namespace io.github.yuku123 已验证过，无需重复
  3. brew install gnupg（如果还没装）

【首次发布（仅当上游没有可用密钥环时）】
  $ bash _doc/003_script/deploy_maven_center.sh gpg-init

【日常发布流】
  $ # 1) 改根 pom 的 <revision>（子 pom 一律 ${revision}，不要动）
  $ # 2) commit + tag
  $ git add pom.xml && git commit -m "release: X.Y.Z" && git tag vX.Y.Z
  $ git push origin main && git push origin vX.Y.Z
  $ # 3) 真发
  $ bash _doc/003_script/deploy_maven_center.sh publish
  $ # 4) 约 30 分钟后
  $ bash _doc/003_script/deploy_maven_center.sh verify

【避坑】
  ✗ 不要贴 token / passphrase 到对话
  ✗ 不要用 mvn -o（离线）跑：publish 目标要求联网，离线时静默跳过却照样 BUILD SUCCESS
  ✗ 不要 -DskipTests / 跳 javadoc / 跳 GPG（Central 强制要求，且测试是本仓唯一的口径回归门）
  ✗ 不要重发同名版本号：1.0.0 一旦发出永久占位
  ✗ 不要用 -pl 摘掉 reactor 最后一个模块：bundle 的打包与上传发生在最后一次 publish 执行里
  ✓ 排除模块只认根 pom 的 <excludeArtifacts>
  ✓ waitMaxTime=1800 防 mvn 提前放弃
EOF
}

case "${1:-publish}" in
    publish)   cmd_publish ;;
    verify)    cmd_verify ;;
    gpg-init)  cmd_gpg_init ;;
    readme)    cmd_readme ;;
    help|-h|--help) sed -n '2,16p' "${BASH_SOURCE[0]}" ;;
    *) die "未知子命令：$1（用 help 看用法）" ;;
esac
