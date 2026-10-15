package com.nexo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.logo_nexo

@Composable
fun HomeScreen(
    onIrLogin: () -> Unit,
    onIrRegistro: () -> Unit
) {

    val fondoGradiente = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1B2A),
            Color(0xFF081427),
            Color(0xFF050C1A)
        )
    )

    val botonGradiente = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFA726),
            Color(0xFFFF5E7E)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoGradiente)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // LOGO
        Image(
            painter = painterResource(Res.drawable.logo_nexo),
            contentDescription = "Logo Nexo",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(20.dp))

        // TITULO
        Text(
            text = "NEXO",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        // SUBTITULO
        Text(
            text = "Tu mundo financiero",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(60.dp))

        // BOTON LOGIN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(40.dp)
                )
                .background(
                    brush = botonGradiente,
                    shape = RoundedCornerShape(40.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onIrLogin() },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Iniciar sesión",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // BOTON REGISTRO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(40.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onIrRegistro() },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Registrarse",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}