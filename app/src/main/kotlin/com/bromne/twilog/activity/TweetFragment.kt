package com.bromne.twilog.activity

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.bromne.stereotypes.async.RegularAsyncTask
import com.bromne.stereotypes.data.Either
import com.bromne.stereotypes.data.toBuilder
import com.bromne.stereotypes.view.startAnimation
import com.bromne.twilog.R
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.ViewExtensions.load
import com.bromne.twilog.app.history
import com.bromne.twilog.app.sharedPreferences
import com.bromne.twilog.client.Result
import com.bromne.twilog.client.Tweet
import com.bromne.twilog.client.TwilogClient
import com.bromne.view.EndlessRecyclerOnScrollListener
import org.joda.time.DateTime
import org.joda.time.LocalDate

class TweetFragment : Fragment(), DatePickerDialog.OnDateSetListener {
    internal var mResult: Result? = null

    lateinit internal var mListener: OnTweetFragmentInteractionListener

    lateinit internal var mWrapper: View
    lateinit internal var mHeader: View
    lateinit internal var mSwipeRefresh: SwipeRefreshLayout
    lateinit internal var mTweets: RecyclerView
    lateinit internal var mProgress: ProgressBar
    lateinit internal var mEmptyMessage: View
    lateinit internal var mToolbar: Toolbar

