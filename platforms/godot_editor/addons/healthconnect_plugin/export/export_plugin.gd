@tool
extends EditorExportPlugin

const PLUGIN_NAME := "HealthConnectPlugin"
const ADDON_PATH := "res://addons/healthconnect_plugin/"

const HEALTH_CONNECT_QUERY: String = """
<queries>
	<package android:name="com.google.android.apps.healthdata" />
</queries>
"""

func _supports_platform(platform: EditorExportPlatform) -> bool:
	if platform is EditorExportPlatformAndroid:
		return true
	return false

func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
	if debug:
		return PackedStringArray([ADDON_PATH + "bin/" + PLUGIN_NAME + "-debug.aar"])
	else:
		return PackedStringArray([ADDON_PATH + "bin/" + PLUGIN_NAME + "-release.aar"])

func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
	if not _supports_platform(platform):
		return PackedStringArray()

	return PackedStringArray([
		"androidx.appcompat:appcompat:1.7.0",
	])

func _get_android_dependencies_maven_repos(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
	return PackedStringArray(["https://maven.google.com/"])

func _get_android_manifest_element_contents(platform: EditorExportPlatform, debug: bool) -> String:
	return HEALTH_CONNECT_QUERY

func _get_android_manifest_application_element_contents(platform: EditorExportPlatform, debug: bool) -> String:
	return ""

func _get_name() -> String:
	return PLUGIN_NAME