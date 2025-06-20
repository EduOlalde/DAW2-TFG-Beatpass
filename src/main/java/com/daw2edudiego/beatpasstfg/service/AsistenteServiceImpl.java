package com.daw2edudiego.beatpasstfg.service;

import com.daw2edudiego.beatpasstfg.dto.AsistenteDTO;
import com.daw2edudiego.beatpasstfg.exception.AsistenteNotFoundException;
import com.daw2edudiego.beatpasstfg.exception.FestivalNotFoundException;
import com.daw2edudiego.beatpasstfg.exception.UsuarioNotFoundException;
import com.daw2edudiego.beatpasstfg.model.Asistente;
import com.daw2edudiego.beatpasstfg.model.EstadoEntrada;
import com.daw2edudiego.beatpasstfg.model.Festival;
import com.daw2edudiego.beatpasstfg.model.RolUsuario; // Import RolUsuario
import com.daw2edudiego.beatpasstfg.model.Usuario; // Import Usuario
import com.daw2edudiego.beatpasstfg.repository.AsistenteRepository;
import com.daw2edudiego.beatpasstfg.repository.AsistenteRepositoryImpl;
import com.daw2edudiego.beatpasstfg.repository.FestivalRepository;
import com.daw2edudiego.beatpasstfg.repository.FestivalRepositoryImpl;
import com.daw2edudiego.beatpasstfg.repository.UsuarioRepository; // Import UsuarioRepository
import com.daw2edudiego.beatpasstfg.repository.UsuarioRepositoryImpl; // Import UsuarioRepositoryImpl
import com.daw2edudiego.beatpasstfg.util.JPAUtil;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.daw2edudiego.beatpasstfg.mapper.AsistenteMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de AsistenteService.
 */
public class AsistenteServiceImpl extends AbstractService implements AsistenteService {

    private static final Logger log = LoggerFactory.getLogger(AsistenteServiceImpl.class);

    private final AsistenteRepository asistenteRepository;
    private final FestivalRepository festivalRepository;
    private final UsuarioRepository usuarioRepository; // Added for fetching User for role check
    private final AsistenteMapper asistenteMapper;

    public AsistenteServiceImpl() {
        this.asistenteRepository = new AsistenteRepositoryImpl();
        this.festivalRepository = new FestivalRepositoryImpl();
        this.usuarioRepository = new UsuarioRepositoryImpl(); // Initialize
        this.asistenteMapper = AsistenteMapper.INSTANCE;
    }

    @Override
    public List<AsistenteDTO> obtenerTodosLosAsistentes() {
        log.debug("Service: Obteniendo todos los asistentes.");
        return executeRead(em -> {
            List<Asistente> asistentes = asistenteRepository.findAll(em);
            List<AsistenteDTO> asistenteDTOs = asistenteMapper.toAsistenteDTOList(asistentes);
            fillFestivalPulseraInfoForAsistentes(em, asistenteDTOs);
            log.info("Encontrados {} asistentes en total.", asistentes.size());
            return asistenteDTOs;
        }, "obtenerTodosLosAsistentes");
    }

    @Override
    public Optional<AsistenteDTO> obtenerAsistentePorId(Integer idAsistente) {
        log.debug("Service: Obteniendo asistente por ID: {}", idAsistente);
        if (idAsistente == null) {
            return Optional.empty();
        }
        return executeRead(em -> {
            Optional<Asistente> asistenteOpt = asistenteRepository.findById(em, idAsistente);
            if (asistenteOpt.isPresent()) {
                AsistenteDTO dto = asistenteMapper.asistenteToAsistenteDTO(asistenteOpt.get());
                fillFestivalPulseraInfoForAsistentes(em, List.of(dto));
                return Optional.of(dto);
            }
            return Optional.empty();
        }, "obtenerAsistentePorId " + idAsistente);
    }

    @Override
    public Asistente obtenerOcrearAsistentePorEmail(String email, String nombre, String telefono) {
        log.info("Service: Obteniendo o creando asistente por email: {}", email);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio para obtener o crear un asistente.");
        }

        EntityManager emLookup = null;
        try {
            emLookup = JPAUtil.createEntityManager();
            Optional<Asistente> existenteOpt = asistenteRepository.findByEmail(emLookup, email);

            if (existenteOpt.isPresent()) {
                log.debug("Asistente encontrado con email {}", email);
                return existenteOpt.get();
            }
        } finally {
            if (emLookup != null && emLookup.isOpen()) {
                emLookup.close();
            }
        }

