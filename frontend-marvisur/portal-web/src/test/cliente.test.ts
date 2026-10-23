import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { consultarBilletera, consultarRastreo, listarSucursales } from '../api/cliente';
import { ApiError } from '../api/tipos';

describe('Cliente API Público (cliente.ts)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('listarSucursales retorna lista de agencias en 200 OK', async () => {
    const mockSucursales = [
      { id: 'SUC-LIM-01', nombreAgencia: 'Agencia Lima Central', ciudad: 'Lima', direccion: 'Av. Grau 450' },
      { id: 'SUC-HYO-01', nombreAgencia: 'Agencia Huancayo', ciudad: 'Huancayo', direccion: 'Av. Ferrocarril 1020' },
    ];

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockSucursales,
    }));

    const resultado = await listarSucursales();
    expect(resultado).toHaveLength(2);
    expect(resultado[1].ciudad).toBe('Huancayo');
  });

  it('consultarRastreo retorna datos con comprobante y documento en 200 OK', async () => {
    const mockRastreo = {
      codigoComprobante: 'MV-DEMO-001',
      estadoLogistico: 'EN_ALMACEN_DESTINO',
      estadoPago: 'PAGADO',
      montoTotal: 16.25,
      sucursalOrigen: 'Agencia Lima Central',
      ciudadOrigen: 'Lima',
      sucursalDestino: 'Agencia Arequipa',
      ciudadDestino: 'Arequipa',
      nombreRemitenteEnmascarado: 'A*** M*** F***',
      nombreDestinatarioEnmascarado: 'L*** A*** M***',
      lineaDeTiempo: [
        { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T10:00:00' },
      ],
    };

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockRastreo,
    }));

    const resultado = await consultarRastreo('MV-DEMO-001', '45678902');
    expect(resultado.codigoComprobante).toBe('MV-DEMO-001');
    expect(resultado.nombreDestinatarioEnmascarado).toBe('L*** A*** M***');
  });

  it('consultarRastreo maneja error 404 con mensaje amigable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: async () => ({
        type: 'https://marvisur.pe/errores/no-encontrado',
        title: 'Recurso No Encontrado',
        status: 404,
        detail: 'No se encontró encomienda',
        codigo: 'RECURSO_NO_ENCONTRADO',
      }),
    }));

    await expect(consultarRastreo('INEXISTENTE', '12345678')).rejects.toThrowError(ApiError);
    await expect(consultarRastreo('INEXISTENTE', '12345678')).rejects.toMatchObject({
      status: 404,
    });
  });

  it('consultarRastreo maneja limitación de tasa 429 con Retry-After', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 429,
      headers: {
        get: (header: string) => (header === 'Retry-After' ? '45' : null),
      },
      json: async () => ({}),
    }));

    try {
      await consultarRastreo('MV-DEMO-001', '45678902');
      expect.fail('Debería haber lanzado ApiError 429');
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.status).toBe(429);
      expect(apiErr.retryAfterSeconds).toBe(45);
      expect(apiErr.message).toContain('45 segundos');
    }
  });

  it('consultarBilletera envía el token estrictamente en el cuerpo POST', async () => {
    let requestBody = '';
    let requestMethod = '';

    vi.stubGlobal('fetch', vi.fn().mockImplementation((_url, init) => {
      requestMethod = init.method;
      requestBody = init.body;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({
          codigoComprobante: 'MV-DEMO-001',
          estadoLogistico: 'EN_ALMACEN_DESTINO',
          estadoPago: 'PAGADO',
          montoTotal: 16.25,
          agenciaDestino: 'Agencia Arequipa',
          ciudadDestino: 'Arequipa',
          estadoToken: 'ACTIVO',
          qr2Base64: 'iVBORw0KGgo...',
        }),
      });
    }));

    const resultado = await consultarBilletera('MI-TOKEN-SECRETO-123');

    expect(requestMethod).toBe('POST');
    expect(JSON.parse(requestBody)).toEqual({ token: 'MI-TOKEN-SECRETO-123' });
    expect(resultado.estadoToken).toBe('ACTIVO');
  });

  it('maneja error de conexión a internet o red caída', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Failed to fetch')));

    try {
      await listarSucursales();
      expect.fail('Debería haber lanzado error de red');
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError);
      const apiErr = err as ApiError;
      expect(apiErr.esErrorRed).toBe(true);
      expect(apiErr.message).toContain('conexión a internet');
    }
  });
});
