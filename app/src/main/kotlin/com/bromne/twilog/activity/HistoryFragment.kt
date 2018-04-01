package com.bromne.twilog.activity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bromne.stereotypes.app.layoutInflater
import com.bromne.stereotypes.async.ParallelAsyncLoader
import com.bromne.stereotypes.async.RegularAsyncTask
import com.bromne.stereotypes.view.startAnimation
import com.bromne.twilog.R
import com.bromne.twilog.app.SavedQuery

class HistoryFragment : Fragment() {
    lateinit var mModel: HistoryViewModel

    val imageLoader: ParallelAsyncLoader<String, Bitmap> = ParallelAsyncLoader()

    lateinit var mListener: Listener
    lateinit var mList: RecyclerView

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mModel = ViewModelProviders.of(this).get(HistoryViewModel::class.java)
        mModel.queries.value = mListener.findHistory()

        val root = inflater.inflate(R.layout.fragment_history, container, false)
        mList = root.findViewById(R.id.list)
        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
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

        class HistoryViewModel : ViewModel() {
            val queries: MutableLiveData<List<SavedQuery>> = MutableLiveData()
        }

        interface Listener {
            fun findHistory(): List<SavedQuery>
        }

        class HistoryAdapter(val fragment: HistoryFragment) : RecyclerView.Adapter<HistoryHolder>() {
            var items: List<SavedQuery> = emptyList()
                set(value) {
                    field = value
                    notifyDataSetChanged()
                }

            internal val cache: LruCache<String, Bitmap> = LruCache(100)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
                return HistoryHolder.of(parent, false)
            }

            override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
                val item = this.items[position]

                holder.setQuery(item)

                val key = "default"
                val restored = this.cache[key]
                if (restored != null) {
                    holder.icon.setImageBitmap(restored)
                } else {
                    this.fragment.imageLoader.loadOrRegister(key, object : RegularAsyncTask.Callbacks<Bitmap> {
                        override fun onPreLoad() {
                            holder.icon.tag = key
                        }

                        override fun loadInBackground(publishProgress: (Int) -> Unit): Bitmap {
                            return BitmapFactory.decodeResource(this@HistoryAdapter.fragment.context.resources, R.drawable.designer_icon)
                        }

                        override fun onLoadFinished(result: Bitmap) {
                            this@HistoryAdapter.cache.put(key, result)
                            if (holder.icon.tag == key) {
                                holder.icon.setImageBitmap(result)
                                holder.icon.startAnimation(R.anim.fade_in_medium)
                            }
                        }

                        override fun onException(e: Exception) {
//                        holder.icon.setImageResource(R.drawable.designer_icon)
//                        holder.icon.startAnimation(R.anim.fade_in_medium)
                        }
                    })
                }
                holder.icon
            }

            override fun getItemCount() = this.items.size
        }

        class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val context: Context = itemView.context
            val icon: ImageView = itemView.findViewById(R.id.icon)
//            val displayName: TextView = itemView.findViewById(R.id.displayName)
            val userName: TextView = itemView.findViewById(R.id.userName)

            fun setQuery(query: SavedQuery): Unit {
                this.userName.text = this.context.resources.getString(R.string.format_username, query.query.userName)
            }

            companion object {
                fun of(parent :ViewGroup, attachToRoot: Boolean): HistoryHolder {
                    return parent.context.layoutInflater
                            .inflate(R.layout.layout_saved_query_user, parent, attachToRoot)
                            .let(::HistoryHolder)
                }
            }
        }
    }
}