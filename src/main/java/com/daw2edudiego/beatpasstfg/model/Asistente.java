package com.daw2edudiego.beatpasstfg.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad JPA que representa un asistente o cliente que compra entradas. Mapea
 * la tabla 'asistentes'.
 */
@Entity
@Table(name = "asistentes", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email", name = "uq_asistente_email")
})
public class Asistente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asistente")
    private Integer idAsistente;

    @NotBlank(message = "El nombre del asistente no puede estar vacío.")
    @Size(max = 100, message = "El nombre del asistente no puede exceder los 100 caracteres.")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El email del asistente no puede estar vacío.")
    @Email(message = "El formato del email no es válido.")
    @Size(max = 100, message = "El email del asistente no puede exceder los 100 caracteres.")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Size(max = 20, message = "El teléfono del asistente no puede exceder los 20 caracteres.")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "fecha_creacion", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime fechaModificacion;

    /**
     * Conjunto de compras realizadas por este asistente. Relación uno a muchos
     * (inversa de Compra.asistente). Cascade ALL, Fetch LAZY, orphanRemoval
     * true.
     */
    @OneToMany(mappedBy = "asistente", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Compra> compras = new HashSet<>();

    /**
     * Conjunto de entradas asignadas (nominadas) a este asistente. Relación uno
     * a muchos (inversa de EntradaAsignada.asistente). Fetch LAZY. No se usa
     * Cascade para evitar borrar entradas al borrar asistente.
     */
    @OneToMany(mappedBy = "asistente", fetch = FetchType.LAZY)
    private Set<EntradaAsignada> entradasAsignadas = new HashSet<>();

    public Asistente() {
    }

    // --- Getters y Setters ---
    public Integer getIdAsistente() {
        return idAsistente;
    }

    public void setIdAsistente(Integer idAsistente) {
        this.idAsistente = idAsistente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public Set<Compra> getCompras() {
        return compras;
    }

    public void setCompras(Set<Compra> compras) {
        this.compras = compras;
    }

    public Set<EntradaAsignada> getEntradasAsignadas() {
        return entradasAsignadas;
    }

    public void setEntradasAsignadas(Set<EntradaAsignada> entradasAsignadas) {
        this.entradasAsignadas = entradasAsignadas;
    }

    // --- equals, hashCode y toString ---
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Asistente asistente = (Asistente) o;
        return idAsistente != null && Objects.equals(idAsistente, asistente.idAsistente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAsistente);
    }

    @Override
    public String toString() {
        return "Asistente{"
                + "idAsistente=" + idAsistente
                + ", nombre='" + nombre + '\''
                + ", email='" + email + '\''
                + '}';
    }
}
