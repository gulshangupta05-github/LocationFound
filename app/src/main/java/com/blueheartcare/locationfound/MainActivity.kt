package com.android.blueheartcare.locationfound

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Sampler
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.demolocation.R
import id.zelory.compressor.Compressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var langlong: EditText
    private lateinit var Address: EditText
    private lateinit var locality: EditText
    private lateinit var postalcode: EditText
    private lateinit var imageView: ImageView
    private var latitude: Double? = 0.0
    private var longitude: Double? = 0.0
    private var gps_loc: Location? = null
    private var network_loc: Location? = null
    private var final_loc: Location? = null
    private var photoFile: File? = null
    private var mCurrentPhotoPath = ""
    internal var photoURI: Uri? = null
    private var compressedImage: String = ""
    //private final static int CAPTURE_IMAGE_RESULT=1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        langlong = findViewById(R.id.langlong)
        Address = findViewById(R.id.Address)
        locality = findViewById(R.id.locality)
        postalcode = findViewById(R.id.PostalCode)
        imageView = findViewById(R.id.imageView)
        imageView.setOnClickListener {
            CoroutineScope(IO).launch {
                captureImage()
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "allowed", Toast.LENGTH_SHORT).show()
            getLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                0
            )

            //if (ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},0);) {
            //} else {
        }
    }

    private suspend fun captureImage() {
        //        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        //                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        //
        //        } else {
        //
        //        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

//                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile()
                var photo = Compressor.compress(this@MainActivity, photoFile!!)
                displayMessage(baseContext, photoFile!!.absolutePath)
                Log.i("Gulshan", photoFile!!.absolutePath)

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    photoURI = FileProvider.getUriForFile(
                        this,
                        "com.blueheartcare.locationfound.android.fileprovider",
                        photoFile!!
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
                }
            } catch (ex: Exception) {
                // Error occurred while creating the File
                ex.printStackTrace()
                displayMessage(baseContext, ex.message)
            }

        } else {
            displayMessage(baseContext, "null")
        }
    }

    private fun displayMessage(ctx: Context, message: String?) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */

        )
        // Save a file: path for use with ACTION_VIEW intents

        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data)
        //        Bundle extras = data.getExtras();
        //        ImageView imageBitmap = (imageView);
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.d("photo_test", mCurrentPhotoPath)
            Log.d("photo_test", photoFile!!.absolutePath)
            val myBitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            imageView.setImageBitmap(myBitmap)
            compressandsaveImage()
        } else {
            displayMessage(baseContext, "Request cancelled or something went wrong.")
        }
    }

    private fun compressandsaveImage() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        val comp = Compressor(MainActivity?.applicationContext)
            .compressToFileAsFlowable(File(pImage))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                compressedImage = it.absolutePath

                val file_size = Integer.parseInt((it.length()/1024) .toString())

                Log.d("image_compression", "$compressedImage")
                Log.d("image_compression", "$file_size")

                val selectedImageuri = Uri.fromFile(it)
                add_shop_img.setImageURI(selectedImageuri)
            })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_COARSE_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
                } else {

                }
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            gps_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            Log.d("test", "1")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (gps_loc != null) {
            final_loc = gps_loc
            latitude = final_loc!!.latitude
            longitude = final_loc!!.longitude
            Log.d("test", "latitude,longitude:  $latitude : $longitude")
        } else if (network_loc != null) {
            final_loc = network_loc
            latitude = final_loc!!.latitude
            longitude = final_loc!!.longitude
            Log.d("test", "3")
        } else {
            latitude = 0.0
            longitude = 0.0
            Log.d("test", "4")
        }
        try {
            langlong.setText("$latitude,$longitude")
            val geocoder = Geocoder(applicationContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
            if (addresses != null && addresses.size > 0) {
                val addres = addresses[0].getAddressLine(0)
                val city = addresses[0].locality
                val postalcode_w = addresses[0].postalCode
                Log.d("test", "5")
                locality.setText(city)
                Address.setText(addres)
                postalcode.setText(postalcode_w)
            }
        } catch (e: Exception) {
            Log.d("test", "6")
            e.printStackTrace()
        }

    }

    companion object {

        internal var Tag = "MainActivity"
        private val MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 2
        private val CAPTURE_IMAGE_REQUEST = 1
    }
}


