#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Las carreras, mallas y asignaturas de punta a punta sobre el perfil levantado (F03-H02;
# quickstart.md de specs/020-mallas-asignaturas, research §9).
# Uso, desde cualquier directorio: deploy/probar_carreras.sh
# El Vicerrector registra las 7 carreras, las 12 mallas y las 300 asignaturas de las cargas 02 y 04
# de los datos de prueba (sintéticos), con un prefijo al azar delante de cada código; las carreras
# cuelgan de la sede matriz que ya tenga la base, o de una temporal. Una asignatura incompleta, con
# una sola palabra clave o con un ciclo de más se rechaza con su campo; los códigos y las siglas
# repetidos, con su 409. La consulta trae los datos para la afinidad; una malla derogada no admite
# asignaturas. El Responsable de consolidación lee sin poder escribir y un docente no lee (D-70). El
# registro de Académico tiene las acciones. Al terminar, aunque un paso falle, borra en la base solo
# lo que creó y sus registros: la API no borra (principio VII).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'

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
        fallas=$((fallas + 1))
    fi
}

# Deja el estado en $estado y el cuerpo en $cuerpo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo '000')
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
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

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

es_invalido_en() {  # es_invalido_en <campos separados por coma, en orden>: 400 con esos detalles
    [[ $estado == 400 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == COMUN_SOLICITUD_INVALIDA ]] \
        && [[ $(jq -r '[.detalles[].campo] | sort | join(",")' <<<"$cuerpo") == "$1" ]]
}

# Las filas de una carga sin el encabezado ni el retorno de carro (los CSV vienen con CRLF).
filas() {
    tail -n +2 "$DATOS/$1" | tr -d '\r'
}

# Cada código de prueba lleva este prefijo: P y tres dígitos, 4 caracteres.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
cod() { printf '%s%s' "$prefijo" "$1"; }

