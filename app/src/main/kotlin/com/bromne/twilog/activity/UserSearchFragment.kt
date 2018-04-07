package com.bromne.twilog.activity

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import com.bromne.stereotypes.data.Either
import com.bromne.twilog.R
import com.bromne.twilog.app.AppActivity.Companion.SearchStarting
import com.bromne.twilog.client.TwilogClient.Query
import com.bromne.twilog.client.TwilogClient.Order

class UserSearchFragment : Fragment() {
    internal lateinit var mListener: UserSearchFragmentListener

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
        mListener = context as UserSearchFragmentListener
    }

    fun onMoveButtonSubmit(userName: String) {
        mListener.onMoveToUser(userName)
    }

    companion object {
        fun newInstance(): UserSearchFragment {
            return UserSearchFragment()
        }

        interface UserSearchFragmentListener : SearchStarting {
            fun onMoveToUser(userName: String) : Unit {
                this.openByQuery(Query(userName, Either.left(null), Order.DESC))
            }
        }
    }
}

