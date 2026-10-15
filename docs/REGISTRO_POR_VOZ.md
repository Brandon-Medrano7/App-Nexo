# Registro de gastos por voz

En Agregar movimiento, Dictar gasto abre el servicio de voz de Android en español de México. La transcripción se interpreta localmente para proponer importe, categoría y fecha. NEXO no conserva audio; el servicio de Android puede necesitar internet para reconocerlo.

## Recorrido
1. Iniciar sesión y pulsar + en Inicio.
2. Elegir Dictar gasto y decir: «Gasté 85 pesos en comida».
3. Revisar y, si hace falta, corregir la transcripción.
4. Usar borrador copia los datos al formulario. No escribe en SQL Server.
5. Revisar importe, tipo, categoría, fecha y concepto. Pulsar Guardar envía el movimiento al servidor existente.
6. Cancelar el dictado o su revisión conserva el formulario anterior.

Escribir frase permite usar el mismo intérprete sin micrófono y facilita la demostración en el emulador. El servicio de voz no siempre está instalado; la app muestra una alternativa si falta o no puede abrirse.

## Alcance del intérprete
- Gastos en MXN con una sola cantidad positiva, hasta dos decimales.
- Cifras sin separadores de miles; palabras para cantidades enteras entre cero y 999 (cero se rechaza como importe).
- Ejemplos: «gasté 85.50 pesos en café», «ayer pagué ochenta y cinco pesos en taxi».
- Si encuentra varios números, no elige uno por su cuenta. Los miles en palabras o formatos ambiguos se corrigen escribiendo cifras.
- Comida, transporte y ocio son sugerencias por palabras clave. Si no hay coincidencia única se exige escoger categoría en el formulario.
- Hoy por defecto, ayer cuando se menciona. Otras fechas detectadas se dejan pendientes para elegir en el calendario.
- Ingresos, devoluciones, cantidades negativas o divisas distintas requieren captura manual.
- Usar borrador sustituye el formulario y lo indica antes de confirmar.

## Validación de guardado
El formulario comprueba sesión, importe positivo con dos decimales como máximo, categoría, fecha válida no futura y concepto. Los controles se deshabilitan durante el envío. Si no se confirma el resultado, se recomienda consultar el historial antes de repetir para evitar duplicados. La API de movimientos existente aún no tiene una clave idempotente.

## Pruebas
Ocho pruebas unitarias del intérprete: gasto simple, palabras/ayer, decimales, ambigüedad, categorías, fechas no admitidas, entradas vacías/largas y centenas. Se ejecutan junto con las cinco pruebas del presupuesto. APK compilado. La prueba de micrófono real debe hacerse en el dispositivo, con voz del usuario; disponer del servicio no garantiza permisos, red ni calidad de reconocimiento.

## Tecnología
API oficial de Android RecognizerIntent, ACTION_RECOGNIZE_SPEECH, contrato StartActivityForResult. No se añadió un proveedor de IA generativa ni claves de pago.
Referencia: https://developer.android.com/reference/android/speech/RecognizerIntent
