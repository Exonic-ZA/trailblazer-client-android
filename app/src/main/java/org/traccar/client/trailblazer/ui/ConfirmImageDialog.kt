package org.traccar.client.trailblazer.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import org.traccar.client.R

class ConfirmImageDialog(private val onRetake: () -> Unit, private val onSend: (()->Unit) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_comfirm_image, null)

        val btnRetake = view.findViewById<Button>(R.id.btnRetake)
        val btnSend = view.findViewById<Button>(R.id.btnSend)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_circular)

        btnRetake.setOnClickListener {
            onRetake.invoke()
            dismiss()
        }

        btnSend.setOnClickListener {
            // Disable buttons & show progress bar
            btnSend.isEnabled = false
            btnRetake.isEnabled = false
            progressBar.visibility = View.VISIBLE

            // Call onSend, and re-enable buttons when finished
            onSend {
                progressBar.visibility = View.GONE
                dismiss()
            }
        }

        builder.setView(view)
        return builder.create()
    }
}

