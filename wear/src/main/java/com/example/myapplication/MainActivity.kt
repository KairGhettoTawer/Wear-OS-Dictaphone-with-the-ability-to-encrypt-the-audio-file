package com.example.myapplication

import androidx.wear.ambient.AmbientModeSupport
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import androidx.wear.ambient.AmbientMode
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.gms.wearable.Wearable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener, AmbientModeSupport.AmbientCallbackProvider {

    private lateinit var amplitudes: ArrayList<Float>
    private lateinit var binding: ActivityMainBinding

    private var permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)
    private var permissionGrantedMIC = false
    private var permissionGrantedReadS = false
    private var permissionGrantedWriteS = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false

    private var duration =""

    private lateinit var vibrator: Vibrator

    private lateinit var timer: Timer

    private lateinit var db: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>


    val folder_main = "Project"

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val f = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            folder_main)
        if (!f.exists()) {
            f.mkdirs()
        }

        permissionGrantedMIC = ActivityCompat.checkSelfPermission(this,
            permissions[0]) == PackageManager.PERMISSION_GRANTED
        permissionGrantedWriteS = ActivityCompat.checkSelfPermission(this,
            permissions[1]) == PackageManager.PERMISSION_GRANTED
        permissionGrantedReadS = ActivityCompat.checkSelfPermission(this,
            permissions[2]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGrantedReadS && !permissionGrantedWriteS && !permissionGrantedMIC)
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)


        db= Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.includeSheet.bottomSheet)
        bottomSheetBehavior.peekHeight=0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        timer = Timer(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        binding.btnRec.setOnClickListener {
            when{
                isPaused -> resumeRec()
                isRecording -> pauseRec()
                else -> startRec()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50,
                VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener{
            startActivity(Intent(this, GalleryActivity:: class.java))

        }

        binding.btnDone.setOnClickListener{
            stopRec()
            Toast.makeText(this, "Record saved", Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE
            binding.includeSheet.filenameInput.setText(filename)
        }

        binding.includeSheet.btnCancel.setOnClickListener{
            File("$dirPath$filename.wav").delete()
            dismiss()
        }

        binding.includeSheet.btnAccept.setOnClickListener {
            dismiss()
            save()
        }

        binding.bottomSheetBG.setOnClickListener {
            File("$dirPath$filename.wav").delete()
            dismiss()

        }

        binding.btnDel.setOnClickListener{
            stopRec()
            File("$dirPath$filename.wav").delete()
            Toast.makeText(this, "Record deleted", Toast.LENGTH_LONG).show()
        }

        binding.btnDel.isClickable = false

        ambientController = AmbientModeSupport.attach(this)
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()

    private class MyAmbientCallback : AmbientModeSupport.AmbientCallback() {

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            // Handle entering ambient mode
        }

        override fun onExitAmbient() {
            // Handle exiting ambient mode
        }

        override fun onUpdateAmbient() {
            // Update the content
        }
    }

    private fun save (){
        val newFilename = binding.includeSheet.filenameInput.text.toString()
        if(newFilename!= filename){
            var newFile = File("$dirPath$newFilename.wav")
            File("$dirPath$filename.wav").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.wav"
        var timestamp = Date().time
        var ampsPath = "$dirPath$newFilename"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }catch (e :IOException){}

        var record = AudioRecord(newFilename, filePath, timestamp, duration, ampsPath)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }

    }

    private fun dismiss () {
        binding.bottomSheetBG.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        hideKeyboard( binding.includeSheet.filenameInput)
    }
    private fun hideKeyboard(view: View){
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out  String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE) {
            permissionGrantedMIC = grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionGrantedReadS = grantResults[1] == PackageManager.PERMISSION_GRANTED
            permissionGrantedWriteS = grantResults[2] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun pauseRec(){
        recorder.pause()
        isPaused = true
        binding.btnRec.setImageResource(R.drawable.ic_round_mic_24)
        timer.pause()
    }

    private fun resumeRec(){
        recorder.resume()
        isPaused = false
        binding.btnRec.setImageResource(R.drawable.ic_round_pause_24)
        timer.start()
    }

    private fun startRec(){
        if(!permissionGrantedReadS && !permissionGrantedWriteS && !permissionGrantedMIC){
            ActivityCompat.requestPermissions(this,permissions, REQUEST_CODE)
            return
        }
        // старт записи

        dirPath = "${Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MUSIC)}/${folder_main}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder = MediaRecorder()
        recorder.apply {
            recorder.reset();
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(3400)
            setAudioChannels(1)
            setOutputFile("$dirPath$filename.wav")
            try{
                prepare()
            }catch (e: IOException){}
            start()
        }

        binding.btnRec.setImageResource(R.drawable.ic_round_pause_24)
        isRecording = true
        isPaused = false

        timer.start()

        binding.btnDel.isClickable = true
        binding.btnDel.setImageResource(R.drawable.ic_round_delete_forever_24)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun stopRec(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording =false

        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDel.isClickable = false
        binding.btnDel.setImageResource(R.drawable.ic_round_delete_forever_24)

        binding.btnRec.setImageResource(R.drawable.ic_round_mic_24)

        binding.tvTimer.text = "00:00.00"

        amplitudes = binding.waveformView.clear()


    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)


        binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}