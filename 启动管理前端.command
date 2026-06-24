#!/bin/zsh
# macOS 一键启动管理前端
# 双击本文件会打开 Terminal 并启动 Vite 前端服务。

set -u

# 让双击 .command 时也能找到 Homebrew / Node.js。
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

# 尝试加载用户 shell 配置，兼容 nvm/fnm/asdf 等 Node 安装方式。
[ -f "$HOME/.zprofile" ] && source "$HOME/.zprofile"
[ -f "$HOME/.zshrc" ] && source "$HOME/.zshrc"
[ -f "$HOME/.bash_profile" ] && source "$HOME/.bash_profile"

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$ROOT_DIR/client"
URL="http://127.0.0.1:5173"

clear
cat <<BANNER
============================================
 WorldCup Management Frontend
 URL: $URL
 Only frontend will start. Backend is not started.
 Close this Terminal window, or press Ctrl+C, to stop the frontend service.
============================================
BANNER

echo

if [ ! -f "$CLIENT_DIR/package.json" ]; then
  echo "[ERROR] package.json not found: $CLIENT_DIR/package.json"
  echo "Please place this script in the project root directory."
  echo
  read -r "?Press Enter to exit..."
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "[ERROR] Node.js was not found. Please install Node.js first."
  echo "Tip: brew install node"
  echo
  read -r "?Press Enter to exit..."
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "[ERROR] npm was not found. Please install Node.js/npm first."
  echo "Tip: brew install node"
  echo
  read -r "?Press Enter to exit..."
  exit 1
fi

cd "$CLIENT_DIR" || exit 1

if [ ! -d "node_modules" ]; then
  echo "[INIT] node_modules not found. Installing frontend dependencies..."
  npm install
  INSTALL_CODE=$?
  if [ "$INSTALL_CODE" -ne 0 ]; then
    echo
    echo "[ERROR] npm install failed."
    echo
    read -r "?Press Enter to exit..."
    exit "$INSTALL_CODE"
  fi
  echo
fi

echo "[START] Starting frontend dev server..."
echo "Open: $URL"
echo

# 自动打开浏览器；如果服务启动较慢，浏览器刷新一次即可。
( sleep 2 && open "$URL" >/dev/null 2>&1 ) &

npm run dev
EXIT_CODE=$?

echo
echo "Frontend process exited with code $EXIT_CODE."
read -r "?Press Enter to exit..."
exit "$EXIT_CODE"
