package com.bromne.twilog.activity

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import com.bromne.twilog.R
import com.bromne.twilog.client.TwilogClient

class SearchDialog : DialogFragment() {
    lateinit internal var criteria: TwilogClient.Criteria

    lateinit internal var keyword: EditText
    lateinit internal var junction: RadioGroup
    lateinit internal var search: Button
    lateinit internal var cancel: Button

    lateinit internal var onClickListener: (TwilogClient.Criteria) -> Unit

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        this.criteria = this.arguments.getSerializable(KEY_CRITERA) as TwilogClient.Criteria
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = LayoutInflater.from(this.activity)
                .inflate(R.layout.dialog_search, null, false)

        this.keyword = layout.findViewById(R.id.keyword)
        this.junction = layout.findViewById(R.id.junction)
        this.search = layout.findViewById(R.id.search_button)
        this.cancel = layout.findViewById(R.id.cancel_button)

        this.keyword.setOnEditorActionListener({v, actionId, e ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                this.search.callOnClick()
                true
            } else {
                false
            }
        })
        this.search.setOnClickListener { v ->
            val criteria = TwilogClient.Criteria(this.keyword.text.toString(), if (this.junction.checkedRadioButtonId == R.id.and) TwilogClient.Joint.AND else TwilogClient.Joint.OR)
            this.dialog.dismiss()
            this.onClickListener(criteria)
        }
        this.cancel.setOnClickListener { v -> this.dialog.dismiss() }

        val dialog = Dialog(this.activity)
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layout)

        this.keyword.setText(this.criteria.keyword)
        this.junction.check(if (this.criteria.joint == TwilogClient.Joint.AND) R.id.and else R.id.or)

        this.keyword.requestFocus()

        return dialog
    }

    companion object {
        val KEY_CRITERA = "Criteria"

        fun newInstance(criteria: TwilogClient.Criteria, onSearchListener: (TwilogClient.Criteria) -> Unit): SearchDialog {
            val args = Bundle()
            args.putSerializable(KEY_CRITERA, criteria)

            val dialog = SearchDialog()
            dialog.arguments = args
            dialog.onClickListener = onSearchListener
            return dialog
        }
    }
}
