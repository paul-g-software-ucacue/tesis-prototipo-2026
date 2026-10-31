#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La carrera del coordinador de punta a punta sobre el perfil levantado, con los dos procesos reales
# (F06-H05; quickstart.md de specs/030-carrera-coordinador).
# Uso, desde cualquier directorio: deploy/probar_carrera_coordinador.sh
# Con una base sin estructura orgánica, carreras ni los datos de prueba: carga las cargas 01 a 07 de
# prueba (ficticias, RNF-SEG-06) y abre 2026-I. Prueba a coordinacion.demo sin carrera; le indica una
# con admin.demo; compara su oferta con un conteo hecho aparte con los CSV; pide sugerencias de su
# carrera y de otra; revisa la afinidad; cambia la carrera y renueva la sesión; revisa que el alcance
# total siga viendo todo; y le quita la carrera y el rol. Si la base ya tiene alguno de esos datos,
# el script falla sin tocar nada. Al terminar, aunque un paso falle, borra lo que creó, le devuelve
# el rol a coordinacion.demo y deja sin carrera a los usuarios de la demo. Termina con 0 si todos los
# pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    politica_carga_horaria docentes)
readonly ARCHIVOS=(01_estructura_organica 02_carreras 03_ciclos_academicos 04_asignaturas_malla
    05_oferta_periodo 07_politica_carga_horaria 06_docentes)
readonly DOCENTE=0109990010
readonly COORDINADOR=coordinacion.demo
readonly SEGUNDOS_DE_LA_OFERTA=3

carpeta=$(mktemp -d)

