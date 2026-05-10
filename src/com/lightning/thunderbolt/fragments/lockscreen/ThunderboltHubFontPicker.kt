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

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.internal.util.lightning.VibrationUtils
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.lightning.thunderbolt.fragments.themes.fonts.FontManager

class MistHubFontPicker : SettingsPreferenceFragment() {

    companion object {
        const val SETTING_KEY = "lightning_hub_font_package"
    }

    private lateinit var fontManager: FontManager
    private var fontPackages: List<String> = emptyList()
    private var selectedPkg: String = "android"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.lightning_hub_font_picker, container, false)

        fontManager = FontManager(requireContext(), false)
        fontPackages = fontManager.getAllFontPackages()

        selectedPkg = Settings.System.getString(
            requireContext().contentResolver, SETTING_KEY
        ) ?: "android"

        val listView: ListView = root.findViewById(R.id.lightning_hub_font_list)
        listView.adapter = FontListAdapter()

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedPkg = fontPackages[position]
            (listView.adapter as FontListAdapter).notifyDataSetChanged()
            VibrationUtils.triggerVibration(context, 3)
        }

        val applyFab: ExtendedFloatingActionButton = root.findViewById(R.id.lightning_hub_font_apply_fab)
        applyFab.setOnClickListener {
            Settings.System.putString(
                requireContext().contentResolver, SETTING_KEY, selectedPkg
            )
            VibrationUtils.triggerVibration(context, 1)
            parentFragmentManager.popBackStack()
        }

        return root
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.LIGHTNING


    inner class FontListAdapter : BaseAdapter() {

        override fun getCount() = fontPackages.size
        override fun getItem(pos: Int) = fontPackages[pos]
        override fun getItemId(pos: Int) = pos.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.lightning_hub_font_list_item, parent, false)

            val pkg = fontPackages[position]
            val label: TextView = view.findViewById(R.id.font_item_label)
            val preview: TextView = view.findViewById(R.id.font_item_preview)
            val checkIcon: ImageView = view.findViewById(R.id.font_item_check)

            label.text = fontManager.getLabel(requireContext(), pkg)

            val typeface: Typeface? = fontManager.getTypeface(requireContext(), pkg)
            preview.typeface = typeface ?: Typeface.DEFAULT
            preview.text = "The quick brown fox — AaBbCc"

            checkIcon.visibility = if (pkg == selectedPkg) View.VISIBLE else View.GONE

            return view
        }
    }
}
