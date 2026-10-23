import React, { useEffect, useState } from 'react';
import { listarSucursales } from '../api/cliente';
import { ApiError, SucursalPublicaDto } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { EsqueletoTarjeta } from '../componentes/Cargando';

export const Agencias: React.FC = () => {
  const [sucursales, setSucursales] = useState<SucursalPublicaDto[]>([]);
  const [filtro, setFiltro] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const cargarSucursales = async () => {
    setCargando(true);
    setError(null);
    try {
      const data = await listarSucursales();
      setSucursales(data);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('No fue posible cargar el listado de agencias.');
      }
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarSucursales();
  }, []);

  const sucursalesFiltradas = sucursales.filter((s) => {
    const termino = filtro.toLowerCase().trim();
    return (
      s.nombreAgencia.toLowerCase().includes(termino) ||
      s.ciudad.toLowerCase().includes(termino) ||
      s.direccion.toLowerCase().includes(termino)
    );
  });

  return (
    <div className="space-y-8">
      {/* Encabezado */}
      <div className="text-center space-y-2 max-w-2xl mx-auto">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-azul-noche">
          Red de Agencias Expreso Marvisur
        </h1>
        <p className="text-sm text-slate-600">
          Contamos con sucursales estratégicas para la recepción y entrega segura de tus encomiendas en Lima, Arequipa, Huancayo y las principales ciudades del Perú.
        </p>
      </div>

      {/* Buscador de agencias */}
      <div className="max-w-md mx-auto">
        <div className="relative">
          <input
            type="search"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
            placeholder="Buscar por ciudad o agencia..."
            className="w-full pl-11 pr-4 py-3 rounded-xl border border-slate-300 focus:border-rojo text-sm bg-white shadow-xs"
            aria-label="Buscar agencia por ciudad o nombre"
          />
          <svg
            className="w-5 h-5 text-slate-400 absolute left-3.5 top-3.5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      {/* Estado de error */}
      {error && (
        <Alerta
          tipo="error"
          titulo="Error al cargar agencias"
          mensaje={error}
          accion={
            <button
              onClick={cargarSucursales}
              className="text-xs font-bold text-red-700 underline hover:no-underline"
            >
              Reintentar carga
            </button>
          }
        />
      )}

      {/* Estado de carga */}
      {cargando && (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
          <EsqueletoTarjeta />
          <EsqueletoTarjeta />
          <EsqueletoTarjeta />
        </div>
      )}

      {/* Listado de tarjetas */}
      {!cargando && !error && (
        <>
          {sucursalesFiltradas.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl border border-slate-200 p-8">
              <svg className="w-12 h-12 text-slate-300 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <h3 className="font-bold text-base text-azul-noche mb-1">
                No se encontraron agencias
              </h3>
              <p className="text-xs text-slate-500">
                Intente con otro término de búsqueda o limpie el filtro.
              </p>
              {filtro && (
                <button
                  onClick={() => setFiltro('')}
                  className="mt-3 text-xs font-bold text-rojo hover:underline"
                >
                  Limpiar búsqueda
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
              {sucursalesFiltradas.map((sucursal) => (
                <article
                  key={sucursal.id}
                  className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between"
                >
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="px-2.5 py-1 rounded-md text-[11px] font-bold tracking-wider uppercase bg-rojo-claro text-rojo">
                        {sucursal.ciudad}
                      </span>
                      <span className="text-[10px] font-mono text-slate-400">
                        {sucursal.id}
                      </span>
                    </div>

                    <h2 className="text-base font-bold text-azul-noche leading-snug">
                      {sucursal.nombreAgencia}
                    </h2>

                    <div className="flex items-start gap-2 text-xs text-slate-600">
                      <svg className="w-4 h-4 text-rojo flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span>{sucursal.direccion}</span>
                    </div>
                  </div>

                  <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                    <span className="inline-flex items-center gap-1.5 text-emerald-700 font-semibold">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                      Atención operativa
                    </span>
                    <span>Lun - Sáb: 08:00 - 18:00</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};
