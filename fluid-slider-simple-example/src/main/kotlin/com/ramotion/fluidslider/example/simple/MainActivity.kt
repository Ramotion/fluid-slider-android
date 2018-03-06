package com.ramotion.fluidslider.example.simple

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.ramotion.fluidslider.FluidSlider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)

        val max = 45
        val min = 10
        val total = max - min

        val slider = findViewById<FluidSlider>(R.id.fluidSlider)
        slider.positionListener = { pos -> slider.bubbleText = "${min + (total  * pos).toInt()}" }
        slider.position = 0.3f
        slider.startText ="$min"
        slider.endText = "$max"

        slider.beginTrackingListener = { textView.visibility = View.INVISIBLE }
        slider.endTrackingListener = { textView.visibility = View.VISIBLE }
    }

}
