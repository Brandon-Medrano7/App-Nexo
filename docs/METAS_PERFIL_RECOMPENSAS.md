# Metas, perfil y constancia de NEXO

## Comportamiento
- Metas: crear, editar nombre/objetivo/fecha, aportar, retirar, archivar y restaurar. Se muestran progreso, monto restante, referencia semanal y el historial de aportaciones/retiros.
- El ahorro es manual: representa dinero ya separado. No transfiere fondos ni crea movimientos de gasto; un retiro tampoco crea un ingreso. El dinero de metas no debe sumarse otra vez al saldo de movimientos.
- Perfil: nombre, correo, edad, fecha de registro real, edición de nombre/edad y cierre de sesión. El correo se mantiene de solo lectura.
- Una revisión consciente del resumen al día da 10 puntos. No exige gastos ni aportaciones. Fechas calculadas en el servidor en America/Monterrey. La meta semanal es revisar tres días, no necesariamente consecutivos.
- La racha incluye hasta ayer mientras hoy no se haya revisado. Una interrupción no borra días acumulados ni premios.
- 3 días: estilo Océano y bufanda. 7 días: Bosque y lentes. 30 días: corona. Los accesorios se dibujan en un avatar vectorial provisional, preparado para sustituirse por la mascota definitiva.
- Los temas cambian fondo, superficies y acentos de las pantallas principales. El acceso y algunos elementos de marca conservan sus colores.
- Puntos sin valor monetario, sin compras, publicidad ni canje. Los premios no dependen del importe ahorrado y no se multiplican registrando gastos.

## Persistencia y contrato
GET /bienestar devuelve perfil, metas con historial y estado de hábitos. POST /bienestar recibe una AccionNexo. Ambas requieren el token Bearer emitido al iniciar sesión; el usuario se obtiene de la sesión, nunca del cuerpo enviado. POST /bienestar/salir revoca la sesión actual.

Acciones: crear, editar, aportar, retirar, archivar, restaurar, perfil, revisar, tema, accesorio. Toda acción tiene UUID de solicitud. Las ediciones de metas y perfil requieren su versión actual. Las aportaciones/retiros actualizan la versión de la meta.

El servidor serializa escrituras de una cuenta en SQL Server. Solicitud y cambios se confirman en la misma transacción. Repetir la misma solicitud devuelve el estado sin duplicar el cambio; reutilizar el UUID con otro contenido se rechaza. Una revisión tiene clave única por usuario y día. El servidor verifica desbloqueos.

Tablas existentes: Usuarios, MetasAhorro (incluidas FechaLimite/Activa), ProgresoMetas. Nuevas tablas aditivas: NexoEstilos, NexoRevisiones, NexoVersionesMeta, NexoSolicitudes, NexoRetirosMeta. Se crean al arrancar. Los retiros se guardan aparte para preservar la restricción positiva y el historial de ProgresoMetas. No se borran ni se transforman datos existentes.

Android conserva una solicitud pendiente por cuenta en DataStore antes de enviarla. Si la respuesta se pierde, ofrece reintentar la misma solicitud; no permite otra escritura hasta confirmar o recibir un rechazo explícito. El pendiente sobrevive al reinicio y al cierre de sesión. Se limpia el estado visible al cambiar de cuenta.

## Verificación realizada
17 pruebas unitarias Android y 9 pruebas de servidor, sin fallos ni omisiones. La integración SQL se ejecuta en una base temporal NexoSyncTest_* y la elimina al terminar. Cubre aislamiento de cuentas, concurrencia/reintento, importes, retiros, límites, archivo/restauración, edición con versión, premios bloqueados, revisión diaria y persistencia al crear otra instancia del repositorio.

La revisión visual de pantallas privadas requiere iniciar sesión en el emulador. No se usaron las credenciales del usuario para pruebas.

## Límites actuales
Las sesiones se conservan en memoria durante 12 horas y se invalidan al reiniciar el servidor; el progreso y los estilos permanecen en SQL Server. Esta entrega no migra la autenticación anterior a contraseñas con hash ni cambia el transporte HTTP de desarrollo. Las rutas antiguas de movimientos deben reforzarse antes de publicar; esta entrega no es una certificación de seguridad para producción.

No se implementó cambio de correo/contraseña, borrado de cuenta ni notificaciones automáticas: no se muestran botones que simulen esas funciones. La revisión diaria certifica una acción del usuario, no comprueba que su información bancaria sea completa. El avatar aún no tiene animaciones.
