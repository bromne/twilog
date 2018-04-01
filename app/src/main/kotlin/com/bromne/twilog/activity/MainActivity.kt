package com.bromne.twilog.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import com.bromne.stereotypes.data.Either
import com.bromne.twilog.R
import com.bromne.twilog.client.HttpTwilogClient
import com.bromne.twilog.client.Tweet
import com.bromne.twilog.client.TwilogClient

class MainActivity : AppCompatActivity(),
        TweetFragment.Companion.OnTweetFragmentInteractionListener {

    lateinit override var query: TwilogClient.Query

    lateinit override var client: TwilogClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.client = HttpTwilogClient()
        this.query = this.intent.getSerializableExtra(QUERY) as TwilogClient.Query

        setContentView(R.layout.activity_main)
    }

    override fun onOpenStatus(tweet: Tweet) {
        try {
            Intent(Intent.ACTION_VIEW, Uri.parse("twitter://status?status_id=" + tweet.id))
                    .let { startActivity(it) }
        } catch (e: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse(tweet.status))
                    .let { startActivity(it) }
        }
    }

    override fun openByQuery(query: TwilogClient.Query): Unit {
        MainActivity.Companion.start(this, query)
    }

    companion object {
        val QUERY = "query"

        fun start(context: Context, query: TwilogClient.Query): Unit {
            Intent(context, MainActivity::class.java)
                    .putExtra(QUERY, query)
                    .let { context.startActivity(it) }
        }
    }
}
