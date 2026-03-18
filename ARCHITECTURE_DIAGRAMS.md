# Bluetooth Scanning - Visual Architecture Guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    BLUETOOTH SYSTEM                         │
└─────────────────────────────────────────────────────────────┘
              ↓                              ↓
    ┌───────────────────┐         ┌──────────────────┐
    │ Android OS        │         │ Nearby Devices   │
    │ Bluetooth Stack   │◄────────│ (Peer Devices)   │
    └────────┬──────────┘         └──────────────────┘
             │
      ACTION_FOUND
        (Intent)
             │
             ▼
    ┌────────────────────┐
    │ FoundDeviceReceiver│ ◄─── BroadcastReceiver
    └────────┬───────────┘
             │
        onReceive()
             │
             ▼
    ┌─────────────────────────────────────────┐
    │ AndroidBluetoothController              │
    │                                         │
    │  ┌──────────────────────────────────┐  │
    │  │ foundDeviceReceiver callback     │  │
    │  │                                  │  │
    │  │ ┌────────────────────────────┐  │  │
    │  │ │ _scannedDevices.update()   │  │  │
    │  │ └────────────────────────────┘  │  │
    │  │                                  │  │
    │  │ ┌────────────────────────────┐  │  │
    │  │ │ _discoveredDevices.update()│  │  │
    │  │ │ (UI observes this) ✅      │  │  │
    │  │ └────────────────────────────┘  │  │
    │  └──────────────────────────────────┘  │
    │                                         │
    └────────┬────────────────────────────────┘
             │
        StateFlow emits
             │
             ▼
    ┌────────────────────┐
    │ UI Layer           │
    │                    │
    │ discoveredDevices  │
    │ .collect { ... }   │
    │                    │
    │ Shows device list  │
    └────────────────────┘
```

---

## State Flow Diagram

### BEFORE FIX (Broken)
```
BroadcastReceiver
      │
      ├─► Update _scannedDevices ──► [Device1, Device2, ...]
      │                              (Internal only, not used)
      │
      └─► Update _discoveredDevices ──► []  (EMPTY!)
                                        ↑
                                        │ UI observes
                                        └─► "No devices"
```

### AFTER FIX (Working)
```
BroadcastReceiver
      │
      ├─► Update _scannedDevices ──────┐
      │                                 │
      │                                 ├─► Synced Devices
      │                                 │   [Device1, Device2]
      └─► Update _discoveredDevices ───┘
                                        ↑
                                        │ UI observes
                                        └─► "Found 2 devices"
```

---

## Registration State Machine

```
                    START
                      │
                      ▼
          ┌───────────────────────┐
          │ isReceiverRegistered? │
          └───────────┬───────────┘
                      │
            ┌─────────┴────────┐
            │                  │
           NO                 YES
            │                  │
            ▼                  ▼
    ┌──────────────────┐   ┌─────────────────┐
    │ Try Register     │   │ Skip (Already   │
    │ Receiver         │   │ Registered)     │
    └────────┬─────────┘   └────────┬────────┘
             │                      │
      ┌──────┴──────┐               │
      │             │               │
    SUCCESS       ERROR            │
      │             │              │
      ▼             ▼              ▼
    ┌──────────┐  ┌──────────┐   ┌──────────┐
    │ Set      │  │ Log      │   │ Continue │
    │ Flag=TRUE│  │ Error    │   │ Normally │
    │ Log OK   │  │ Log OK   │   │ Log OK   │
    └────┬─────┘  └────┬─────┘   └────┬─────┘
         │             │              │
         └─────────────┴──────────────┘
                       │
                       ▼
              ┌─────────────────────┐
              │ Start Discovery     │
              │ bluetoothAdapter    │
              │ .startDiscovery()   │
              └─────────────────────┘
```

---

## Receiver Lifecycle

```
┌─────────────────────────────────────────────────────────┐
│           BROADCAST RECEIVER LIFECYCLE                  │
└─────────────────────────────────────────────────────────┘

startDiscovery()
    │
    ├─► Check permission
    │       ✓ Pass ──┐
    │       ✗ Fail ──┼─► Log error & return
    │               │
    ├─► Check flag (isReceiverRegistered)
    │       │
    │       ├─ FALSE: ┐
    │       │         │
    │       │         ▼
    │       │    Register Receiver
    │       │    with IntentFilter
    │       │         │
    │       │    ┌────┴────┐
    │       │    │          │
    │       │  SUCCESS    ERROR
    │       │    │          │
    │       │    ▼          ▼
    │       │  Set flag   Log error
    │       │  Log OK     Continue
    │       │    │          │
    │       ├─ TRUE: Skip (already registered)
    │       │
    ├─► Call bluetoothAdapter.startDiscovery()
    │
    └─► Log result

─────────────────────────────────────────────────────────

[Waiting for devices...]

ACTION_FOUND intent received
    │
    ▼
FoundDeviceReceiver.onReceive()
    │
    ├─► Update _scannedDevices
    │
    ├─► Update _discoveredDevices ← UI receives update here
    │
    └─► StateFlow emits to collectors

─────────────────────────────────────────────────────────

stopDiscovery()
    │
    ├─► Check flag (isReceiverRegistered)
    │       │
    │       ├─ TRUE: ┐
    │       │        │
    │       │        ▼
    │       │   Unregister Receiver
    │       │   Set flag = FALSE
    │       │   Log OK
    │       │
    │       └─ FALSE: Skip (not registered)
    │
    ├─► Call bluetoothAdapter.cancelDiscovery()
    │
    └─► Log result
