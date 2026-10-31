#!/system/bin/sh
# Usage: sh probe.sh X Y FRAMES
# Taps (X, Y), then for FRAMES screenshots prints: uptime-seconds and the R G B A of three
# pixels at y=1950 (x = 100, 300, 540), below the celebration card.
f=/data/local/tmp/probe.raw
px() { dd if=$f bs=4 skip=$((4 + 1950 * 1080 + $1)) count=1 2>/dev/null | od -An -tu1; }
start=$(cut -d' ' -f1 /proc/uptime)
input tap $1 $2
echo "tap $start"
i=0
while [ $i -lt $3 ]; do
  t=$(cut -d' ' -f1 /proc/uptime)
  screencap $f
  echo "$t $(px 100) | $(px 300) | $(px 540)"
  i=$((i + 1))
done
