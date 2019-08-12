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

package com.example.android.trackmysleepquality.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.security.AccessControlContext

// for each new table, just add another class to the list of entities
// for the version number, if you change the schema, you must up the version number or it wont work
    // anymore
// for the last, exportSchema is true by default
@Database(entities = [SleepNight::class], version = 1, exportSchema = false)
abstract class SleepDatabase : RoomDatabase() {

    // associate the database with the dao
    // this app only uses one table and one dao, you can add many of both
    abstract val sleepDatabaseDao: SleepDatabaseDao

    // allows clients to access the methods for creating and getting the database without instantiating
        // the class
    // since the only purpose of this is to privide us with a database, there is no neec to ever instantiate it
    companion object {

        // volitile make sure that the value of INSTANCE is always up to date and the same to all execution threads
            // value of INSTANCE will never be cached and all writes and reads will always be done to mainmemory
        @Volatile
        private var INSTANCE: SleepDatabase? = null // keeps a ref to the db, help to keep us from repetedly opening connections to the db bc its expensive

        fun getInstance(context: Context) : SleepDatabase {
            // ensures only one db gets initialized
            // pass this into syncronized so that we have access to the context
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext, // context passed into the synch block
                            SleepDatabase::class.java, // which db to build
                            "sleep_history_database" // the name of the db
                    )
                            .fallbackToDestructiveMigration()
                            // to avoid creating a migration object and strategy (migration is a way to convert old data in old table to new data that fits into new table, possilby because we changed the new tables by removbing columns)
                            .build()
                    INSTANCE = instance
                }

                return instance
            }
        }


    }
}
