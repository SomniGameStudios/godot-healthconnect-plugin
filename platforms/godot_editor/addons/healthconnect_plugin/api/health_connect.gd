extends Node

## HealthConnect Singleton
##
## This singleton provides access to Google Health Connect on Android.
## It falls back to mock data or empty responses on other platforms.

signal connected()
signal today_steps(steps: int)
signal step_detected()
signal step_count_updated(count: int)
signal activity_permission_result(granted: bool)

var _plugin: Object = null
var _plugin_name := "HealthConnectPlugin"

func _ready() -> void:
	if Engine.has_singleton(_plugin_name):
		_plugin = Engine.get_singleton(_plugin_name)
		_plugin.connect("connected", func(): connected.emit())
		_plugin.connect("today_steps", func(steps): today_steps.emit(steps))
		_plugin.connect("step_detected", func(): step_detected.emit())
		_plugin.connect("step_count_updated", func(count): step_count_updated.emit(count))
		_plugin.connect("activity_permission_result", func(granted): activity_permission_result.emit(granted))
		print("HealthConnect: Plugin found and signals connected.")
	else:
		print("HealthConnect: Plugin not found. Using mock data.")

## Check if Health Connect app is installed
func is_installed() -> bool:
	if _plugin:
		return _plugin.checkHealthConnectInstalled()
	return false

## Check if Health Connect app is updated
func is_updated() -> bool:
	if _plugin:
		return _plugin.checkHealthConnectUpdated()
	return true

## Open Play Store to install Health Connect
func prompt_install() -> void:
	if _plugin:
		_plugin.promptHealthConnectInstall()

## Open Play Store to update Health Connect
func prompt_update() -> void:
	if _plugin:
		_plugin.promptHealthConnectUpdate()

## Request Health Connect permissions
func request_permissions() -> void:
	if _plugin:
		_plugin.requestHealthConnectPermissions()
	else:
		# Mock success in editor
		permissions_granted.emit(true)

## Request activity/sensor permissions
func request_activity_permission() -> void:
	if _plugin:
		_plugin.requestActivityPermission()

## Check if Health Connect permissions are granted
func are_permissions_granted() -> bool:
	if _plugin:
		return _plugin.arePermissionsGranted()
	return true

## Check if activity/sensor permissions are granted
func is_activity_permission_granted() -> bool:
	if _plugin:
		return _plugin.isActivityPermissionGranted()
	return true

## Start step sensors
func start_step_sensors() -> void:
	if _plugin:
		_plugin.startStepSensors()

## Get today's total steps
func get_today_steps() -> int:
	if _plugin:
		return _plugin.getTodaySteps()
	return 0

## Get yesterday's total steps
func get_yesterday_steps() -> int:
	if _plugin:
		return _plugin.getYesterdaySteps()
	return 0

## Get steps since last check (delta)
func get_steps_since_last_check() -> int:
	if _plugin:
		return _plugin.getStepsSinceLastCheck()
	return 0
