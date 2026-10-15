# Pagos próximos y presupuesto · 14 de octubre de 2026

## Qué cambió
Inicio permite planear un pago con concepto, importe y fecha; editarlo, cancelarlo y restaurarlo. El historial conserva pagos cancelados y confirmados. La opción «Repetir próximo mes» abre un nuevo borrador revisable: no genera cargos ni una suscripción automática.

La tarjeta de presupuesto muestra restante antes de pagos, reservado, libre para gastar y referencia diaria. La guía local usa la misma regla de reserva.

Libre = presupuesto del periodo − gastos registrados del periodo − pagos pendientes con vencimiento anterior a la próxima paga.

Incluye vencidos de periodos anteriores que sigan pendientes. Excluye cancelados, pagados y vencimientos del propio día de paga, que pertenecen al siguiente periodo. Si el resultado es negativo, muestra lo que falta y una referencia diaria de cero. Si no se han actualizado movimientos y pagos, no afirma cuánto queda libre.

## Confirmar un pago sin duplicar gastos
1. Registrar el gasto real desde Agregar movimiento.
2. Volver a Inicio y elegir «Ya lo pagué» en el pago previsto.
3. Seleccionar el gasto correspondiente por el importe exacto.
4. El pago deja de reservarse; el gasto ya registrado sigue descontándose una sola vez.

El servidor comprueba dueño, signo, fecha e importe del gasto. No permite usar el mismo gasto para dos pagos. Si el importe real fue distinto, editar antes el importe previsto. Desvincular no elimina el gasto: devuelve el pago al estado pendiente. La pantalla lo explica antes de la acción.

## Persistencia y API
Nueva tabla NexoCompromisos: IdCompromiso (UUID), IdUsuario (FK), Nombre, Importe DECIMAL(11,2), Vencimiento, Estado, IdMovimiento opcional (FK), Version. Se crea de forma aditiva al iniciar el servidor; no reemplaza ni vacía tablas existentes.

- GET /compromisos: lista solo los pagos de la sesión.
- PUT /compromisos: crea o actualiza con validación y versión.
- 400: datos inválidos o gasto que no corresponde.
- 401: sesión ausente o vencida.
- 409: edición antigua, identificador ocupado o gasto ya vinculado.
- 503: no se pudo consultar o confirmar; actualizar antes de repetir.

Las escrituras se serializan por cuenta en una transacción SQL. Repetir el mismo contenido y versión confirmada no genera otra fila. Las ediciones antiguas con contenido diferente se rechazan. Ante una respuesta incierta, la app exige actualizar antes de volver a guardar. No se aceptan escrituras sin conexión y los borradores de formulario no sobreviven a cerrar el proceso.

## Validación realizada
Pruebas unitarias Android: vencimientos, día de paga, gasto confirmado sin doble descuento, cancelación, déficit y cero días.
Pruebas HTTP/servidor: validación de datos, autenticación, usuario de sesión, conflictos y fallos controlados; guía con reserva de pagos.
Prueba SQL real en una base NexoSyncTest temporal: creación de tabla repetible, reintentos simultáneos, persistencia, separación de cuentas, versión antigua, vínculo válido/ajeno/importe incorrecto, gasto no reutilizable, cancelación, restauración y desvinculación. La base temporal se eliminó tras la prueba.

Resultados y fechas: calidad/RESULTADOS.md. Aprobar pruebas no equivale a cobertura total ni revisión completa con usuarios.

## Revisión manual
- Crear Internet por $400 antes de la próxima paga: baja el dinero libre $400.
- Registrar gasto de $400 y vincularlo: no descontarlo dos veces después de confirmar.
- Cancelar/restaurar: cambia la reserva sin alterar el historial de movimientos.
- Cerrar servidor y actualizar: mostrar error y ocultar disponible no verificable.
- Probar texto grande y un teléfono real; estas revisiones permanecen pendientes hasta completarlas.

## Límites de esta entrega
No efectúa pagos bancarios, no envía notificaciones, no cobra suscripciones automáticamente y no genera recurrencias sin revisión. Continúan pendientes alojamiento seguro, recuperación de cuenta y eliminación de datos antes de lanzamiento público.
