package com.bromne.twilog.activity

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
    val imageLoader: ParallelAsyncLoader<String, Bitmap> = ParallelAsyncLoader()

    lateinit var mListener: Listener
    lateinit var mList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_history, container, false)
        mList = root.findViewById(R.id.list) as RecyclerView
        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mList.layoutManager = LinearLayoutManager(this.context)
        mList.adapter = HistoryAdapter(this, { mListener.findHistory() })
    }

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    interface Listener {
        fun findHistory(): List<SavedQuery>
    }

    class HistoryAdapter(val fragment: HistoryFragment, val items: () -> List<SavedQuery>) : RecyclerView.Adapter<HistoryHolder>() {
        internal val cache: LruCache<String, Bitmap> = LruCache(100)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
            return HistoryHolder.of(parent, false)
        }

        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
            val item = this.items()[position]

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

        override fun getItemCount() = this.items().size
    }
    class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val context: Context = itemView.context
        val icon = itemView.findViewById(R.id.icon) as ImageView
        val displayName = itemView.findViewById(R.id.displayName) as TextView
        val userName = itemView.findViewById(R.id.userName) as TextView

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