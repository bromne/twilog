package com.bromne.twilog.activity

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import org.joda.time.LocalDate

class DatePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // TODO: 呼び出し元フラグメントがリスナーであることを前提としているが、本当はよくない。
        // このフラグメントのパラメーターとして与えたいが……。
        val listener = targetFragment as DatePickerDialog.OnDateSetListener

        val date = this.arguments!!.getSerializable(KEY_DATE) as LocalDate
        val dialog = DatePickerDialog(this.context, listener, date.year, date.monthOfYear - 1, date.dayOfMonth)
        return dialog
    }

    companion object {
        val KEY_DATE = "date"

        fun newInstance(date: LocalDate): DatePickerDialogFragment {
            val fragment = DatePickerDialogFragment()
            val args = Bundle()
            args.putSerializable(KEY_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }
}
