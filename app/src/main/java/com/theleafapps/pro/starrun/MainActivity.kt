package com.theleafapps.pro.starrun

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.theleafapps.pro.starrun.db.RunDAO
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var runDAO: RunDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}