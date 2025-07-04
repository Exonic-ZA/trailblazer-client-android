package org.traccar.client.trailblazer.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.traccar.client.R
import org.traccar.client.trailblazer.util.CredentialHelper

class LoginDialog(
    private val onLoginSuccess: (username: String, password: String) -> Unit,
    private val onLoginCancel: () -> Unit = {}
) : DialogFragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()

        // Make dialog non-cancelable by touching outside
        dialog?.setCanceledOnTouchOutside(false)
        isCancelable = false
    }

    private fun initViews(view: View) {
        usernameEditText = view.findViewById(R.id.edittext_username)
        passwordEditText = view.findViewById(R.id.edittext_password)
        loginButton = view.findViewById(R.id.button_login)
        cancelButton = view.findViewById(R.id.button_cancel)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(username, password)) {
                performLogin(username, password)
            }
        }

        cancelButton.setOnClickListener {
            onLoginCancel()
            dismiss()
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            usernameEditText.error = "Username is required"
            usernameEditText.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin(username: String, password: String) {
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                // Save credentials to Credential Manager
                val credentialHelper = CredentialHelper(requireContext())
                credentialHelper.saveCredentials(username, password)

                setLoadingState(false)
                onLoginSuccess(username, password)
                dismiss()

            } catch (e: Exception) {
                setLoadingState(false)
                Toast.makeText(context, "Failed to save credentials: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        loginButton.isEnabled = !isLoading
        cancelButton.isEnabled = !isLoading
        usernameEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}