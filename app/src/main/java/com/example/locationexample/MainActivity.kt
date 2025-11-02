package com.example.locationexample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.locationexample.ui.theme.LocationExampleTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { scaffoldPadding ->
                    MainComposable(scaffoldPadding)
                }
            }
        }
    }

    @Composable
    private fun MainComposable(scaffoldPadding: PaddingValues) {
        Column(modifier = Modifier.padding(scaffoldPadding)) {
            var locationInfo by remember { mutableStateOf("") }
            Text("Location")
            if (locationInfo.isNotEmpty()) {
                Text(locationInfo)
                Log.d("MY LOCATION", locationInfo)
            }
            RequestLocationPermission(
                onPermissionDenied = { locationInfo = "Location permission denied" },
                onPermissionGranted = {
                    getLastUserLocation(
                        onLocationRetrieved = { location ->
                            locationInfo =
                                "Latitude: ${location.first}, Longitude: ${location.second}"
                        },
                        onLocationRetrievalFailed = { retrievalException ->
                            locationInfo = "Error getting location: ${retrievalException.message}"
                        }
                    )
                },
                onPermissionsRevoked = { locationInfo = "Location permission revoked" }
            )
        }
    }

    // https://medium.com/@munbonecci/how-to-get-your-location-in-jetpack-compose-f085031df4c1

    /**
     * Retrieves the last known user location asynchronously.
     *
     * @param onLocationRetrieved Callback function invoked when the location is successfully retrieved.
     *        It provides a Pair representing latitude and longitude.
     * @param onLocationRetrievalFailed Callback function invoked when an error occurs while retrieving the location.
     *        It provides the Exception that occurred.
     */
    @SuppressLint("MissingPermission")
    private fun getLastUserLocation(
        onLocationRetrieved: (Pair<Double, Double>) -> Unit,
        onLocationRetrievalFailed: (Exception) -> Unit
    ) {
        // Check if location permissions are granted
        //if (areLocationPermissionsGranted()) {
        // Retrieve the last known location
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    // If location is not null, invoke the success callback with latitude and longitude
                    onLocationRetrieved(Pair(it.latitude, it.longitude))
                }
            }
            .addOnFailureListener { retrievalException ->
                // If an error occurs, invoke the failure callback with the exception
                onLocationRetrievalFailed(retrievalException)
            }
        //}
    }
}

/**
 * Composable function to request location permissions and handle different scenarios.
 *
 * @param onPermissionGranted Callback to be executed when all requested permissions are granted.
 * @param onPermissionDenied Callback to be executed when any requested permission is denied.
 * @param onPermissionsRevoked Callback to be executed when previously granted permissions are revoked.
 */
@OptIn(ExperimentalPermissionsApi::class) // accompanist-permissions
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionsRevoked: () -> Unit
) {
    // Initialize the state for managing multiple location permissions.
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    // Use LaunchedEffect to handle permissions logic when the composition is launched.
    LaunchedEffect(key1 = locationPermissionState) {
        // Filter permissions that need to be requested.
        val pendingPermissions = locationPermissionState.permissions.filter {
            !it.status.isGranted
        }

        // If there are permissions to request, launch the permission request.
        if (pendingPermissions.isNotEmpty()) locationPermissionState.launchMultiplePermissionRequest()

        // Check if all previously granted permissions are revoked.
        val allLocationPermissionsRevoked =
            locationPermissionState.permissions.size == locationPermissionState.revokedPermissions.size

        // Execute callbacks based on permission status.
        if (allLocationPermissionsRevoked) {
            onPermissionsRevoked()
        } else {
            if (locationPermissionState.allPermissionsGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }
}