# El valor de una variable: el de .env si existe, si no el de .env.ejemplo.
valor_de_entorno() {
    local archivo linea valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        linea=$(grep -E "^$1=" "$archivo" | tail -1 || true)
        [[ -n $linea ]] && valor=${linea#*=}
    done
    printf '%s' "$valor"
}

fallas=0
verificar() {
    local numero=$1 descripcion=$2
    shift 2
    if "$@"; then
        echo "OK     $numero. $descripcion"
    else
        echo "FALLA  $numero. $descripcion"
        echo "       última respuesta: $estado ${cuerpo:0:200}" | tr -d '\r'
        fallas=$((fallas + 1))
    fi
}

# Deja el estado en $estado, el cuerpo en $cuerpo y los segundos en $segundos.
estado=''
cuerpo=''
segundos=0
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo resultado
    archivo=$(mktemp -p "$carpeta")
    local argumentos=(-s -o "$archivo" -w '%{http_code} %{time_total}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    resultado=$(curl "${argumentos[@]}" || echo '000 0')
    estado=${resultado%% *}
    segundos=${resultado##* }
    cuerpo=$(cat "$archivo")
}

# subir <carga> <archivo>: valida el archivo y deja la respuesta.
subir() {
    local archivo
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X POST -H "Authorization: Bearer $token" \
        -F "archivo=@$2;filename=$(basename "$2")" \
        "$GATEWAY/api/cargas/plantillas/$1/archivo" || echo '000')
    cuerpo=$(cat "$archivo")
}

# confirmar <carga> <archivo> <huella>: lo confirma y deja la respuesta.
confirmar() {
    local archivo
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X POST -H "Authorization: Bearer $token" \
        -F "archivo=@$2;filename=$(basename "$2")" -F "huella=$3" \
        "$GATEWAY/api/cargas/plantillas/$1/archivo/confirmacion" || echo '000')
    cuerpo=$(cat "$archivo")
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# psql en la base del perfil; imprime el resultado sin alineación ni encabezados.
consultar_base() {
    docker compose exec -T postgres psql -q -At -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
        -v ON_ERROR_STOP=1 -c "$1"
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

# Cada código de prueba nuevo lleva este prefijo: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"

# --- Los archivos: los de prueba, con la política con el código nuevo ---
python3 -I - "$DATOS" "$carpeta" "$version" "${ARCHIVOS[@]}" <<'PY'
import csv, io, sys
datos, carpeta, version, *nombres = sys.argv[1:]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return filas[0], filas[1:]

for nombre in nombres:
    encabezados, filas = leer(nombre + '.csv')
    if nombre.startswith('07'):
        i = encabezados.index('codigo_version_politica')
        for fila in filas:
            fila[i] = version
    salida = io.StringIO()
    csv.writer(salida, lineterminator='\r\n').writerows([encabezados, *filas])
    with open(f'{carpeta}/{nombre}.csv', 'w', encoding='utf-8', newline='') as archivo:
        archivo.write(salida.getvalue())

def lista(nombre, archivo, campo):
    encabezados, filas = leer(archivo)
    i = encabezados.index(campo)
    valores = sorted({fila[i] for fila in filas if fila[i]})
    with open(f'{carpeta}/{nombre}.lista', 'w') as salida:
        salida.write(','.join("'" + v + "'" for v in valores))
    with open(f'{carpeta}/{nombre}.valores', 'w') as salida:
        salida.write('\n'.join(valores) + '\n')

lista('unidades', '01_estructura_organica.csv', 'codigo_unidad')
lista('carreras', '02_carreras.csv', 'codigo_carrera')
lista('periodos', '03_ciclos_academicos.csv', 'codigo_periodo')
lista('docentes', '06_docentes.csv', 'numero_identificacion')

encabezados, filas = leer('04_asignaturas_malla.csv')
asignaturas = {fila[encabezados.index('codigo_asignatura')]: dict(zip(encabezados, fila)) for fila in filas}
encabezados, filas = leer('05_oferta_periodo.csv')
oferta = [dict(zip(encabezados, fila)) for fila in filas]
por_carrera = {}
for una in oferta:
    por_carrera.setdefault(asignaturas[una['codigo_asignatura']]['codigo_carrera'], []).append(una)
codigos = sorted(por_carrera)
with open(f'{carpeta}/carreras.tsv', 'w') as salida:
    for codigo in codigos:
        unas = por_carrera[codigo]
        horas = sum(int(asignaturas[una['codigo_asignatura']]['horas_semanales_clase']) for una in unas)
        salida.write('\t'.join([codigo, str(len(unas)), str(horas), unas[0]['codigo_asignatura'],
                                unas[0]['paralelo']]) + '\n')
for codigo in codigos:
    with open(f'{carpeta}/asignaturas_{codigo}.valores', 'w') as salida:
        salida.write('\n'.join(sorted({una['codigo_asignatura'] for una in por_carrera[codigo]})) + '\n')
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
periodos=$(cat "$carpeta/periodos.lista")
docentes=$(cat "$carpeta/docentes.lista")
inicio=$(consultar_base "select now()")
{
    IFS=$'\t' read -r carrera_a paralelos_a horas_a asignatura_a paralelo_a
    IFS=$'\t' read -r carrera_b paralelos_b horas_b asignatura_b paralelo_b
} <"$carpeta/carreras.tsv"
total_de_carreras=$(wc -l <"$carpeta/carreras.tsv")
limpiar() {
    consultar_base "delete from docentes.cargo where docente in ($docentes);
delete from docentes.docente where identificacion in ($docentes);
delete from docentes.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from academico.asignatura_paralelo where periodo in ($periodos);
delete from academico.asignatura where carrera in ($carreras);
delete from academico.malla where carrera in ($carreras);
delete from academico.carrera where codigo in ($carreras);
delete from academico.periodo_academico where codigo in ($periodos);
delete from academico.unidad_organica where codigo in ($unidades);
delete from academico.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from normativa.asignacion_normativa where periodo in ($periodos);
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.version_normativa where codigo = '$version';
delete from normativa.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from cargas.historial_carga where momento >= '$inicio';
delete from roles.carrera_de_coordinador;
insert into roles.asignacion_rol (nombre_usuario, rol_id, asignado_en, asignado_por)
    select '$COORDINADOR', id, now(), 'sistema' from roles.rol where identificador = 'COORDINADOR_CARRERA'
    on conflict (nombre_usuario, rol_id) do nothing;
delete from roles.registro_cambio where momento >= '$inicio' and usuario = 'admin.demo';
delete from roles.periodo_conocido where codigo in ($periodos);
delete from roles.registro_cambio where detalle ->> 'periodo' in ($periodos);
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos));
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos);" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado por el script"
    rm -rf "$carpeta"
}

# --- 0. La base no tiene estructura, carreras ni los datos de prueba ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from roles.carrera_de_coordinador)")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras, datos de prueba o carreras de coordinadores: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
trap limpiar EXIT

iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
echo "OK     0. La base no tiene estructura ni carreras; la política entra como la versión $version"

