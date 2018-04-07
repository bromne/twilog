package com.bromne.twilog.app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import com.bromne.twilog.activity.MainActivity
import com.bromne.twilog.client.TwilogClient

object Activities {
    interface IContext {
        val context: Context
    }
}