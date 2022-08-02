package com.example.myapplication

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.Duration

@Entity(tableName = "audioRecords")
data class AudioRecord(
    var filename: String,
    var filepath: String,
    var timestamp: Long,
    var duration: String,
    var ampsPath: String,
    var encrypted: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
}