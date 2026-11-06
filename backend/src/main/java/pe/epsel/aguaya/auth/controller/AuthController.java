package pe.epsel.aguaya.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.epsel.aguaya.auth.dto.AuthResponse;
import pe.epsel.aguaya.auth.dto.LoginCodigoRequest;
import pe.epsel.aguaya.auth.dto.LoginRequest;
import pe.epsel.aguaya.auth.dto.RefreshRequest;
import pe.epsel.aguaya.auth.dto.RegistroVecinoRequest;
import pe.epsel.aguaya.auth.dto.RestablecerClaveRequest;
import pe.epsel.aguaya.auth.dto.SolicitarCodigoLoginRequest;
import pe.epsel.aguaya.auth.dto.SolicitarCodigoLoginResponse;
import pe.epsel.aguaya.auth.dto.SolicitarOtpRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpResponse;
import pe.epsel.aguaya.auth.service.AuthService;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Envía por SMS el código que confirma el teléfono del vecino antes de registrarse")
    @PostMapping("/otp/solicitar")
    public SolicitarOtpResponse solicitarOtp(@Valid @RequestBody SolicitarOtpRequest request) {
        return authService.solicitarOtp(request);
    }

    @Operation(summary = "Registra un vecino con DNI, código SMS, clave de 4 dígitos y ubicación; los nombres salen del DNI y queda con sesión iniciada")
    @PostMapping("/registro/vecino")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrarVecino(@Valid @RequestBody RegistroVecinoRequest request) {
        return authService.registrarVecino(request);
    }

    @Operation(summary = "Login con DNI + contraseña (administrador y conductor) o DNI + clave de 4 dígitos (vecino)")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Envía al celular registrado el código para que el vecino ingrese")
    @PostMapping("/login/codigo/solicitar")
    public SolicitarCodigoLoginResponse solicitarCodigoLogin(@Valid @RequestBody SolicitarCodigoLoginRequest request) {
        return authService.solicitarCodigoLogin(request);
    }

    @Operation(summary = "Login del vecino con DNI + código SMS")
    @PostMapping("/login/codigo")
    public AuthResponse loginConCodigo(@Valid @RequestBody LoginCodigoRequest request) {
        return authService.loginConCodigo(request);
    }

    @Operation(summary = "El vecino cambia su clave con el código SMS (si la olvidó) y queda con sesión iniciada")
    @PostMapping("/clave/restablecer")
    public AuthResponse restablecerClave(@Valid @RequestBody RestablecerClaveRequest request) {
        return authService.restablecerClave(request);
    }

    @Operation(summary = "Renueva el access token; el refresh token se rota")
    @PostMapping("/refresh")
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request.refreshToken());
    }

    @Operation(summary = "Cierra la sesión revocando el refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }
}
