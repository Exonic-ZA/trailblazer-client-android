package org.traccar.client.trailblazer.util

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CredentialHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    companion object {
        private const val CREDENTIAL_ID = "trailblazer_api_credentials"
        private const val TAG = "CredentialManager"
    }

    /**
     * Attempts to retrieve saved credentials from Credential Manager
     * This will automatically select the first credential if only one exists,
     * or show picker if multiple exist
     * @return Pair of username and password, or null if no credentials found
     */
    suspend fun getStoredCredentials(username: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val getPasswordOption = GetPasswordOption(allowedUserIds = setOf(username),
                isAutoSelectAllowed = true)
            val getCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(getPasswordOption)
                .build()

            val credentialResponse = credentialManager.getCredential(
                context = context,
                request = getCredentialRequest
            )

            val credential = credentialResponse.credential
            if (credential is PasswordCredential) {
                return@withContext Pair(credential.id, credential.password)
            }

            return@withContext null

        } catch (e: NoCredentialException) {
            // No credentials found - this is expected for first-time users
            Log.e(TAG, "getStoredCredentials: ", e)
            return@withContext null
        } catch (e: GetCredentialException) {
            // Handle other credential retrieval errors
            throw Exception("Failed to retrieve credentials: ${e.message}", e)
        }
    }

    /**
     * Saves credentials to Android Credential Manager
     * This will replace any existing credentials with the same username
     * @param username The username to save
     * @param password The password to save
     */
    suspend fun saveCredentials(username: String, password: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "saveCredentials: username $username")
            val createPasswordRequest = CreatePasswordRequest(
                id = username, // Use username as the credential ID
                password = password
            )

            credentialManager.createCredential(
                context = context,
                request = createPasswordRequest
            )

        } catch (e: CreateCredentialException) {
            Log.d(TAG, "saveCredentials: " + e.type)
            Log.d(TAG, "saveCredentials: " + e.cause)
//            throw Exception("Failed to save credentials: ${e.message}", e)
        }
    }


    /**
     * Clears all stored credentials for this app
     * Note: This requires API level 34+ for programmatic deletion
     * For older versions, users need to manually delete through system settings
     */
    suspend fun clearCredentials() = withContext(Dispatchers.IO) {
        try {
            // Unfortunately, there's no direct API to delete credentials programmatically
            // in older Android versions. Users would need to go to:
            // Settings > Passwords & accounts > [Your App] > Delete

            // For now, we can only clear our local reference
            // The actual credential deletion depends on Android version and user action

        } catch (e: Exception) {
            throw Exception("Failed to clear credentials: ${e.message}", e)
        }
    }
}