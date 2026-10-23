import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as clienteApi from '../api/cliente';
import { ApiError } from '../api/tipos';
import { Cotiza } from '../paginas/Cotiza';

const mockSucursales = [
  { id: 'SUC-LIM-01', nombreAgencia: 'Agencia Lima Central', ciudad: 'Lima', direccion: 'Av. Grau 450' },
  { id: 'SUC-HYO-01', nombreAgencia: 'Agencia Huancayo', ciudad: 'Huancayo', direccion: 'Av. Ferrocarril 1020' },
];

describe('Página de Cotización (Cotiza.tsx)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    vi.spyOn(clienteApi, 'listarSucursales').mockResolvedValue(mockSucursales);
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('carga sucursales y valida peso obligatorio y mayor a 0', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <Cotiza />
      </MemoryRouter>
    );

    // Esperar a que carguen las agencias
    await waitFor(() => {
      expect(screen.getByLabelText(/agencia origen/i)).toBeInTheDocument();
    });

    const boton = screen.getByRole('button', { name: /calcular tarifa estimada/i });
    await user.click(boton);

    expect(screen.getByText(/ingresa el peso estimado en kilogramos/i)).toBeInTheDocument();

    // Peso inválido 0 o negativo
    const inputPeso = screen.getByLabelText(/peso estimado/i);
    await user.type(inputPeso, '0');
    await user.click(boton);

    expect(screen.getByText(/el peso debe ser mayor a 0 kg/i)).toBeInTheDocument();
  });

  it('calcula la tarifa estimada y muestra el aviso de tarifa referencial', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'cotizarTarifa').mockResolvedValue({
      montoEstimado: 16.25,
      moneda: 'PEN',
    });

    render(
      <MemoryRouter>
        <Cotiza />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/agencia origen/i)).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/peso estimado/i), '4.5');
    await user.click(screen.getByRole('button', { name: /calcular tarifa estimada/i }));

    await waitFor(() => {
      expect(screen.getByText(/s\/ 16\.25/i)).toBeInTheDocument();
    });

    expect(
      screen.getByText(/tarifa referencial; el monto final se confirma en agencia/i)
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /continuar al pre-registro/i })
    ).toBeInTheDocument();

    // Verificar CERO persistencia en disco o almacenamiento
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('permite continuar al pre-registro pasando los datos por estado de React Router', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'cotizarTarifa').mockResolvedValue({
      montoEstimado: 18.5,
      moneda: 'PEN',
    });

    render(
      <MemoryRouter initialEntries={['/cotiza']}>
        <Routes>
          <Route path="/cotiza" element={<Cotiza />} />
          <Route
            path="/pre-registro"
            element={
              <div data-testid="destino-preregistro">
                Pantalla Pre-registro
              </div>
            }
          />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/agencia origen/i)).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/peso estimado/i), '5.4');
    await user.click(screen.getByRole('button', { name: /calcular tarifa estimada/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /continuar al pre-registro/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /continuar al pre-registro/i }));

    expect(screen.getByTestId('destino-preregistro')).toBeInTheDocument();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('muestra alerta ante limitación de tasa 429 con Retry-After', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'cotizarTarifa').mockRejectedValue(
      new ApiError('Has superado el límite de consultas permitidas. Espera 60 segundos.', 429, 'LIMITE_EXCEDIDO', 60)
    );

    render(
      <MemoryRouter>
        <Cotiza />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/peso estimado/i), '2.0');
    await user.click(screen.getByRole('button', { name: /calcular tarifa estimada/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /límite de consultas/i })).toBeInTheDocument();
    });
    expect(screen.getByText(/espera 60 segundos/i)).toBeInTheDocument();
  });
});
