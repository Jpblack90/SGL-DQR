import {
  ApiError,
  ProblemDetail,
  RastreoPublicoDto,
  RespuestaBilleteraDto,
  SucursalPublicaDto,
} from './tipos';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

async function parsearRespuesta<T>(respuesta: Response, contexto: 'rastreo' | 'billetera' | 'sucursales' | 'general'): Promise<T> {
  if (respuesta.ok) {
    return (await respuesta.json()) as T;
  }

  // Manejo de limitación de tasa (HTTP 429)
  if (respuesta.status === 429) {
    const retryAfterHeader = respuesta.headers.get('Retry-After');
    const retryAfter = retryAfterHeader ? parseInt(retryAfterHeader, 10) : 60;
    const segundos = isNaN(retryAfter) || retryAfter <= 0 ? 60 : retryAfter;
    throw new ApiError(
      `Has superado el límite de consultas permitidas. Por favor, espera ${segundos} segundos antes de intentar nuevamente.`,
      429,
      'LIMITE_EXCEDIDO',
      segundos
    );
  }

  let problem: ProblemDetail | null = null;
  try {
    problem = (await respuesta.json()) as ProblemDetail;
  } catch {
    // Si no es JSON válido RFC 7807, se maneja según código HTTP
  }

  // Manejo de HTTP 404
  if (respuesta.status === 404) {
    if (contexto === 'billetera') {
      throw new ApiError(
        'El enlace de retiro o llave digital no existe o ya no se encuentra disponible.',
        404,
        problem?.codigo || 'RECURSO_NO_ENCONTRADO'
      );
    }
    if (contexto === 'rastreo') {
      throw new ApiError(
        'No encontramos encomiendas asociadas a este número de comprobante y documento. Verifique los datos ingresados.',
        404,
        problem?.codigo || 'RECURSO_NO_ENCONTRADO'
      );
    }
    throw new ApiError(
      problem?.detail || 'El recurso solicitado no fue encontrado.',
      404,
      problem?.codigo || 'RECURSO_NO_ENCONTRADO'
    );
  }

  // Manejo de HTTP 400
  if (respuesta.status === 400) {
    throw new ApiError(
      problem?.detail || 'Los datos ingresados no son válidos. Por favor, revise el formulario.',
      400,
      problem?.codigo || 'PETICION_INVALIDA'
    );
  }

  // Otros errores HTTP (500, etc.)
  throw new ApiError(
    problem?.detail || `Error en el servicio (${respuesta.status}). Intente nuevamente en unos minutos.`,
    respuesta.status,
    problem?.codigo
  );
}

function manejarErrorRed(error: unknown): never {
  if (error instanceof ApiError) {
    throw error;
  }
  throw new ApiError(
    'No fue posible comunicarse con los servidores de Expreso Marvisur. Verifique su conexión a internet.',
    0,
    'ERROR_RED',
    undefined,
    true
  );
}

/**
 * Consulta el catálogo público de sucursales activas
 */
export async function listarSucursales(): Promise<SucursalPublicaDto[]> {
  const url = `${BASE_URL}/api/v1/publico/sucursales`;
  try {
    const respuesta = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });
    return await parsearRespuesta<SucursalPublicaDto[]>(respuesta, 'sucursales');
  } catch (err) {
    return manejarErrorRed(err);
  }
}

/**
 * Consulta la línea de tiempo y estado de una encomienda autorizada por DNI o RUC
 */
export async function consultarRastreo(comprobante: string, documento: string): Promise<RastreoPublicoDto> {
  const params = new URLSearchParams({
    comprobante: comprobante.trim(),
    documento: documento.trim(),
  });
  const url = `${BASE_URL}/api/v1/publico/rastreo?${params.toString()}`;

  try {
    const respuesta = await fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
      },
    });
    return await parsearRespuesta<RastreoPublicoDto>(respuesta, 'rastreo');
  } catch (err) {
    return manejarErrorRed(err);
  }
}

/**
 * Consulta el estado y obtiene el QR2 de la Billetera Digital.
 * El token se envía estrictamente en el cuerpo de la petición POST.
 * NUNCA se persiste ni se imprime en logs.
 */
export async function consultarBilletera(token: string): Promise<RespuestaBilleteraDto> {
  const url = `${BASE_URL}/api/v1/publico/billetera`;

  try {
    const respuesta = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({ token: token.trim() }),
    });
    return await parsearRespuesta<RespuestaBilleteraDto>(respuesta, 'billetera');
  } catch (err) {
    return manejarErrorRed(err);
  }
}
