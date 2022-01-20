package com.elfefe.goodwine.ui

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.foundation.*
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
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.elfefe.goodwine.mvvm.viewmodel.CameraViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.FirebaseViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.OltpViewmodel
import com.elfefe.goodwine.mvvm.viewmodel.UiViewmodel
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.ui.theme.GoodWineTheme
import com.elfefe.goodwine.utils.*
import com.gowtham.ratingbar.RatingBar
import com.gowtham.ratingbar.StepSize


class MainActivity : ComponentActivity() {
    private val uiViewmodel: UiViewmodel by viewModels()
    private val cameraViewmodel: CameraViewmodel by viewModels()
    private val oltpViewmodel: OltpViewmodel by viewModels()
    private val firebaseViewmodel: FirebaseViewmodel by viewModels()

    @OptIn(ExperimentalComposeUiApi::class)
    private lateinit var keyboardController: SoftwareKeyboardController
    private lateinit var localFocus: FocusManager

    private val registerPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            askPermission(permissions.keys.toTypedArray())
        }

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

        firebaseViewmodel.connect()
        firebaseViewmodel.connectionLivedata.observe(this) {
            println("CONNECTION $it ${firebaseViewmodel.user?.run { ".$displayName .$tenantId .$providerId .$uid" }}")
        }

        askPermission(arrayOf(CAMERA))

        uiViewmodel.setBottle(true)
    }

    private fun askPermission(permissions: Array<String>) {
        permissions
            .filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_DENIED
            }
            .run {
                if (isEmpty()) uiViewmodel.setPermitted(true)
                else registerPermission.launch(toTypedArray())
            }
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
        var isPermitted by remember {
            mutableStateOf(false)
        }

        val loadingAlpha by animateFloatAsState(targetValue = if (isPermitted) 0f else 1f)
        val mainAlpha by animateFloatAsState(targetValue = if (isPermitted && loadingAlpha == 0f) 1f else 0f)

        uiViewmodel.permittedLivedata.observe(this) {
            if (it) isPermitted = true
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
                if (isPermitted && loadingAlpha == 0f) {
                    Main(Modifier.alpha(mainAlpha))
                    if (prefs.getBoolean(FIRST_USE_TAG, false))
                        Tutorial()
                } else Loading(Modifier.alpha(loadingAlpha))
            }
        }
    }

    @Composable
    fun Tutorial(modifier: Modifier = Modifier) {

        var asChanged by remember {
            mutableStateOf(".")
        }

        var tutorialItem by remember {
            mutableStateOf(TutorialItem(Offset.Zero, IntSize.Zero))
        }
        val tutorialItemOffset by animateOffsetAsState(
            targetValue = tutorialItem.offset
        )
        val tutorialItemSize by animateSizeAsState(
            targetValue = Size(
                tutorialItem.size.width.toFloat(),
                tutorialItem.size.height.toFloat()
            )
        )

        uiViewmodel.descriptionItemLivedata.observe(this) {
            tutorialItem = it.copy(
                offset = it.offset.plus(Offset(-10f, 10f)),
                size = IntSize(
                    it.size.width + 20,
                    it.size.height
                )
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layoutId(asChanged)
                .clickable(true) { }
                .graphicsLayer(alpha = 0.5f),
            onDraw = {
                println("REDRAW")
                drawRect(
                    color = Color.Black,
                    size = size,
                    alpha = 1f,
                    blendMode = BlendMode.Xor
                )
                drawRoundRect(
                    color = Color.Black,
                    topLeft = tutorialItemOffset,
                    size = tutorialItemSize,
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = 1f,
                    blendMode = BlendMode.Xor
                )
            })
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

        firebaseViewmodel.syncBottles()

        ConstraintLayout(
            ConstraintSet {
                val front = createRefFor("front")
                val bottle = createRefFor("bottle")
                val options = createRefFor("options")
                val fab = createRefFor("floating button")

                constrain(front) {
                    top.linkTo(parent.top)
                    bottom.linkTo(bottle.top)
                    height = Dimension.fillToConstraints
                }
                constrain(options) {
                    bottom.linkTo(fab.bottom)
                    top.linkTo(fab.top)
                    start.linkTo(parent.start)
                    end.linkTo(fab.start)
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
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
            Options(
                modifier = Modifier
                    .layoutId("options")
                    .padding(8.dp)
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
                                            .fillMaxHeight(.8f),
                                        style = TextStyle(color = MaterialTheme.colorScheme.onPrimary)
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

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    fun Options(modifier: Modifier = Modifier) {
        val initCategory = painterResource(id = R.drawable.baseline_view_agenda_black_48)
        var category: Painter by remember {
            mutableStateOf(initCategory)
        }
        val initFilter = painterResource(id = R.drawable.baseline_menu_black_48)
        var filter: Painter by remember {
            mutableStateOf(initFilter)
        }
        val initRating = painterResource(id = R.drawable.baseline_star_rate_black_48)
        var rating: Painter by remember {
            mutableStateOf(initRating)
        }
        val initRatingArrow = painterResource(id = R.drawable.baseline_arrow_right_alt_black_48)
        var ratingArrow: Painter by remember {
            mutableStateOf(initRatingArrow)
        }

        var isAddBottle by remember { mutableStateOf(false) }
        val iconSize by animateDpAsState(targetValue = if (isAddBottle) 24.dp else 36.dp)

        var isRatingArrowVisible by remember { mutableStateOf(true) }
        val ratingArrowApha by animateFloatAsState(targetValue = if (isRatingArrowVisible) 1f else 0f)

        var isRatingArrowAsc by remember { mutableStateOf(false) }
        val ratingArrowRotation by animateFloatAsState(targetValue = if (isRatingArrowVisible) 90f else -90f)

        uiViewmodel.addBottleLivedata.observe(this) {
            isAddBottle = it
        }

        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
            elevation = 3.dp,
            border = BorderStroke(0.dp, Color.Transparent),
            backgroundColor = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(modifier = Modifier.size(iconSize), onClick = {

                    }) {
                        Icon(
                            painter = category,
                            contentDescription = "category",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(modifier = Modifier.size(iconSize), onClick = {

                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(.7f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(modifier = Modifier.size(iconSize), onClick = {

                    }) {
                        Icon(
                            painter = filter,
                            contentDescription = "filter",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(modifier = Modifier.size(iconSize), onClick = {
                        if (isRatingArrowVisible) {
                            if (!isRatingArrowAsc)
                                isRatingArrowVisible = false
                            isRatingArrowAsc = !isRatingArrowAsc
                        } else isRatingArrowVisible = true
                    }) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = rating,
                                contentDescription = "rating",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                painter = ratingArrow,
                                contentDescription = "rating arrow",
                                modifier = Modifier
                                    .rotate(ratingArrowRotation)
                                    .alpha(ratingArrowApha),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
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
                            }
                            .onGloballyPositioned {
                                uiViewmodel.setDescriptionItem(
                                    TutorialItem(
                                        it.positionInRoot(),
                                        it.size
                                    )
                                )
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { hideKeyboard() }),
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            backgroundColor = Color.Transparent
                        )
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