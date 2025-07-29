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
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf // Import for easy bundle creation
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult // For sending results back
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.traccar.client.R
import org.traccar.client.trailblazer.util.CredentialHelper

// 1. Remove parameters from the primary constructor
class LoginDialog : DialogFragment() {

    // Define keys for arguments and results
    companion object {
        const val REQUEST_KEY_LOGIN = "login_request_key"
        const val BUNDLE_KEY_USERNAME = "username"
        const val BUNDLE_KEY_PASSWORD = "password"
        const val BUNDLE_KEY_IS_SUCCESS = "is_success"
        private const val ARG_IS_RETRY = "arg_is_retry" // Key for the isRetry argument

        // Factory method to create an instance with arguments
        fun newInstance(isRetry: Boolean): LoginDialog {
            val args = bundleOf(ARG_IS_RETRY to isRetry)
            val fragment = LoginDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var titleText: TextView
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar

    // You can retrieve the isRetry value here
    private var isRetry: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve arguments here
        arguments?.let {
            isRetry = it.getBoolean(ARG_IS_RETRY, false)
        }
    }

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
        isCancelable = false // Prevent dismissal by pressing back button
    }

    private fun initViews(view: View) {
        usernameEditText = view.findViewById(R.id.edittext_username)
        passwordEditText = view.findViewById(R.id.edittext_password)
        loginButton = view.findViewById(R.id.button_login)
        cancelButton = view.findViewById(R.id.button_cancel)
        progressBar = view.findViewById(R.id.progress_bar)

        if (isRetry) {
            titleText = view.findViewById(R.id.login_title)
            titleText.setText(R.string.login_check_credentials)
        }
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
            // Use setFragmentResult to communicate back that login was cancelled
            setFragmentResult(REQUEST_KEY_LOGIN, bundleOf(BUNDLE_KEY_IS_SUCCESS to false))
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
                val credentialHelper = CredentialHelper(requireContext())
                credentialHelper.saveCredentials(username, password)

                setLoadingState(false)
                // Use setFragmentResult to communicate success and data back
                setFragmentResult(
                    REQUEST_KEY_LOGIN,
                    bundleOf(
                        BUNDLE_KEY_IS_SUCCESS to true,
                        BUNDLE_KEY_USERNAME to username,
                        BUNDLE_KEY_PASSWORD to password
                    )
                )
                dismiss()

            } catch (e: Exception) {
                setLoadingState(false)
                Toast.makeText(context, "Failed to save credentials: ${e.message}", Toast.LENGTH_LONG).show()
                // You might want to send a failed result back too
                setFragmentResult(REQUEST_KEY_LOGIN, bundleOf(BUNDLE_KEY_IS_SUCCESS to false))
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