import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as clienteApi from '../api/cliente';
import { ApiError, RastreoPublicoDto } from '../api/tipos';
import { Rastreo } from '../paginas/Rastreo';

describe('Página de Rastreo (Rastreo.tsx)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('valida campos obligatorios y formato de documento antes de enviar', async () => {
    const user = userEvent.setup();
    render(<Rastreo />);

    const boton = screen.getByRole('button', { name: /consultar estado/i });
    await user.click(boton);

    expect(screen.getByText(/el número de comprobante es obligatorio/i)).toBeInTheDocument();
    expect(screen.getByText(/el número de documento de identidad es obligatorio/i)).toBeInTheDocument();

    // DNI inválido (menos de 8 dígitos)
    const inputDoc = screen.getByLabelText(/dni o ruc/i);
    await user.type(inputDoc, '12345');
    await user.click(boton);

    expect(screen.getByText(/ingrese un dni válido de 8 dígitos o un ruc de 11 dígitos/i)).toBeInTheDocument();
  });

  it('renderiza resultados del rastreo con línea de tiempo y nombres enmascarados', async () => {
    const user = userEvent.setup();

    const mockRespuesta: RastreoPublicoDto = {
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
        { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T13:00:00' },
        { estadoEvento: 'EN_TRANSITO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-21T01:00:00' },
        { estadoEvento: 'EN_ALMACEN_DESTINO', sucursal: 'Agencia Arequipa', fechaHora: '2026-10-22T07:00:00' },
      ],
    };

    vi.spyOn(clienteApi, 'consultarRastreo').mockResolvedValue(mockRespuesta);

    render(<Rastreo />);

    await user.type(screen.getByLabelText(/comprobante/i), 'MV-DEMO-001');
    await user.type(screen.getByLabelText(/dni o ruc/i), '45678902');
    await user.click(screen.getByRole('button', { name: /consultar estado/i }));

    await waitFor(() => {
      expect(screen.getByText('MV-DEMO-001')).toBeInTheDocument();
    });

    expect(screen.getByText('A*** M*** F***')).toBeInTheDocument();
    expect(screen.getByText('L*** A*** M***')).toBeInTheDocument();
    expect(screen.getByText(/recibido en agencia/i)).toBeInTheDocument();
    expect(screen.getByText(/en tránsito/i)).toBeInTheDocument();
    expect(screen.getAllByText(/en almacén destino/i).length).toBeGreaterThanOrEqual(1);

    // Verificación estricta: NO se guardó nada en localStorage
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('muestra banner de alerta cuando el pago está PENDIENTE (Contra Entrega)', async () => {
    const user = userEvent.setup();

    const mockPendiente: RastreoPublicoDto = {
      codigoComprobante: 'MV-DEMO-002',
      estadoLogistico: 'EN_ALMACEN_DESTINO',
      estadoPago: 'PENDIENTE',
      montoTotal: 17.50,
      sucursalOrigen: 'Agencia Lima Central',
      ciudadOrigen: 'Lima',
      sucursalDestino: 'Agencia Arequipa',
      ciudadDestino: 'Arequipa',
      nombreRemitenteEnmascarado: 'C*** A*** S***',
      nombreDestinatarioEnmascarado: 'E*** R*** C***',
      lineaDeTiempo: [
        { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T13:00:00' },
      ],
    };

    vi.spyOn(clienteApi, 'consultarRastreo').mockResolvedValue(mockPendiente);

    render(<Rastreo />);

    await user.type(screen.getByLabelText(/comprobante/i), 'MV-DEMO-002');
    await user.type(screen.getByLabelText(/dni o ruc/i), '45678904');
    await user.click(screen.getByRole('button', { name: /consultar estado/i }));

    await waitFor(() => {
      expect(screen.getByText(/pago contra entrega pendiente/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/s\/\. 17\.50/i)).toBeInTheDocument();
  });

  it('muestra mensaje de error amigable en 404', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'consultarRastreo').mockRejectedValue(
      new ApiError('No encontramos encomiendas asociadas a este número de comprobante y documento.', 404)
    );

    render(<Rastreo />);

    await user.type(screen.getByLabelText(/comprobante/i), 'NO-EXISTE');
    await user.type(screen.getByLabelText(/dni o ruc/i), '12345678');
    await user.click(screen.getByRole('button', { name: /consultar estado/i }));

    await waitFor(() => {
      expect(screen.getByText(/no encontramos encomiendas asociadas/i)).toBeInTheDocument();
    });
  });
});
