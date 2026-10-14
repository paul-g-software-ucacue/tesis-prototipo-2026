#!/usr/bin/env bash
# CA de prueba y certificados de los firmantes ficticios de la demo (B00-S01, data-model.md).
# Crea ca-prueba.crt (pública) y, por firmante, firmante-N.p12 (PKCS #12 de OpenSSL 3) y
# firmante-N-legacy.p12 (3DES y MAC SHA-1), por si FirmaEC no lee el formato moderno.
# La llave privada de la CA no se guarda: se usa en un directorio temporal y se borra.
# Solo datos ficticios (principio IX, S-10); estos archivos viven solo en ca-prueba/ (Gitleaks).
set -euo pipefail

CLAVE="cambiar-en-local-firma"   # contraseña ficticia de la demo
DESTINO="$(cd "$(dirname "$0")" && pwd)"
TEMPORAL="$(mktemp -d)"
trap 'rm -rf "$TEMPORAL"' EXIT

cd "$TEMPORAL"

cat > ca.cnf <<'CNF'
[req]
distinguished_name = dn
prompt = no
[dn]
CN = CA de Prueba MDD Demo
O = MDD Demo
C = EC
[ext_ca]
basicConstraints = critical,CA:TRUE
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
CNF

cat > firmante.cnf <<'CNF'
[ext_firmante]
basicConstraints = critical,CA:FALSE
keyUsage = critical,digitalSignature,nonRepudiation
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid
CNF

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out ca.key 2>/dev/null
openssl req -new -x509 -key ca.key -days 3650 -sha256 -config ca.cnf -extensions ext_ca \
    -out ca-prueba.crt

nombres=("Firmante Uno Prueba (F1)" "Firmante Dos Prueba (F2)" "Firmante Tres Prueba (F3)")
for i in 1 2 3; do
    nombre="${nombres[$((i - 1))]}"
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "f$i.key" 2>/dev/null
    openssl req -new -key "f$i.key" -subj "/CN=$nombre/O=MDD Demo/C=EC" -out "f$i.csr"
    openssl x509 -req -in "f$i.csr" -CA ca-prueba.crt -CAkey ca.key -CAcreateserial \
        -days 1095 -sha256 -extfile firmante.cnf -extensions ext_firmante -out "f$i.crt" 2>/dev/null
    openssl pkcs12 -export -inkey "f$i.key" -in "f$i.crt" -certfile ca-prueba.crt \
        -name "$nombre" -passout "pass:$CLAVE" -out "firmante-$i.p12"
    openssl pkcs12 -export -inkey "f$i.key" -in "f$i.crt" -certfile ca-prueba.crt \
        -certpbe PBE-SHA1-3DES -keypbe PBE-SHA1-3DES -macalg sha1 \
        -name "$nombre" -passout "pass:$CLAVE" -out "firmante-$i-legacy.p12"
done

cp ca-prueba.crt firmante-?.p12 firmante-?-legacy.p12 "$DESTINO/"
echo "CA de prueba y firmantes generados en $DESTINO (la llave de la CA se borró)."
