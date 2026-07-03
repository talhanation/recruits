#!/usr/bin/env python3
"""Convert `new ResourceLocation(...)` to 1.21.1 factory methods.
Two-arg  -> ResourceLocation.fromNamespaceAndPath(ns, path)
One-arg  -> ResourceLocation.parse(str)
Handles balanced parentheses to find the argument span and detect a top-level comma.
"""
import os, re

ROOT = "src/main/java"
NEEDLE = "new ResourceLocation("

def convert(text):
    out = []
    i = 0
    while True:
        j = text.find(NEEDLE, i)
        if j == -1:
            out.append(text[i:])
            break
        out.append(text[i:j])
        # find matching close paren
        start = j + len(NEEDLE)
        depth = 1
        k = start
        top_comma = -1
        while k < len(text) and depth > 0:
            c = text[k]
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    break
            elif c == ',' and depth == 1 and top_comma == -1:
                top_comma = k
            k += 1
        args = text[start:k]
        if top_comma != -1:
            out.append("ResourceLocation.fromNamespaceAndPath(" + args + ")")
        else:
            out.append("ResourceLocation.parse(" + args + ")")
        i = k + 1
    return "".join(out)

count = 0
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith(".java"):
            continue
        p = os.path.join(dirpath, f)
        with open(p, "r", encoding="utf-8") as fh:
            orig = fh.read()
        if NEEDLE not in orig:
            continue
        new = convert(orig)
        if new != orig:
            with open(p, "w", encoding="utf-8") as fh:
                fh.write(new)
            count += 1
print(f"converted ResourceLocation in {count} files")
