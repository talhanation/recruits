#!/usr/bin/env python3
"""Bulk Forge -> NeoForge import & token migration for Recruits (1.20.1 -> 1.21.1).
Mechanical layer only. Networking messages are handled by a separate script.
Non-mechanical APIs (TickEvent, NetworkHooks, PacketDistributor send, ForgeChunkManager,
TextureStitchEvent, RenderGuiOverlayEvent, capabilities, SimpleChannel) are left to
surface as compile errors and fixed by hand.
"""
import os, re, sys

ROOT = "src/main/java"

# Exact import-line replacements (renamed classes). Applied before prefix rules.
EXACT_IMPORTS = {
    "import net.minecraftforge.common.MinecraftForge;": "import net.neoforged.neoforge.common.NeoForge;",
    "import net.minecraftforge.common.ForgeConfigSpec;": "import net.neoforged.neoforge.common.ModConfigSpec;",
    "import net.minecraftforge.common.ForgeMod;": "import net.neoforged.neoforge.common.NeoForgeMod;",
    "import net.minecraftforge.common.ForgeSpawnEggItem;": "import net.neoforged.neoforge.common.DeferredSpawnEggItem;",
    "import net.minecraftforge.common.ToolActions;": "import net.neoforged.neoforge.common.ItemAbilities;",
    "import net.minecraftforge.common.extensions.IForgeMenuType;": "import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;",
    "import net.minecraftforge.common.extensions.IForgeEntity;": "import net.neoforged.neoforge.common.extensions.IEntityExtension;",
    "import net.minecraftforge.registries.RegistryObject;": "import net.neoforged.neoforge.registries.DeferredHolder;",
    "import net.minecraftforge.registries.ForgeRegistries;": "import net.minecraft.core.registries.BuiltInRegistries;",
}

# Prefix rewrites (longest-first). For unambiguous package moves.
PREFIX_RULES = [
    ("net.minecraftforge.api.distmarker.", "net.neoforged.api.distmarker."),
    ("net.minecraftforge.eventbus.api.", "net.neoforged.bus.api."),
    ("net.minecraftforge.fml.javafmlmod.", "net.neoforged.fml.javafmlmod."),  # FMLJavaModLoadingContext removed; fixed in Main.java
    ("net.minecraftforge.fml.", "net.neoforged.fml."),
    ("net.minecraftforge.client.gui.widget.", "net.neoforged.neoforge.client.gui.widget."),
    ("net.minecraftforge.client.model.data.", "net.neoforged.neoforge.client.model.data."),
    ("net.minecraftforge.client.event.", "net.neoforged.neoforge.client.event."),
    ("net.minecraftforge.event.", "net.neoforged.neoforge.event."),
    ("net.minecraftforge.network.", "net.neoforged.neoforge.network."),
    ("net.minecraftforge.registries.", "net.neoforged.neoforge.registries."),
    ("net.minecraftforge.common.capabilities.", "net.neoforged.neoforge.capabilities."),
    ("net.minecraftforge.common.extensions.", "net.neoforged.neoforge.common.extensions."),
    ("net.minecraftforge.common.world.", "net.neoforged.neoforge.common.world."),
    ("net.minecraftforge.common.", "net.neoforged.neoforge.common."),
    ("net.minecraftforge.server.command.", "net.neoforged.neoforge.server.command."),
]

# Whole-word token replacements in code bodies (renamed type simple names & members).
TOKEN_RULES = [
    (r"\bMinecraftForge\.EVENT_BUS\b", "NeoForge.EVENT_BUS"),
    (r"\bForgeConfigSpec\b", "ModConfigSpec"),
    (r"\bForgeMod\b", "NeoForgeMod"),
    (r"\bToolActions\b", "ItemAbilities"),
    (r"\bIForgeMenuType\b", "IMenuTypeExtension"),
    (r"\bIForgeEntity\b", "IEntityExtension"),
    (r"\bForgeSpawnEggItem\b", "DeferredSpawnEggItem"),
    (r"\bForgeRegistries\.ENTITY_TYPES\b", "BuiltInRegistries.ENTITY_TYPE"),
    (r"\bForgeRegistries\.ITEMS\b", "BuiltInRegistries.ITEM"),
    (r"\bForgeRegistries\.BLOCKS\b", "BuiltInRegistries.BLOCK"),
    (r"\bForgeRegistries\.MENU_TYPES\b", "BuiltInRegistries.MENU"),
    (r"\bForgeRegistries\.POI_TYPES\b", "BuiltInRegistries.POINT_OF_INTEREST_TYPE"),
    (r"\bForgeRegistries\.VILLAGER_PROFESSIONS\b", "BuiltInRegistries.VILLAGER_PROFESSION"),
]

def migrate(text):
    lines = text.split("\n")
    out = []
    for line in lines:
        stripped = line.strip()
        if stripped in EXACT_IMPORTS:
            indent = line[:len(line) - len(line.lstrip())]
            out.append(indent + EXACT_IMPORTS[stripped])
            continue
        # prefix import rewrite (only on import lines)
        if stripped.startswith("import "):
            for old, new in PREFIX_RULES:
                if old in line:
                    line = line.replace(old, new)
                    break
        out.append(line)
    text = "\n".join(out)
    # token rules across whole file
    for pat, repl in TOKEN_RULES:
        text = re.sub(pat, repl, text)
    return text

changed = 0
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith(".java"):
            continue
        p = os.path.join(dirpath, f)
        with open(p, "r", encoding="utf-8") as fh:
            orig = fh.read()
        new = migrate(orig)
        if new != orig:
            with open(p, "w", encoding="utf-8") as fh:
                fh.write(new)
            changed += 1
print(f"migrated {changed} files")