        log.info("Asistente con email {} no encontrado, creando uno nuevo.", email);
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio al crear un nuevo asistente.");
        }

        return executeTransactional(em -> {
            Asistente nuevoAsistente = new Asistente();
            nuevoAsistente.setEmail(email.trim().toLowerCase());
            nuevoAsistente.setNombre(nombre.trim());
            nuevoAsistente.setTelefono(telefono != null ? telefono.trim() : null);
            nuevoAsistente = asistenteRepository.save(em, nuevoAsistente);
            log.info("Nuevo asistente creado con ID {}", nuevoAsistente.getIdAsistente());
            return nuevoAsistente;
        }, "obtenerOcrearAsistentePorEmail " + email);
    }

    @Override
    public List<AsistenteDTO> buscarAsistentes(String searchTerm) {
        log.debug("Service: Buscando asistentes con término: '{}'", searchTerm);
        if (searchTerm == null || searchTerm.isBlank()) {
            return obtenerTodosLosAsistentes();
        }
        return executeRead(em -> {
            String jpql = "SELECT a FROM Asistente a WHERE lower(a.nombre) LIKE :term OR lower(a.email) LIKE :term ORDER BY a.nombre";
            TypedQuery<Asistente> query = em.createQuery(jpql, Asistente.class);
            query.setParameter("term", "%" + searchTerm.toLowerCase() + "%");
            List<Asistente> asistentes = query.getResultList();
            List<AsistenteDTO> asistenteDTOs = asistenteMapper.toAsistenteDTOList(asistentes);
            fillFestivalPulseraInfoForAsistentes(em, asistenteDTOs);
            log.info("Encontrados {} asistentes para el término '{}'", asistentes.size(), searchTerm);
            return asistenteDTOs;
        }, "buscarAsistentes " + searchTerm);
    }

    @Override
    public AsistenteDTO actualizarAsistente(Integer idAsistente, AsistenteDTO asistenteDTO) {
        log.info("Service: Actualizando asistente ID {}", idAsistente);
        if (idAsistente == null || asistenteDTO == null) {
            throw new IllegalArgumentException("ID y DTO del asistente son requeridos para actualizar.");
        }
        if (asistenteDTO.getNombre() == null || asistenteDTO.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del asistente no puede estar vacío.");
        }
        if (asistenteDTO.getEmail() != null) {
            log.warn("Se ignorará el intento de actualizar el email del asistente ID {}. El email no es actualizable.", idAsistente);
        }

        return executeTransactional(em -> {
            Asistente asistente = asistenteRepository.findById(em, idAsistente)
                    .orElseThrow(() -> new AsistenteNotFoundException("Asistente no encontrado con ID: " + idAsistente));

            asistenteMapper.updateAsistenteFromDto(asistenteDTO, asistente);

            asistente = asistenteRepository.save(em, asistente);
            AsistenteDTO updatedDto = asistenteMapper.asistenteToAsistenteDTO(asistente);
            fillFestivalPulseraInfoForAsistentes(em, List.of(updatedDto));
            log.info("Asistente ID {} actualizado correctamente.", idAsistente);
            return updatedDto;
        }, "actualizarAsistente " + idAsistente);
    }

    @Override
    public List<AsistenteDTO> obtenerAsistentesPorFestival(Integer idFestival, Integer idActor) { // Renamed idPromotor to idActor
        log.debug("Service: Obteniendo asistentes para festival ID {} por actor ID {}", idFestival, idActor);
        if (idFestival == null || idActor == null) {
            throw new IllegalArgumentException("ID de festival e ID de actor son requeridos.");
        }

        return executeRead(em -> {
            Festival festival = festivalRepository.findById(em, idFestival)
                    .orElseThrow(() -> new FestivalNotFoundException("Festival no encontrado con ID: " + idFestival));
            verificarPropiedadFestival(em, festival, idActor); // Pass em

            List<Asistente> asistentes = asistenteRepository.findAsistentesByFestivalId(em, idFestival);
            List<AsistenteDTO> asistenteDTOs = asistenteMapper.toAsistenteDTOList(asistentes);
            fillFestivalPulseraInfoForAsistentes(em, asistenteDTOs, idFestival);
            log.info("Encontrados {} asistentes únicos para el festival ID {}", asistentes.size(), idFestival);
            return asistenteDTOs;
        }, "obtenerAsistentesPorFestival " + idFestival);
    }

    @Override
    public List<AsistenteDTO> obtenerTodosLosAsistentesConFiltro(String searchTerm) {
        log.debug("Service: Obteniendo todos los asistentes con filtro: '{}'", searchTerm);
        return executeRead(em -> {
            List<Asistente> asistentes;
            if (searchTerm == null || searchTerm.isBlank()) {
                asistentes = asistenteRepository.findAll(em);
            } else {
                TypedQuery<Asistente> query = em.createQuery(
                        "SELECT a FROM Asistente a WHERE lower(a.nombre) LIKE :term OR lower(a.email) LIKE :term ORDER BY a.nombre", Asistente.class);
                query.setParameter("term", "%" + searchTerm.toLowerCase() + "%");
                asistentes = query.getResultList();
            }

            List<AsistenteDTO> asistenteDTOs = asistenteMapper.toAsistenteDTOList(asistentes);
            fillFestivalPulseraInfoForAsistentes(em, asistenteDTOs);
            log.info("Encontrados {} asistentes para el término '{}'", asistentes.size(), searchTerm);
            return asistenteDTOs;
        }, "obtenerTodosLosAsistentesConFiltro " + searchTerm);
    }

    // --- Métodos Privados de Ayuda ---
    // Método auxiliar para rellenar festivalPulseraInfo para una lista de AsistenteDTOs
    private void fillFestivalPulseraInfoForAsistentes(EntityManager em, List<AsistenteDTO> asistenteDTOs) {
        fillFestivalPulseraInfoForAsistentes(em, asistenteDTOs, null); // Llama a la versión sobrecargada sin filtro de festival
    }

    // Sobrecarga para permitir filtrar por un festival específico
    private void fillFestivalPulseraInfoForAsistentes(EntityManager em, List<AsistenteDTO> asistenteDTOs, Integer festivalIdFilter) {
        if (asistenteDTOs.isEmpty()) {
            return;
        }

        Set<Integer> asistenteIds = asistenteDTOs.stream()
                .map(AsistenteDTO::getIdAsistente)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (asistenteIds.isEmpty()) {
            return;
        }

        String jpql = "SELECT ea.asistente.idAsistente, te.festival.nombre, p.codigoUid "
                + "FROM Entrada ea JOIN ea.compraEntrada ce JOIN ce.tipoEntrada te LEFT JOIN ea.pulseraAsociada p "
                + "WHERE ea.asistente.idAsistente IN :asistenteIds AND ea.estado = :estadoActiva ";

        if (festivalIdFilter != null) {
            jpql += "AND te.festival.idFestival = :festivalIdFilter ";
        }

        jpql += "ORDER BY ea.asistente.idAsistente, te.festival.nombre";

        TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
        query.setParameter("asistenteIds", asistenteIds);
        query.setParameter("estadoActiva", EstadoEntrada.ACTIVA);
        if (festivalIdFilter != null) {
            query.setParameter("festivalIdFilter", festivalIdFilter);
        }

        List<Object[]> results = query.getResultList();

        // Mapear los resultados a un mapa temporal para un acceso rápido por asistenteId
        Map<Integer, Map<String, String>> tempPulseraInfo = new HashMap<>();
        for (Object[] row : results) {
            Integer asistenteId = (Integer) row[0];
            String festivalName = (String) row[1];
            String codigoUid = (String) row[2]; // Puede ser null si no hay pulsera asociada

            tempPulseraInfo
                    .computeIfAbsent(asistenteId, k -> new LinkedHashMap<>())
                    .put(festivalName, codigoUid);
        }

        for (AsistenteDTO dto : asistenteDTOs) {
            dto.setFestivalPulseraInfo(tempPulseraInfo.getOrDefault(dto.getIdAsistente(), new LinkedHashMap<>()));
        }
    }

    /**
     * Verifica que el usuario actor (ADMIN o PROMOTOR) tenga permiso sobre el
     * festival. Si es PROMOTOR, debe ser el propietario del festival. Si es
     * ADMIN, siempre tiene acceso.
     */
    private void verificarPropiedadFestival(EntityManager em, Festival festival, Integer idActor) { // Added EntityManager parameter
        if (festival == null) {
            throw new FestivalNotFoundException("El festival asociado no puede ser nulo.");
        }
        if (idActor == null) {
            throw new IllegalArgumentException("El ID del usuario actor no puede ser nulo.");
        }

        // Fetch the acting user (actor) to check their role.
        Usuario actor = em.find(Usuario.class, idActor); // Use em.find directly here for simplicity and direct management

        if (actor == null) {
            throw new UsuarioNotFoundException("Usuario actor no encontrado con ID: " + idActor);
        }

        boolean isActorAdmin = (actor.getRol() != null && actor.getRol() == RolUsuario.ADMIN); // Added null check for safety
        boolean isActorPromotorOwner = (festival.getPromotor() != null && festival.getPromotor().getIdUsuario().equals(idActor));

        if (!(isActorAdmin || isActorPromotorOwner)) {
            log.warn("Intento de acceso no autorizado por usuario ID {} (Rol: {}) al festival ID {} (Prop. por Promotor ID {})",
                    idActor, (actor.getRol() != null ? actor.getRol() : "NULL_ROL"), festival.getIdFestival(), festival.getPromotor() != null ? festival.getPromotor().getIdUsuario() : "N/A");
            throw new SecurityException("El usuario no tiene permiso para acceder a los recursos de este festival.");
        }
    }
}
