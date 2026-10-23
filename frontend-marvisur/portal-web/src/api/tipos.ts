/**
 * Contratos de datos tipados del portal público según docs/openapi-publico.json
 */

export interface SucursalPublicaDto {
  id: string;
  nombreAgencia: string;
  ciudad: string;
  direccion: string;
}

export interface EventoTrackingPublicoDto {
  estadoEvento: string;
  sucursal: string;
  fechaHora: string;
}

export type EstadoPago = 'PAGADO' | 'PENDIENTE' | string;

export interface RastreoPublicoDto {
  codigoComprobante: string;
  estadoLogistico: string;
  estadoPago: EstadoPago;
  montoTotal: number;
  sucursalOrigen: string;
  ciudadOrigen: string;
  sucursalDestino: string;
  ciudadDestino: string;
  nombreRemitenteEnmascarado: string;
  nombreDestinatarioEnmascarado: string;
  lineaDeTiempo: EventoTrackingPublicoDto[];
}

export type EstadoTokenQR2 = 'ACTIVO' | 'INVALIDADO' | 'EXPIRADO' | string;

export interface RespuestaBilleteraDto {
  codigoComprobante: string;
  estadoLogistico: string;
  estadoPago: EstadoPago;
  montoTotal: number;
  agenciaDestino: string;
  ciudadDestino: string;
  estadoToken: EstadoTokenQR2;
  qr2Base64: string;
}

// =============================================================================
// Tipos para Cotización y Pre-Registro Express (RF-10)
// =============================================================================

export interface CotizacionRequest {
  idSucursalOrigen: string;
  idSucursalDestino: string;
  pesoKg: number;
  volumenM3?: number;
}

export interface CotizacionResponse {
  montoEstimado: number;
  moneda: string;
}

export type TipoDocumento = 'DNI' | 'RUC';

export interface ClienteInputDto {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombresRazonSocial: string;
  telefono?: string;
  correo?: string;
  documentoValido?: boolean;
}

export interface CrearPreRegistroPublicoRequest {
  remitente: ClienteInputDto;
  destinatario: ClienteInputDto;
  idSucursalDestino: string;
  pesoEstimado: number;
}

export interface RespuestaPreRegistroPublicoDto {
  codigoExpress: string;
  fechaExpiracion: string;
  montoCotizado: number;
  pesoEstimado: number;
  sucursalDestino: string;
  remitenteEnmascarado: string;
  destinatarioEnmascarado: string;
}

// =============================================================================
// Errores RFC 7807 (ProblemDetail)
// =============================================================================

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  codigo?: string;
  campos?: Record<string, string>;
  invalidParams?: Record<string, string>;
  errores?: Record<string, string>;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly codigo?: string;
  public readonly retryAfterSeconds?: number;
  public readonly esErrorRed: boolean;
  public readonly problemDetail?: ProblemDetail;
  public readonly campos?: Record<string, string>;

  constructor(
    mensaje: string,
    status = 500,
    codigo?: string,
    retryAfterSeconds?: number,
    esErrorRed = false,
    problemDetail?: ProblemDetail
  ) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
    this.retryAfterSeconds = retryAfterSeconds;
    this.esErrorRed = esErrorRed;
    this.problemDetail = problemDetail;
    this.campos = problemDetail?.campos || problemDetail?.invalidParams || problemDetail?.errores;
  }
}

/**
 * Diccionario de etiquetas legibles y amigables para los estados logísticos generales
 */
export const ETIQUETAS_ESTADO_LOGISTICO: Record<string, string> = {
  RECEPCIONADO: 'Recibido en agencia',
  REGISTRADO: 'Registrado en ventanilla',
  EN_ALMACEN_ORIGEN: 'En almacén de origen',
  EN_TRANSITO: 'En tránsito',
  EN_RUTA: 'En ruta hacia destino',
  EN_ALMACEN_DESTINO: 'En almacén destino',
  LISTO_PARA_RETIRO: 'Listo para retiro',
  ENTREGADO: 'Entregado',
};

export function obtenerEtiquetaEstado(estado: string): string {
  return ETIQUETAS_ESTADO_LOGISTICO[estado] || estado;
}

/**
 * Mapa oficial de etiquetas legibles para los eventos emitidos por la API de tracking:
 * RECEPCIONADO, EN_ALMACEN_ORIGEN, EN_TRANSITO, LISTO_PARA_RETIRO, ENTREGADO.
 * EN_ALMACEN_DESTINO actúa como alias tolerado de LISTO_PARA_RETIRO.
 */
export const MAPA_ESTADOS_TRACKING: Record<string, string> = {
  RECEPCIONADO: 'Recibido en agencia',
  EN_ALMACEN_ORIGEN: 'En almacén de origen',
  EN_TRANSITO: 'En tránsito',
  LISTO_PARA_RETIRO: 'Listo para retiro',
  EN_ALMACEN_DESTINO: 'Listo para retiro', // Alias tolerado
  ENTREGADO: 'Entregado',
};

/**
 * Devuelve la etiqueta amigable del estado de tracking o el texto crudo si es desconocido.
 */
export function obtenerEtiquetaTracking(estado: string): string {
  if (!estado) return '';
  return MAPA_ESTADOS_TRACKING[estado] || estado;
}
