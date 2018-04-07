package com.bromne.twilog.app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import com.bromne.twilog.activity.MainActivity
import com.bromne.twilog.app.Activities.IContext
import com.bromne.twilog.client.TwilogClient

/**
 * アプリケーション共通のアクティビティーです。
 */
abstract class AppActivity : AppCompatActivity(), IContext {
    override val context: Context
        get() = this

    companion object {
        /**
         * 検索結果アクティビティーを起動可能であることを表現するインターフェイスです。
         */
        interface SearchStarting : IContext {
            fun openByQuery(query: TwilogClient.Query): Unit {
                MainActivity.Companion.start(this.context, query)
            }
        }
    }
}