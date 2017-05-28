package com.bromne.twilog

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
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
import com.bromne.twilog.client.*
import com.bromne.twilog.client.TwilogClient.Joint
import com.bromne.view.startAnimation
import java.lang.ref.WeakReference
import java.net.URL

class TweetFragment : Fragment() {
    lateinit internal var mListener: OnTweetFragmentInteractionListener

    lateinit internal var mTweets: RecyclerView
    lateinit internal var mProgress: ProgressBar
    lateinit internal var mEmptyMessage: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.fragment_tweet_list, container, false)

        mTweets = root.findViewById(R.id.list) as RecyclerView
        mProgress = root.findViewById(R.id.progress) as ProgressBar
        mEmptyMessage = root.findViewById(R.id.no_items)

        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Result> {
            override fun onPreLoad() {
                mProgress.visibility = View.VISIBLE
            }

            override fun loadInBackground(publishProgress: (Int) -> Unit): Result {
                return mListener.client.search("takeda25", "藻さん", Joint.AND)
            }

            override fun onLoadFinished(result: Result): Unit {
                mProgress.visibility = View.INVISIBLE
                this@TweetFragment.onLoad(result)
                mTweets.startAnimation(this@TweetFragment.context, R.anim.fade_in_short)
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
        mTweets.layoutManager = LinearLayoutManager(this.context)
        val adapter = TweetAdapter(this.context, this, { result })
        mTweets.adapter = adapter
    }


    class TweetAdapter(val context: Context, val fragment: TweetFragment, val data: () -> Result) : RecyclerView.Adapter<TweetHolder>() {
        val imageLoader: ParallelAsyncLoader<String, Bitmap> = ParallelAsyncLoader()
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

                this.imageLoader.loadOrRegister(tweet.user.image.bigger, callbacks)
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

    class BitmapWorkerTask(val url : String , view : ImageView) : AsyncTask<Void, Void, Bitmap>() {
        internal val viewReference : WeakReference<ImageView> = WeakReference(view)

        override fun doInBackground(vararg params: Void): Bitmap {
            return BitmapFactory.decodeStream(URL(url).openStream())
        }

        override fun onPostExecute(result: Bitmap?) {
            if (this.viewReference.get() == null || result == null)
                return

            val view = this.viewReference.get()
            val task = (view.drawable as AsyncDrawable).task

            if (this == task) {
                view.setImageBitmap(result)
            }
        }
    }

    class AsyncDrawable(res: Resources, bitmap: Bitmap, task: BitmapWorkerTask) : BitmapDrawable(res, bitmap) {
        internal val taskReference : WeakReference<BitmapWorkerTask> = WeakReference(task)

        val task: BitmapWorkerTask? get() = this.taskReference.get()
    }

    interface OnTweetFragmentInteractionListener {
        var client: TwilogClient
    }
}
