package com.bromne.twilog.activity

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bromne.stereotypes.async.ParallelAsyncLoader
import com.bromne.stereotypes.async.RegularAsyncTask
import com.bromne.stereotypes.data.Either
import com.bromne.stereotypes.data.toBuilder
import com.bromne.stereotypes.view.startAnimation
import com.bromne.twilog.R
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.history
import com.bromne.twilog.app.sharedPreferences
import com.bromne.twilog.client.Result
import com.bromne.twilog.client.Tweet
import com.bromne.twilog.client.TwilogClient
import com.bromne.view.EndlessRecyclerOnScrollListener
import org.joda.time.DateTime
import org.joda.time.LocalDate

class TweetFragment : Fragment(), DatePickerDialog.OnDateSetListener {
    val imageLoader: ParallelAsyncLoader<String, Bitmap> = ParallelAsyncLoader()

    lateinit internal var mListener: OnTweetFragmentInteractionListener

    lateinit internal var mWrapper: View
    lateinit internal var mHeader: View
    lateinit internal var mTweets: RecyclerView
    lateinit internal var mProgress: ProgressBar
    lateinit internal var mEmptyMessage: View
    lateinit internal var mToolbar: Toolbar

    var mHasNext: Boolean = true

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

        mToolbar = root.findViewById(R.id.toolbar) as Toolbar
        mToolbar.inflateMenu(R.menu.menu_main)
        mToolbar.setOnMenuItemClickListener({
            val date: LocalDate = mListener.query.body.map({ it }, { null }) ?: LocalDate.now()
            when (it.itemId) {
                R.id.select_date -> {
                    val dialog = DatePickerDialogFragment.newInstance(date)
                    dialog.setTargetFragment(this, 0)
                    dialog.show(this.fragmentManager, "Calendar")
                }
                R.id.search_with_text -> {
                    val criteria = mListener.query.body.map({ d -> TwilogClient.Criteria("", TwilogClient.Joint.AND) }, { c -> c })
                    val dialog = SearchDialog.newInstance(criteria, { c -> mListener.openByQuery(TwilogClient.Query(mListener.query.userName, Either.right(c), TwilogClient.Order.DESC)) })
                    dialog.show(this.fragmentManager, "SearchDialog")
                }
                R.id.change_tweet_order -> {
                    val query = mListener.query
                    val reversed = TwilogClient.Query(query.userName, query.body, query.order.reversed)
                    mListener.query = reversed
                    loadTweets(mListener.query)
                }
            }
            true
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTweets(mListener.query)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as OnTweetFragmentInteractionListener
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val query = TwilogClient.Query(mListener.query.userName, Either.left(LocalDate(year, month + 1, dayOfMonth)), TwilogClient.Order.DESC)
        mListener.openByQuery(query)
    }

    internal fun loadTweets(query: TwilogClient.Query): Unit {
        RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Result> {
            override fun onPreLoad() {
                mProgress.visibility = View.VISIBLE
            }

            override fun loadInBackground(publishProgress: (Int) -> Unit): Result {
                return mListener.client.find(query)
            }

            override fun onLoadFinished(result: Result): Unit {
                mProgress.visibility = View.INVISIBLE
                this@TweetFragment.onLoad(result)
                if (mTweets.visibility == View.INVISIBLE) {
                    mTweets.visibility = View.VISIBLE
                    mTweets.startAnimation(R.anim.fade_in_short)
                }
            }

            override fun onException(e: Exception) {
                mProgress.visibility = View.INVISIBLE
                mTweets.visibility = View.INVISIBLE
                Toast.makeText(this@TweetFragment.context, "失敗", Toast.LENGTH_SHORT)
                        .show()
                mEmptyMessage.visibility = View.VISIBLE
            }
        })
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

            @Suppress("NAME_SHADOWING")
            override fun onLoadFinished(result: Bitmap) {
                if (icon.drawable == null) {
                    icon.startAnimation(R.anim.fade_in_medium)
                }
                icon.setImageBitmap(result)
            }

            override fun onException(e: Exception) {
                if (icon.drawable == null) {
                    icon.startAnimation(R.anim.fade_in_medium)
                }
                icon.setImageResource(R.drawable.designer_icon)
            }
        })
        displayName.text = result.user.display
        userName.text = String.format(getString(R.string.format_username), result.user.name)

        functionIcon.text = mListener.query.body.map({
            if (it != null)
                R.string.fontawesome_calendar
            else
                R.string.fontawesome_clock_o
        } , {
            R.string.fontawesome_search
        }).let { context.getString(it) }

        val sort = (if (mListener.query.order == TwilogClient.Order.ASC) R.string.ascending else R.string.descending).let { context.getString(it) }
        val condition_text = mListener.query.body.map({
            it?.toString(context.getString(R.string.date_format_with_day)) ?: context.getString(R.string.recent_tweets)
        }, {
            "\""+ it.keyword + "\""
        })
        condition.text = context.getString(R.string.query_representation_format, condition_text, sort)

        val manager = LinearLayoutManager(this.context)
        val adapter = TweetAdapter(this.context, this, result)
        mTweets.layoutManager = manager
        mTweets.adapter = adapter
        mTweets.addOnScrollListener(object : EndlessRecyclerOnScrollListener(manager) {
            override fun onLoadMore(currentPage: Int) {
                if (!mHasNext)
                    return

                val criteria: TwilogClient.Criteria? = mListener.query.body.map({ null }, { it })

                @Suppress("FoldInitializerAndIfToElvis")
                if (criteria == null)
                    return

                val next = criteria.copy(page = currentPage)
                RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Result> {
                    override fun loadInBackground(publishProgress: (Int) -> Unit): Result {
                        return mListener.client.find(mListener.query.copy(body = Either.right(next)))
                    }

                    @Suppress("NAME_SHADOWING")
                    override fun onLoadFinished(result: Result): Unit {
                        mHasNext = result.hasNext
                        adapter.appendTweets(result.tweets)
                    }

                    override fun onException(e: Exception) {
                        Toast.makeText(this@TweetFragment.context, "失敗", Toast.LENGTH_SHORT)
                                .show()
                    }
                })

            }
        })
    }

    class TweetAdapter(val context: Context, val fragment: TweetFragment, var data: Result) : RecyclerView.Adapter<TweetHolder>() {
        internal val cache: LruCache<String, Bitmap> = LruCache(100)

        fun appendTweets(tweets: List<Tweet>) {
            this.data = this.data.copy(tweets = this.data.tweets.plus(tweets))
            notifyDataSetChanged()
        }

        override fun getItemCount() = this.data.tweets.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetHolder {
            val view = LayoutInflater.from(this.context)
                    .inflate(R.layout.layout_tweet_list_item, parent, false)
            return TweetHolder(view)
        }

        override fun onBindViewHolder(holder: TweetHolder, position: Int) {
            val tweet = this.data.tweets[position]
            holder.setTweet(this.context, tweet)
            holder.itemView.setOnClickListener({ this@TweetAdapter.fragment.mListener.onOpenStatus(tweet) })

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
                            holder.icon.startAnimation(R.anim.fade_in_medium)
                        }
                    }

                    override fun onException(e: Exception) {
                        holder.icon.setImageResource(R.drawable.designer_icon)
                        holder.icon.startAnimation(R.anim.fade_in_medium)
                    }
                }

                this.fragment.imageLoader.loadOrRegister(key, callbacks)
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

        init {
            this.message.movementMethod = LinkMovementMethod.getInstance()
        }

        fun setTweet(context: Context, tweet: Tweet): Unit {
            this.userName.text = context.getString(R.string.format_username, tweet.user.name)
            this.displayName.text = tweet.user.display
            this.created.text = tweet.created.toString("yyyy/MM/dd HH:mm:ss")
            this.message.text = Html.fromHtml(tweet.raw)
        }
    }

    interface OnTweetFragmentInteractionListener {
        var query: TwilogClient.Query
        var client: TwilogClient

        fun onOpenStatus(tweet: Tweet): Unit
        fun openByQuery(query: TwilogClient.Query): Unit
    }
}
