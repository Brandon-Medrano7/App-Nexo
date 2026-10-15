# Presupuestos por periodo de pago

La tarjeta de Inicio consulta el servidor. Cada periodo contiene UUID, importe decimal en MXN, fecha de última paga, próxima paga y versión. La próxima fecha de pago es exclusiva: sus gastos pertenecen al siguiente periodo. El disponible se calcula con movimientos confirmados; los ingresos no aumentan el límite.

## Uso
1. Ejecutar `nexo-server` y la app actualizados.
2. Iniciar sesión otra vez para obtener una sesión del servidor.
3. En Inicio, configurar el presupuesto o revisar el presupuesto local pendiente de importar.
4. Guardar y esperar la confirmación. Actualizar vuelve a consultar el servidor.
5. Ver periodos muestra los presupuestos anteriores. Al finalizar uno, Nuevo periodo conserva el anterior.

La tabla `PresupuestosPago` se crea al iniciar el servidor mediante Exposed. No se eliminan tablas ni movimientos existentes. Índice por usuario/inicio y FK a Usuarios. Los intervalos superpuestos se rechazan dentro de una transacción que bloquea las escrituras de la cuenta. Los periodos finalizados son de consulta.

## API
- `POST /login`: conserva IdUsuario y agrega token; clientes anteriores pueden ignorarlo.
- `GET /presupuestos`: lista de periodos de la cuenta autenticada, inicio descendente.
- `PUT /presupuestos`: `{id, importe, inicio, pago, version}`. Importe es texto decimal; fechas ISO yyyy-MM-dd. Version 0 crea; la versión vigente edita. UUID se conserva al reintentar.
- Las dos rutas usan `Authorization: Bearer <token>`. No reciben IdUsuario como autoridad.
- 400: datos inválidos. 401: sesión vencida/ausente. 409: versión desactualizada, solapamiento o periodo cerrado. 503: error temporal de persistencia.

La sesión de desarrollo dura 12 horas y se invalida al reiniciar el servidor; los presupuestos permanecen en SQL Server. Esta autenticación se aplica a las nuevas rutas de presupuestos. Las rutas antiguas, el transporte HTTP local y el almacenamiento de contraseñas requieren una revisión separada antes de desplegar el proyecto fuera del entorno de desarrollo.

## Validación
- Android: 5 pruebas del cálculo del presupuesto, compilación y APK debug.
- Servidor: 4 pruebas de validación, intervalos, sesiones y rutas aisladas por usuario.
- Integración SQL Server: base temporal exclusiva, guardado, lectura, reintento idempotente, conflicto entre versiones, aislamiento, historial y escrituras simultáneas. Base temporal eliminada al finalizar.
- La prueba PresupuestosSqlTest requiere NEXO_SQL_TEST_URL apuntando a una base NexoSyncTest_* y NEXO_SQL_TEST_PASSWORD. No usar la base de la aplicación para estas pruebas.

## Siguiente etapa acordada
1. Registro de gastos por voz con revisión y confirmación del usuario.
2. Mejoras de formularios y navegación manteniendo la identidad visual.
3. Asistente de IA con alcance y proveedor definidos, sin enviar datos financieros a servicios externos automáticamente.
