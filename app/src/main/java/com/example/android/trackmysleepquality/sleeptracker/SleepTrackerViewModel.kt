/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        // AndroidViewModel is basically the same as ViewModel but it allows the application thing to be used as a property of this class
        application: Application) : AndroidViewModel(application) {

    private val _showSnackbarEvent = MutableLiveData<Boolean>()
    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    fun doneShowingSnackbar() {
        _showSnackbarEvent.value = false
    }

    // use coroutines when you do db operations
    // to manage coroutines we need a job
    // job allows us to cancel all coroutine started by this view model when the view model is no longer used and destroyed
    // when view model is destroyed, onCleared is called and we can use this method to cancel all jobs started by this viewmodel
    private var viewModelJob = Job()

    // we need a scope for the coroutines, scope is the thread the coroutine will run on
    // scope also needs to know about the job
                        // using dispatchers.Main means that uiDispatchers will run on main thread
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()


    // this references live data bc we defined getAllNights to return LiveData
    private val nights = database.getAllNights()

    // cannot just display the nights variable because it is an objecvt
    // we use a transformation map to take care of this
    // this function is called every time nights recieves new data from the database
    val nightsString = Transformations.map(nights) { nights ->
        // we pass in application.resources to have access to the strings resources
            // now in the xml we can repladce the text for this text view
        formatNights(nights, application.resources)
    }

    // state of the variable changes based on the state of tonight
    val startButtonVisible = Transformations.map(tonight) {
        // tonight is null at the beginning, so if that is the case, we want the start button visible
        null == it
    }

    val stopButtonVisible = Transformations.map(tonight) {
        null != it
    }

    val clearButtonVisible = Transformations.map(nights) {
        // want to hide the button if there is no days recorded
        it?.isNotEmpty()
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        // use a coroutine to get tonight from database, so we are not blocking the ui while waiting
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
            // want to make sure that getTonightFromDatabase does not block
            // and it should return a SleepNight or null
        }
    }


    // suspend means we want to call it from inside the coroutine and not block
    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()

            insert(newNight)

            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }

    fun onStopTracking() {
        uiScope.launch {
            // return @ label to specify which function among severl nested ones to return from
            // in this case, we specify to return from launch, and not from the lambda
            val oldNight = tonight.value ?: return@launch

            oldNight.endTimeMilli = System.currentTimeMillis()

            update(oldNight)
            // this variable is only non null when we can set a sleep quality. if we cant set it, we dont navigate
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun update(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
            _showSnackbarEvent.value = true
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.deleteAllRows()
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

