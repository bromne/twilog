package com.bromne.view

import android.content.Context
import android.graphics.Typeface
import android.support.design.widget.TabLayout
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import com.bromne.twilog.R

class SpecialFaceTabLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : TabLayout(context, attrs, defStyleAttr) {
    internal val mTypeface: Typeface

    init {
        val attribites = context.obtainStyledAttributes(attrs, R.styleable.SpecialFaceTabLayout)
        val facePath = attribites.getString(R.styleable.SpecialFaceTabLayout_typeface)
        mTypeface = if (facePath != null) Typeface.createFromAsset(context.assets, facePath) else Typeface.DEFAULT
    }

    override fun addTab(tab: TabLayout.Tab, setSelected: Boolean) {
        super.addTab(tab, setSelected)

        val mainView = getChildAt(0) as ViewGroup
        val tabView = mainView.getChildAt(tab.position) as ViewGroup

        val tabChildCount = tabView.childCount
        for (i in 0..tabChildCount - 1) {
            val tabViewChild = tabView.getChildAt(i)
            if (tabViewChild is TextView) {
                tabViewChild.setTypeface(mTypeface, Typeface.NORMAL)
            }
        }
    }
}