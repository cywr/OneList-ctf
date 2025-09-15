# 🎯 OneList CTF Challenge

Welcome to the OneList Android Reverse Engineering CTF! This application contains **10 hidden flags** with the format `CYWR{...}` that progressively increase in difficulty.

## 🚀 Getting Started

1. Install the APK on an Android device or emulator
2. Use your favorite reverse engineering tools (jadx, apktool, frida, etc.)
3. Find all 10 flags to complete the challenge!

## 📋 Challenge Overview

**Target:** Find 10 flags in total
**Format:** `CYWR{flag_content_here}`
**Difficulty:** Beginner → Expert

## 💡 Hints & Tips

### 🔍 Static Analysis (Flags 1-5)
**Tools:** jadx, apktool, strings, grep, hex editors

- **Flag 1**: Start with the basics - sometimes the simplest approach works
- **Flag 2**: Not all builds are created equal - check different build variants
- **Flag 3**: Developers often leave breadcrumbs in utility classes - look for processing functions
- **Flag 4**: Database configurations need validation - analyze the signature processing logic
- **Flag 5**: Manifest files hold more than just permissions

### 🏃 Dynamic Analysis (Flags 6-10)
**Tools:** frida, adb logcat, sqlite3, objection, dynamic analysis

- **Flag 6**: Your actions in the app matter - try creating and managing lists
- **Flag 7**: Some flags only appear when you follow specific patterns with your data
- **Flag 8**: Settings screens sometimes hide developer options - persistence pays off
- **Flag 9**: The app title isn't just decorative - try different interaction methods
- **Flag 10**: The ultimate challenge requires understanding cryptography and runtime behavior

## 🛠️ Recommended Tools

### Static Analysis
- **jadx** - Java decompiler for Android APK
- **apktool** - Resource extraction and analysis
- **strings** - Extract string constants
- **grep/ripgrep** - Search through code
- **hexdump/xxd** - Binary analysis

### Dynamic Analysis
- **frida** - Runtime instrumentation
- **adb logcat** - Monitor application logs
- **adb shell** - Access device filesystem and databases
- **sqlite3** - Inspect SQLite databases
- **objection** - Runtime mobile exploration

## 🎓 Learning Objectives

This CTF will teach you:
- Android APK analysis techniques
- Static vs dynamic analysis approaches
- Algorithm discovery through reverse engineering
- Common encoding schemes (Base64, ROT13, Hex, XOR, custom ciphers)
- Android-specific reverse engineering (SharedPreferences, Room databases)
- Advanced techniques (DexClassLoader, reflection, cryptography)
- Runtime instrumentation and debugging
- Multi-layer encoding chains and decryption

## ⚡ Progressive Difficulty

- **Flags 1-2**: Basic static analysis and simple encoding
- **Flags 3-4**: Algorithm discovery and multi-layer encoding
- **Flag 5**: Hidden resources and manifest analysis
- **Flags 6-7**: Runtime behavior and data storage
- **Flags 8-10**: Advanced techniques, cryptography, and dynamic instrumentation

## 🏆 Completion

Once you find all 10 flags, you've mastered Android reverse engineering fundamentals!

Good luck, and happy hacking! 🔓

---
*This CTF is based on the open-source 1List application for educational purposes.*