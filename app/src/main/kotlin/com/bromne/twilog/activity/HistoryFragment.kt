package com.bromne.twilog.activity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bromne.stereotypes.app.layoutInflater
import com.bromne.stereotypes.data.Either
import com.bromne.twilog.R
import com.bromne.twilog.app.AppActivity.Companion.SearchStarting
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.ViewExtensions.load
import com.bromne.twilog.client.TwilogClient
import com.bromne.twilog.client.User

class HistoryFragment : Fragment() {
    lateinit var mModel: HistoryViewModel

    lateinit var mListener: HistoryFragmentListener
    lateinit var mList: RecyclerView

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as HistoryFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mModel = ViewModelProviders.of(this).get(HistoryViewModel::class.java)
        mModel.queries.value = mListener.findHistory()

        val root = inflater.inflate(R.layout.fragment_history, container, false)
        mList = root.findViewById(R.id.list)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val historyAdapter = HistoryAdapter(this)
        mList.layoutManager = LinearLayoutManager(this.context)
        mList.adapter = historyAdapter
        mModel.queries.observe(this, Observer {
            items -> historyAdapter.items = items!!
        })
    }

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }

        interface HistoryFragmentListener : SearchStarting {
            fun findHistory(): List<SavedQuery>
        }

        class HistoryViewModel : ViewModel() {
            val queries: MutableLiveData<List<SavedQuery>> = MutableLiveData()
        }

        class HistoryAdapter(val fragment: HistoryFragment) : RecyclerView.Adapter<ViewHolder>() {
            var items: List<SavedQuery> = emptyList()
                set(value) {
                    field = value
                    notifyDataSetChanged()
                }

            override fun getItemCount() = this.items.size

            override fun getItemViewType(position: Int): Int {
                val body = items[position].query.body
                if (body is Either.Left && body.value == null)
                    return TYPE_USER
                else
                    return TYPE_QUERY
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
                TYPE_USER -> ViewHolder.UserHolder.of(parent, false)
                else -> ViewHolder.QueryHolder.of(parent, false)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = this.items[position]

                holder.setData(item.query, item.user)
            }

            companion object {
                val TYPE_USER = 1
                val TYPE_QUERY = 2
            }
        }

        sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val context: Context = itemView.context
            val icon: ImageView = itemView.findViewById(R.id.icon)
            val userName: TextView = itemView.findViewById(R.id.userName)
            val displayName: TextView = itemView.findViewById(R.id.displayName)

            open fun setData(query: TwilogClient.Query, user: User) {
                this.itemView.setOnClickListener({})

                this.icon.load(user.image.bigger)
                this.displayName.text = user.display
                this.userName.text = String.format(context.getString(R.string.format_username), user.name)
            }

            class UserHolder(itemView: View) : ViewHolder(itemView) {
                companion object {
                    fun of(parent :ViewGroup, attachToRoot: Boolean): UserHolder {
                        return parent.context.layoutInflater
                                .inflate(R.layout.layout_saved_user, parent, attachToRoot)
                                .let(::UserHolder)
                    }
                }
            }

            class QueryHolder(itemView: View) : ViewHolder(itemView) {
                val functionIcon: TextView = itemView.findViewById(R.id.function_icon)
                val condition: TextView = itemView.findViewById(R.id.condition)

                override fun setData(query: TwilogClient.Query, user: User) {
                    super.setData(query, user)

                    this.functionIcon.text = query.body.map({
                        if (it != null)
                            R.string.fontawesome_calendar
                        else
                            R.string.fontawesome_clock_o
                    } , {
                        R.string.fontawesome_search
                    }).let { this.context.getString(it) }

                    val sort = (if (query.order == TwilogClient.Order.ASC) R.string.ascending else R.string.descending)
                            .let { this.context.getString(it) }
                    val conditionText = query.body.map({
                        it?.toString(this.context.getString(R.string.date_format_with_day)) ?: this.context.getString(R.string.recent_tweets)
                    }, {
                        "\""+ it.keyword + "\""
                    })
                    condition.text = this.context.getString(R.string.query_representation_format, conditionText, sort)
                }

                companion object {
                    fun of(parent :ViewGroup, attachToRoot: Boolean): QueryHolder {
                        return parent.context.layoutInflater
                                .inflate(R.layout.layout_saved_query, parent, attachToRoot)
                                .let(::QueryHolder)
                    }
                }
            }
        }
    }
}