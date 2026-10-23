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

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  codigo?: string;
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly codigo?: string;
  public readonly retryAfterSeconds?: number;
  public readonly esErrorRed: boolean;

  constructor(mensaje: string, status = 500, codigo?: string, retryAfterSeconds?: number, esErrorRed = false) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.codigo = codigo;
    this.retryAfterSeconds = retryAfterSeconds;
    this.esErrorRed = esErrorRed;
  }
}

/**
 * Diccionario de etiquetas legibles y amigables para los estados logísticos
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
  return ETIQUETAS_ESTADO_LOGISTICO[estado] || estado.replace(/_/g, ' ');
}
