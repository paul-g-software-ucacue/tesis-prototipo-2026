# Contrato: Normativa

## `GET /interno/normativa/periodos/{periodo}/reglas-del-panel`

Solo entre procesos. Un periodo sin versión responde 404, como las demás rutas internas.

```json
{
  "version": 2,
  "severidades": { "RF-24": "DURA", "RN-02": "DURA", "RN-05": "BLANDA", "RN-10": "BLANDA", "RN-12": "DURA", "RN-17": "DURA", "RN-18": "DURA" },
  "horasMaximasDocente": 40,
  "metaClase": 60,
  "metaOtrasDocencia": 40,
  "margenPuntos": 5,
  "gestoresMinimo": 8,
  "gestoresMaximo": 11,
  "actividadesObligatorias": ["140-b", "140-g"]
}
```

`margen_puntos` se edita con los demás parámetros de la versión, y la SPA le da un nombre: «Margen (puntos)».
