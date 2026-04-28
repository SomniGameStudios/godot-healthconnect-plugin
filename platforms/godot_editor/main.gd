extends Control

@onready var status_label: Label = %StatusLabel
@onready var perm_label: Label = %PermLabel
@onready var today_steps_label: Label = %TodayStepsLabel
@onready var total_steps_label: Label = %TotalStepsLabel
@onready var live_steps_label: Label = %LiveStepsLabel
@onready var log_label: RichTextLabel = %LogLabel

func _ready() -> void:
	_log("Demo started.")
	
	# Connect to HealthConnect API signals
	HealthConnect.permission_result.connect(_on_permission_result)
	HealthConnect.steps_updated.connect(_on_steps_updated)
	HealthConnect.pedometer_steps_updated.connect(_on_pedometer_steps_updated)
	HealthConnect.today_steps_ready.connect(_on_today_steps_ready)
	HealthConnect.total_steps_ready.connect(_on_total_steps_ready)
	HealthConnect.period_steps_ready.connect(_on_period_steps_ready)
	HealthConnect.connected.connect(_on_connected)
	
	# Connect UI buttons
	$MarginContainer/VBoxContainer/BtnRequestPerm.pressed.connect(_on_btn_request_perm_pressed)
	$MarginContainer/VBoxContainer/BtnStartObserver.pressed.connect(_on_btn_start_observer_pressed)
	$MarginContainer/VBoxContainer/BtnStopObserver.pressed.connect(_on_btn_stop_observer_pressed)
	$MarginContainer/VBoxContainer/BtnTodayQuery.pressed.connect(_on_btn_today_query_pressed)
	$MarginContainer/VBoxContainer/BtnTotalQuery.pressed.connect(_on_btn_total_query_pressed)
	$MarginContainer/VBoxContainer/BtnPeriodQuery.pressed.connect(_on_btn_period_query_pressed)

	_update_permission_status()

func _log(msg: String) -> void:
	print(msg)
	log_label.text += "\n" + msg

func _update_permission_status() -> void:
	var status = HealthConnect.get_permission_status()
	var status_str = "NOT_DETERMINED"
	if status == HealthConnect.AuthorizationStatus.AUTHORIZED:
		status_str = "AUTHORIZED"
	elif status == HealthConnect.AuthorizationStatus.DENIED:
		status_str = "DENIED"
	perm_label.text = "Permission: " + status_str
	_log("Current permission status: " + status_str)

# --- UI Button Handlers ---

func _on_btn_request_perm_pressed() -> void:
	_log("Requesting permission...")
	HealthConnect.request_permission()

func _on_btn_start_observer_pressed() -> void:
	_log("Starting step observer...")
	if HealthConnect.is_pedometer_available():
		_log("Pedometer is available, using pedometer observer.")
		HealthConnect.start_pedometer_observer()
	else:
		_log("Pedometer not available, using standard step observer.")
		HealthConnect.start_step_observer()

func _on_btn_stop_observer_pressed() -> void:
	_log("Stopping observers...")
	HealthConnect.stop_pedometer_observer()
	HealthConnect.stop_step_observer()

func _on_btn_today_query_pressed() -> void:
	_log("Running today's steps query...")
	HealthConnect.run_today_steps_query()

func _on_btn_total_query_pressed() -> void:
	_log("Running total steps query...")
	HealthConnect.run_total_steps_query()

func _on_btn_period_query_pressed() -> void:
	_log("Running period query for last 7 days...")
	HealthConnect.run_period_steps_query(7)

# --- HealthConnect Signal Handlers ---

func _on_connected() -> void:
	status_label.text = "Status: Connected to Native Plugin"
	_log("Received connected signal.")

func _on_permission_result(granted: bool) -> void:
	_log("Permission result received: " + str(granted))
	_update_permission_status()

func _on_steps_updated(steps: int) -> void:
	live_steps_label.text = "Live/Pedometer Steps: " + str(steps)
	_log("Live steps updated: " + str(steps))

func _on_pedometer_steps_updated(steps: int) -> void:
	live_steps_label.text = "Live/Pedometer Steps: " + str(steps)
	_log("Pedometer steps updated: " + str(steps))

func _on_today_steps_ready(steps: int) -> void:
	today_steps_label.text = "Today's Steps: " + str(steps)
	_log("Today steps ready: " + str(steps))

func _on_total_steps_ready(steps: int) -> void:
	total_steps_label.text = "Total Steps: " + str(steps)
	_log("Total steps ready: " + str(steps))

func _on_period_steps_ready(steps_dict: Dictionary) -> void:
	_log("Period steps ready:")
	for date in steps_dict:
		_log(" - " + date + ": " + str(steps_dict[date]))
