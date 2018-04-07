package com.bromne.twilog.activity

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bromne.twilog.R
import com.bromne.twilog.activity.HistoryFragment.Companion.HistoryFragmentListener
import com.bromne.twilog.activity.UserSearchFragment.Companion.UserSearchFragmentListener
import com.bromne.twilog.app.AppActivity
import com.bromne.twilog.app.SavedQuery
import com.bromne.twilog.app.history
import com.bromne.twilog.app.sharedPreferences

class HomeActivity : AppActivity(), UserSearchFragmentListener, HistoryFragmentListener {
    lateinit internal var mSectionsPagerAdapter: SectionsPagerAdapter

    lateinit internal var mViewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        mSectionsPagerAdapter = SectionsPagerAdapter(this)

        mViewPager = findViewById(R.id.container)
        mViewPager.adapter = mSectionsPagerAdapter

        val onPageChange = object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                return when (position) {
                    0 -> R.string.find_user
                    1 -> R.string.title_favorites
                    2 -> R.string.title_history
                    else -> throw IllegalArgumentException()
                }.let { this@HomeActivity.setTitle(it) }
            }
        }
        mViewPager.addOnPageChangeListener(onPageChange)
        onPageChange.onPageSelected(mViewPager.currentItem)

        val tabLayout: TabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(mViewPager)
    }

    override fun findHistory(): List<SavedQuery> = this.sharedPreferences.history.toList()

    class PlaceholderFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_home, container, false)
            return rootView
        }

        companion object {
            private val ARG_SECTION_NUMBER = "section_number"

            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    class SectionsPagerAdapter(val activity: HomeActivity) : FragmentPagerAdapter(activity.supportFragmentManager) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> UserSearchFragment.newInstance()
                2 -> HistoryFragment.newInstance()
                else -> PlaceholderFragment.newInstance(position + 1)
            }
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> R.string.fontawesome_user
                1 -> R.string.fontawesome_star
                2 -> R.string.fontawesome_history
                else -> throw IllegalArgumentException()
            }.let { this.activity.getString(it) }
        }
    }
}
