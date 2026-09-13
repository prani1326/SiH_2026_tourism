import sys, base64; dest, b64_file = sys.argv[1], sys.argv[2]; data = base64.b64decode(open(b64_file, "r").read().strip()); open(dest, "wb").write(data); print(f"Wrote {dest}")
