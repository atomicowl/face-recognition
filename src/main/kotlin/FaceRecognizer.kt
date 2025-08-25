import org.bytedeco.opencv.global.opencv_core.CV_32SC1
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_imgcodecs.imdecode
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier

object FaceRecognizer {
    private val faceRecognizer = LBPHFaceRecognizer.create()
    private val labelToName = HashMap<Int, String>()
    private var nextLabelId = 0

    // Haar cascade classifier
    private val faceCascade: CascadeClassifier = CascadeClassifier("haarcascade_frontalface_alt.xml")

    // Dataset: Mat images and labels
    private val images = ArrayList<Mat>()
    private val labels = ArrayList<Int>()

    init {
        // You may need to download this XML from:
        // https://github.com/opencv/opencv/blob/master/data/haarcascades/haarcascade_frontalface_alt.xml
        if (faceCascade.empty()) {
            throw RuntimeException("Failed to load Haar cascade classifier!")
        }
    }

    fun enroll(imageBytes: ByteArray, name: String) {
        val mat = imdecode(Mat(*imageBytes), IMREAD_COLOR)
        val gray = Mat()
        cvtColor(mat, gray, COLOR_BGR2GRAY)

        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)

        if (faces.size() == 0L) {
            println("No face found for $name")
            return
        }

        val faceMat = Mat(gray, faces[0])
        resize(faceMat, faceMat, Size(200, 200)) // LBPH expects uniform size

        val label = nextLabelId++

        // Add BEFORE training
        images.add(faceMat)
        labels.add(label)
        labelToName[label] = name

        if (images.size != labels.size) {
            println("Mismatch: images=${images.size}, labels=${labels.size}")
            return
        }

        // Prepare MatVector and label Mat
        val imagesMat = MatVector(images.size.toLong())
        val labelsMat = Mat(images.size, 1, CV_32SC1) // Corrected label mat size/type

        for (i in images.indices) {
            imagesMat.put(i.toLong(), images[i])
            labelsMat.ptr(i).putInt(labels[i])
        }

        faceRecognizer.train(imagesMat, labelsMat)
        println("Enrolled $name with label $label")
    }


    fun processImage(imageBytes: ByteArray): String {
        val mat = imdecode(Mat(*imageBytes), IMREAD_COLOR)
        val gray = Mat()
        cvtColor(mat, gray, COLOR_BGR2GRAY)

        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)

        if (faces.size() == 0L) {
            println("No face found")
            return "unidentified"
        }

        val faceMat = Mat(gray, faces[0])
        resize(faceMat, faceMat, Size(200, 200))

        val label = IntArray(1)
        val confidence = DoubleArray(1)
        faceRecognizer.predict(faceMat, label, confidence)

        return if (confidence[0] < 70.0) {
            labelToName[label[0]] ?: "unidentified"
        } else {
            "unidentified"
        }
    }
}
