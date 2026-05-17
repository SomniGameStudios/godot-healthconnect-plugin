## HealthConnect autoload — wraps the HealthConnectPlugin Android singleton.
## On non-Android platforms, returns mock data for editor testing.
## API mirrors godot-healthkit-plugin/health_kit.gd for cross-platform consistency.

extends Node

signal permission_result(granted: bool)
signal steps_updated(steps: int)
signal pedometer_steps_updated(steps: int)
signal pedometer_error(reason: String)
signal today_steps_ready(steps: int)
signal total_steps_ready(steps: int)
signal period_steps_ready(steps_dict: Dictionary)

# Low-level sensor signals (kept for compatibility)
signal step_detected()
signal step_count_updated(count: int)
signal connected()
signal disconnected()

var _plugin: Object = null
var _is_android := false

enum AuthorizationStatus {
	NOT_DETERMINED = 0,
	DENIED = 1,
	AUTHORIZED = 2,
}

func _ready() -> void:
	_is_android = OS.get_name() == "Android"

	if _is_android:
		if Engine.has_singleton("HealthConnectPlugin"):
			_plugin = Engine.get_singleton("HealthConnectPlugin")
			_plugin.connect("permission_result", func(g): permission_result.emit(g))
			_plugin.connect("today_steps_ready", func(s): today_steps_ready.emit(s))
			_plugin.connect("total_steps_ready", func(s): total_steps_ready.emit(s))
			_plugin.connect("period_steps_ready", func(d): period_steps_ready.emit(d))
			_plugin.connect("steps_updated", func(s): steps_updated.emit(s))
			_plugin.connect("pedometer_steps_updated", func(s): pedometer_steps_updated.emit(s))
			_plugin.connect("pedometer_error", func(r): pedometer_error.emit(r))
			_plugin.connect("step_detected", func(): step_detected.emit())
			_plugin.connect("step_count_updated", func(c): step_count_updated.emit(c))
			_plugin.connect("sensor_connected", func(): connected.emit())
			_plugin.connect("sensor_disconnected", func(): disconnected.emit())
			print("HealthConnect: Android plugin initialized")
		else:
			printerr("HealthConnect: HealthConnectPlugin singleton not found")
	else:
		print("HealthConnect: Non-Android platform, using mock data")

# --- Permission ---

func request_permission() -> void:
	if _plugin:
		_plugin.requestPermission()
	else:
		permission_result.emit.call_deferred(true)

func get_permission_status() -> int:
	if _plugin:
		return _plugin.getPermissionStatus()
	return AuthorizationStatus.AUTHORIZED

func is_health_data_available() -> bool:
	if _plugin:
		return _plugin.isHealthDataAvailable()
	return true

func open_settings() -> void:
	if _plugin:
		_plugin.openSettings()
	else:
		print("HealthConnect: Mock open_settings() called")

# --- Step Observer (mirrors start_step_observer from iOS) ---

func start_step_observer() -> void:
	if _plugin:
		_plugin.startStepObserver()

func stop_step_observer() -> void:
	if _plugin:
		_plugin.stopStepObserver()

# --- Pedometer (mirrors CMPedometer API from iOS) ---

func is_pedometer_available() -> bool:
	if _plugin:
		return _plugin.isPedometerAvailable()
	return false

func get_pedometer_permission_status() -> int:
	if _plugin:
		return _plugin.getPedometerPermissionStatus()
	return AuthorizationStatus.AUTHORIZED

func start_pedometer_observer() -> void:
	if _plugin:
		_plugin.startPedometerObserver()

func stop_pedometer_observer() -> void:
	if _plugin:
		_plugin.stopPedometerObserver()

func get_live_pedometer_steps() -> int:
	if _plugin:
		return _plugin.getLivePedometerSteps()
	return 0

# --- Step Queries (async, mirror iOS HKStatisticsQuery pattern) ---

func run_today_steps_query() -> void:
	if _plugin:
		_plugin.runTodayStepsQuery()
	else:
		today_steps_ready.emit.call_deferred(1234)

func run_total_steps_query() -> void:
	if _plugin:
		_plugin.runTotalStepsQuery()
	else:
		total_steps_ready.emit.call_deferred(56789)

func run_period_steps_query(days: int) -> void:
	if _plugin:
		_plugin.runPeriodStepsQuery(days)
	else:
		var mock: Dictionary = {}
		var today: Dictionary = Time.get_date_dict_from_system()
		for i in range(days):
			var date: String = Time.get_date_string_from_unix_time(
				Time.get_unix_time_from_datetime_dict(today) - i * 86400
			)
			mock[date] = randi_range(2000, 12000)
		period_steps_ready.emit.call_deferred(mock)

# --- Sync getters (cached values, valid after running the corresponding query) ---

func get_today_steps() -> int:
	if _plugin:
		return _plugin.getTodaySteps()
	return 1234

func get_total_steps() -> int:
	if _plugin:
		return _plugin.getTotalSteps()
	return 56789

func get_period_steps_dict() -> Dictionary:
	if _plugin:
		return _plugin.getPeriodStepsDict()
	var mock: Dictionary = {}
	var today: Dictionary = Time.get_date_dict_from_system()
	for i in range(7):
		var date: String = Time.get_date_string_from_unix_time(
			Time.get_unix_time_from_datetime_dict(today) - i * 86400
		)
		mock[date] = randi_range(2000, 12000)
	return mock
