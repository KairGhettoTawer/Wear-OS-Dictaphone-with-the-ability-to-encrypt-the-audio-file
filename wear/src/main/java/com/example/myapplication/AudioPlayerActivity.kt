package com.example.myapplication

import android.content.ContentValues.TAG
import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.room.Room
import com.example.myapplication.databinding.ActivityAudioPlayerBinding
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.ChannelClient.ChannelCallback
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.Wearable.getNodeClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor


class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var record: AudioRecord
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var db: AppDatabase

    private var filePath : String? = ""
    private var fileName : String? = ""
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvFilename: TextView

    private lateinit var btnPlay : ImageButton
    private lateinit var btnBackward : ImageButton
    private lateinit var btnForward : ImageButton
    private lateinit var speedChip : Chip
    private lateinit var seekBar: SeekBar


    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 1000

    private var playbackSpeed = 1.0f

    private var executorService: ThreadPoolExecutor = ThreadPoolExecutor(4, 5, 60L, TimeUnit.SECONDS, LinkedBlockingDeque<Runnable>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        filePath = intent.getStringExtra("filepath")
        fileName = intent.getStringExtra("filename")

        toolbar = findViewById(R.id.toolbar)
        tvFilename = findViewById(R.id.tvFilename)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_round_arrow_back_24)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        tvFilename.text = fileName

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        GlobalScope.launch {
            record = db.audioRecordDao().getOne(fileName.toString())
            if(record.encrypted == 0){
                binding.btnEncrypt.setImageResource(R.drawable.ic_round_enhanced_encryption_24)
            } else
                binding.btnEncrypt.setImageResource(R.drawable.ic_round_no_encryption_24)
        }



        btnBackward = findViewById(R.id.btnBackward)
        btnForward = findViewById(R.id.btnForward)
        btnPlay = findViewById(R.id.btnPlay)
        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable, delay)
        }


        btnPlay.setOnClickListener {
            playPausePlayer()
        }

        playPausePlayer()
        seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_play_circle_filled_24, theme)
        }

        btnForward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }

        btnBackward.setOnClickListener{
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress -= jumpValue
        }

        binding.chip.setOnClickListener {
            if(playbackSpeed != 2f)
                playbackSpeed += 0.5f
            else
                playbackSpeed = 0.5f

            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
            binding.chip.text = "x $playbackSpeed"
            playPausePlayer()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2)
                    mediaPlayer.seekTo(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })




        binding.btnEncrypt.setOnClickListener {
            EDVoice(filePath.toString())
            GlobalScope.launch {
                if(record.encrypted == 0){
                    record.encrypted = 1
                } else {
                    record.encrypted = 0
                }
                db.audioRecordDao().update(record)
            }

            if(record.encrypted == 0){
                binding.btnEncrypt.setImageResource(R.drawable.ic_round_no_encryption_24)
                Toast.makeText(this, "Encrypted", Toast.LENGTH_LONG).show()
            } else {
                binding.btnEncrypt.setImageResource(R.drawable.ic_round_enhanced_encryption_24)
                Toast.makeText(this, "Decrypted", Toast.LENGTH_LONG).show()
            }
        }



        }

    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_pause_circle_filled_24, theme)
            handler.postDelayed(runnable, delay)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_round_play_circle_filled_24, theme)
            handler.removeCallbacks(runnable)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }



    private fun EDVoice(string: String){
        var input: ByteArray = File(string).readBytes()
        var byteBuffer: ByteArray = File(string).readBytes()
        var sampleIdentifier = 16
        var j = 0
        var k = 0
        for (i in 0..input.lastIndex){
            println("$i byte is ${input[i]}")
        }

        /*for (i in 1..400){
        if((input.lastIndex-3364+1)%i==0) println(i)
    }*/

        if(record.encrypted==1){
            j = 0
            k = 0
            for(i in 3280..input.lastIndex) {
                if ((j != 0) && (j % 16 == 0)) {
                    k = 0
                }
                if(k>=1)input[i] = input[i] xor byteBuffer[k+3264]
                k++
                j++

            }
            sampleIdentifier = 16
            j = 0
            k = 0
            for (i in 3280..input.lastIndex) {

                if ((j != 0) && (j % 16 == 0)) {
                    sampleIdentifier += 32
                    k = 0
                }

                var index = input.lastIndex - sampleIdentifier + j + 1
                byteBuffer[i] = input[index]
                k++
                j++
            }
        }
        else {

            sampleIdentifier = 16
            j = 0
            k = 0
            for (i in 3280..input.lastIndex) {
                if ((j != 0) && (j % 16 == 0)) {
                    sampleIdentifier += 32
                    k = 0
                }

                var index = input.lastIndex - sampleIdentifier + j + 1
                byteBuffer[i] = input[index]
                k++
                j++
            }

            j = 0
            k = 0
            for(i in 3280..input.lastIndex) {
                if ((j != 0) && (j % 16 == 0)) {
                    k = 0
                }
                if(k>=1)byteBuffer[i] = byteBuffer[i] xor byteBuffer[k+3264]
                k++
                j++

            }
        }

        for (i in 0..input.lastIndex){
            println("$i byte is ${byteBuffer[i]} b")
        }

        var fos = FileOutputStream(string)
        fos.write(byteBuffer)
        fos.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
    }
}