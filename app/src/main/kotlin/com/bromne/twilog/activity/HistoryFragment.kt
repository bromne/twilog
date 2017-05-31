package com.bromne.twilog.activity

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
import com.bromne.twilog.R
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.layoutInflater

class HistoryFragment : Fragment() {

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
        mList.adapter = HistoryAdapter(this.context, { mListener.findHistory() })
    }

    companion object {
        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }

    interface Listener {
        fun findHistory(): List<SavedQuery>
    }

    class HistoryAdapter(val context: Context, val items: () -> List<SavedQuery>) : RecyclerView.Adapter<HistoryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
            return HistoryHolder.of(parent, false)
        }

        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
            val item = this.items()[position]

            holder.setQuery(item)
        }

        override fun getItemCount() = this.items().size
    }

    class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon = itemView.findViewById(R.id.icon) as ImageView
        val displayName = itemView.findViewById(R.id.displayName) as TextView
        val userName = itemView.findViewById(R.id.userName) as TextView

        fun setQuery(query: SavedQuery): Unit {
            this.icon.setImageResource(R.drawable.designer_icon)
            this.userName.text = "@" + query.query.userName
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