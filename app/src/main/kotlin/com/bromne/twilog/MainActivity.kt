package com.bromne.twilog

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bromne.twilog.client.HttpTwilogClient
import com.bromne.twilog.client.TwilogClient

class MainActivity() : AppCompatActivity(),
        TweetFragment.OnTweetFragmentInteractionListener {

    lateinit override var client: TwilogClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = HttpTwilogClient()
        this.title = "検索結果"

        setContentView(R.layout.activity_main)
    }
}
