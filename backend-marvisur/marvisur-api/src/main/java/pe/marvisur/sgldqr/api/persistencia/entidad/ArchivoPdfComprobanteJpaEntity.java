package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'archivos_pdf_comprobante' (V6__modulo6_tracking_comprobantes.sql).
 */
@Entity
@Table(name = "archivos_pdf_comprobante")
public class ArchivoPdfComprobanteJpaEntity {

    @Id
    @Column(name = "id_pdf", length = 36, nullable = false)
    private String idPdf;

    @Column(name = "id_comprobante", length = 36, nullable = false, unique = true)
    private String idComprobante;

    @Column(name = "nombre_archivo", length = 255, nullable = false)
    private String nombreArchivo;

    @Column(name = "ruta_almacenamiento_vps", columnDefinition = "TEXT", nullable = false)
    private String rutaAlmacenamientoVps;

    @Column(name = "hash_sha256_archivo", length = 64, nullable = false)
    private String hashSha256Archivo;

    @Column(name = "fecha_generacion", nullable = false)
    private OffsetDateTime fechaGeneracion;

    public ArchivoPdfComprobanteJpaEntity() {
    }

    public ArchivoPdfComprobanteJpaEntity(String idPdf, String idComprobante, String nombreArchivo,
                                          String rutaAlmacenamientoVps, String hashSha256Archivo,
                                          OffsetDateTime fechaGeneracion) {
        this.idPdf = idPdf;
        this.idComprobante = idComprobante;
        this.nombreArchivo = nombreArchivo;
        this.rutaAlmacenamientoVps = rutaAlmacenamientoVps;
        this.hashSha256Archivo = hashSha256Archivo;
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getIdPdf() {
        return idPdf;
    }

    public void setIdPdf(String idPdf) {
        this.idPdf = idPdf;
    }

    public String getIdComprobante() {
        return idComprobante;
    }

    public void setIdComprobante(String idComprobante) {
        this.idComprobante = idComprobante;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaAlmacenamientoVps() {
        return rutaAlmacenamientoVps;
    }

    public void setRutaAlmacenamientoVps(String rutaAlmacenamientoVps) {
        this.rutaAlmacenamientoVps = rutaAlmacenamientoVps;
    }

    public String getHashSha256Archivo() {
        return hashSha256Archivo;
    }

    public void setHashSha256Archivo(String hashSha256Archivo) {
        this.hashSha256Archivo = hashSha256Archivo;
    }

    public OffsetDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(OffsetDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchivoPdfComprobanteJpaEntity that = (ArchivoPdfComprobanteJpaEntity) o;
        return Objects.equals(idPdf, that.idPdf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPdf);
    }
}
