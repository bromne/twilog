package com.bromne.twilog.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bromne.async.ParallelAsyncLoader
import com.bromne.async.RegularAsyncTask
import com.bromne.data.toBuilder
import com.bromne.io.update
import com.bromne.twilog.R
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.favorites
import com.bromne.twilog.app.history
import com.bromne.twilog.app.sharedPreferences
import com.bromne.twilog.client.Result
import com.bromne.twilog.client.Tweet
import com.bromne.twilog.client.TwilogClient
import com.bromne.view.startAnimation
import com.google.common.collect.ImmutableSet
import org.joda.time.DateTime

class TweetFragment : Fragment() {
    val imageLoader: ParallelAsyncLoader<String, Bitmap> = ParallelAsyncLoader()

    lateinit internal var mListener: OnTweetFragmentInteractionListener

    lateinit internal var mWrapper: View
    lateinit internal var mHeader: View
    lateinit internal var mTweets: RecyclerView
    lateinit internal var mProgress: ProgressBar
    lateinit internal var mEmptyMessage: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.fragment_tweet_list, container, false)

        mWrapper = root.findViewById(R.id.wrapper)
        mHeader = root.findViewById(R.id.header)
        mTweets = root.findViewById(R.id.list) as RecyclerView
        mProgress = root.findViewById(R.id.progress) as ProgressBar
        mEmptyMessage = root.findViewById(R.id.no_items)

        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Result> {
            override fun onPreLoad() {
                mWrapper.visibility =  View.INVISIBLE
                mProgress.visibility = View.VISIBLE
            }

            override fun loadInBackground(publishProgress: (Int) -> Unit): Result {
                return mListener.client.find(mListener.query)
            }

            override fun onLoadFinished(result: Result): Unit {
                mProgress.visibility = View.INVISIBLE
                this@TweetFragment.onLoad(result)
                mWrapper.visibility = View.VISIBLE
                mWrapper.startAnimation(this@TweetFragment.context, R.anim.fade_in_short)
            }

            override fun onException(e: Exception) {
                mProgress.visibility = View.INVISIBLE
                Toast.makeText(this@TweetFragment.context, "失敗", Toast.LENGTH_SHORT)
                        .show()
                mEmptyMessage.visibility = View.VISIBLE
            }
        })
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as OnTweetFragmentInteractionListener
    }

    internal fun onLoad(result: Result): Unit {
        val pref = this.activity.sharedPreferences
        pref.history = pref.history.toBuilder()
                .add(SavedQuery(this.mListener.query, DateTime.now()))
                .build()

        val icon = mHeader.findViewById(R.id.icon) as ImageView
        val displayName = mHeader.findViewById(R.id.displayName) as TextView
        val userName = mHeader.findViewById(R.id.userName) as TextView
        val functionIcon = mHeader.findViewById(R.id.function_icon) as TextView
        val condition = mHeader.findViewById(R.id.condition) as TextView

        this.imageLoader.loadOrRegister(result.user.image.bigger, object : RegularAsyncTask.Callbacks<Bitmap> {
            override fun loadInBackground(publishProgress: (Int) -> Unit): Bitmap {
                return mListener.client.loadUserIcon(result.user)
            }

            override fun onLoadFinished(result: Bitmap) {
                icon.setImageBitmap(result)
                icon.startAnimation(this@TweetFragment.context, R.anim.fade_in_medium)
            }

            override fun onException(e: Exception) {
                icon.setImageResource(R.drawable.designer_icon)
                icon.startAnimation(this@TweetFragment.context, R.anim.fade_in_medium)
            }
        })
        displayName.text = result.user.display
        userName.text = "@" + result.user.name

        functionIcon.text = mListener.query.body.map({
            if (it != null)
                R.string.fontawesome_calendar
            else
                R.string.fontawesome_calendar_check_o
        } , {
            R.string.fontawesome_search
        }).let { context.getString(it) }

        condition.text = mListener.query.body.map({
            it?.toString(context.getString(R.string.date_format_with_day)) ?: context.getString(R.string.recent)
        }, {
            "\""+ it.keyword + "\""
        })

        mTweets.layoutManager = LinearLayoutManager(this.context)
        mTweets.adapter = TweetAdapter(this.context, this, { result })
    }

    class TweetAdapter(val context: Context, val fragment: TweetFragment, val data: () -> Result) : RecyclerView.Adapter<TweetHolder>() {
        internal val cache: LruCache<String, Bitmap> = LruCache(100)

        override fun getItemCount() = this.data().tweets.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetHolder {
            val view = LayoutInflater.from(this.context)
                    .inflate(R.layout.layout_tweet_list_item, parent, false)
            return TweetHolder(view)
        }

        override fun onBindViewHolder(holder: TweetHolder, position: Int) {
            val tweet = this.data().tweets[position]
            holder.setTweet(tweet)

            val key = tweet.user.image.bigger
            if (this.cache[key] != null) {
                holder.icon.setImageBitmap(this.cache[key]!!)
            } else {
                val callbacks = object : RegularAsyncTask.Callbacks<Bitmap> {
                    override fun onPreLoad() {
                        holder.icon.tag = key
                    }

                    override fun loadInBackground(publishProgress: (Int) -> Unit): Bitmap {
                        return this@TweetAdapter.fragment.mListener.client.loadUserIcon(tweet.user)
                    }

                    override fun onLoadFinished(result: Bitmap) {
                        this@TweetAdapter.cache.put(key, result)
                        if (holder.icon.tag == key) {
                            holder.icon.setImageBitmap(result)
                            holder.icon.startAnimation(this@TweetAdapter.context, R.anim.fade_in_medium)
                        }
                    }

                    override fun onException(e: Exception) {
                        holder.icon.setImageResource(R.drawable.designer_icon)
                        holder.icon.startAnimation(this@TweetAdapter.context, R.anim.fade_in_medium)
                    }
                }

                this.fragment.imageLoader.loadOrRegister(tweet.user.image.bigger, callbacks)
            }
        }

        override fun onViewRecycled(holder: TweetHolder?) {
             holder?.icon
             holder?.icon?.setImageResource(0)
        }
    }

    class TweetHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val displayName: TextView = itemView.findViewById(R.id.displayName) as TextView
        val userName: TextView = itemView.findViewById(R.id.userName) as TextView
        val created: TextView = itemView.findViewById(R.id.created) as TextView
        val message: TextView = itemView.findViewById(R.id.message) as TextView
        val icon: ImageView = itemView.findViewById(R.id.icon) as ImageView

        fun setTweet(tweet: Tweet): Unit {
            this.userName.text = "@" + tweet.user.name
            this.displayName.text = tweet.user.display
            this.created.text = tweet.created.toString("yyyy/MM/dd HH:mm:ss")
            this.message.text = tweet.message
        }
    }

    interface OnTweetFragmentInteractionListener {
        var query: TwilogClient.Query
        var client: TwilogClient
    }
}
