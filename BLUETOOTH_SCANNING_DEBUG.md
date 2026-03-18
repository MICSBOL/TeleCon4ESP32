# Bluetooth Device Scanning - Debugging Guide

## What Was Fixed

Your Bluetooth device scanning wasn't working because **discovered devices were being added to the wrong state flow**. The receiver was updating `_scannedDevices` internally, but the public API exposed `_discoveredDevices`. These were never synchronized, so your UI never saw the discovered devices.

### All Issues Fixed:

1. ✅ **Discovered devices state flow now gets updated** - Both internal and public state flows are kept in sync
2. ✅ **Receiver duplicate registration prevented** - Added flag to track registration state
3. ✅ **Proper error logging** - All errors are now logged for debugging
4. ✅ **Proper cleanup** - Receiver is properly unregistered on stop/release
5. ✅ **Android 12+ compatibility** - Added `Context.RECEIVER_EXPORTED` flag

---

## How to Test

### Step 1: Ensure Permissions are Requested at Runtime

Your app must request Bluetooth permissions at runtime (Android 6.0+). Add this to your MainActivity or permission handler:

```kotlin
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, isGranted) ->
            Log.d("Permissions", "$permission: $isGranted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        }
    }
}
```

### Step 2: Test Discovery

```kotlin
// In your ViewModel or wherever you use RemoteController
override fun startDeviceDiscovery() {
    Log.d("Test", "Starting Bluetooth discovery...")
    controller.startDiscovery()
    
    // Observe discovered devices
    viewModelScope.launch {
        controller.discoveredDevices.collect { devices ->
            Log.d("Test", "Discovered devices: ${devices.size}")
            devices.forEach {
                Log.d("Test", "  - ${it.name} (${it.address})")
            }
        }
    }
}

override fun stopDeviceDiscovery() {
    Log.d("Test", "Stopping Bluetooth discovery...")
    controller.stopDiscovery()
}
```

### Step 3: Check LogCat for These Messages

**Success indicators:**
```
D/BluetoothController: Missing BLUETOOTH_SCAN permission          <- If permissions aren't granted
D/BluetoothController: BroadcastReceiver registered successfully   <- Registration worked
D/BluetoothController: startDiscovery() called, result: true       <- Discovery started
D/BluetoothController: BroadcastReceiver unregistered successfully <- Cleanup worked
```

**Error indicators:**
```
E/BluetoothController: Missing BLUETOOTH_SCAN permission          <- Permissions not granted
E/BluetoothController: Failed to register BroadcastReceiver: ...  <- Registration failed
E/BluetoothController: Failed to unregister BroadcastReceiver: ... <- Cleanup failed
```

---

## Common Issues & Solutions

### Issue: "Missing BLUETOOTH_SCAN permission"

**Solution:** You need to request permissions at runtime:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val permissionsToRequest = mutableListOf<String>()
    if (ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_SCAN
    ) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
    }
    if (ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.BLUETOOTH_CONNECT
    ) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (permissionsToRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            this,
            permissionsToRequest.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }
}
```

### Issue: "BroadcastReceiver already registered"

**Solution:** This is now handled by the `isReceiverRegistered` flag. You can safely call `startDiscovery()` multiple times without errors.

### Issue: No devices found

**Checklist:**
1. ✅ Check that Bluetooth is enabled on the device
2. ✅ Check that permissions are granted (Settings > Apps > YourApp > Permissions)
3. ✅ Check that nearby Bluetooth devices are in pairing mode or have been paired before
4. ✅ Check LogCat for "BroadcastReceiver registered successfully"
5. ✅ Check that `discoveredDevices` StateFlow is being observed
6. ✅ Wait 10-15 seconds - Bluetooth discovery takes time

### Issue: App crashes with "ReceiverNotRegisteredException"

**Solution:** The code now properly tracks registration state with the `isReceiverRegistered` flag. Make sure you're using the fixed version.

---

## Code Flow

```
User calls startDiscovery()
    ↓
Check BLUETOOTH_SCAN permission
    ↓
Check if receiver already registered (isReceiverRegistered flag)
    ↓
Register FoundDeviceReceiver with BluetoothDevice.ACTION_FOUND
    ↓
Call bluetoothAdapter.startDiscovery()
    ↓
Wait for Bluetooth devices to respond
    ↓
BroadcastReceiver receives ACTION_FOUND intent
    ↓
FoundDeviceReceiver callback executes
    ↓
Update BOTH _scannedDevices AND _discoveredDevices ✨ (This was the bug!)
    ↓
UI observes discoveredDevices StateFlow and updates
```

---

## Related Code Changes

### Main Changes in AndroidBluetoothController.kt:

**Before:**
```kotlin
private val foundDeviceReceiver = FoundDeviceReceiver { device ->
    _scannedDevices.update { ... } // Only updating internal state!
}
```

**After:**
```kotlin
private val foundDeviceReceiver = FoundDeviceReceiver { device ->
    _scannedDevices.update { devices ->
        // ... update
    }
    // Also update the public discoveredDevices state flow ✨
    _discoveredDevices.update { devices ->
        // ... update
    }
}
```

---

## Need More Help?

Check these files:
- `AndroidBluetoothController.kt` - Main Bluetooth controller with all fixes
- `FoundDeviceReceiver.kt` - BroadcastReceiver that finds devices
- `AndroidManifest.xml` - Permissions configuration
- `BluetoothDeviceMapper.kt` - Maps Bluetooth devices to domain objects

All files have been fixed and are ready to use!

