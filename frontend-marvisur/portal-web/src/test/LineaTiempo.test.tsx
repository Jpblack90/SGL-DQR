import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LineaTiempo } from '../componentes/LineaTiempo';
import { EventoTrackingPublicoDto, MAPA_ESTADOS_TRACKING, obtenerEtiquetaTracking } from '../api/tipos';

describe('Componente LineaTiempo (LineaTiempo.tsx)', () => {
  it('cubre todos los estados oficiales emitidos por la API, PAGO_REGISTRADO y alias tolerado', () => {
    expect(MAPA_ESTADOS_TRACKING.RECEPCIONADO).toBe('Recibido en agencia');
    expect(obtenerEtiquetaTracking('RECEPCIONADO')).toBe('Recibido en agencia');
    expect(obtenerEtiquetaTracking('EN_ALMACEN_ORIGEN')).toBe('En almacén de origen');
    expect(obtenerEtiquetaTracking('EN_TRANSITO')).toBe('En tránsito');
    expect(obtenerEtiquetaTracking('LISTO_PARA_RETIRO')).toBe('Listo para retiro');
    expect(obtenerEtiquetaTracking('EN_ALMACEN_DESTINO')).toBe('Listo para retiro'); // alias tolerado
    expect(MAPA_ESTADOS_TRACKING.PAGO_REGISTRADO).toBe('Pago registrado en agencia');
    expect(obtenerEtiquetaTracking('PAGO_REGISTRADO')).toBe('Pago registrado en agencia');
    expect(obtenerEtiquetaTracking('ENTREGADO')).toBe('Entregado');
  });

  it('muestra el texto crudo si llega un estado desconocido', () => {
    expect(obtenerEtiquetaTracking('ESTADO_PERSONALIZADO_RAW')).toBe('ESTADO_PERSONALIZADO_RAW');
    expect(obtenerEtiquetaTracking('EN_ADUANA_ESPECIAL')).toBe('EN_ADUANA_ESPECIAL');
  });

  it('renderiza la lista de eventos con sus etiquetas amigables, PAGO_REGISTRADO y estado desconocido crudo', () => {
    const eventos: EventoTrackingPublicoDto[] = [
      { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T10:00:00Z' },
      { estadoEvento: 'EN_ALMACEN_ORIGEN', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T14:00:00Z' },
      { estadoEvento: 'EN_TRANSITO', sucursal: 'Ruta Lima - Huancayo', fechaHora: '2026-10-21T02:00:00Z' },
      { estadoEvento: 'LISTO_PARA_RETIRO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T08:00:00Z' },
      { estadoEvento: 'PAGO_REGISTRADO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T09:30:00Z' },
      { estadoEvento: 'ENTREGADO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T11:00:00Z' },
      { estadoEvento: 'ESTADO_EXTRA_CRUDO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T12:00:00Z' },
    ];

    render(<LineaTiempo eventos={eventos} />);

    expect(screen.getByText('Recibido en agencia')).toBeInTheDocument();
    expect(screen.getByText('En almacén de origen')).toBeInTheDocument();
    expect(screen.getByText('En tránsito')).toBeInTheDocument();
    expect(screen.getByText('Listo para retiro')).toBeInTheDocument();
    expect(screen.getByText('Pago registrado en agencia')).toBeInTheDocument();
    expect(screen.getByText('Entregado')).toBeInTheDocument();
    expect(screen.getByText('ESTADO_EXTRA_CRUDO')).toBeInTheDocument();
  });

  it('formatea fechas ISO con y sin indicador de zona horaria usando la hora local del navegador', () => {
    // 23:03:48 UTC corresponde al instante de las 18:03:48 en zona horaria America/Lima (UTC-5)
    const eventos: EventoTrackingPublicoDto[] = [
      { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima', fechaHora: '2026-10-23T23:03:48.646676Z' },
      { estadoEvento: 'PAGO_REGISTRADO', sucursal: 'Agencia Arequipa', fechaHora: '2026-10-23T23:30:00' },
    ];

    const { container } = render(<LineaTiempo eventos={eventos} />);
    const timeElements = container.querySelectorAll('time');
    expect(timeElements).toHaveLength(2);
    // Cada elemento <time> tiene dateTime y contenido formateado no vacío
    expect(timeElements[0].getAttribute('dateTime')).toBe('2026-10-23T23:03:48.646676Z');
    expect(timeElements[0].textContent).toBeTruthy();
    expect(timeElements[1].textContent).toBeTruthy();
  });

  it('muestra mensaje cuando no hay eventos de seguimiento', () => {
    render(<LineaTiempo eventos={[]} />);
    expect(screen.getByText(/no se registran eventos de seguimiento/i)).toBeInTheDocument();
  });
});
