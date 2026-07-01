#!/usr/bin/env python3
"""Transform corelib Forge Message classes -> corelib 2.x (NeoForge payload) Message classes.

Per message class:
  - Dist getExecutingSide()      -> PacketFlow getExecutingSide()
    return Dist.DEDICATED_SERVER -> return PacketFlow.SERVERBOUND
    return Dist.CLIENT           -> return PacketFlow.CLIENTBOUND
  - execute{Server,Client}Side(NetworkEvent.Context) -> (IPayloadContext)
  - fromBytes/toBytes(FriendlyByteBuf) -> (RegistryFriendlyByteBuf)
  - context.getSender() -> ((ServerPlayer) context.player())
  - add CustomPacketPayload.Type<X> TYPE field + type() method
  - ensure required imports, drop NetworkEvent import
"""
import os, re

NET_DIR = "src/main/java/com/talhanation/recruits/network"

NEEDED_IMPORTS = [
    "import net.minecraft.network.RegistryFriendlyByteBuf;",
    "import net.minecraft.network.protocol.PacketFlow;",
    "import net.minecraft.network.protocol.common.custom.CustomPacketPayload;",
    "import net.minecraft.resources.ResourceLocation;",
    "import net.neoforged.neoforge.network.handling.IPayloadContext;",
]

def transform(text, classname):
    uses_sender = "context.getSender()" in text

    # drop NetworkEvent import (was rewritten from forge by the general pass)
    text = re.sub(r"^\s*import net\.neoforged\.neoforge\.network\.NetworkEvent;\s*\n", "", text, flags=re.M)

    # FriendlyByteBuf -> RegistryFriendlyByteBuf (import + all type refs)
    text = text.replace("import net.minecraft.network.FriendlyByteBuf;", "import net.minecraft.network.RegistryFriendlyByteBuf;")
    text = re.sub(r"\bFriendlyByteBuf\b", "RegistryFriendlyByteBuf", text)
    # guard against double-rewrite of an already-Registry buf
    text = text.replace("RegistryRegistryFriendlyByteBuf", "RegistryFriendlyByteBuf")

    # getExecutingSide signature + returns
    text = text.replace("public Dist getExecutingSide()", "public PacketFlow getExecutingSide()")
    text = text.replace("return Dist.DEDICATED_SERVER;", "return PacketFlow.SERVERBOUND;")
    text = text.replace("return Dist.CLIENT;", "return PacketFlow.CLIENTBOUND;")

    # execute side signatures
    text = text.replace("executeServerSide(NetworkEvent.Context context)", "executeServerSide(IPayloadContext context)")
    text = text.replace("executeClientSide(NetworkEvent.Context context)", "executeClientSide(IPayloadContext context)")
    # any other param name
    text = re.sub(r"execute(Server|Client)Side\(NetworkEvent\.Context (\w+)\)", r"execute\1Side(IPayloadContext \2)", text)

    # context.getSender() -> ((ServerPlayer) context.player())
    text = text.replace("context.getSender()", "((ServerPlayer) context.player())")

    # ensure imports
    needed = list(NEEDED_IMPORTS)
    if uses_sender:
        needed.append("import net.minecraft.server.level.ServerPlayer;")
    to_add = [imp for imp in needed if imp not in text]
    if to_add:
        # insert after package line
        m = re.search(r"^(package [^\n]*\n)", text, flags=re.M)
        insert_at = m.end()
        block = "\n".join(to_add) + "\n"
        text = text[:insert_at] + block + text[insert_at:]

    # add TYPE field after the class declaration opening brace
    msgid = classname.lower()
    type_field = (
        f"    public static final CustomPacketPayload.Type<{classname}> TYPE =\n"
        f"            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(\"recruits\", \"{msgid}\"));\n"
    )
    decl = re.search(r"(public class " + re.escape(classname) + r"[^\{]*\{\s*\n)", text)
    if decl and "CustomPacketPayload.Type<" + classname + "> TYPE" not in text:
        text = text[:decl.end()] + type_field + text[decl.end():]

    # add type() method before the final closing brace
    if "public CustomPacketPayload.Type<" + classname + "> type()" not in text:
        type_method = (
            "\n    @Override\n"
            f"    public CustomPacketPayload.Type<{classname}> type() {{\n"
            "        return TYPE;\n"
            "    }\n"
        )
        idx = text.rfind("}")
        text = text[:idx] + type_method + text[idx:]

    return text

count = 0
for dirpath, _, files in os.walk(NET_DIR):
    for f in files:
        if not f.endswith(".java"):
            continue
        p = os.path.join(dirpath, f)
        with open(p, "r", encoding="utf-8") as fh:
            orig = fh.read()
        if "implements Message<" not in orig:
            continue
        m = re.search(r"public class (\w+) implements Message<", orig)
        if not m:
            print("WARN: could not find class name in", p)
            continue
        classname = m.group(1)
        new = transform(orig, classname)
        if new != orig:
            with open(p, "w", encoding="utf-8") as fh:
                fh.write(new)
            count += 1
print(f"transformed {count} message classes")
