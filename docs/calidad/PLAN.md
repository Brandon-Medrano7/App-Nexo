# Plan de calidad y APIs · NEXO
Fecha: 13 de octubre de 2026. Responsable: Equipo 1. Alcance: app Android y API local Ktor.

## Referencias y límites
ISO/IEC 25010:2023 aporta el modelo de calidad del producto; IEEE 730 organiza el aseguramiento de calidad. Estas referencias orientan el trabajo: no se afirma certificación ni cumplimiento integral. Confirmar con el docente la edición IEEE que evaluará (se trabajó en clase con referencia a 730).
- https://www.iso.org/standard/78176.html
- https://standards.ieee.org/ieee/730/10854/

## Métricas de aceptación propuestas
| Área | Métrica y cálculo | Meta | Cómo verificar |
|---|---|---|---|
| Adecuación funcional | Pruebas aprobadas / pruebas totales × 100 | 100% y ninguna prueba crítica omitida | XML JUnit; reporte_calidad.py |
| Fiabilidad | Registros duplicados por reintento de una solicitud | 0 | Prueba SQL con mismo UUID y conteo de filas |
| Seguridad | Solicitudes protegidas sin sesión rechazadas / intentos × 100 | 100% | Pruebas HTTP de autorización |
| Interacción | Usuarios que completan registro de gasto sin ayuda / participantes × 100 | ≥90% | 5–10 participantes; anotar tamaño y resultados, sin inferir población |
| Interacción | Tiempo mediano para registrar gasto | ≤30 s | Cronometrar desde Inicio hasta confirmación; misma tarea y dispositivo |
| Rendimiento | p95 de respuestas de API propia sin proveedor externo | ≤1 s | 100 solicitudes con sesión en entorno documentado |
| Integración externa | Fallos simulados que dan mensaje controlado / escenarios × 100 | 100% | Timeout, HTTP fallido, datos inválidos; nunca mostrar una tasa inventada |
| Accesibilidad | Flujos críticos utilizables con TalkBack y texto al 200% | Todos | Registro, gasto, meta, conversor; revisión manual pendiente |

Las metas son decisiones del equipo, no umbrales impuestos por ISO. RESULTADOS.md contiene lo observado; cualquier métrica sin evidencia permanece pendiente.

## Ciclo de aseguramiento
1. Definir requisito y criterio de aceptación antes del cambio.
2. Implementar, revisar validaciones y privacidad; mantener copia previa.
3. Ejecutar pruebas unitarias y HTTP; SQL solo en base aislada de pruebas.
4. Compilar APK, revisar flujo real y registrar defectos (pasos, esperado, observado, severidad).
5. Corregir y repetir el caso afectado; registrar fecha, responsable y evidencia.
6. No cerrar entrega con defectos que impidan acceso, pierdan datos o permitan otra cuenta.

## Inventario de APIs
| API | Función | Datos enviados | Estado |
|---|---|---|---|
| REST NEXO /movimientos, /presupuestos, /bienestar | Registros, presupuesto y constancia | Datos propios bajo sesión | Existente |
| Android RecognizerIntent | Dictado de borrador de gasto | Audio al servicio de reconocimiento según proveedor/dispositivo | Existente; confirmar antes de guardar |
| GET /herramientas/tipo-cambio?moneda=USD o EUR | Consulta autenticada para conversión a MXN | Token solo al servidor NEXO | Implementada en esta etapa |
| Frankfurter v2 /rate/{moneda}/mxn | Tipo de cambio diario | Solo par de monedas, sin token NEXO, importe ni perfil | Nueva integración externa |
| IA externa | Asistencia generativa | Por definir con consentimiento y minimización | Pendiente |

Frankfurter: https://frankfurter.dev/es/. API gratuita sin clave. No equivale al tipo cobrado por BBVA o Mercado Pago ni consulta sus cuentas. Respuesta fechada, validación de par/importe/antigüedad máxima 7 días; caché compartida 30 minutos; conexión 4 s y petición 6 s. Sin datos guardados se devuelve 503 y se permite reintentar. Si expira la caché y falla el proveedor no se presenta el valor viejo como actual. No altera la base de datos.

## Cómo repetir
Desde la raíz del proyecto, con JDK 21:
```
bash gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug -Pkotlin.compiler.execution.strategy=in-process
cd nexo-server
bash gradlew test --tests com.example.DivisasTest --tests com.example.UxServiciosTest --tests com.example.BienestarTest --tests com.example.PresupuestosTest -Pkotlin.compiler.execution.strategy=in-process
cd ..
python3 tools/reporte_calidad.py
```
Las pruebas SQL existentes necesitan su base aislada y configuración específica; no apuntarlas a los datos personales. El reporte distingue fechas y omisiones. Para probar el conversor: reiniciar EngineMain, ejecutar composeApp, entrar a Guía, escribir 12.50 USD y pulsar Consultar conversión. Probar también sin servidor y sin sesión.

## Siguiente etapa propuesta
Gastos fijos y compromisos futuros para que el presupuesto no confunda dinero todavía disponible con dinero ya comprometido. Después: exportación de movimientos, recuperación de cuenta y pruebas de UX con compañeros. Mantener la IA detrás del servidor y preparada para fallar sin impedir registrar gastos.
