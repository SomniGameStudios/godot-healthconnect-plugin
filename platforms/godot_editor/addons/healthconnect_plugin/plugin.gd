@tool
extends EditorPlugin

const SINGLETON_NAME := "HealthConnect"
const SINGLETON_PATH := "res://addons/healthconnect_plugin/api/health_connect.gd"

var export_plugin: EditorExportPlugin

func _enter_tree() -> void:
	add_autoload_singleton(SINGLETON_NAME, SINGLETON_PATH)

	var ExportScript = load("res://addons/healthconnect_plugin/export/export_plugin.gd")
	if ExportScript:
		export_plugin = ExportScript.new()
		add_export_plugin(export_plugin)
		print("HealthConnect: Export plugin registered.")
	else:
		printerr("HealthConnect: Could not load export script.")

func _exit_tree() -> void:
	remove_autoload_singleton(SINGLETON_NAME)

	if export_plugin:
		remove_export_plugin(export_plugin)
		export_plugin = null
