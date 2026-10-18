package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.fixtures.dominio.EsperaParaReintentar;
import ec.mdd.plataforma.errores.fixtures.dominio.HorasExcedidas;
import ec.mdd.plataforma.errores.fixtures.dominio.HorasExcedidasEnFeriado;
import ec.mdd.plataforma.errores.fixtures.dominio.ReglaSinCatalogo;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Aplicación mínima para probar la traducción de errores con un servidor real. */
@SpringBootApplication
class AplicacionDePrueba {

    /** Catálogo de prueba: registra el error de dominio de prueba (D-41). */
    @Component
    static class CatalogoDePrueba implements CatalogoDeErrores {

        @Override
        public List<EntradaCatalogo> entradas() {
            return List.of(
                    EntradaCatalogo.de(
                            HorasExcedidas.class,
                            "PRUEBA_HORAS_EXCEDIDAS",
                            409,
                            "Horas excedidas",
                            "La asignación supera las horas del docente. Quita horas y vuelve a intentarlo."),
                    EntradaCatalogo.conEncabezados(
                            EsperaParaReintentar.class,
                            new DefinicionDeCodigo(
                                    "PRUEBA_ESPERA", 429, "Espera", "Vuelve a intentarlo luego."),
                            error -> Map.of("Retry-After", String.valueOf(error.segundos()))));
        }
    }

    record Datos(
            @NotNull String nombre,
            @Size(min = 3, max = 10) String codigo,
            @Pattern(regexp = "[a-z]+") String clave) {}

    @RestController
    static class Prueba {

        @GetMapping("/api/prueba/horas")
        String horas() {
            throw new HorasExcedidas();
        }

        @GetMapping("/api/prueba/feriado")
        String feriado() {
            throw new HorasExcedidasEnFeriado();
        }

        @GetMapping("/api/prueba/espera")
        String espera() {
            throw new EsperaParaReintentar(42);
        }

        @GetMapping("/api/prueba/sin-catalogo")
        String sinCatalogo() {
            throw new ReglaSinCatalogo();
        }

        @GetMapping("/api/prueba/tecnico")
        String tecnico() {
            throw new IllegalStateException(
                    "fallo con eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.firma y clave=secreta");
        }

        @GetMapping("/api/prueba/regla-de-entrada")
        String reglaDeEntrada() {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    "permisos",
                                    "«Cargas de datos» no admite el permiso de firma.")));
        }

        @GetMapping("/api/prueba/prohibido")
        String prohibido() {
            throw new AccessDeniedException("sin permiso");
        }

        @GetMapping(value = "/api/prueba/json", produces = "application/json")
        String json() {
            return "{}";
        }

        @PostMapping(value = "/api/prueba/datos", consumes = "application/json")
        String datos(@Valid @RequestBody Datos datos) {
            return "{}";
        }
    }
}
