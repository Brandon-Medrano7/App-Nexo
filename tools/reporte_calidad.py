"""Lee resultados JUnit reales. No calcula cobertura ni inventa datos de UX."""
from pathlib import Path
from datetime import datetime, timezone
import xml.etree.ElementTree as ET
import json
root=Path(__file__).resolve().parents[1]
rows=[]
for modulo,relative in [('Android','composeApp/build/test-results/testDebugUnitTest'),('Servidor','nexo-server/build/test-results/test')]:
    for path in sorted((root/relative).glob('TEST-*.xml')):
        suite=ET.parse(path).getroot()
        rows.append(dict(modulo=modulo,suite=suite.get('name'),fecha=suite.get('timestamp'),pruebas=int(suite.get('tests',0)),fallos=int(suite.get('failures',0)),errores=int(suite.get('errors',0)),omitidas=int(suite.get('skipped',0)),segundos=float(suite.get('time',0))))
if not rows: raise SystemExit('Sin resultados: ejecuta las pruebas antes de generar el reporte.')
total=sum(r['pruebas'] for r in rows);fails=sum(r['fallos']+r['errores'] for r in rows);skipped=sum(r['omitidas'] for r in rows)
passed=total-fails-skipped
out=root/'docs/calidad';out.mkdir(parents=True,exist_ok=True)
data=dict(generado=datetime.now(timezone.utc).isoformat(),pruebas=total,aprobadas=passed,fallidas=fails,omitidas=skipped,porcentaje_aprobacion=round(100*passed/total,2) if total else None,suites=rows)
(out/'resultados.json').write_text(json.dumps(data,ensure_ascii=False,indent=2))
lines=['# Evidencia automática de calidad NEXO','',f'Generado: {data["generado"]}',f'Pruebas: {total}; aprobadas: {passed}; fallidas: {fails}; omitidas: {skipped}.',f'Aprobación: {data["porcentaje_aprobacion"]}%. No equivale a cobertura de código.','', '| Módulo | Suite | Fecha de ejecución | Pruebas | Fallos/errores | Omitidas | Segundos |','|---|---|---|---:|---:|---:|---:|']
for r in rows: lines.append(f'| {r["modulo"]} | {r["suite"]} | {r["fecha"]} | {r["pruebas"]} | {r["fallos"]+r["errores"]} | {r["omitidas"]} | {r["segundos"]} |')
lines+=['','Resultados del último XML disponible de cada suite; las fechas indican si corresponden a distintas ejecuciones. Las duraciones son del entorno de pruebas, no latencia de producción. No se ejecutan pruebas mediante este script.','', 'Pendiente de medir: cobertura, p95 en dispositivo, finalización de tareas con usuarios, accesibilidad y estabilidad en sesiones reales.']
(out/'RESULTADOS.md').write_text('\n'.join(lines)+'\n')
print(json.dumps({k:v for k,v in data.items() if k!='suites'},ensure_ascii=False))
