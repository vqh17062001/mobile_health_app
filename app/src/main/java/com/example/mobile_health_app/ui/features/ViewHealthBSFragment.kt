package com.example.mobile_health_app.ui.features

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import com.example.mobile_health_app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A bottom sheet dialog fragment that displays user health information
 * and occupies 90% of the screen height when shown.
 */
class ViewHealthBSFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_health_bs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up close button
        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        // Initialize your views and data here
        setupHealthInformation()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Set the height to 90% of screen height when displayed
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                // Set the height to 90% of screen height
                val layoutParams = it.layoutParams
                val windowHeight = requireActivity().window.decorView.height
                layoutParams.height = (windowHeight * 0.9).toInt()
                it.layoutParams = layoutParams

                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    private fun setupHealthInformation() {
        // Here you would fetch and display the user's health information
        // This is a placeholder for your actual implementation
    }

    companion object {
        const val TAG = "ViewHealthBottomSheet"

        fun newInstance(): ViewHealthBSFragment {
            return ViewHealthBSFragment()
        }
    }
}