# Los datos de prueba dejan sitio para el prefijo; si no, se avisa antes de empezar.
largos_bien=true
while IFS=, read -r codigo _ siglas _; do
    if (( ${#codigo} + 4 > 20 || ${#siglas} + 4 > 10 )); then largos_bien=false; fi
done < <(filas 02_carreras.csv)
while IFS=, read -r _ codigo _ _ _ _ _ _ _ _ _ _ malla _; do
    if (( ${#codigo} + 4 > 30 || ${#malla} + 4 > 20 )); then largos_bien=false; fi
done < <(filas 04_asignaturas_malla.csv)
if [[ $largos_bien != true ]]; then
    echo "FALLA  0. Algún código de los datos de prueba no cabe con el prefijo."
    exit 1
fi

limpiar() {
    consultar_base "delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.asignatura where codigo like '$prefijo%';
delete from academico.malla where codigo like '$prefijo%';
delete from academico.carrera where codigo like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado con el prefijo $prefijo"
}
trap limpiar EXIT

# La sede matriz que ya tenga la base; si no hay, una temporal, que registra el Administrador.
iniciar admin.demo
token_admin=$token
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token_admin"
sede=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ" and .activa) | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
sede_temporal=""
if [[ -z $sede ]]; then
    sede_temporal=$(cod MATRIZ)
    pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
        "$(jq -cn --arg c "$sede_temporal" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
    sede=$sede_temporal
fi

iniciar vicerrectorado.demo
token_vicerrector=$token

# 1. Las 7 carreras, las 12 mallas y las 300 asignaturas (SC-002)
carreras=0
while IFS=, read -r codigo nombre siglas _ nivel modalidad jornadas amplio especifico detallado ciclos _; do
    pedir POST "$GATEWAY/api/academico/carreras" "Bearer $token_vicerrector" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg n "$nombre" --arg s "$(cod "$siglas")" --arg sede "$sede" \
            --arg nivel "$nivel" --arg m "$modalidad" --arg j "$jornadas" --arg a "$amplio" \
            --arg e "$especifico" --arg d "$detallado" --arg ciclos "$ciclos" \
            '{codigo: $c, nombre: $n, siglas: $s, sede: $sede, nivelFormacion: $nivel, modalidad: $m,
              jornadas: ($j | split(";") | map(gsub("^ +| +$"; ""))), campoAmplio: $a,
              campoEspecifico: $e, numeroCiclos: ($ciclos | tonumber)}
            + (if $d != "" then {campoDetallado: $d} else {} end)')"
    if [[ $estado == 201 ]]; then
        carreras=$((carreras + 1))
    else
        echo "       $(cod "$codigo"): $estado $cuerpo"
    fi
done < <(filas 02_carreras.csv)
siete_carreras() { [[ $carreras == 7 ]]; }
verificar 1 "vicerrectorado.demo registra $carreras de 7 carreras (en la sede $sede)" siete_carreras

mallas=0
while IFS=' ' read -r carrera malla; do
    pedir POST "$GATEWAY/api/academico/carreras/$(cod "$carrera")/mallas" "Bearer $token_vicerrector" \
        "$(jq -cn --arg c "$(cod "$malla")" '{codigo: $c}')"
    if [[ $estado == 201 ]]; then
        mallas=$((mallas + 1))
    else
        echo "       $(cod "$malla"): $estado $cuerpo"
    fi
done < <(filas 04_asignaturas_malla.csv | awk -F, '{print $1, $13}' | sort -u)
doce_mallas() { [[ $mallas == 12 ]]; }
verificar 1 "vicerrectorado.demo registra $mallas de 12 mallas" doce_mallas

asignaturas=0
while IFS=, read -r carrera codigo nombre ciclo unidad clase practicas creditos tronco amplio especifico palabras malla _; do
    pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg car "$(cod "$carrera")" --arg m "$(cod "$malla")" \
            --arg n "$nombre" --arg ciclo "$ciclo" --arg u "$unidad" --arg h "$clase" --arg p "$practicas" \
            --arg cr "$creditos" --arg t "$tronco" --arg a "$amplio" --arg e "$especifico" --arg k "$palabras" \
            '{codigo: $c, carrera: $car, malla: $m, nombre: $n, ciclo: ($ciclo | tonumber),
              unidadOrganizacionCurricular: $u, horasSemanalesClase: ($h | tonumber),
              troncoComun: ($t == "S"), campoAmplio: $a, campoEspecifico: $e,
              palabrasClave: ($k | split(";") | map(gsub("^ +| +$"; "")) | map(select(. != "")))}
            + (if $p != "" then {horasPracticasSemanales: ($p | tonumber)} else {} end)
            + (if $cr != "" then {creditos: ($cr | tonumber)} else {} end)')"
    if [[ $estado == 201 ]]; then
        asignaturas=$((asignaturas + 1))
    else
        echo "       $(cod "$codigo"): $estado $cuerpo"
    fi
done < <(filas 04_asignaturas_malla.csv)
trescientas() { [[ $asignaturas == 300 ]]; }
verificar 1 "vicerrectorado.demo registra $asignaturas de 300 asignaturas" trescientas

pedir GET "$GATEWAY/api/academico/carreras" "Bearer $token_vicerrector"
resumen=$(jq -r --arg p "$prefijo" '[.[] | select(.codigo | startswith($p)) | .mallas[]
    | "\(.asignaturas):\(.horasSemanalesClase + 0)"] | group_by(.) | map("\(length)x\(.[0])") | join(" ")' \
    <<<"$cuerpo" 2>/dev/null || true)
cada_malla_100() { [[ $resumen == "12x25:100" ]]; }
verificar 1 "Las 12 mallas traen 25 asignaturas y 100 horas semanales de clase cada una (recibido: ${resumen:-?})" \
    cada_malla_100

# 2. Una asignatura incompleta, con una sola palabra clave o con un ciclo de más (escenario 1)
asignatura_de_prueba() {  # asignatura_de_prueba <código> <filtro jq que la cambia>
    jq -cn --arg c "$(cod "$1")" --arg car "$(cod 550613A01-P-0301)" --arg m "$(cod DS-2019)" \
        '{codigo: $c, carrera: $car, malla: $m, nombre: "ASIGNATURA DE PRUEBA", ciclo: 1,
          unidadOrganizacionCurricular: "BASICA", horasSemanalesClase: 3, troncoComun: false,
          campoAmplio: "06", campoEspecifico: "061", palabrasClave: ["software", "pruebas"]} | '"$2"
}
pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
    "$(asignatura_de_prueba X-1 'del(.carrera, .horasSemanalesClase)')"
verificar 2 "Una asignatura sin carrera ni horas: 400 en carrera y horasSemanalesClase (recibido: $estado)" \
    es_invalido_en carrera,horasSemanalesClase
pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
    "$(asignatura_de_prueba X-2 '.palabrasClave = ["software"]')"
verificar 2 "Una asignatura con una sola palabra clave: 400 en palabrasClave (recibido: $estado)" \
    es_invalido_en palabrasClave
pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
    "$(asignatura_de_prueba X-3 '.ciclo = 6')"
verificar 2 "Una asignatura en el ciclo 6 de una carrera de 5: 400 en ciclo (recibido: $estado)" \
    es_invalido_en ciclo

# 3. Códigos y siglas repetidos
carrera_ds=$(jq -cn --arg c "$(cod 550613A01-P-0301)" --arg s "$(cod DS)" --arg sede "$sede" \
    '{codigo: $c, nombre: "DESARROLLO DE SOFTWARE", siglas: $s, sede: $sede,
      nivelFormacion: "TECNOLOGICO_SUPERIOR", modalidad: "PRESENCIAL", jornadas: ["NOCTURNA"],
      campoAmplio: "06", campoEspecifico: "061", numeroCiclos: 5}')
pedir POST "$GATEWAY/api/academico/carreras" "Bearer $token_vicerrector" "$carrera_ds"
verificar 3 "La carrera DS otra vez: 409 ACADEMICO_CARRERA_REPETIDA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_CARRERA_REPETIDA
pedir POST "$GATEWAY/api/academico/carreras" "Bearer $token_vicerrector" \
    "$(jq -c --arg c "$(cod OTRA)" '.codigo = $c' <<<"$carrera_ds")"
verificar 3 "Otra carrera con las siglas de DS: 409 ACADEMICO_SIGLAS_REPETIDAS (recibido: $estado)" \
    es_codigo 409 ACADEMICO_SIGLAS_REPETIDAS
pedir POST "$GATEWAY/api/academico/carreras/$(cod 550613A01-P-0301)/mallas" "Bearer $token_vicerrector" \
    "$(jq -cn --arg c "$(cod DS-2019)" '{codigo: $c}')"
verificar 3 "La malla DS-2019 otra vez: 409 ACADEMICO_MALLA_REPETIDA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_MALLA_REPETIDA
pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
    "$(asignatura_de_prueba DS-B-I-MD-101 '.')"
verificar 3 "La asignatura DS-B-I-MD-101 otra vez: 409 ACADEMICO_ASIGNATURA_REPETIDA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_ASIGNATURA_REPETIDA

# 4. Los datos para la afinidad (escenario 2)
pedir GET "$GATEWAY/api/academico/asignaturas/$(cod DS-B-I-MD-101)" "Bearer $token_vicerrector"
afinidad=$(jq -r '"\(.campoAmplio.codigo)|\(.campoAmplio.nombre)|\(.campoEspecifico)|\(.palabrasClave | join(";"))|\(.troncoComun)"' \
    <<<"$cuerpo" 2>/dev/null || true)
con_afinidad() {
    [[ $estado == 200 && $afinidad == "05|Ciencias naturales, matemáticas y estadística|054|matemática;lógica;conjuntos|true" ]]
}
verificar 4 "DS-B-I-MD-101 trae su campo amplio con su nombre, su campo específico, sus palabras clave y el tronco común (recibido: $estado ${afinidad:-?})" \
    con_afinidad

# 5. Una malla derogada no admite asignaturas
pedir PUT "$GATEWAY/api/academico/mallas/$(cod DS-AC-2025)/estado" "Bearer $token_vicerrector" '{"estado":"DEROGADA"}'
derogada() { [[ $estado == 200 && $(jq -r '.estado' <<<"$cuerpo" 2>/dev/null) == DEROGADA ]]; }
verificar 5 "Derogar DS-AC-2025: 200 derogada (recibido: $estado)" derogada
pedir POST "$GATEWAY/api/academico/asignaturas" "Bearer $token_vicerrector" \
    "$(asignatura_de_prueba X-4 ".malla = \"$(cod DS-AC-2025)\"")"
verificar 5 "Una asignatura en DS-AC-2025 derogada: 400 en malla (recibido: $estado)" es_invalido_en malla

# 6. Quién la ve (D-70): consolidación lee sin escribir; un docente no lee
iniciar consolidacion.demo
pedir GET "$GATEWAY/api/academico/carreras/$(cod 550613A01-P-0301)/asignaturas" "Bearer $token"
cincuenta() { [[ $estado == 200 && $(jq -r 'length' <<<"$cuerpo" 2>/dev/null) == 50 ]]; }
verificar 6 "consolidacion.demo lista las asignaturas de DS: 200 con 50 (recibido: $estado)" cincuenta
pedir POST "$GATEWAY/api/academico/carreras" "Bearer $token" "$(jq -c --arg c "$(cod OTRA)" '.codigo = $c' <<<"$carrera_ds")"
verificar 6 "consolidacion.demo registra una carrera: 403 común (recibido: $estado)" es_403_comun
iniciar docente.demo
pedir GET "$GATEWAY/api/academico/carreras" "Bearer $token"
verificar 6 "docente.demo lista las carreras: 403 común (recibido: $estado)" es_403_comun

# 7. El registro de Académico (FR-010)
acciones=$(consultar_base "select accion || ':' || usuario || ':' || count(*) from academico.registro_cambio
    where detalle ->> 'codigo' like '$prefijo%' group by accion, usuario order by accion" 2>/dev/null \
    | paste -sd' ' || true)
esperadas="ASIGNATURA_REGISTRADA:vicerrectorado.demo:300 CARRERA_REGISTRADA:vicerrectorado.demo:7 MALLA_ESTADO_CAMBIADO:vicerrectorado.demo:1 MALLA_REGISTRADA:vicerrectorado.demo:12"
[[ -n $sede_temporal ]] && esperadas="$esperadas UNIDAD_REGISTRADA:admin.demo:1"
registro_completo() { [[ $acciones == "$esperadas" ]]; }
verificar 7 "El registro tiene 7 carreras, 12 mallas, 300 asignaturas y 1 malla derogada de vicerrectorado.demo (recibido: ${acciones:-?})" \
    registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Carreras, mallas y asignaturas de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Carreras, mallas y asignaturas de punta a punta: $fallas paso(s) fallan."
exit 1