    var mHasNext: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tweet_list, container, false)

        mWrapper = root.findViewById(R.id.wrapper)
        mHeader = root.findViewById(R.id.header)
        mSwipeRefresh = root.findViewById(R.id.refresh)
        mTweets = root.findViewById(R.id.list)
        mProgress = root.findViewById(R.id.progress)
        mEmptyMessage = root.findViewById(R.id.no_items)

        mSwipeRefresh.setOnRefreshListener({
            val result = mResult
            if (result != null) {
                RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Unit> {
                    override fun loadInBackground(publishProgress: (Int) -> Unit) = mListener.client.forceUpdate(result.user)

                    override fun onException(e: Exception) {
                    }

                    @Suppress("NAME_SHADOWING")
                    override fun onLoadFinished(result: Unit) {
                        loadTweets(mListener.query, false)
                    }
                })
            }
        })

        mToolbar = root.findViewById(R.id.toolbar)
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
                    val reversed = query.copy(order = query.order.reversed)
                    mListener.query = reversed
                    loadTweets(mListener.query, true)
                }
            }
            true
        })

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTweets(mListener.query, true)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as OnTweetFragmentInteractionListener
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val query = mListener.query.copy(body = Either.left(LocalDate(year, month + 1, dayOfMonth)))
        mListener.openByQuery(query)
    }

    internal fun loadTweets(query: TwilogClient.Query, enableIndicator: Boolean): Unit {
        RegularAsyncTask.execute(object : RegularAsyncTask.Callbacks<Result> {
            override fun onPreLoad() {
                if (enableIndicator)
                    mProgress.visibility = View.VISIBLE
            }

            override fun loadInBackground(publishProgress: (Int) -> Unit): Result {
                Log.d("Twilog", "loadInBackground")
                return mListener.client.find(query)
            }

            override fun onLoadFinished(result: Result): Unit {
                Log.d("Twilog", "onLoadFinished")
                mProgress.visibility = View.INVISIBLE
                mSwipeRefresh.isRefreshing = false
                mEmptyMessage.visibility = View.INVISIBLE
                this@TweetFragment.onLoad(result)
                if (mTweets.visibility == View.INVISIBLE) {
                    mTweets.visibility = View.VISIBLE
                    mTweets.startAnimation(R.anim.fade_in_short)
                }
            }

            override fun onException(e: Exception) {
                Log.d("Twilog", "onException")
                mProgress.visibility = View.INVISIBLE
                mSwipeRefresh.isRefreshing = false
                mTweets.visibility = View.INVISIBLE
                Toast.makeText(this@TweetFragment.context, "失敗", Toast.LENGTH_SHORT)
                        .show()
                mEmptyMessage.visibility = View.VISIBLE
            }
        })
    }

    internal fun onLoad(result: Result): Unit {
        val activity = this.activity ?: return

        mResult = result
        val pref = activity.sharedPreferences
        pref.history = pref.history.toBuilder()
                .add(SavedQuery(this.mListener.query, DateTime.now()))
                .build()

        val icon: ImageView = mHeader.findViewById(R.id.icon)
        val displayName: TextView = mHeader.findViewById(R.id.displayName)
        val userName: TextView = mHeader.findViewById(R.id.userName)
        val functionIcon: TextView = mHeader.findViewById(R.id.function_icon)
        val condition: TextView = mHeader.findViewById(R.id.condition)

        icon.load(result.user.image.bigger)
        displayName.text = result.user.display
        userName.text = String.format(getString(R.string.format_username), result.user.name)

        functionIcon.text = mListener.query.body.map({
            if (it != null)
                R.string.fontawesome_calendar
            else
                R.string.fontawesome_clock_o
        } , {
            R.string.fontawesome_search
        }).let { getString(it) }

        val sort = (if (mListener.query.order == TwilogClient.Order.ASC) R.string.ascending else R.string.descending).let { getString(it) }
        val condition_text = mListener.query.body.map({
            it?.toString(getString(R.string.date_format_with_day)) ?: getString(R.string.recent_tweets)
        }, {
            "\""+ it.keyword + "\""
        })
        condition.text = getString(R.string.query_representation_format, condition_text, sort)

        val criteria: TwilogClient.Criteria? = mListener.query.body.map({ null }, { it })
        mHasNext = criteria != null

        val manager = LinearLayoutManager(this.context)
        val adapter = TweetAdapter(this, result)
        mTweets.layoutManager = manager
        mTweets.adapter = adapter
        mTweets.addOnScrollListener(object : EndlessRecyclerOnScrollListener(manager) {
            override fun onLoadMore(currentPage: Int) {
                if (criteria == null || !mHasNext)
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

    companion object {
        class TweetAdapter(val fragment: TweetFragment, var data: Result) : RecyclerView.Adapter<ViewHolder>() {
            val context = fragment.context!!
            fun appendTweets(tweets: List<Tweet?>) {
                this.data = this.data.copy(tweets = this.data.tweets.plus(tweets))
                notifyDataSetChanged()
            }

            override fun getItemCount() = this.data.tweets.size + 1

            override fun getItemViewType(position: Int): Int {
                return when {
                    position < this.data.tweets.size -> TYPE_TWEET
                    else -> TYPE_SENTINEL
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return when (viewType) {
                    TYPE_TWEET -> {
                        val view = LayoutInflater.from(this.context)
                                .inflate(R.layout.layout_tweet_list_item, parent, false)
                        val retweetedBy: TextView = view.findViewById(R.id.retweeted_by)
                        retweetedBy.text = this.context.getString(R.string.retweeted_by, this.data.user.name)
                        ViewHolder.TweetHolder(view)
                    }
                    else -> {
                        val view = LayoutInflater.from(this.context)
                                .inflate(R.layout.layout_tweet_sentinel, parent, false)
                        ViewHolder.Sentinel(view)
                    }
                }
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                when (holder) {
                    is ViewHolder.TweetHolder -> {
                        val tweet = this.data.tweets[position]
                        holder.setTweet(tweet, this.fragment)
                    }
                    is ViewHolder.Sentinel -> {
                        holder.setLoading(this.fragment.mHasNext)
                    }
                }
            }

            override fun onViewRecycled(holder: ViewHolder) {
                return when (holder) {
                    is ViewHolder.TweetHolder -> {
                        holder.icon.setImageResource(0)
                    }
                    is ViewHolder.Sentinel -> {}
                }
            }

            companion object {
                val TYPE_TWEET = 1
                val TYPE_SENTINEL = 2
            }
        }

        sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            class TweetHolder(itemView: View) : ViewHolder(itemView) {
                val displayName: TextView = itemView.findViewById(R.id.displayName)
                val userName: TextView = itemView.findViewById(R.id.userName)
                val created: TextView = itemView.findViewById(R.id.created)
                val message: TextView = itemView.findViewById(R.id.message)
                val retweet: RelativeLayout = itemView.findViewById(R.id.retweet)
                val icon: ImageView = itemView.findViewById(R.id.icon)

                init {
                    this.message.movementMethod = LinkMovementMethod.getInstance()
                }

                @Suppress("DEPRECATION")
                fun setTweet(tweet: Tweet?, fragment: TweetFragment): Unit {
                    this.userName.text = if (tweet != null) fragment.getString(R.string.format_username, tweet.user.name) else ""
                    this.displayName.text = tweet?.user?.display ?: fragment.getString(R.string.refusal)
                    this.created.text = tweet?.created?.toString(fragment.getString(R.string.format_date)) ?: ""
                    this.message.text = if (tweet != null) Html.fromHtml(tweet.raw) else ""
                    this.retweet.visibility = if (tweet != null && tweet.isRetweet) RelativeLayout.VISIBLE else RelativeLayout.GONE

                    if (tweet != null) {
                        this.icon.load(tweet.user.image.bigger)
                        this.itemView.setOnClickListener({ fragment.mListener.onOpenStatus(tweet) })
                    } else {
                        this.icon.setImageResource(R.drawable.designer_icon)
                        this.itemView.setOnClickListener(null)
                    }
                }
            }

            class Sentinel(itemView: View) : ViewHolder(itemView) {
                val progress: ProgressBar = itemView.findViewById(R.id.progress)
                val sentinel: ImageView = itemView.findViewById(R.id.sentinel)

                fun setLoading(loading: Boolean): Unit {
                    this.progress.visibility = if (loading) View.VISIBLE else View.INVISIBLE
                    this.sentinel.visibility = if (loading) View.INVISIBLE else View.VISIBLE
                }
            }
        }

        interface OnTweetFragmentInteractionListener {
            var query: TwilogClient.Query
            var client: TwilogClient

            fun onOpenStatus(tweet: Tweet): Unit
            fun openByQuery(query: TwilogClient.Query): Unit
        }
    }
}

