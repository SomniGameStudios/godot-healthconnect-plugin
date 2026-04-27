---
name: gdscript_standards
description: Project-specific GDScript coding standards and patterns.
---

# GDScript Standards

## Type Safety
- Always use the `:=` operator for type inference to ensure static typing.
  - ✅ `var count := 0`
  - ❌ `var count = 0`

## Internal Folders
- Any folder named `internal` within the plugin structure must follow these rules:
  1. No scripts may use the `class_name` keyword.
  2. Scripts must be loaded via `preload("path/to/script.gd")`.

## Architecture
- Strictly follow **SOLID** principles.
- Use meaningful names and maintain separation of concerns between the HealthKit interface and the Godot plugin logic.
