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
import pe.epsel.aguaya.auth.dto.LoginRequest;
import pe.epsel.aguaya.auth.dto.LoginVecinoRequest;
import pe.epsel.aguaya.auth.dto.RefreshRequest;
import pe.epsel.aguaya.auth.dto.RegistroVecinoRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpRequest;
import pe.epsel.aguaya.auth.dto.SolicitarOtpResponse;
import pe.epsel.aguaya.auth.service.AuthService;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Envía un código OTP por SMS (registro o login de vecino)")
    @PostMapping("/otp/solicitar")
    public SolicitarOtpResponse solicitarOtp(@Valid @RequestBody SolicitarOtpRequest request) {
        return authService.solicitarOtp(request);
    }

    @Operation(summary = "Registra un vecino verificando el OTP; queda pendiente de validación")
    @PostMapping("/registro/vecino")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrarVecino(@Valid @RequestBody RegistroVecinoRequest request) {
        return authService.registrarVecino(request);
    }

    @Operation(summary = "Login de vecino con teléfono + OTP")
    @PostMapping("/login/vecino")
    public AuthResponse loginVecino(@Valid @RequestBody LoginVecinoRequest request) {
        return authService.loginVecino(request);
    }

    @Operation(summary = "Login de administrador o conductor con DNI + contraseña")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
