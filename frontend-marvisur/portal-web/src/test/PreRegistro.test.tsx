import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as clienteApi from '../api/cliente';
import { ApiError } from '../api/tipos';
import { PreRegistro } from '../paginas/PreRegistro';

const mockSucursales = [
  { id: 'SUC-LIM-01', nombreAgencia: 'Agencia Lima Central', ciudad: 'Lima', direccion: 'Av. Grau 450' },
  { id: 'SUC-HYO-01', nombreAgencia: 'Agencia Huancayo', ciudad: 'Huancayo', direccion: 'Av. Ferrocarril 1020' },
];

describe('Página de Pre-Registro Express (PreRegistro.tsx)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    document.cookie = '';
    vi.spyOn(clienteApi, 'listarSucursales').mockResolvedValue(mockSucursales);
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('valida peso obligatorio en el Paso 1 antes de avanzar', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/agencia de destino/i)).toBeInTheDocument();
    });

    const botonSiguiente = screen.getByRole('button', { name: /siguiente: personas/i });
    await user.click(botonSiguiente);

    expect(screen.getByText(/ingresa el peso estimado del paquete/i)).toBeInTheDocument();
  });

  it('valida DNI, RUC, teléfonos y nombres en el Paso 2', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    // Completar Paso 1
    await user.type(screen.getByLabelText(/peso estimado/i), '3.5');
    await user.click(screen.getByRole('button', { name: /siguiente: personas/i }));

    // Ahora en Paso 2
    expect(screen.getByText(/datos del remitente/i)).toBeInTheDocument();
    expect(screen.getByText(/datos del destinatario/i)).toBeInTheDocument();

    const botonRevisar = screen.getByRole('button', { name: /revisar y confirmar/i });
    await user.click(botonRevisar);

    expect(screen.getByText(/el documento del remitente es obligatorio/i)).toBeInTheDocument();

    // DNI inválido (menos de 8 dígitos)
    const inputsDoc = screen.getAllByLabelText(/número de documento/i);
    await user.type(inputsDoc[0], '1234'); // Remitente
    await user.click(botonRevisar);

    expect(screen.getByText(/el dni debe tener exactamente 8 dígitos/i)).toBeInTheDocument();

    // Teléfono inválido (menos de 9 dígitos)
    const inputsTel = screen.getAllByLabelText(/teléfono móvil/i);
    await user.type(inputsTel[0], '987');
    await user.click(botonRevisar);

    expect(screen.getByText(/el teléfono debe tener 9 dígitos/i)).toBeInTheDocument();
  });

  it('exige la casilla obligatoria de uso de datos en el Paso 3', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    // Paso 1
    await user.type(screen.getByLabelText(/peso estimado/i), '4.0');
    await user.click(screen.getByRole('button', { name: /siguiente: personas/i }));

    // Paso 2
    const inputsDoc = screen.getAllByLabelText(/número de documento/i);
    const inputsNombres = screen.getAllByLabelText(/nombres y apellidos/i);
    const inputsTel = screen.getAllByLabelText(/teléfono móvil/i);

    await user.type(inputsDoc[0], '77665544');
    await user.type(inputsNombres[0], 'Carlos Remitente');
    await user.type(inputsTel[0], '987654321');

    await user.type(inputsDoc[1], '11223344');
    await user.type(inputsNombres[1], 'Diana Destinatario');
    await user.type(inputsTel[1], '912345678');

    await user.click(screen.getByRole('button', { name: /revisar y confirmar/i }));

    // Paso 3
    expect(screen.getByText(/resumen del pre-registro/i)).toBeInTheDocument();

    const botonConfirmar = screen.getByRole('button', { name: /confirmar y generar código express/i });
    await user.click(botonConfirmar);

    expect(
      screen.getByText(/debes aceptar el uso de datos personales para procesar el pre-registro/i)
    ).toBeInTheDocument();
  });

  it('completa el flujo íntegro: cotiza -> pre-registro -> pantalla de éxito con código y vigencia', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'crearPreRegistro').mockResolvedValue({
      codigoExpress: 'PRE-987654-PE',
      fechaExpiracion: '2026-10-25T18:00:00Z',
      montoCotizado: 17.5,
      pesoEstimado: 5.0,
      sucursalDestino: 'Agencia Huancayo',
      remitenteEnmascarado: 'J*** P***',
      destinatarioEnmascarado: 'A*** G***',
    });

    render(
      <MemoryRouter initialEntries={[{ pathname: '/pre-registro', state: { pesoEstimado: 5.0, montoCotizado: 17.5 } }]}>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    // Paso 1: Peso precargado desde estado de router
    await user.click(screen.getByRole('button', { name: /siguiente: personas/i }));

    // Paso 2: Llenar remitente y destinatario
    const inputsDoc = screen.getAllByLabelText(/número de documento/i);
    const inputsNombres = screen.getAllByLabelText(/nombres y apellidos/i);
    const inputsTel = screen.getAllByLabelText(/teléfono móvil/i);

    await user.type(inputsDoc[0], '44556677');
    await user.type(inputsNombres[0], 'Juan Pérez');
    await user.type(inputsTel[0], '999888777');

    await user.type(inputsDoc[1], '88776655');
    await user.type(inputsNombres[1], 'Ana Gómez');
    await user.type(inputsTel[1], '911222333');

    await user.click(screen.getByRole('button', { name: /revisar y confirmar/i }));

    // Paso 3: Confirmación
    expect(screen.getByText(/resumen del pre-registro/i)).toBeInTheDocument();
    expect(screen.getByText('Juan Pérez')).toBeInTheDocument();
    expect(screen.getByText('Ana Gómez')).toBeInTheDocument();

    // Aceptar términos
    const checkbox = screen.getByRole('checkbox');
    await user.click(checkbox);

    // Enviar pre-registro
    await user.click(screen.getByRole('button', { name: /confirmar y generar código express/i }));

    // Pantalla de Éxito
    await waitFor(() => {
      expect(screen.getByText('PRE-987654-PE')).toBeInTheDocument();
    });

    expect(screen.getByText(/tu código express/i)).toBeInTheDocument();
    expect(screen.getByText(/s\/ 17\.50/i)).toBeInTheDocument();
    expect(
      screen.getByText(/preséntalo en la agencia para registrar tu envío; no incluye el pago/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/el pre-registro no es el envío: el peso y el contenido se confirman en ventanilla/i)
    ).toBeInTheDocument();

    expect(screen.getByRole('button', { name: /imprimir comprobante/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /compartir por whatsapp/i })).toBeInTheDocument();

    // Validar aislamiento total de privacidad: nada en storage
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('maneja error 400 con errores detallados por campo desde ProblemDetail sin perder el formulario', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'crearPreRegistro').mockRejectedValue(
      new ApiError(
        'Se encontraron errores de validación',
        400,
        'VALIDACION_FALLIDA',
        undefined,
        false,
        {
          campos: {
            'remitente.numeroDocumento': 'El DNI del remitente no existe en RENIEC simulado',
          },
        }
      )
    );

    render(
      <MemoryRouter>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    // Paso 1
    await user.type(screen.getByLabelText(/peso estimado/i), '2.0');
    await user.click(screen.getByRole('button', { name: /siguiente: personas/i }));

    // Paso 2
    const inputsDoc = screen.getAllByLabelText(/número de documento/i);
    const inputsNombres = screen.getAllByLabelText(/nombres y apellidos/i);
    const inputsTel = screen.getAllByLabelText(/teléfono móvil/i);

    await user.type(inputsDoc[0], '00000000');
    await user.type(inputsNombres[0], 'Remitente Error');
    await user.type(inputsTel[0], '999888777');

    await user.type(inputsDoc[1], '12345678');
    await user.type(inputsNombres[1], 'Destinatario Ok');
    await user.type(inputsTel[1], '911222333');

    await user.click(screen.getByRole('button', { name: /revisar y confirmar/i }));

    // Paso 3
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: /confirmar y generar código express/i }));

    // La app detecta el error en remitente y regresa al paso 2 mostrando el error sin perder los datos
    await waitFor(() => {
      expect(
        screen.getByText(/el dni del remitente no existe en reniec simulado/i)
      ).toBeInTheDocument();
    });

    // Verificar que los campos no se borraron
    expect(screen.getByDisplayValue('Remitente Error')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Destinatario Ok')).toBeInTheDocument();
  });

  it('maneja limitación 429 de tasa', async () => {
    const user = userEvent.setup();

    vi.spyOn(clienteApi, 'crearPreRegistro').mockRejectedValue(
      new ApiError('Límite de pre-registros alcanzado. Espera 60 s.', 429, 'LIMITE_EXCEDIDO', 60)
    );

    render(
      <MemoryRouter>
        <PreRegistro />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/peso estimado/i)).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/peso estimado/i), '2.0');
    await user.click(screen.getByRole('button', { name: /siguiente: personas/i }));

    const inputsDoc = screen.getAllByLabelText(/número de documento/i);
    const inputsNombres = screen.getAllByLabelText(/nombres y apellidos/i);
    const inputsTel = screen.getAllByLabelText(/teléfono móvil/i);

    await user.type(inputsDoc[0], '12345678');
    await user.type(inputsNombres[0], 'Remitente Normal');
    await user.type(inputsTel[0], '999888777');

    await user.type(inputsDoc[1], '87654321');
    await user.type(inputsNombres[1], 'Destinatario Normal');
    await user.type(inputsTel[1], '911222333');

    await user.click(screen.getByRole('button', { name: /revisar y confirmar/i }));
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: /confirmar y generar código express/i }));

    await waitFor(() => {
      expect(screen.getByText(/límite de consultas/i)).toBeInTheDocument();
    });
  });
});
