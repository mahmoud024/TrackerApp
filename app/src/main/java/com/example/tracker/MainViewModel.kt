package com.example.tracker

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    private val _location = MutableLiveData<Location>()
    val location: LiveData<Location> = _location

    private val _isTracking = MutableLiveData<Boolean>(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    fun updateLocation(location: Location) {
        _location.postValue(location)
    }

    fun startTracking() {
        val intent = Intent(context, TrackingService::class.java)
        context.startForegroundService(intent)
        _isTracking.postValue(true)
        _toastMessage.postValue("Tracking started")
    }

    fun stopTracking() {
        val intent = Intent(context, TrackingService::class.java)
        context.stopService(intent)
        _isTracking.postValue(false)
        _toastMessage.postValue("Tracking stopped")
    }

    fun registerDeviceIfNeeded() {
        val id = DeviceManager.getDeviceId(context)
        if (id == -1) {
            // لا يوجد ID محفوظ، حاول التسجيل
            DeviceManager.registerDevice(context) { success, message ->
                _toastMessage.postValue(message)
            }
        } else {
            // تحقق مما إذا كان هذا الـ ID لا يزال موجودًا في السيرفر
            DeviceManager.checkDeviceExists(context, id) { exists ->
                if (!exists) {
                    // احذف الـ ID وسجّل مجددًا
                    DeviceManager.clearDeviceId(context)
                    DeviceManager.registerDevice(context) { success, message ->
                        _toastMessage.postValue(message)
                    }
                }
            }
        }
    }

}
