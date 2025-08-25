import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_imgcodecs.imdecode
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import java.nio.IntBuffer

data class RecognitionResult(
    val name: String,
    val rect: FaceRect? = null
)

data class FaceRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

class FaceRecognizer(
    cascadeFilePath: String
) {
    private val faceCascade = CascadeClassifier(cascadeFilePath)
    private val faceRecognizer: LBPHFaceRecognizer = LBPHFaceRecognizer.create()

    private val images = mutableListOf<Mat>()
    private val labels = mutableListOf<Int>()
    private val labelToName = mutableMapOf<Int, String>()
    private var nextLabelId = 0
    private var isTrained = false

    fun enroll(imageBytes: ByteArray, name: String): Boolean {
        val mat = imdecode(Mat(*imageBytes), IMREAD_COLOR)
        if (mat.empty()) {
            println("Invalid image")
            return false
        }

        val gray = Mat()
        cvtColor(mat, gray, COLOR_BGR2GRAY)

        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)
        if (faces.size() == 0L) {
            println("No face detected for enrollment.")
            return false
        }

        // Assume first face
        val faceRect = faces[0]
        val faceMat = Mat(gray, faceRect)
        resize(faceMat, faceMat, Size(200, 200))

        val label = nextLabelId++
        images.add(faceMat)
        labels.add(label)
        labelToName[label] = name

        trainOrUpdateRecognizer()

        println("Enrolled $name with label $label")
        return true
    }

    private fun trainOrUpdateRecognizer() {
        val imagesMat = MatVector(images.size.toLong())
        for ((i, img) in images.withIndex()) {
            imagesMat.put(i.toLong(), img)
        }

        // Create label Mat (1 column, N rows, int type)
        val labelsMat = Mat(labels.size, 1, CV_32SC1)
        val labelsBuf = labelsMat.createBuffer<IntBuffer>()
        for (i in labels.indices) {
            labelsBuf.put(i, labels[i])
        }

        if (isTrained) {
            faceRecognizer.update(imagesMat, labelsMat)
        } else {
            faceRecognizer.train(imagesMat, labelsMat)
            isTrained = true
        }
    }

    fun recognize(imageBytes: ByteArray): RecognitionResult {
        val mat = imdecode(Mat(*imageBytes), IMREAD_COLOR)
        if (mat.empty()) {
            return RecognitionResult("unidentified", null)
        }

        val gray = Mat()
        cvtColor(mat, gray, COLOR_BGR2GRAY)

        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)
        if (faces.size() == 0L) {
            return RecognitionResult("unidentified", null)
        }

        val faceRect = faces[0]
        val faceMat = Mat(gray, faceRect)
        resize(faceMat, faceMat, Size(200, 200))

        val labelArr = IntArray(1)
        val confidenceArr = DoubleArray(1)
        faceRecognizer.predict(faceMat, labelArr, confidenceArr)

        val confidenceThreshold = 70.0
        val recognizedName =
            if (confidenceArr[0] < confidenceThreshold && labelToName.containsKey(labelArr[0])) {
                labelToName[labelArr[0]] ?: "unidentified"
            } else {
                "unidentified"
            }

        return RecognitionResult(
            name = recognizedName,
            rect = FaceRect(
                x = faceRect.x(),
                y = faceRect.y(),
                width = faceRect.width(),
                height = faceRect.height()
            )
        )
    }
}