# Las cargas, en orden.
cargar() {
    local i archivo huella
    for i in "${!CARGAS[@]}"; do
        archivo="$carpeta/${ARCHIVOS[$i]}.csv"
        subir "${CARGAS[$i]}" "$archivo"
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       ${CARGAS[$i]}: la vista previa falló" >&2; return 1; }
        confirmar "${CARGAS[$i]}" "$archivo" "$huella"
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       ${CARGAS[$i]}: la confirmación falló" >&2; return 1; }
    done
}

sugerencia() {  # sugerencia <asignatura> <paralelo>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/2026-I/oferta/$1/$2/sugerencia" "Bearer $token"
}

oferta() {  # con el token de $token
    pedir GET "$GATEWAY/api/academico/periodos/2026-I/oferta" "Bearer $token"
}

afinidad() {  # con el token de $token
    pedir GET "$GATEWAY/api/generacion/docentes/$DOCENTE/afinidad?periodo=2026-I" "Bearer $token"
}

abrir() {  # abrir <periodo> <número de versión>: con el token de $token
    pedir PUT "$GATEWAY/api/academico/periodos/$1/normativa" "Bearer $token" "{\"version\":${2:-0}}"
    [[ $estado == 200 ]]
}

indicar() {  # indicar <usuario> <carrera>: con el token de $token
    pedir PUT "$GATEWAY/api/roles/usuarios/$1/carrera" "Bearer $token" "{\"carrera\":\"$2\"}"
}

abierto_y_cargado() {
    cargar || return 1
    iniciar vicerrectorado.demo
    numero_a=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    abrir 2026-I "$numero_a"
}

sin_carrera() {
    iniciar "$COORDINADOR"
    oferta
    es_codigo 403 COMUN_CARRERA_SIN_INDICAR || return 1
    sugerencia "$asignatura_a" "$paralelo_a"
    es_codigo 403 COMUN_CARRERA_SIN_INDICAR || return 1
    afinidad
    es_codigo 403 COMUN_CARRERA_SIN_INDICAR
}

reglas_al_indicar() {
    iniciar "$COORDINADOR"
    indicar "$COORDINADOR" "$carrera_a"
    [[ $estado == 403 ]] || return 1
    iniciar admin.demo
    indicar docente.demo "$carrera_a"
    es_codigo 409 ROLES_CARRERA_SIN_ROL || return 1
    indicar "$COORDINADOR" 'carrera mal'
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || return 1
    pedir DELETE "$GATEWAY/api/roles/usuarios/$COORDINADOR/carrera" "Bearer $token"
    es_codigo 404 ROLES_USUARIO_SIN_CARRERA
}

indicada() {
    iniciar admin.demo
    indicar "$COORDINADOR" "$carrera_a"
    [[ $estado == 200 ]] || return 1
    pedir GET "$GATEWAY/api/roles/usuarios" "Bearer $token"
    jq -e --arg u "$COORDINADOR" --arg c "$carrera_a" \
        'any(.[]; .usuario == $u and .carrera == $c)' <<<"$cuerpo" >/dev/null
}

# La oferta de su carrera, igual a un conteo hecho aparte con los CSV (SC-001).
solo_su_oferta() {
    iniciar "$COORDINADOR"
    oferta
    [[ $estado == 200 ]] || return 1
    jq -e --arg c "$carrera_a" --argjson p "$paralelos_a" --argjson h "$horas_a" '
        (.asignaturasParalelo | length) == $p
        and all(.asignaturasParalelo[]; .carrera == $c)
        and (.carreras | length) == 1 and .carreras[0].carrera == $c
        and .horasACubrir == $h' <<<"$cuerpo" >/dev/null
}

sugerencias() {
    sugerencia "$asignatura_a" "$paralelo_a"
    [[ $estado == 200 ]] || return 1
    sugerencia "$asignatura_b" "$paralelo_b"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    [[ $cuerpo != *"$asignatura_b"* ]]
}

solo_su_afinidad() {
    afinidad
    [[ $estado == 200 ]] || return 1
    diff <(jq -r '.puntajes[].asignatura' <<<"$cuerpo" | sort) \
        "$carpeta/asignaturas_$carrera_a.valores" >/dev/null
}

