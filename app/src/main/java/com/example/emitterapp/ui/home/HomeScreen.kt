package com.example.emitterapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.emitterapp.R
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.theme.backgroundPurple
import com.example.emitterapp.ui.theme.backgroundSplash
import com.example.emitterapp.ui.theme.syncopate
import com.example.emitterapp.ui.theme.titanOneRegular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController? = null) {

    val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle

//    LaunchedEffect(savedStateHandle) {
//        savedStateHandle?.getLiveData<ConnectionResult>("bt_connection_result")
//            ?.observeForever { result ->
//                savedStateHandle.remove<ConnectionResult>("bt_connection_result")
//            }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "RC-Emitter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundPurple,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Handle help action */ }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(start = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Rocket,
                        contentDescription = "Rocket",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "Launch RC control",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.joystick),
                        contentDescription = "Launch RC control",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "Select UI",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.personalize),
                        contentDescription = "Personalize",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "Personalize",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable{navController?.navigate(Screen.Bluetooth.route)}
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "Bluetooth",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tutorial),
                        contentDescription = "Tutorial",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "tutorial",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help),
                        contentDescription = "Rocket",
                        tint = Color.Black,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(45.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        text = "Help",
                        fontSize = 20.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Left,
                    )
                }

            }
            Image(
                painter = painterResource(R.drawable.car_bouncing01),
                contentDescription = "car",
                modifier = Modifier
                    .padding(bottom = 120.dp, end = 20.dp)
                    .size(250.dp)
                    .align(Alignment.BottomEnd),
            )
            Button(
                onClick = {
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp)
            ) {
                Text(
                    text = "Start",
                    fontSize = 18.sp,
                    fontFamily = syncopate,
                    color = backgroundSplash,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upgrade to pro",
                    fontSize = 18.sp,
                    fontFamily = titanOneRegular,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "v1.0.2 - About this App",
                    fontSize = 18.sp,
                    fontFamily = titanOneRegular,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
