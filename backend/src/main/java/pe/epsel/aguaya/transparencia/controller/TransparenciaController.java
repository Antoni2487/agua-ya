package pe.epsel.aguaya.transparencia.controller;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.transparencia.dto.CronogramaResponse;
import pe.epsel.aguaya.transparencia.dto.ResumenTransparenciaResponse;
import pe.epsel.aguaya.transparencia.dto.SectorTransparenciaResponse;
import pe.epsel.aguaya.transparencia.service.TransparenciaService;

@Tag(name = "Público - Transparencia")
@RestController
@RequestMapping("/api/public/transparencia")
@RequiredArgsConstructor
public class TransparenciaController {

    private final TransparenciaService transparenciaService;

    @Operation(summary = "Indicadores generales del periodo (por defecto, los últimos 30 días) (RF09)")
    @GetMapping("/resumen")
    public ResumenTransparenciaResponse resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return transparenciaService.resumen(desde, hasta);
    }

    @Operation(summary = "Sectores activos con su última y próxima atención y el cumplimiento del periodo")
    @GetMapping("/sectores")
    public List<SectorTransparenciaResponse> sectores(
            @RequestParam(required = false) String distrito,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return transparenciaService.sectores(distrito, desde, hasta);
    }

    @Operation(summary = "Rutas programadas por fecha y turno (por defecto, hoy y los 6 días siguientes)")
    @GetMapping("/cronograma")
    public List<CronogramaResponse> cronograma(
            @RequestParam(required = false) String distrito,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return transparenciaService.cronograma(distrito, desde, hasta);
    }
}
