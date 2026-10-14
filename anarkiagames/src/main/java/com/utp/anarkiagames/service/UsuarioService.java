package com.utp.anarkiagames.service;

import com.utp.anarkiagames.controller.PaginaResponse;
import com.utp.anarkiagames.controller.UsuarioResponse;
import com.utp.anarkiagames.exception.ConflictoException;
import com.utp.anarkiagames.exception.RecursoNoEncontradoException;
import com.utp.anarkiagames.model.Role;
import com.utp.anarkiagames.model.User;
import com.utp.anarkiagames.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UserRepository userRepository;

    /** Lista usuarios paginados. Con {@code buscar}, filtra por nombre o email (sin distinguir mayúsculas). */
    public PaginaResponse<UsuarioResponse> listar(final String buscar, final Pageable pageable){
        final Page<User> usuarios = (buscar == null || buscar.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        buscar.trim(), buscar.trim(), pageable);
        return PaginaResponse.of(usuarios.map(UsuarioResponse::of));
    }

    /**
     * Un admin no puede cambiar su propio rol. Así siempre queda al menos un ADMIN:
     * para quitarle el rol al último, otro admin tendría que hacerlo.
     */
    @Transactional
    public UsuarioResponse cambiarRol(final Long usuarioId, final Role role, final User actual){
        if (usuarioId.equals(actual.getId())) {
            throw new ConflictoException("No puedes cambiar tu propio rol");
        }
        final User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setRole(role);
        return UsuarioResponse.of(usuario);
    }
}