```

---

## Data Flow

```
┌──────────────────────────────────────────────────────────┐
│              DISCOVERED DEVICE DATA FLOW                 │
└──────────────────────────────────────────────────────────┘

1. DISCOVERY STARTS
   startDiscovery()
   │
   └─► Register FoundDeviceReceiver
       │
       └─► Intent filter: ACTION_FOUND

2. DEVICE DETECTED
   Bluetooth Stack detects device
   │
   └─► Broadcasts ACTION_FOUND intent with BluetoothDevice

3. RECEIVER INVOKED
   FoundDeviceReceiver.onReceive(context, intent)
   │
   └─► Extract BluetoothDevice from intent

4. CALLBACK EXECUTED
   Callback: { device: BluetoothDevice ->
   │
   ├─► Convert to domain: device.toBluetoothDeviceDomain()
   │
   ├─► Update _scannedDevices StateFlow
   │   │
   │   └─► _scannedDevices.update { devices ->
   │        if (newDevice !in devices)
   │            devices + newDevice
   │
   └─► Update _discoveredDevices StateFlow ← CRITICAL FIX
       │
       └─► _discoveredDevices.update { devices ->
            if (newDevice !in devices)
                devices + newDevice

5. UI UPDATES
   discoveredDevices.collect { devices ->
   │
   ├─► devices = [Device1, Device2, ...] ✅
   │
   └─► Display devices to user

6. DISCOVERY STOPS
   stopDiscovery()
   │
   ├─► Unregister FoundDeviceReceiver
   │
   └─► Call bluetoothAdapter.cancelDiscovery()
```

---

## Threading Model

```
┌─────────────────────────────────────────────────────────┐
│              THREAD SAFETY ARCHITECTURE                 │
└─────────────────────────────────────────────────────────┘

Main Thread (UI)
    │
    ├─► startDiscovery()
    │       └─► Sets isReceiverRegistered = true
    │
    ├─► Observes discoveredDevices.collect()
    │       └─► Receives StateFlow updates
    │
    └─► stopDiscovery()
            └─► Sets isReceiverRegistered = false

Bluetooth Service Thread
    │
    └─► ACTION_FOUND broadcast
            │
            ├─► FoundDeviceReceiver.onReceive()
            │   │
            │   └─► Callback lambda executes
            │       │
            │       ├─► _scannedDevices.update()
            │       │   (StateFlow handles threading)
            │       │
            │       └─► _discoveredDevices.update()
            │           (StateFlow handles threading)
            │
            └─► StateFlow emits to collectors (on Dispatchers.Main)

@Volatile Flag
    │
    ├─► Ensures visibility across threads
    │
    ├─► Main thread can read/write safely
    │
    └─► Service thread can read safely
```

---

## Permission Flow

```
┌───────────────────────────────────────────────────────┐
│           PERMISSION CHECKING FLOW                    │
└───────────────────────────────────────────────────────┘

startDiscovery()
    │
    ▼
hasPermission(BLUETOOTH_SCAN)?
    │
    ├─ NO ──► Log.e("Missing BLUETOOTH_SCAN permission")
    │        Return (discovery not started)
    │
    └─ YES ──► Continue to receiver registration
             │
             └─► registerReceiver()
                 (Requires BLUETOOTH_SCAN at runtime)

stopDiscovery()
    │
    ▼
hasPermission(BLUETOOTH_SCAN)?
    │
    ├─ NO ──► Return (skip unregistration)
    │
    └─ YES ──► Continue to unregister
             │
             └─► unregisterReceiver()

connect()
    │
    ▼
hasPermission(BLUETOOTH_CONNECT)?
    │
    ├─ NO ──► Throw SecurityException
    │
    └─ YES ──► Continue to connect
             │
             └─► Create socket & connect
```

---

## Error Handling Flow

```
┌────────────────────────────────────────────────────┐
│          ERROR HANDLING & LOGGING                  │
└────────────────────────────────────────────────────┘

register Receiver
    │
    ├─ SUCCESS
    │   │
    │   └─► Log.d("BroadcastReceiver registered successfully")
    │       isReceiverRegistered = true
    │       Continue
    │
    └─ EXCEPTION
        │
        ├─► Catch Exception
        │
        └─► Log.e("Failed to register BroadcastReceiver: ${e.message}", e)
            (Full stack trace logged for debugging)
            Continue (graceful failure)

unregister Receiver
    │
    ├─ SUCCESS
    │   │
    │   └─► Log.d("BroadcastReceiver unregistered successfully")
    │       isReceiverRegistered = false
    │
    └─ EXCEPTION (if not registered)
        │
        └─► Log.e() or Log.w() depending on context
            (Handled gracefully, doesn't crash)
```

---

## Key Improvements Visualized

### BEFORE
```
Found Device ──► _scannedDevices ──► Nowhere
                                     ✗ Not observed

UI observes ──► _discoveredDevices ──► Empty ✗
               (Never updated)        "No devices"
```

### AFTER
```
Found Device ──► _scannedDevices ──┐
                                   ├─► Synced
                 _discoveredDevices┘
                                   │
                                   ▼
UI observes ──────────────────► Populated ✓
                              "Found devices"
```

---

## Summary

All components now work together to:
1. ✅ Detect Bluetooth devices reliably
2. ✅ Store them in the correct state flows
3. ✅ Display them to the UI
4. ✅ Handle errors gracefully
5. ✅ Clean up safely on stop

The architecture is now **correct, safe, and fully functional**!

