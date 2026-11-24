#!/usr/bin/env bash
# Sprint 8: análisis estático con SonarQube Community (bugs, vulnerabilidades y duplicación).
#
# Levanta SonarQube en Docker, analiza el proyecto con Maven y escribe el reporte en
# sonar-reporte.md. No necesita cuentas: usa un SonarQube local y desechable.
#
# Uso:   ./scripts/analisis-estatico.sh
# Requisitos: Docker, curl y python3 (o python). MVN permite cambiar el comando de Maven.
set -euo pipefail

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
SONAR_IMAGEN="${SONAR_IMAGEN:-sonarqube:community}"
PROYECTO="${PROYECTO:-registro-maritimo-digital}"
REPORTE="${REPORTE:-sonar-reporte.md}"
MVN="${MVN:-./mvnw}"
CLAVE_ADMIN="Analisis-S8-$(date +%s)"
PY="$(command -v python3 || command -v python)"

json() { "$PY" -c "import sys,json; d=json.load(sys.stdin); print($1)"; }

echo "== Arrancando SonarQube ($SONAR_IMAGEN)"
docker rm -f sonarqube-s8 >/dev/null 2>&1 || true
docker run -d --name sonarqube-s8 -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 "$SONAR_IMAGEN" >/dev/null

for i in $(seq 1 90); do
  estado="$(curl -s "$SONAR_URL/api/system/status" | json "d.get('status','')" 2>/dev/null || true)"
  [ "$estado" = "UP" ] && break
  sleep 5
done
[ "$estado" = "UP" ] || { echo "SonarQube no arrancó"; docker logs sonarqube-s8 | tail -50; exit 1; }

echo "== Creando un token de análisis"
curl -sf -u admin:admin -X POST \
  "$SONAR_URL/api/users/change_password?login=admin&previousPassword=admin&password=$CLAVE_ADMIN" >/dev/null
TOKEN="$(curl -sf -u "admin:$CLAVE_ADMIN" -X POST "$SONAR_URL/api/user_tokens/generate?name=analisis-s8" | json "d['token']")"

echo "== Analizando con Maven"
$MVN -B -q -DskipTests compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
  -Dsonar.host.url="$SONAR_URL" -Dsonar.token="$TOKEN" \
  -Dsonar.projectKey="$PROYECTO" -Dsonar.projectName="Registro Marítimo Digital"

echo "== Esperando que SonarQube procese el análisis"
for i in $(seq 1 60); do
  pendientes="$(curl -s -u "$TOKEN:" "$SONAR_URL/api/ce/component?component=$PROYECTO" | json "len(d.get('queue',[]))")"
  ultimo="$(curl -s -u "$TOKEN:" "$SONAR_URL/api/ce/component?component=$PROYECTO" | json "d.get('current',{}).get('status','')")"
  [ "$pendientes" = "0" ] && [ -n "$ultimo" ] && break
  sleep 3
done
[ "$ultimo" = "SUCCESS" ] || { echo "El análisis terminó con estado: $ultimo"; exit 1; }

echo "== Escribiendo $REPORTE"
METRICAS="bugs,vulnerabilities,security_hotspots,code_smells,duplicated_lines_density,duplicated_blocks,ncloc,sqale_rating,reliability_rating,security_rating"
curl -s -u "$TOKEN:" "$SONAR_URL/api/measures/component?component=$PROYECTO&metricKeys=$METRICAS" > sonar-medidas.json
curl -s -u "$TOKEN:" "$SONAR_URL/api/issues/search?componentKeys=$PROYECTO&types=BUG,VULNERABILITY&ps=100" > sonar-bugs.json
curl -s -u "$TOKEN:" "$SONAR_URL/api/measures/component_tree?component=$PROYECTO&metricKeys=duplicated_lines,duplicated_lines_density&qualifiers=FIL&s=metric&metricSort=duplicated_lines&asc=false&ps=10" > sonar-duplicacion.json

"$PY" - "$REPORTE" <<'PYEOF'
import json, sys
reporte = sys.argv[1]
medidas = {m["metric"]: m.get("value", "") for m in json.load(open("sonar-medidas.json"))["component"]["measures"]}
bugs = json.load(open("sonar-bugs.json")).get("issues", [])
dup = json.load(open("sonar-duplicacion.json")).get("components", [])
nota = {"1.0": "A", "2.0": "B", "3.0": "C", "4.0": "D", "5.0": "E"}
filas = [
    ("Bugs", medidas.get("bugs")),
    ("Vulnerabilidades", medidas.get("vulnerabilities")),
    ("Puntos de seguridad a revisar", medidas.get("security_hotspots")),
    ("Code smells", medidas.get("code_smells")),
    ("Duplicación", f'{medidas.get("duplicated_lines_density")} %'),
    ("Bloques duplicados", medidas.get("duplicated_blocks")),
    ("Líneas de código", medidas.get("ncloc")),
    ("Calificación de confiabilidad", nota.get(medidas.get("reliability_rating"), "")),
    ("Calificación de seguridad", nota.get(medidas.get("security_rating"), "")),
    ("Calificación de mantenibilidad", nota.get(medidas.get("sqale_rating"), "")),
]
lineas = ["# Análisis estático con SonarQube (Sprint 8)", "", "| Métrica | Valor |", "| --- | --- |"]
lineas += [f"| {k} | {v} |" for k, v in filas]
lineas += ["", "## Bugs y vulnerabilidades", ""]
if bugs:
    lineas += ["| Tipo | Severidad | Archivo | Línea | Mensaje |", "| --- | --- | --- | --- | --- |"]
    for b in bugs:
        archivo = b.get("component", "").split(":", 1)[-1]
        lineas.append(f'| {b.get("type")} | {b.get("severity")} | {archivo} | {b.get("line", "")} | {b.get("message", "").replace("|", "/")} |')
else:
    lineas.append("Sin bugs ni vulnerabilidades.")
lineas += ["", "## Archivos con más líneas duplicadas", ""]
con_dup = [c for c in dup if any(m["metric"] == "duplicated_lines" and float(m.get("value", 0)) > 0 for m in c.get("measures", []))]
if con_dup:
    lineas += ["| Archivo | Líneas duplicadas | % |", "| --- | --- | --- |"]
    for c in con_dup:
        m = {x["metric"]: x.get("value") for x in c["measures"]}
        lineas.append(f'| {c.get("path")} | {m.get("duplicated_lines")} | {m.get("duplicated_lines_density")} % |')
else:
    lineas.append("Sin duplicación.")
open(reporte, "w", encoding="utf-8").write("\n".join(lineas) + "\n")
print("\n".join(lineas))
PYEOF

rm -f sonar-medidas.json sonar-bugs.json sonar-duplicacion.json
docker rm -f sonarqube-s8 >/dev/null 2>&1 || true
