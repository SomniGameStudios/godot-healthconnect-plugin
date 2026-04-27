# Agent Guide: Godot HealthConnect Plugin

This document provides essential context and rules for AI agents working on the `godot-healthconnect-plugin`.

## 🎯 Project Mission
Integrate Google Health Connect functionality into Godot games for Android, providing a clean GDScript API to access health and fitness data.

## 🛠 Working Rules (MANDATORY)
1.  **Language**: Always communicate and write documentation/code in **English**.
2.  **GDScript Standards**:
    *   Use static typing with the `:=` operator whenever possible (e.g., `var health_value := 0.0`).
    *   Follow **SOLID** principles.
3.  **Architecture**:
    *   The "internal" folder in the addon **MUST NOT** have any script with `class_name`.
    *   Scripts in the "internal" folder must be used with `preload()`.
4.  **Version Control**: **NEVER** commit directly. Propose changes through the planning and execution workflow.
5.  **Comments**: Only add comments if the code is genuinely complex or explicitly requested.

## 📂 Agent Resources
*   `.agents/skills/`: Detailed technical manuals for specific tasks.
*   `.agents/workflows/`: Standardized multi-step procedures for the project.

---
*Last Updated: 2026-04-27*
