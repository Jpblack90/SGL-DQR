import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { cotizarTarifa, listarSucursales } from '../api/cliente';
import { ApiError, CotizacionResponse, SucursalPublicaDto } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Cargando } from '../componentes/Cargando';

export const Cotiza: React.FC = () => {
  const navigate = useNavigate();

  // Catálogo de sucursales
  const [sucursales, setSucursales] = useState<SucursalPublicaDto[]>([]);
  const [cargandoSucursales, setCargandoSucursales] = useState<boolean>(true);
  const [errorSucursales, setErrorSucursales] = useState<string | null>(null);

  // Campos del formulario
  const [origen, setOrigen] = useState<string>('');
  const [destino, setDestino] = useState<string>('');
  const [pesoKg, setPesoKg] = useState<string>('');
  const [volumenM3, setVolumenM3] = useState<string>('');

  // Errores de validación local
  const [erroresLocales, setErroresLocales] = useState<Record<string, string>>({});

  // Estado de cotización
  const [cargandoCotizacion, setCargandoCotizacion] = useState<boolean>(false);
  const [cotizacion, setCotizacion] = useState<CotizacionResponse | null>(null);
  const [errorApi, setErrorApi] = useState<string | null>(null);
  const [reintentoSegundos, setReintentoSegundos] = useState<number | null>(null);

  useEffect(() => {
    let activo = true;
    async function cargar() {
      setCargandoSucursales(true);
      setErrorSucursales(null);
      try {
        const datos = await listarSucursales();
        if (activo) {
          setSucursales(datos);
          if (datos.length >= 2) {
            setOrigen(datos[0].id);
            setDestino(datos[1].id);
          } else if (datos.length === 1) {
            setOrigen(datos[0].id);
            setDestino(datos[0].id);
          }
        }
      } catch (err) {
        if (activo) {
          const mensaje = err instanceof ApiError ? err.message : 'No fue posible cargar las agencias.';
          setErrorSucursales(mensaje);
        }
      } finally {
        if (activo) setCargandoSucursales(false);
      }
    }
    cargar();
    return () => {
      activo = false;
    };
  }, []);

  const validarFormulario = (): boolean => {
    const nuevosErrores: Record<string, string> = {};

    if (!origen.trim()) {
      nuevosErrores.origen = 'Selecciona la agencia de origen.';
    }
    if (!destino.trim()) {
      nuevosErrores.destino = 'Selecciona la agencia de destino.';
    }
    if (origen.trim() && destino.trim() && origen === destino && sucursales.length > 1) {
      nuevosErrores.destino = 'La agencia de destino debe ser diferente a la de origen.';
    }

    const pesoNum = parseFloat(pesoKg);
    if (!pesoKg.trim()) {
      nuevosErrores.peso = 'Ingresa el peso estimado en kilogramos.';
    } else if (isNaN(pesoNum) || pesoNum <= 0) {
      nuevosErrores.peso = 'El peso debe ser mayor a 0 kg (mínimo 0.01 kg).';
    }

    if (volumenM3.trim()) {
      const volNum = parseFloat(volumenM3);
      if (isNaN(volNum) || volNum < 0) {
        nuevosErrores.volumen = 'El volumen no puede ser negativo.';
      }
    }

    setErroresLocales(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  };

  const handleCotizar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validarFormulario()) return;

    setCargandoCotizacion(true);
    setErrorApi(null);
    setReintentoSegundos(null);

    try {
      const resp = await cotizarTarifa({
        idSucursalOrigen: origen,
        idSucursalDestino: destino,
        pesoKg: parseFloat(pesoKg),
        volumenM3: volumenM3.trim() ? parseFloat(volumenM3) : undefined,
      });
      setCotizacion(resp);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorApi(err.message);
        if (err.status === 429 && err.retryAfterSeconds) {
          setReintentoSegundos(err.retryAfterSeconds);
        }
      } else {
        setErrorApi('Ocurrió un error inesperado al cotizar. Intenta nuevamente.');
      }
    } finally {
      setCargandoCotizacion(false);
    }
  };

  const continuarAlPreRegistro = () => {
    if (!cotizacion) return;
    // Pasar los datos exclusivamente por estado de React Router (no por URL)
    navigate('/pre-registro', {
      state: {
        idSucursalOrigen: origen,
        idSucursalDestino: destino,
        pesoEstimado: parseFloat(pesoKg),
        volumenM3: volumenM3.trim() ? parseFloat(volumenM3) : undefined,
        montoCotizado: cotizacion.montoEstimado,
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8">
      {/* Cabecera */}
      <div className="text-center space-y-2">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-rojo-claro text-rojo">
          Cotizador Oficial en Línea
        </span>
        <h1 className="text-2xl sm:text-4xl font-extrabold text-azul-noche tracking-tight">
          Cotiza tu Envío
        </h1>
        <p className="text-sm sm:text-base text-slate-600 max-w-xl mx-auto">
          Calcula el costo aproximado de transporte interprovincial según el peso y las agencias de origen y destino.
        </p>
      </div>

      {/* Alerta de error en catálogo */}
      {errorSucursales && (
        <Alerta
          tipo="error"
          titulo="Error al cargar sucursales"
          mensaje={errorSucursales}
        />
      )}

      {/* Formulario */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8">
        {cargandoSucursales ? (
          <Cargando texto="Cargando agencias autorizadas de Expreso Marvisur..." />
        ) : (
          <form onSubmit={handleCotizar} className="space-y-6" noValidate>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Origen */}
              <div>
                <label htmlFor="origen" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Agencia Origen <span className="text-rojo">*</span>
                </label>
                <select
                  id="origen"
                  value={origen}
                  onChange={(e) => {
                    setOrigen(e.target.value);
                    setCotizacion(null);
                  }}
                  className={`w-full rounded-xl border px-3.5 py-3 text-sm text-azul-noche bg-white focus:outline-none focus:ring-2 focus:ring-rojo/30 ${
                    erroresLocales.origen ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                >
                  <option value="">Selecciona origen...</option>
                  {sucursales.map((s) => (
                    <option key={`orig-${s.id}`} value={s.id}>
                      {s.nombreAgencia} ({s.ciudad})
                    </option>
                  ))}
                </select>
                {erroresLocales.origen && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresLocales.origen}</p>
                )}
              </div>

              {/* Destino */}
              <div>
                <label htmlFor="destino" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Agencia Destino <span className="text-rojo">*</span>
                </label>
                <select
                  id="destino"
                  value={destino}
                  onChange={(e) => {
                    setDestino(e.target.value);
                    setCotizacion(null);
                  }}
                  className={`w-full rounded-xl border px-3.5 py-3 text-sm text-azul-noche bg-white focus:outline-none focus:ring-2 focus:ring-rojo/30 ${
                    erroresLocales.destino ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                  }`}
                >
                  <option value="">Selecciona destino...</option>
                  {sucursales.map((s) => (
                    <option key={`dest-${s.id}`} value={s.id}>
                      {s.nombreAgencia} ({s.ciudad})
                    </option>
                  ))}
                </select>
                {erroresLocales.destino && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresLocales.destino}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Peso en Kg */}
              <div>
                <label htmlFor="pesoKg" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Peso Estimado (kg) <span className="text-rojo">*</span>
                </label>
                <div className="relative">
                  <input
                    id="pesoKg"
                    type="number"
                    step="0.1"
                    min="0.1"
                    placeholder="Ej. 4.5"
                    value={pesoKg}
                    onChange={(e) => {
                      setPesoKg(e.target.value);
                      setCotizacion(null);
                    }}
                    className={`w-full rounded-xl border px-3.5 py-3 text-sm text-azul-noche placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-rojo/30 ${
                      erroresLocales.peso ? 'border-rojo bg-rojo-claro/20' : 'border-slate-300'
                    }`}
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 pointer-events-none">
                    kg
                  </span>
                </div>
                {erroresLocales.peso && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresLocales.peso}</p>
                )}
              </div>

              {/* Volumen opcional */}
              <div>
                <label htmlFor="volumenM3" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1.5">
                  Volumen Opcional (m³)
                </label>
                <div className="relative">
                  <input
                    id="volumenM3"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="Ej. 0.05"
                    value={volumenM3}
                    onChange={(e) => {
                      setVolumenM3(e.target.value);
                      setCotizacion(null);
                    }}
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-3 text-sm text-azul-noche placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-rojo/30"
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 pointer-events-none">
                    m³
                  </span>
                </div>
                {erroresLocales.volumen && (
                  <p className="mt-1 text-xs text-rojo font-medium">{erroresLocales.volumen}</p>
                )}
              </div>
            </div>

            {/* Error de la API */}
            {errorApi && (
              <Alerta
                tipo={reintentoSegundos ? 'advertencia' : 'error'}
                titulo={reintentoSegundos ? 'Límite de consultas' : 'Error al cotizar'}
                mensaje={errorApi}
              />
            )}

            {/* Botón cotizar */}
            <button
              type="submit"
              disabled={cargandoCotizacion || cargandoSucursales}
              className="w-full py-3.5 px-6 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-md shadow-rojo/20 transition-all disabled:opacity-50 disabled:cursor-not-allowed focus-visible:outline-rojo"
            >
              {cargandoCotizacion ? 'Calculando tarifa...' : 'Calcular Tarifa Estimada'}
            </button>
          </form>
        )}
      </div>

      {/* Resultado de la cotización */}
      {cotizacion && (
        <div className="bg-gradient-to-br from-azul-noche to-azul-noche-suave text-white rounded-2xl p-6 sm:p-8 shadow-xl animate-fadeIn space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-700/60">
            <div>
              <span className="text-xs uppercase font-bold text-slate-300 block tracking-widest">
                Tarifa Estimada de Envío
              </span>
              <p className="text-3xl sm:text-5xl font-black text-white mt-1">
                S/ {cotizacion.montoEstimado.toFixed(2)}
              </p>
            </div>
            <div className="text-left sm:text-right text-xs text-slate-300 space-y-1">
              <p>
                <span className="font-semibold text-white">Moneda:</span> Soles ({cotizacion.moneda})
              </p>
              <p>
                <span className="font-semibold text-white">Peso cotizado:</span> {parseFloat(pesoKg).toFixed(2)} kg
              </p>
            </div>
          </div>

          <div className="bg-white/10 rounded-xl p-4 border border-white/15">
            <p className="text-xs sm:text-sm text-slate-200 leading-relaxed font-medium">
              ℹ️ <strong>Tarifa referencial; el monto final se confirma en agencia</strong> según pesaje físico y cubicaje oficial en ventanilla.
            </p>
          </div>

          <button
            type="button"
            onClick={continuarAlPreRegistro}
            className="w-full py-3.5 px-6 rounded-xl font-bold text-azul-noche bg-white hover:bg-slate-100 shadow-lg transition-all transform active:scale-95 focus-visible:outline-white text-center"
          >
            Continuar al pre-registro →
          </button>
        </div>
      )}
    </div>
  );
};
