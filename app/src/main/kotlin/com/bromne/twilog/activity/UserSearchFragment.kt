package com.bromne.twilog.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText

import com.bromne.twilog.R

class UserSearchFragment : Fragment() {
    internal lateinit var mListener: Listener

    internal lateinit var mUserName: EditText
    internal lateinit var mMove: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_search, container, false)
        mUserName = root.findViewById(R.id.name)
        mMove = root.findViewById(R.id.move)

        mUserName.setOnEditorActionListener { sender, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onMoveButtonSubmit(sender.text.toString())
                true
            } else {
                false
            }
        }
        mMove.setOnClickListener { sender -> onMoveButtonSubmit(mUserName.text.toString()) }
        return root
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mListener = context as Listener
    }

    fun onMoveButtonSubmit(userName: String) {
        mListener.onMoveToUser(userName)
    }

    interface Listener {
        fun onMoveToUser(userName: String)
    }

    companion object {
        fun newInstance(): UserSearchFragment {
            return UserSearchFragment()
        }
    }
}

