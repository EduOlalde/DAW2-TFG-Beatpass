package com.daw2edudiego.beatpasstfg.service;

import com.daw2edudiego.beatpasstfg.dto.UsuarioCreacionDTO;
import com.daw2edudiego.beatpasstfg.dto.UsuarioDTO;
import com.daw2edudiego.beatpasstfg.exception.EmailExistenteException;
import com.daw2edudiego.beatpasstfg.exception.PasswordIncorrectoException;
import com.daw2edudiego.beatpasstfg.exception.UsuarioNotFoundException;
import com.daw2edudiego.beatpasstfg.model.RolUsuario;
import com.daw2edudiego.beatpasstfg.model.Usuario;
import com.daw2edudiego.beatpasstfg.repository.UsuarioRepository;
import com.daw2edudiego.beatpasstfg.repository.UsuarioRepositoryImpl;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.daw2edudiego.beatpasstfg.mapper.UsuarioMapper;

/**
 * Implementación de UsuarioService.
 */
public class UsuarioServiceImpl extends AbstractService implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl() {
        this.usuarioRepository = new UsuarioRepositoryImpl();
        this.usuarioMapper = UsuarioMapper.INSTANCE;
    }

    @Override
    public UsuarioDTO crearUsuario(UsuarioCreacionDTO ucDTO) {
        log.info("Service: Iniciando creación de usuario con email: {}", ucDTO != null ? ucDTO.getEmail() : "null");
        validarUsuarioCreacionDTO(ucDTO);

        return executeTransactional(em -> {
            if (usuarioRepository.findByEmail(em, ucDTO.getEmail()).isPresent()) {
                throw new EmailExistenteException("El email '" + ucDTO.getEmail() + "' ya está registrado.");
            }

            Usuario usuario = usuarioMapper.usuarioCreacionDTOToUsuario(ucDTO);
            usuario.setEstado(true); // Propiedades no mapeadas directamente, se setean aquí
            usuario.setCambioPasswordRequerido(true);
            usuario.setPassword(com.daw2edudiego.beatpasstfg.util.PasswordUtil.hashPassword(ucDTO.getPassword()));

            usuario = usuarioRepository.save(em, usuario);
            log.info("Usuario creado exitosamente con ID: {} y email: {}", usuario.getIdUsuario(), usuario.getEmail());
            return usuarioMapper.usuarioToUsuarioDTO(usuario);
        }, "crear usuario con email " + (ucDTO != null ? ucDTO.getEmail() : "null"));
    }

    @Override
    public Optional<UsuarioDTO> obtenerUsuarioPorId(Integer id) {
        log.debug("Service: Buscando usuario (DTO) por ID: {}", id);
        if (id == null) {
            return Optional.empty();
        }
        return executeRead(em -> {
            return usuarioRepository.findById(em, id).map(usuarioMapper::usuarioToUsuarioDTO);
        }, "obtener usuario (DTO) por ID " + id);
    }

    @Override
    public Optional<UsuarioDTO> obtenerUsuarioPorEmail(String email) {
        log.debug("Service: Buscando usuario (DTO) por email: {}", email);
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return executeRead(em -> {
            return usuarioRepository.findByEmail(em, email).map(usuarioMapper::usuarioToUsuarioDTO);
        }, "obtener usuario (DTO) por email " + email);
    }

    @Override
    public Optional<Usuario> obtenerEntidadUsuarioPorEmailParaAuth(String email) {
        log.debug("Service: Buscando entidad completa de usuario por email para auth: {}", email);
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return executeRead(em -> {
            return usuarioRepository.findByEmail(em, email);
        }, "obtener entidad usuario por email para auth " + email);
    }

    @Override
    public List<UsuarioDTO> obtenerUsuariosPorRol(RolUsuario rol) {
        log.debug("Service: Obteniendo usuarios con rol: {}", rol);
        if (rol == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo.");
        }
        return executeRead(em -> {
            List<Usuario> usuarios = usuarioRepository.findByRol(em, rol);
            log.info("Encontrados {} usuarios con rol {}", usuarios.size(), rol);
            return usuarioMapper.toUsuarioDTOList(usuarios);
        }, "obtener usuarios por rol " + rol);
    }

    @Override
    public UsuarioDTO actualizarEstadoUsuario(Integer id, boolean nuevoEstado) {
        log.info("Service: Iniciando actualización de estado a {} para usuario ID: {}", nuevoEstado, id);
        if (id == null) {
            throw new IllegalArgumentException("ID de usuario es requerido.");
        }

        return executeTransactional(em -> {
            Usuario usuario = usuarioRepository.findById(em, id)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado con ID: " + id));

            if (usuario.getEstado().equals(nuevoEstado)) {
                log.info("El estado del usuario ID {} ya es {}. No se requiere actualización.", id, nuevoEstado);
                return usuarioMapper.usuarioToUsuarioDTO(usuario);
            }

            usuario.setEstado(nuevoEstado);
            usuario = usuarioRepository.save(em, usuario);
            log.info("Estado de usuario ID: {} actualizado a {} correctamente.", id, nuevoEstado);
            return usuarioMapper.usuarioToUsuarioDTO(usuario);
        }, "actualizar estado usuario ID " + id);
    }

    @Override
    public void eliminarUsuario(Integer id) {
        log.info("Service: Iniciando eliminación de usuario ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("ID de usuario es requerido.");
        }

        executeTransactional(em -> {
            boolean eliminado = usuarioRepository.deleteById(em, id);
            if (!eliminado) {
                log.warn("deleteById devolvió false para usuario ID {} sin lanzar excepción.", id);
                throw new RuntimeException("No se pudo completar la eliminación del usuario ID: " + id);
            }
            log.info("Usuario ID: {} eliminado correctamente.", id);
            return null;
        }, "eliminar usuario ID " + id);
    }

    @Override
    public void cambiarPassword(Integer userId, String passwordAntigua, String passwordNueva) {
        log.info("Service: Iniciando cambio de contraseña para usuario ID: {}", userId);
        validarCambioPassword(userId, passwordAntigua, passwordNueva);

        executeTransactional(em -> {
            Usuario usuario = usuarioRepository.findById(em, userId)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado con ID: " + userId));

            if (!com.daw2edudiego.beatpasstfg.util.PasswordUtil.checkPassword(passwordAntigua, usuario.getPassword())) {
                throw new PasswordIncorrectoException("La contraseña actual introducida es incorrecta.");
            }

            usuario.setPassword(com.daw2edudiego.beatpasstfg.util.PasswordUtil.hashPassword(passwordNueva));
            usuario.setCambioPasswordRequerido(false); // Marcar como actualizado
            usuarioRepository.save(em, usuario);
            log.info("Contraseña cambiada exitosamente para usuario ID: {}", userId);
            return null;
        }, "cambiar contraseña para usuario ID " + userId);
    }

    @Override
    public void cambiarPasswordYMarcarActualizada(Integer userId, String passwordNueva) {
        log.info("Service: Iniciando cambio de contraseña obligatorio para usuario ID: {}", userId);
        validarPasswordNueva(userId, passwordNueva);

        executeTransactional(em -> {
            Usuario usuario = usuarioRepository.findById(em, userId)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado con ID: " + userId));

            usuario.setPassword(com.daw2edudiego.beatpasstfg.util.PasswordUtil.hashPassword(passwordNueva));
            usuario.setCambioPasswordRequerido(false);
            usuarioRepository.save(em, usuario);
            log.info("Contraseña cambiada (obligatorio) exitosamente para usuario ID: {}", userId);
            return null;
        }, "cambiar contraseña obligatoria para usuario ID " + userId);
    }

    @Override
    public UsuarioDTO actualizarNombreUsuario(Integer id, String nuevoNombre) {
        log.info("Service: Iniciando actualización de nombre a '{}' para usuario ID: {}", nuevoNombre, id);
        if (id == null || nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new IllegalArgumentException("ID y nuevo nombre (no vacío) son requeridos.");
        }

        return executeTransactional(em -> {
            Usuario usuario = usuarioRepository.findById(em, id)
                    .orElseThrow(() -> new UsuarioNotFoundException("Usuario no encontrado con ID: " + id));

            if (usuario.getNombre().equals(nuevoNombre.trim())) {
                log.info("El nombre del usuario ID {} ya es '{}'. No se requiere actualización.", id, nuevoNombre);
                return usuarioMapper.usuarioToUsuarioDTO(usuario);
            }

            UsuarioDTO tempDto = new UsuarioDTO();
            tempDto.setNombre(nuevoNombre.trim());
            usuarioMapper.updateUsuarioFromDto(tempDto, usuario);

            usuario = usuarioRepository.save(em, usuario);
            log.info("Nombre de usuario ID: {} actualizado a '{}' correctamente.", id, nuevoNombre);
            return usuarioMapper.usuarioToUsuarioDTO(usuario);
        }, "actualizar nombre usuario ID " + id);
    }

    // --- Métodos Privados de Ayuda ---
    /**
     * Valida los campos del DTO de creación.
     */
    private void validarUsuarioCreacionDTO(UsuarioCreacionDTO dto) {
        if (dto == null || dto.getEmail() == null || dto.getEmail().isBlank()
                || dto.getPassword() == null || dto.getPassword().isEmpty()
                || dto.getNombre() == null || dto.getNombre().isBlank() || dto.getRol() == null) {
            throw new IllegalArgumentException("Datos de usuario (nombre, email, password, rol) son requeridos.");
        }
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }

    /**
     * Valida los parámetros para el cambio de contraseña iniciado por el
     * usuario.
     */
    private void validarCambioPassword(Integer userId, String antigua, String nueva) {
        if (userId == null || antigua == null || antigua.isEmpty() || nueva == null || nueva.isEmpty()) {
            throw new IllegalArgumentException("ID usuario, contraseña antigua y nueva son obligatorios.");
        }
        if (nueva.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
        if (antigua.equals(nueva)) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la anterior.");
        }
    }

    /**
     * Valida los parámetros para el cambio de contraseña obligatorio.
     */
    private void validarPasswordNueva(Integer userId, String nueva) {
        if (userId == null || nueva == null || nueva.isEmpty()) {
            throw new IllegalArgumentException("ID usuario y nueva contraseña son obligatorios.");
        }
        if (nueva.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
    }
}
