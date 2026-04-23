package com.ptsl.networksdk_event

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ViewPagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pager)

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val viewPager = findViewById<ViewPager2>(R.id.view_pager)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Fragment ${position + 1}"
        }.attach()

        startAutoScroll(viewPager)
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var runnable: Runnable? = null

    private fun startAutoScroll(viewPager: ViewPager2) {
        runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager.currentItem
                val nextItem = if (currentItem == 4) 0 else currentItem + 1
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable!!, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }

    private class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> com.ptsl.networksdk_event.ui.FragmentOne()
                1 -> com.ptsl.networksdk_event.ui.FragmentTwo()
                2 -> com.ptsl.networksdk_event.ui.FragmentThree()
                3 -> com.ptsl.networksdk_event.ui.FragmentFour()
                4 -> com.ptsl.networksdk_event.ui.FragmentFive()
                else -> com.ptsl.networksdk_event.ui.FragmentOne()
            }
        }
    }
}
