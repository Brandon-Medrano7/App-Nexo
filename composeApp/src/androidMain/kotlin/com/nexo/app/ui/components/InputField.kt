package com.nexo.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp

@Composable fun InputField(value:String,placeholder:String,isPassword:Boolean=false,enabled:Boolean=true,onValueChange:(String)->Unit) {
    var visible by remember {mutableStateOf(false)}
    OutlinedTextField(value=value,onValueChange=onValueChange,label={Text(placeholder)},singleLine=true,enabled=enabled,
        modifier=Modifier.fillMaxWidth().heightIn(min=60.dp),shape=MaterialTheme.shapes.medium,
        visualTransformation=if(isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions=KeyboardOptions(keyboardType=when{isPassword->KeyboardType.Password;placeholder.contains("Correo")->KeyboardType.Email;placeholder=="Edad"->KeyboardType.Number;else->KeyboardType.Text},imeAction=ImeAction.Next),
        trailingIcon={if(isPassword) TextButton(onClick={visible=!visible}){Text(if(visible)"Ocultar" else "Ver")}})
}
