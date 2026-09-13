import sys
import os

target = sys.argv[1]
content = sys.stdin.read()
os.makedirs(os.path.dirname(target), exist_ok=True)
with open(target, 'w', encoding='utf-8') as f:
    f.write(content)
print(f'Successfully wrote {target} ({len(content)} bytes)')
