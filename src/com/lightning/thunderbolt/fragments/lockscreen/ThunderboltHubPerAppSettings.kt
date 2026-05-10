/*
 * Copyright (C) 2026 LightningOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightning.thunderbolt.fragments.lockscreen

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.android.settings.R
import org.json.JSONArray

class MistHubPerAppSettings : Fragment(R.layout.hide_applist_layout) {

    private lateinit var packageManager: PackageManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var packageList: List<PackageInfo>

    private var appBarLayout: AppBarLayout? = null
    private var searchText = ""
    private var showSystem = false
    private var optionsMenu: Menu? = null

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().setTitle(R.string.lightning_hub_per_app_title)
        appBarLayout = requireActivity().findViewById(R.id.app_bar)
        packageManager = requireContext().packageManager
        packageList = packageManager.getInstalledPackages(PackageManager.MATCH_ANY_USER)
    }

    override fun onStart() {
        super.onStart()
        updateOptionsMenu()
        activity?.invalidateOptionsMenu()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppListAdapter()
        recyclerView = view.findViewById<RecyclerView>(R.id.user_list_view).also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        optionsMenu = menu
        inflater.inflate(R.menu.hide_applist_menu, menu)

        menu.findItem(R.id.show_overlay)?.isVisible = false
        menu.findItem(R.id.hide_overlay)?.isVisible = false

        val searchMenuItem = menu.findItem(R.id.search) as MenuItem
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                appBarLayout?.setExpanded(false, false)
                ViewCompat.setNestedScrollingEnabled(recyclerView, false)
                return true
            }
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                appBarLayout?.setExpanded(false, false)
                ViewCompat.setNestedScrollingEnabled(recyclerView, true)
                return true
            }
        })
        val searchView = searchMenuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_apps)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false
            override fun onQueryTextChange(newText: String): Boolean {
                searchText = newText
                refreshList()
                return true
            }
        })
        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_system, R.id.hide_system -> {
                showSystem = !showSystem
                refreshList()
            }
        }
        updateOptionsMenu()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) = updateOptionsMenu()
    override fun onDestroyOptionsMenu() { optionsMenu = null }

    private fun updateOptionsMenu() {
        val menu = optionsMenu ?: return
        menu.findItem(R.id.show_system)?.isVisible = !showSystem
        menu.findItem(R.id.hide_system)?.isVisible = showSystem
        menu.findItem(R.id.show_overlay)?.isVisible = false
        menu.findItem(R.id.hide_overlay)?.isVisible = false
    }

    private fun getCheckedPackages(): List<String> {
        val json = Settings.System.getString(requireContext().contentResolver, SETTING_KEY)
            ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveCheckedPackages(packages: Set<String>) {
        val arr = JSONArray(packages.toList())
        Settings.System.putString(requireContext().contentResolver, SETTING_KEY, arr.toString())
    }

    private fun refreshList() {
        var list = packageList
            .filter { pkg ->
                if (!showSystem) !pkg.applicationInfo!!.isSystemApp()
                else true
            }
            .filter { getLabel(it).contains(searchText, ignoreCase = true) }
            .sortedWith { a, b -> getLabel(a).compareTo(getLabel(b)) }
        if (::adapter.isInitialized) adapter.submitList(list.map { appInfoFromPackageInfo(it) })
    }

    private fun appInfoFromPackageInfo(pkg: PackageInfo) = AppInfo(
        pkg.packageName,
        getLabel(pkg),
        pkg.applicationInfo!!.loadIcon(packageManager)
    )

    private fun getLabel(pkg: PackageInfo) =
        pkg.applicationInfo!!.loadLabel(packageManager).toString()

    private inner class AppListAdapter : ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {

        private val selectedPackages = mutableSetOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(layoutInflater.inflate(R.layout.hide_applist_list_item, parent, false))

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            val item = getItem(position)
            holder.label?.text = item.label
            holder.packageName?.text = item.packageName
            holder.icon?.setImageDrawable(item.icon)
            holder.checkBox?.isChecked = selectedPackages.contains(item.packageName)
            holder.checkBox?.setOnCheckedChangeListener(null)

            holder.itemView.setOnClickListener {
                val pkg = item.packageName
                if (selectedPackages.contains(pkg)) {
                    selectedPackages.remove(pkg)
                } else {
                    selectedPackages.add(pkg)
                }
                saveCheckedPackages(selectedPackages)
                notifyItemChanged(position)
            }
        }

        override fun submitList(list: List<AppInfo>?) {
            selectedPackages.clear()
            selectedPackages.addAll(getCheckedPackages())
            super.submitList(list)
        }
    }

    private class AppListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView? = view.findViewById(R.id.app_icon)
        val label: TextView? = view.findViewById(R.id.app_name)
        val packageName: TextView? = view.findViewById(R.id.package_name)
        val checkBox: CheckBox? = view.findViewById(R.id.check_box)
    }

    private data class AppInfo(val packageName: String, val label: String, val icon: Drawable)

    companion object {
        const val SETTING_KEY = "lightning_hub_allowed_apps"

        private val itemCallback = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a == b
        }
    }
}
