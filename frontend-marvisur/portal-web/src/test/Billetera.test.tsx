import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as clienteApi from '../api/cliente';
import { ApiError, RespuestaBilleteraDto } from '../api/tipos';
import { Billetera } from '../paginas/Billetera';

describe('Página Billetera Digital (Billetera.tsx)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    window.location.hash = '';
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    window.location.hash = '';
  });

  it('muestra mensaje genérico si se accede sin token en el hash', async () => {
    render(
      <MemoryRouter initialEntries={['/billetera']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/no se proporcionó una llave digital de retiro/i)).toBeInTheDocument();
    });
  });

  it('lee el token del hash y lo envía en el cuerpo para token ACTIVO con pago PAGADO', async () => {
    const mockBilletera: RespuestaBilleteraDto = {
      codigoComprobante: 'MV-DEMO-001',
      estadoLogistico: 'EN_ALMACEN_DESTINO',
      estadoPago: 'PAGADO',
      montoTotal: 16.25,
      agenciaDestino: 'Agencia Arequipa',
      ciudadDestino: 'Arequipa',
      estadoToken: 'ACTIVO',
      qr2Base64: 'iVBORw0KGgoAAAANSUhEUgAAASw...',
    };

    const spyConsultar = vi.spyOn(clienteApi, 'consultarBilletera').mockResolvedValue(mockBilletera);

    render(
      <MemoryRouter initialEntries={['/billetera#DEMO-TOKEN-PAGADA-001']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(spyConsultar).toHaveBeenCalledWith('DEMO-TOKEN-PAGADA-001');
    });

    expect(screen.getByText('MV-DEMO-001')).toBeInTheDocument();
    expect(screen.getByText(/código activo para retiro/i)).toBeInTheDocument();
    expect(screen.getByAltText(/código qr2 digital de retiro/i)).toBeInTheDocument();
    expect(screen.getByText(/no compartas este enlace/i)).toBeInTheDocument();
    expect(screen.getByText(/muestra este código al personal en ventanilla/i)).toBeInTheDocument();

    // Verificación estricta: NUNCA se persiste el token
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    expect(document.cookie).toBe('');
  });

  it('muestra banner de monto a pagar cuando estadoToken es ACTIVO y estadoPago es PENDIENTE', async () => {
    const mockPendiente: RespuestaBilleteraDto = {
      codigoComprobante: 'MV-DEMO-002',
      estadoLogistico: 'EN_ALMACEN_DESTINO',
      estadoPago: 'PENDIENTE',
      montoTotal: 17.50,
      agenciaDestino: 'Agencia Arequipa',
      ciudadDestino: 'Arequipa',
      estadoToken: 'ACTIVO',
      qr2Base64: 'iVBORw0KGgo...',
    };

    vi.spyOn(clienteApi, 'consultarBilletera').mockResolvedValue(mockPendiente);

    render(
      <MemoryRouter initialEntries={['/billetera#DEMO-TOKEN-PENDIENTE-002']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/pago contra entrega requerido/i)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/17\.50/).length).toBeGreaterThanOrEqual(1);
  });

  it('muestra mensaje adecuado cuando estadoToken es INVALIDADO (ya entregado)', async () => {
    const mockInvalidado: RespuestaBilleteraDto = {
      codigoComprobante: 'MV-DEMO-003',
      estadoLogistico: 'ENTREGADO',
      estadoPago: 'PAGADO',
      montoTotal: 10.00,
      agenciaDestino: 'Agencia Huancayo',
      ciudadDestino: 'Huancayo',
      estadoToken: 'INVALIDADO',
      qr2Base64: 'iVBORw0KGgo...',
    };

    vi.spyOn(clienteApi, 'consultarBilletera').mockResolvedValue(mockInvalidado);

    render(
      <MemoryRouter initialEntries={['/billetera#DEMO-TOKEN-REGISTRADA-003']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/este envío ya fue entregado y el código ya no es válido/i)).toBeInTheDocument();
    });
    expect(screen.getByText('INVALIDADO')).toBeInTheDocument();
  });

  it('muestra mensaje adecuado cuando estadoToken es EXPIRADO', async () => {
    const mockExpirado: RespuestaBilleteraDto = {
      codigoComprobante: 'MV-DEMO-EXP',
      estadoLogistico: 'EN_ALMACEN_DESTINO',
      estadoPago: 'PAGADO',
      montoTotal: 20.00,
      agenciaDestino: 'Agencia Lima Central',
      ciudadDestino: 'Lima',
      estadoToken: 'EXPIRADO',
      qr2Base64: 'iVBORw0KGgo...',
    };

    vi.spyOn(clienteApi, 'consultarBilletera').mockResolvedValue(mockExpirado);

    render(
      <MemoryRouter initialEntries={['/billetera#TOKEN-EXPIRADO']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/plazo de retiro expirado/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/este código ha expirado/i)).toBeInTheDocument();
  });

  it('muestra mensaje genérico en 404 cuando el token no existe', async () => {
    vi.spyOn(clienteApi, 'consultarBilletera').mockRejectedValue(
      new ApiError('El enlace de retiro o llave digital no existe o ya no se encuentra disponible.', 404)
    );

    render(
      <MemoryRouter initialEntries={['/billetera#TOKEN-INEXISTENTE']}>
        <Billetera />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/el enlace de retiro o llave digital no existe/i)).toBeInTheDocument();
    });
  });
});
