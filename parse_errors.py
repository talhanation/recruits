import re
text = open("build_log.txt", encoding="utf-8", errors="ignore").read().split(":compileJava FAILED")[0].replace("\n", " ")
seen = set(); out = []
for m in re.finditer(r"([A-Za-z0-9_]+\.java):(\d+):\s+error:\s+(.+?)(?=\s+[A-Za-z]:\\|\s+symbol:|\s+location:|\s+\^|$)", text):
    k = (m.group(1), m.group(2))
    if k in seen: continue
    seen.add(k); out.append((m.group(1), int(m.group(2)), m.group(3).strip()[:58]))
out.sort()
for f, l, msg in out:
    print(f"{f}:{l}: {msg}")
print("TOTAL:", len(out))
