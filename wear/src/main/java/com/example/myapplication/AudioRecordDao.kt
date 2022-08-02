package com.example.myapplication

import androidx.room.*

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audioRecords")
    fun getAll(): List<AudioRecord>

    @Query("SELECT * FROM audioRecords WHERE filename=:string")
    fun getOne(string: String): AudioRecord

    @Query("DELETE FROM audioRecords WHERE filename=:string")
    fun deleteOne(string: String)

    @Insert
    fun insert(vararg audioRecord: AudioRecord)

    @Delete
    fun delete(audioRecords: Array<AudioRecord>)

    @Update
    fun update(audioRecord: AudioRecord)
}