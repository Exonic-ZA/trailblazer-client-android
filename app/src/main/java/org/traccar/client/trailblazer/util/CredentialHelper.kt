package org.traccar.client.trailblazer.util

import android.content.Context
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
    }

    /**
     * Attempts to retrieve saved credentials from Credential Manager
     * @return Pair of username and password, or null if no credentials found
     */
    suspend fun getStoredCredentials(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val getPasswordOption = GetPasswordOption()
            val getCredentialRequest = GetCredentialRequest(
                listOf(getPasswordOption)
            )

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
            return@withContext null
        } catch (e: GetCredentialException) {
            // Handle other credential retrieval errors
            throw Exception("Failed to retrieve credentials: ${e.message}", e)
        }
    }

    /**
     * Saves credentials to Android Credential Manager
     * @param username The username to save
     * @param password The password to save
     */
    suspend fun saveCredentials(username: String, password: String) = withContext(Dispatchers.IO) {
        try {
            val createPasswordRequest = CreatePasswordRequest(
                id = username, // Use username as the credential ID
                password = password
            )

            credentialManager.createCredential(
                context = context,
                request = createPasswordRequest
            )

        } catch (e: CreateCredentialException) {
            throw Exception("Failed to save credentials: ${e.message}", e)
        }
    }

    /**
     * Checks if credentials are available in Credential Manager
     * @return true if credentials exist, false otherwise
     */
    suspend fun hasStoredCredentials(): Boolean = withContext(Dispatchers.IO) {
        try {
            getStoredCredentials() != null
        } catch (e: Exception) {
            false
        }
    }
}