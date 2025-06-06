package com.example.mobile_health_app.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.mobile_health_app.R
import com.example.mobile_health_app.data.model.Feature

class FeaturesAdapter(
    private val context: Context,
    private val features: List<Feature>,
    private val onItemClickListener: (Feature) -> Unit // Add click listener callback
) : BaseAdapter() {
    private val TAG = "FeaturesAdapter"

    override fun getCount(): Int = features.size

    override fun getItem(position: Int): Any = features[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            Log.d(TAG, "getView: Creating new view for position $position")
            view = LayoutInflater.from(context).inflate(R.layout.item_feature, parent, false)

            holder = ViewHolder()
            holder.featureIcon = view.findViewById(R.id.ivFeatureIcon)
            holder.featureTitle = view.findViewById(R.id.tvFeatureTitle)
            holder.featureDescription = view.findViewById(R.id.tvFeatureDescription)

            view.tag = holder
        } else {
            Log.d(TAG, "getView: Reusing view for position $position")
            view = convertView
            holder = view.tag as ViewHolder
        }

        val feature = features[position]
        holder.featureIcon.setImageResource(feature.iconResourceId)
        holder.featureTitle.text = feature.title
        holder.featureDescription.text = feature.description

        // Make each item explicitly clickable
        view.isClickable = true
        view.isFocusable = true

        // Set explicit onClick listener at the item view level
        view.setOnClickListener {
            Log.d(TAG, "Item clicked at position: $position")
            onItemClickListener(feature)
        }

        return view
    }

    class ViewHolder {
        lateinit var featureIcon: ImageView
        lateinit var featureTitle: TextView
        lateinit var featureDescription: TextView
    }
}
