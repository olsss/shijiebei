@echo off
chcp 65001 >nul
echo ============================================
echo  世界杯竞彩看板 - 本地服务
echo  本机访问: http://localhost:5699
echo  natapp 隧道请把 [本地端口] 填 5699，协议选 web/http
echo  关掉此窗口即停止服务
echo ============================================
python -m http.server 5699 --directory "%~dp0skill\archive\dashboard"
pause
