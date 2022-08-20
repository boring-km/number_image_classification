package com.boringkm.imageclassification.view.realtime

import android.annotation.SuppressLint
import android.app.Fragment
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.*
import android.widget.Toast
import com.boringkm.imageclassification.R
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.sign


@SuppressLint("ValidFragment")
class CameraFragment private constructor(
    private val connectionCallback: ConnectionCallback,
    private val imageAvailableListener: ImageReader.OnImageAvailableListener,
    private val inputSize: Size,
    private val cameraId: String
) : Fragment() {

    companion object {
        fun newInstance(
            connectionCallback: ConnectionCallback,
            imageAvailableListener: ImageReader.OnImageAvailableListener,
            inputSize: Size,
            cameraId: String
        ): CameraFragment {
            return CameraFragment(connectionCallback, imageAvailableListener, inputSize, cameraId)
        }
    }

    private var autoFitTextureView: AutoFitTextureView? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewSize = Size(0, 0)
    private var sensorOrientation = 0

    private val cameraOpenCloseLock: Semaphore = Semaphore(1)

    private var cameraDevice: CameraDevice? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var previewReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        autoFitTextureView = view.findViewById(R.id.autoFitTextureView)
    }

    interface ConnectionCallback {
        fun onPreviewSizeChosen(size: Size, cameraRotation: Int)
    }

    interface OnImageAvailableListener {
        fun onImageAvailable(reader: ImageReader)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (!autoFitTextureView!!.isAvailable) {
            autoFitTextureView!!.surfaceTextureListener = surfaceTextureListener
        } else {
            openCamera(autoFitTextureView!!.width, autoFitTextureView!!.height)
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private val surfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera(width, height)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                configureTransform(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }

    private fun openCamera(width: Int, height: Int) {
        val activity = activity!!
        val manager: CameraManager =
            activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        setupCameraOutputs(manager)
        configureTransform(width, height)

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Toast.makeText(
                    context,
                    "Time out waiting to lock camera opening.",
                    Toast.LENGTH_SHORT
                ).show()
                activity.finish()
            } else {
                manager.openCamera(cameraId, stateCallback, backgroundHandler)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (ee: CameraAccessException) {
            ee.printStackTrace()
        }
    }

    private fun setupCameraOutputs(manager: CameraManager) {
        try {
            val chars = manager.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            previewSize = chooseOptimalSize(
                map!!.getOutputSizes(SurfaceTexture::class.java),
                inputSize.width,
                inputSize.height
            )

            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                autoFitTextureView!!.setAspectRatio(previewSize.width, previewSize.height)
            } else {
                autoFitTextureView!!.setAspectRatio(previewSize.height, previewSize.width)
            }
        } catch (cae: CameraAccessException) {
            cae.printStackTrace()
        }
        connectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation)
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val minSize = Math.min(width, height)
        val desiredSize = Size(width, height)

        val bigEnough = ArrayList<Size>()
        val tooSmall = ArrayList<Size>()

        for (option in choices) {
            if (option == desiredSize) {
                return desiredSize
            }

            if (option.height >= minSize && option.width >= minSize) {
                bigEnough.add(option)
            } else {
                tooSmall.add(option)
            }
        }

        if (bigEnough.size > 0) {
            return Collections.min(bigEnough, CompareSizesByArea())
        } else {
            return Collections.max(tooSmall, CompareSizesByArea())
        }
    }

    class CompareSizesByArea: Comparator<Size> {
        override fun compare(left: Size, right: Size): Int {
            return sign(left.width.toFloat() * left.height.toFloat() - right.width.toFloat() * right.height.toFloat()).toInt()
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity
        if (autoFitTextureView == null || activity == null) return

        val rotation = activity.display!!.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(
                centerX - bufferRect.centerX(),
                centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = max(viewHeight.toFloat() / previewSize.height, viewWidth.toFloat() / previewSize.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        autoFitTextureView!!.setTransform(matrix)
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            val activity = activity
            activity.let { activity!!.finish() }
        }

    }

    private fun createCameraPreviewSession() {
        try {
            val texture = autoFitTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)
            previewReader = ImageReader.newInstance(previewSize.width,
                previewSize.height, ImageFormat.YUV_420_888, 2)
            previewReader!!.setOnImageAvailableListener(imageAvailableListener,
                backgroundHandler)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder!!.addTarget(surface)
            previewRequestBuilder!!.addTarget(previewReader!!.surface)

            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            previewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
            previewRequestBuilder!!.set(
                CaptureRequest.FLASH_MODE,
                CameraMetadata.FLASH_MODE_TORCH)


            // FIXME https://stackoverflow.com/questions/67077568/how-to-correctly-use-the-new-createcapturesession-in-camera2-in-android
            @Suppress("DEPRECATION")
            cameraDevice!!.createCaptureSession(
                listOf(surface, previewReader!!.surface),
                sessionStateCallback,
                null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val sessionStateCallback: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                if (null == cameraDevice) {
                    return
                }
                captureSession = cameraCaptureSession
                try {
                    previewRequestBuilder?.let {
                        captureSession!!.setRepeatingRequest(
                            it.build(),
                            null, backgroundHandler
                        )
                    }
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                Toast.makeText(activity, "CameraCaptureSession Failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (null != captureSession) {
                captureSession!!.close()
                captureSession = null
            }
            if (null != cameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (null != previewReader) {
                previewReader!!.close()
                previewReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageListener")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}