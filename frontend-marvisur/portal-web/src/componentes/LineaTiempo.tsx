import React from 'react';
import { EventoTrackingPublicoDto, obtenerEtiquetaTracking } from '../api/tipos';

interface LineaTiempoProps {
  eventos: EventoTrackingPublicoDto[];
  className?: string;
}

function formatearFecha(isoString: string): string {
  try {
    const tieneZona = isoString.endsWith('Z') || /[+-]\d{2}(:\d{2})?$/.test(isoString);
    const cadenaParseable = (!tieneZona && isoString.includes('T')) ? `${isoString}Z` : isoString;
    const fecha = new Date(cadenaParseable);
    if (isNaN(fecha.getTime())) return isoString;
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(fecha);
  } catch {
    return isoString;
  }
}

export const LineaTiempo: React.FC<LineaTiempoProps> = ({ eventos, className = '' }) => {
  if (!eventos || eventos.length === 0) {
    return (
      <p className="text-sm text-slate-500 italic py-4">
        No se registran eventos de seguimiento para esta encomienda.
      </p>
    );
  }

  return (
    <div className={`relative pl-6 border-l-2 border-slate-200 space-y-6 ${className}`}>
      {eventos.map((evento, index) => {
        const esUltimo = index === eventos.length - 1;
        const etiqueta = obtenerEtiquetaTracking(evento.estadoEvento);

        return (
          <div key={`${evento.estadoEvento}-${index}`} className="relative group">
            {/* Indicador del nodo */}
            <span
              className={`absolute -left-[31px] top-1 w-4 h-4 rounded-full border-2 border-white transition-all shadow-sm ${
                esUltimo ? 'bg-rojo ring-4 ring-rojo/20' : 'bg-slate-400'
              }`}
              aria-hidden="true"
            />

            <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-1">
              <h5
                className={`text-sm font-semibold tracking-wide ${
                  esUltimo ? 'text-rojo' : 'text-azul-noche'
                }`}
              >
                {etiqueta}
              </h5>
              <time
                dateTime={evento.fechaHora}
                className="text-xs text-slate-500 font-medium"
              >
                {formatearFecha(evento.fechaHora)}
              </time>
            </div>

            <p className="text-xs text-slate-600 mt-0.5">
              <span className="font-medium text-slate-700">Ubicación:</span> {evento.sucursal}
            </p>
          </div>
        );
      })}
    </div>
  );
};
