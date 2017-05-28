package com.bromne.twilog.activity

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bromne.data.Either
import com.bromne.twilog.R
import com.bromne.twilog.client.TwilogClient

class HomeActivity : AppCompatActivity(), UserSearchFragment.Listener {
    lateinit internal var mSectionsPagerAdapter: SectionsPagerAdapter

    lateinit internal var mViewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        mSectionsPagerAdapter = SectionsPagerAdapter(this)

        mViewPager = findViewById(R.id.container) as ViewPager
        mViewPager.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)

    }

    override fun onMoveToUser(userName: String) {
        MainActivity.Companion.start(this, TwilogClient.Query(userName, Either.left(null), TwilogClient.Order.DESC))
    }

    class PlaceholderFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                savedInstanceState: Bundle?): View? {
            val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
            val textView = rootView.findViewById(R.id.section_label) as TextView
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
                else -> PlaceholderFragment.newInstance(position + 1)
                // else -> throw IllegalArgumentException()
            }
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> this.activity.getString(R.string.fontawesome_user)
                1 -> this.activity.getString(R.string.fontawesome_star)
                2 -> this.activity.getString(R.string.fontawesome_history)
                else -> throw IllegalArgumentException()
            }
        }
    }
}
