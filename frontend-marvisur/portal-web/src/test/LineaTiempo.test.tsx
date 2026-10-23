import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LineaTiempo } from '../componentes/LineaTiempo';
import { EventoTrackingPublicoDto, MAPA_ESTADOS_TRACKING, obtenerEtiquetaTracking } from '../api/tipos';

describe('Componente LineaTiempo (LineaTiempo.tsx)', () => {
  it('cubre todos los estados oficiales emitidos por la API y alias tolerado', () => {
    expect(MAPA_ESTADOS_TRACKING.RECEPCIONADO).toBe('Recibido en agencia');
    expect(obtenerEtiquetaTracking('RECEPCIONADO')).toBe('Recibido en agencia');
    expect(obtenerEtiquetaTracking('EN_ALMACEN_ORIGEN')).toBe('En almacén de origen');
    expect(obtenerEtiquetaTracking('EN_TRANSITO')).toBe('En tránsito');
    expect(obtenerEtiquetaTracking('LISTO_PARA_RETIRO')).toBe('Listo para retiro');
    expect(obtenerEtiquetaTracking('EN_ALMACEN_DESTINO')).toBe('Listo para retiro'); // alias tolerado
    expect(obtenerEtiquetaTracking('ENTREGADO')).toBe('Entregado');
  });

  it('muestra el texto crudo si llega un estado desconocido', () => {
    expect(obtenerEtiquetaTracking('ESTADO_PERSONALIZADO_RAW')).toBe('ESTADO_PERSONALIZADO_RAW');
    expect(obtenerEtiquetaTracking('EN_ADUANA_ESPECIAL')).toBe('EN_ADUANA_ESPECIAL');
  });

  it('renderiza la lista de eventos con sus etiquetas amigables y estado desconocido crudo', () => {
    const eventos: EventoTrackingPublicoDto[] = [
      { estadoEvento: 'RECEPCIONADO', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T10:00:00' },
      { estadoEvento: 'EN_ALMACEN_ORIGEN', sucursal: 'Agencia Lima Central', fechaHora: '2026-10-20T14:00:00' },
      { estadoEvento: 'EN_TRANSITO', sucursal: 'Ruta Lima - Huancayo', fechaHora: '2026-10-21T02:00:00' },
      { estadoEvento: 'LISTO_PARA_RETIRO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T08:00:00' },
      { estadoEvento: 'EN_ALMACEN_DESTINO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T09:00:00' },
      { estadoEvento: 'ENTREGADO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T11:00:00' },
      { estadoEvento: 'ESTADO_EXTRA_CRUDO', sucursal: 'Agencia Huancayo', fechaHora: '2026-10-21T12:00:00' },
    ];

    render(<LineaTiempo eventos={eventos} />);

    expect(screen.getByText('Recibido en agencia')).toBeInTheDocument();
    expect(screen.getByText('En almacén de origen')).toBeInTheDocument();
    expect(screen.getByText('En tránsito')).toBeInTheDocument();
    // Debe haber dos "Listo para retiro" (LISTO_PARA_RETIRO y el alias EN_ALMACEN_DESTINO)
    expect(screen.getAllByText('Listo para retiro')).toHaveLength(2);
    expect(screen.getByText('Entregado')).toBeInTheDocument();
    expect(screen.getByText('ESTADO_EXTRA_CRUDO')).toBeInTheDocument();
  });

  it('muestra mensaje cuando no hay eventos de seguimiento', () => {
    render(<LineaTiempo eventos={[]} />);
    expect(screen.getByText(/no se registran eventos de seguimiento/i)).toBeInTheDocument();
  });
});