# Con la sesión abierta, el cambio vale al renovarla (D-62).
cambiar_y_renovar() {
    local suyo=$token
    iniciar admin.demo
    indicar "$COORDINADOR" "$carrera_b"
    [[ $estado == 200 ]] || return 1
    token=$suyo
    oferta
    [[ $(jq -r '.carreras[0].carrera' <<<"$cuerpo") == "$carrera_a" ]] || return 1
    pedir POST "$GATEWAY/api/usuarios/sesion/renovacion" "Bearer $suyo"
    token=$(jq -r '.token // empty' <<<"$cuerpo")
    oferta
    jq -e --arg c "$carrera_b" --argjson p "$paralelos_b" --argjson h "$horas_b" '
        (.asignaturasParalelo | length) == $p and .horasACubrir == $h
        and all(.asignaturasParalelo[]; .carrera == $c)' <<<"$cuerpo" >/dev/null
}

alcance_total() {
    iniciar vicerrectorado.demo
    oferta
    [[ $estado == 200 && $(jq -r '.carreras | length' <<<"$cuerpo") == "$total_de_carreras" ]] || return 1
    sugerencia "$asignatura_a" "$paralelo_a"
    [[ $estado == 200 ]] || return 1
    sugerencia "$asignatura_b" "$paralelo_b"
    [[ $estado == 200 ]] || return 1
    iniciar datos.demo
    afinidad
    [[ $estado == 200 && $(jq -r '.puntajes | length' <<<"$cuerpo") -gt 0 ]]
}

quitar_la_carrera() {
    iniciar admin.demo
    pedir DELETE "$GATEWAY/api/roles/usuarios/$COORDINADOR/carrera" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR"
    oferta
    es_codigo 403 COMUN_CARRERA_SIN_INDICAR
}

quitar_el_rol() {
    iniciar admin.demo
    indicar "$COORDINADOR" "$carrera_a"
    [[ $estado == 200 ]] || return 1
    pedir PUT "$GATEWAY/api/roles/usuarios/$COORDINADOR" "Bearer $token" '{"roles":["DOCENTE"]}'
    [[ $estado == 200 ]] || return 1
    [[ $(jq -r '.carrera // "sin carrera"' <<<"$cuerpo") == 'sin carrera' ]] || return 1
    pedir PUT "$GATEWAY/api/roles/usuarios/$COORDINADOR" "Bearer $token" \
        '{"roles":["COORDINADOR_CARRERA"]}'
    [[ $estado == 200 && $(consultar_base "select count(*) from roles.carrera_de_coordinador") == 0 ]]
}

# La oferta filtrada en menos de $SEGUNDOS_DE_LA_OFERTA segundos (SC-005).
tarda_poco() {
    iniciar admin.demo
    indicar "$COORDINADOR" "$carrera_a"
    iniciar "$COORDINADOR"
    oferta
    echo "       la oferta de su carrera tardó $segundos s"
    [[ $estado == 200 ]] && awk -v s="$segundos" -v m="$SEGUNDOS_DE_LA_OFERTA" 'BEGIN { exit !(s < m) }'
}

numero_a=''

verificar 1 "Las cargas de prueba y 2026-I abierto con la versión $version" abierto_y_cargado
verificar 2 "Sin carrera: 403 COMUN_CARRERA_SIN_INDICAR en la oferta, la sugerencia y la afinidad" sin_carrera
verificar 3 "Indicarla: 403 sin permiso, 409 sin el rol, 400 mal escrita y 404 al quitar la que no hay" reglas_al_indicar
verificar 4 "admin.demo indica su carrera y se ve junto a sus roles" indicada
verificar 5 "Su oferta son los $paralelos_a paralelos y las $horas_a h de su carrera, iguales al conteo aparte" solo_su_oferta
verificar 6 "La sugerencia de su carrera, 200; la de otra, 403 sin datos" sugerencias
verificar 7 "La afinidad trae solo las asignaturas de su carrera" solo_su_afinidad
verificar 8 "Con otra carrera, la sesión abierta no cambia hasta renovarla" cambiar_y_renovar
verificar 9 "El alcance total ve las $total_de_carreras carreras, y Talento Humano la afinidad" alcance_total
verificar 10 "Quitar la carrera vuelve a pedirla" quitar_la_carrera
verificar 11 "Quitar el rol borra la carrera" quitar_el_rol
verificar 12 "La oferta de su carrera tarda menos de $SEGUNDOS_DE_LA_OFERTA s" tarda_poco

if ((fallas > 0)); then
    echo "La carrera del coordinador de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "La carrera del coordinador de punta a punta: todos los pasos pasan."
