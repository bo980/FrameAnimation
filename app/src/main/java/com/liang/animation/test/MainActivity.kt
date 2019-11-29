package com.liang.animation.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start.setOnClickListener {
            anim.start()
        }

        stop.setOnClickListener {
            anim.stop()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                anim.setProgress(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })


        show.setOnClickListener {
            anim.setAnimResource(R.array.refresh)
//            anim.setZOrderMediaOverlay(true)
//            anim.holder.setFormat(PixelFormat.TRANSLUCENT)
        }

        gone.setOnClickListener {
            anim.setAnimAssets("refresh")
        }


        anim.observe { _, fl ->
            seekBar.progress = (fl * 100).toInt()
        }

    }
}
