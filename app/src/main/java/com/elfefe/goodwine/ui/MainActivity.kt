package com.elfefe.goodwine.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.ContextCompat
import com.elfefe.goodwine.R
import com.elfefe.goodwine.databinding.CameraViewBinding
import com.elfefe.goodwine.mvvm.repository.FirebaseRepository
import com.elfefe.goodwine.mvvm.viewmodel.CameraViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.FirebaseViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.OltpViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.UiViewmodel
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.ui.theme.GoodWineTheme
import com.elfefe.goodwine.utils.enums.Connection
import com.elfefe.goodwine.utils.resString
import com.elfefe.goodwine.utils.saveImage
import com.elfefe.goodwine.utils.timestamp
import com.gowtham.ratingbar.RatingBar
import com.gowtham.ratingbar.StepSize


class MainActivity : ComponentActivity() {
    private val uiViewmodel: UiViewmodel by viewModels()
    private val cameraViewmodel: CameraViewmodel by viewModels()
    private val oltpViewmodel: OltpViewmodel by viewModels()
    private val firebaseViewmodel: FirebaseViewmodel by viewModels()

    private val registerPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
//                googleConnection()
                firebaseViewmodel.setConnection(Connection.Success)
            } else askPermission()
        }
    private val googleConnection = FirebaseRepository.connectGoogle(
        activity = this,
        onSuccess = {
            firebaseViewmodel.setConnection(Connection.Success)
        },
        onFailure = {
            firebaseViewmodel.setConnection(
                Connection.Failure(
                    it ?: Exception(resString(R.string.connection_google_failure))
                )
            )
            connectGoogle()
        }
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private lateinit var keyboardController: SoftwareKeyboardController
    private lateinit var localFocus: FocusManager

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.R)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocalSoftwareKeyboardController.current?.let {
                keyboardController = it
            }
            localFocus = LocalFocusManager.current
            Content()
        }

        // TODO:
        connectGoogle()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askPermission()
        } else {
            firebaseViewmodel.setConnection(Connection.Success)
//            connectGoogle()
        }

        uiViewmodel.setBottle(true)
    }

    private fun askPermission() {
        registerPermission.launch(Manifest.permission.CAMERA)
    }

    private fun connectGoogle() {
        googleConnection()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun hideKeyboard() {
        keyboardController.hide()
        uiViewmodel.setKeyboard(false)
        localFocus.clearFocus()
    }

    @Composable
    fun Loading(modifier: Modifier = Modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier,
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        var isConnected by remember {
            mutableStateOf(false)
        }

        val loadingAlpha by animateFloatAsState(targetValue = if (isConnected) 0f else 1f)
        val mainAlpha by animateFloatAsState(targetValue = if (isConnected && loadingAlpha == 0f) 1f else 0f)

        firebaseViewmodel.connectionLivedata.observe(this) {
            isConnected = it == Connection.Success
        }

        GoodWineTheme {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        uiViewmodel.setScreenSize(it)
                    },
                color = MaterialTheme.colorScheme.background
            ) {
                if (isConnected && loadingAlpha == 0f) Main(Modifier.alpha(mainAlpha))
                else Loading(Modifier.alpha(loadingAlpha))
            }
        }
    }

    @Composable
    fun Main(modifier: Modifier = Modifier) {
        var isAddBottle by remember { mutableStateOf(false) }

        val animateBottleHeight: Dp by animateDpAsState(
            targetValue = if (isAddBottle) 350.dp else 0.dp
        )

        uiViewmodel.addBottleLivedata.observe(this) {
            isAddBottle = it
        }

        ConstraintLayout(
            ConstraintSet {
                val front = createRefFor("front")
                val bottle = createRefFor("bottle")
                val fab = createRefFor("floating button")

                constrain(front) {
                    top.linkTo(parent.top)
                    bottom.linkTo(bottle.top)
                    height = Dimension.fillToConstraints
                }
                constrain(fab) {
                    bottom.linkTo(bottle.top, 8.dp)
                    end.linkTo(parent.end, 8.dp)
                    height = Dimension.wrapContent
                    width = Dimension.wrapContent
                }
                constrain(bottle) {
                    bottom.linkTo(parent.bottom)
                }
            },
            modifier = modifier
        ) {
            Front(
                modifier = Modifier
                    .layoutId("front")
                    .fillMaxWidth()
            )
            FloatingButton(
                modifier = Modifier
                    .layoutId("floating button")
            )
            Bottle(
                modifier = Modifier
                    .layoutId("bottle")
                    .fillMaxWidth()
                    .requiredHeight(animateBottleHeight),
                isHidden = !isAddBottle
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Front(modifier: Modifier = Modifier) {
        var bottles by remember {
            mutableStateOf<List<Bottle>>(listOf())
        }

        oltpViewmodel.bottlesLivedata.observe(this) {
            bottles = it
        }

        Column(
            modifier = modifier
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp, 16.dp, 16.dp, 0.dp)
                    .fillMaxSize(),
                content = {
                    items(bottles) { bottle ->
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(256.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = 6.dp,
                            backgroundColor = MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                            ) {
                                val picture: ImageBitmap by remember {
                                    mutableStateOf(
                                        BitmapFactory.decodeFile(bottle.picture).asImageBitmap()
                                    )
                                }
                                Image(
                                    bitmap = picture,
                                    contentDescription = "Bottle picture",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .wrapContentSize(),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier
                                        .padding(0.dp, 0.dp, 0.dp, 8.dp)
                                        .fillMaxHeight()
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bottle.description,
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.secondary,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                            .fillMaxWidth()
                                            .fillMaxHeight(.8f)
                                    )
                                    RatingBar(
                                        value = bottle.rating,
                                        onValueChange = {},
                                        onRatingChanged = {},
                                        hideInactiveStars = true
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                })
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun FloatingButton(modifier: Modifier = Modifier) {
        var addBottleState by remember { mutableStateOf(false) }
        val iconRotation by animateFloatAsState(targetValue = if (addBottleState) 45f else 0f)

        uiViewmodel.addBottleLivedata.observe(this) {
            addBottleState = it
            if (it) cameraViewmodel.startCamera(this)
            else {
                cameraViewmodel.stopCamera()
                hideKeyboard()
            }
        }
        FloatingActionButton(
            onClick = {
                uiViewmodel.setBottle(!addBottleState)
            },
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            elevation = FloatingActionButtonDefaults.elevation(5.dp, 8.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier
                    .size(36.dp)
                    .rotate(iconRotation),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Bottle(modifier: Modifier = Modifier, isHidden: Boolean) {
        var image: Bitmap? = null

        var description by remember {
            mutableStateOf("")
        }

        var rating: Float by remember { mutableStateOf(0f) }

        var asPicture by remember { mutableStateOf(false) }
        var asDescription by remember { mutableStateOf(false) }
        var asRating by remember { mutableStateOf(false) }
        val checkSize by animateFloatAsState(
            targetValue = if (asPicture && asDescription && asRating) 1f else 0f
        )

        if (isHidden) {
            rating = 0f
            description = ""
            asPicture = false
            asDescription = false
            asRating = false
            image = null
        }

        cameraViewmodel.pictureLivedata.observe(this) {
            image = it
            asPicture = true
        }

        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
            elevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            backgroundColor = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(.6f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            asDescription = true
                        },
                        placeholder = { BasicText(text = "Enter a description") },
                        label = { BasicText(text = "Description") },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(.8f)
                            .onFocusEvent {
                                if (it.isFocused) uiViewmodel.setKeyboard(true)
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { hideKeyboard() })
                    )
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RatingBar(
                            modifier = Modifier,
                            value = rating,
                            numStars = 5,
                            stepSize = StepSize.HALF,
                            onValueChange = { rating = it },
                            onRatingChanged = {
                                asRating = true
                                hideKeyboard()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Camera(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(256.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize(checkSize),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .size(48.dp, 48.dp),
                            onClick = {
                                image?.let { bitmap ->
                                    val timestamp = timestamp
                                    oltpViewmodel.saveBottle(
                                        Bottle(
                                            date = timestamp,
                                            picture = saveImage(bitmap, "${timestamp}_$rating.png"),
                                            description = description,
                                            rating = rating
                                        )
                                    )
                                    uiViewmodel.setBottle(false)
                                    hideKeyboard()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Ok",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Camera(modifier: Modifier = Modifier) {
        AndroidViewBinding(
            factory = CameraViewBinding::inflate,
            modifier = modifier
        ) {
            cameraViewmodel.addPreviewView(camera)
            buttonCamera.setOnClickListener {
                camera.bitmap?.let { image -> cameraViewmodel.setPicture(image) }
                cameraViewmodel.stopCamera()
            }
        }
    }
}