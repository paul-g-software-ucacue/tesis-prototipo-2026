"""API de GitHub simulada para probar los informes de despliegue (B00-H08, D-56).

La usa deploy/equipo/probar_equipo.sh en lugar de api.github.com. Acepta solo lo que usa
actualizar.sh: crear un deployment y ponerle estados. Exige el encabezado
«Authorization: Bearer <token>» con el token de prueba y guarda cada petición aceptada como una
línea JSON en el archivo de registro, para que la prueba compruebe qué llegó.

Uso: python3 -I deploy/equipo/github_simulado.py <puerto> <token> <registro.jsonl>
Solo la biblioteca estándar; escucha en 127.0.0.1.
"""

import json
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

RUTA_DEPLOYMENTS = re.compile(r"^/repos/[^/]+/[^/]+/deployments$")
RUTA_ESTADOS = re.compile(r"^/repos/[^/]+/[^/]+/deployments/(\d+)/statuses$")


class Simulador(BaseHTTPRequestHandler):
    token = ""
    registro = ""
    siguiente_id = 1

    def do_POST(self):  # noqa: N802 (nombre que exige http.server)
        if self.headers.get("Authorization") != f"Bearer {self.token}":
            self._responder(401, {"message": "Bad credentials"})
            return
        largo = int(self.headers.get("Content-Length") or 0)
        try:
            cuerpo = json.loads(self.rfile.read(largo) or b"{}")
        except json.JSONDecodeError:
            self._responder(400, {"message": "Problems parsing JSON"})
            return

        if RUTA_DEPLOYMENTS.match(self.path):
            identificador = Simulador.siguiente_id
            Simulador.siguiente_id += 1
            self._guardar({"tipo": "deployment", "id": identificador, **cuerpo})
            self._responder(201, {"id": identificador, **cuerpo})
        elif coincidencia := RUTA_ESTADOS.match(self.path):
            identificador = int(coincidencia.group(1))
            self._guardar({"tipo": "estado", "deployment": identificador, **cuerpo})
            self._responder(201, {"id": identificador, **cuerpo})
        else:
            self._responder(404, {"message": "Not Found"})

    def _guardar(self, entrada):
        with open(self.registro, "a", encoding="utf-8") as archivo:
            archivo.write(json.dumps(entrada, ensure_ascii=False) + "\n")

    def _responder(self, estado, cuerpo):
        datos = json.dumps(cuerpo).encode()
        self.send_response(estado)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        self.wfile.write(datos)

    def log_message(self, formato, *argumentos):
        pass  # sin ruido en la salida de la prueba


def main():
    if len(sys.argv) != 4:
        sys.exit("Uso: github_simulado.py <puerto> <token> <registro.jsonl>")
    puerto, Simulador.token, Simulador.registro = int(sys.argv[1]), sys.argv[2], sys.argv[3]
    ThreadingHTTPServer(("127.0.0.1", puerto), Simulador).serve_forever()


if __name__ == "__main__":
    main()
