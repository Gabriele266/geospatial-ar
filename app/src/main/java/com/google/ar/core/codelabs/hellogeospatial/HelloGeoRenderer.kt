/*
 * Copyright 2022 Google LLC
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
package com.google.ar.core.codelabs.hellogeospatial

import android.opengl.Matrix
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import com.google.ar.core.TrackingState
import com.google.ar.core.codelabs.hellogeospatial.persistence.GeoAnchor
import com.google.ar.core.codelabs.hellogeospatial.persistence.serialization.AnchorStore
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException

class HelloGeoRenderer(val activity: HelloGeoActivity) :
    SampleRender.Renderer, DefaultLifecycleObserver {
    //<editor-fold desc="ARCore initialization" defaultstate="collapsed">
    companion object {
        val TAG = "HelloGeoRenderer"

        private val Z_NEAR = 0.1f
        private val Z_FAR = 1000f
        const val FILE_NAME = "anchors.adb"
    }

    lateinit var backgroundRenderer: BackgroundRenderer
    lateinit var virtualSceneFramebuffer: Framebuffer
    var hasSetTextureNames = false

    // Virtual object (ARCore pawn)
    lateinit var virtualObjectMesh: Mesh
    lateinit var virtualObjectShader: Shader
    lateinit var virtualObjectTexture: Texture

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    val modelMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16) // view x model

    val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

    val session
        get() = activity.arCoreSessionHelper.session

    val displayRotationHelper = DisplayRotationHelper(activity)
    val trackingStateHelper = TrackingStateHelper(activity)
    var loaded = false

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceCreated(render: SampleRender) {
        // Prepare the rendering objects.
        // This involves reading shaders and 3D model files, so may throw an IOException.
        try {
            // Attach button listeners
            activity.view.clearAnchorBtn.setOnClickListener {
                onClearAnchorClick()
            }

            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

            // Virtual object to render (Geospatial Marker)
            virtualObjectTexture =
                Texture.createFromAsset(
                    render,
                    "models/spatial_marker_baked.png",
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    Texture.ColorFormat.SRGB
                )

            virtualObjectMesh = Mesh.createFromAsset(render, "models/geospatial_marker.obj");
            virtualObjectShader =
                Shader.createFromAssets(
                    render,
                    "shaders/ar_unlit_object.vert",
                    "shaders/ar_unlit_object.frag",
                    /*defines=*/ null
                )
                    .setTexture("u_Texture", virtualObjectTexture)

            backgroundRenderer.setUseDepthVisualization(render, false)
            backgroundRenderer.setUseOcclusion(render, false)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }
    //</editor-fold>

    var earthAnchors: MutableList<Anchor> = mutableListOf()
    private val anchorStore: AnchorStore =
        AnchorStore("${activity.applicationContext.filesDir}/$FILE_NAME")
    private var initialized = false

    /**
     * Called when a frame needs to be drawn
     */
    override fun onDrawFrame(render: SampleRender) {
        val session = session ?: return

        //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame =
            try {
                session.update()
            } catch (e: CameraNotAvailableException) {
                Log.e(TAG, "Camera not available during onDrawFrame", e)
                showError("Camera not available. Try restarting the app.")
                return
            }

        val camera = frame.camera

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // -- Draw background
        if (frame.timestamp != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render)
        }

        // If not tracking, don't draw 3D objects.
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0)

        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        //</editor-fold>

        geospatialAware {
            // We have got the earth
            val cameraPose = it.cameraGeospatialPose
            // Update map position
            activity.view.mapView?.updateMapPosition(
                latitude = cameraPose.latitude,
                longitude = cameraPose.longitude,
                heading = cameraPose.heading
            )

            // Update status text
            activity.view.updateStatusText(it, null)

            // Initialize data if already not done
            if(!initialized) {
                onCreated()
                initialized = true
            }
        }

        // Draw the placed anchor, if it exists.
        earthAnchors.forEach {
            render.renderCompassAtAnchor(it)
        }

        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    /**
     * Called when the renderer is created and needs initialization
     */
    private fun onCreated() {
        // Load anchors into memory
        println("Load anchors")
        val anchors = loadSavedAnchors()

        geospatialAware { heart ->
            anchors.forEach {
                addAnchor(it)
            }
        }
    }

    /**
     * Load all saved anchors
     */
    private fun loadSavedAnchors(): List<GeoAnchor> =
        try {
            val anchors = anchorStore.loadContent()
            Log.i(null, "Loaded ${anchors.size} anchors")
            anchors
        } catch (e: Exception) {
            println("Unable to load anchors $e")
            listOf()
        }

    /**
     * Called when the clear anchor is required
     */
    private fun onClearAnchorClick() {
        earthAnchors.forEach {
            it.detach()
        }

        earthAnchors = mutableListOf()

        activity.runOnUiThread {
            activity.view.updateAnchorCounter(earthAnchors.size)
            activity.view.mapView?.clearEarthMarkers()
            Toast.makeText(activity.applicationContext, "Ancore rimosse", Toast.LENGTH_SHORT).show()
        }

        anchorStore.clearStore()
    }

    /**
     * Called when the map gets clicked to add a new anchor
     */
    fun onMapClick(latLng: LatLng) {
        val earth = session?.earth ?: return

        geospatialAware {
            // All the necessary
            val altitude = earth.cameraGeospatialPose.altitude - 1
            val qx = 0f
            val qy = 0f
            val qz = 0f
            val qw = 1f

            // Create the GeoAnchor
            val anchorData = GeoAnchor("23", "Name")
            anchorData.setPosition(
                latLng.latitude,
                latLng.longitude,
                altitude,
                floatArrayOf(qx, qy, qz, qw)
            )

            // Effectively add the anchor
            addAnchor(anchorData, true)

            // Show a toast
            Toast.makeText(activity.applicationContext, "Ancora piazzata", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Execute a function only if the api are tracking all the necessary
     */
    fun geospatialAware(work: (earth: Earth) -> Unit) {
        val earth = session?.earth ?: return

        if (earth.trackingState == TrackingState.TRACKING && earth.earthState == Earth.EarthState.ENABLED) {
            // All the necessary to run the work
            work(earth)
        }
    }

    /**
     * Add an anchor to the renderer
     */
    private fun addAnchor(anchor: GeoAnchor, persist: Boolean = false) {
        geospatialAware {
            println("Add anchor $anchor")
            // Create the anchor and add it
            earthAnchors.add(
                it.createAnchor(
                    anchor.latitude,
                    anchor.longitude,
                    anchor.altitude,
                    anchor.rotation
            )
                )

            // Update counter
            activity.runOnUiThread {
                val mapView = activity.view.mapView ?: return@runOnUiThread

                // Place the marker on the map
                mapView.earthMarkers.add(
                    mapView.addEarthMarker(
                        LatLng(anchor.latitude, anchor.longitude)
                    )
                )

                // Update the counter
                activity.view.updateAnchorCounter(earthAnchors.size)
            }

            // Persist if necessary
            if (persist) {
                anchorStore.appendToStore(listOf(anchor))
            }
        }
    }

    private fun SampleRender.renderCompassAtAnchor(anchor: Anchor) {
        // Get the current pose of the Anchor in world space. The Anchor pose is updated
        // during calls to session.update() as ARCore refines its estimate of the world.
        anchor.pose.toMatrix(modelMatrix, 0)

        // Calculate model/view/projection matrices
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Update shader properties and draw
        virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
        draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
    }

    private fun showError(errorMessage: String) =
        activity.view.snackbarHelper.showError(activity, errorMessage)
}
