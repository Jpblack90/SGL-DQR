import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { consultarBilletera } from '../api/cliente';
import { ApiError, RespuestaBilleteraDto } from '../api/tipos';
import { Alerta } from '../componentes/Alerta';
import { Cargando } from '../componentes/Cargando';

export const Billetera: React.FC = () => {
  const location = useLocation();

  const [cargando, setCargando] = useState(true);
  const [billetera, setBilletera] = useState<RespuestaBilleteraDto | null>(null);
  const [errorMensaje, setErrorMensaje] = useState<string | null>(null);
  const [reintentoSegundos, setReintentoSegundos] = useState<number | null>(null);

  // Obtener el token EXCLUSIVAMENTE del fragmento (#) de la URL
  // NUNCA de query params, cookies ni localStorage.
  const extraerTokenDelHash = (): string => {
    const rawHash = (location.hash || window.location.hash || '').replace(/^#/, '').trim();
    if (rawHash.startsWith('token=')) {
      return rawHash.substring(6).trim();
    }
    return rawHash;
  };

  const cargarBilletera = async () => {
    setCargando(true);
    setErrorMensaje(null);
    setReintentoSegundos(null);

    const token = extraerTokenDelHash();

    // Validación: si no hay token en el hash
    if (!token) {
      setCargando(false);
      setErrorMensaje(
        'No se proporcionó una llave digital de retiro en el enlace. Para acceder a su Billetera Digital, utilice el enlace oficial recibido al registrar su encomienda (ej. /billetera#TOKEN).'
      );
      return;
    }

    try {
      // Envío en el CUERPO de POST /api/v1/publico/billetera.
      // NUNCA se persiste en storage ni se imprime en consola.
      const datos = await consultarBilletera(token);
      setBilletera(datos);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMensaje(err.message);
        if (err.retryAfterSeconds) {
          setReintentoSegundos(err.retryAfterSeconds);
        }
      } else {
        // Mensaje genérico para no filtrar información sensible
        setErrorMensaje(
          'No se pudo verificar la llave digital. Verifique el enlace recibido o acérquese a ventanilla.'
        );
      }
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarBilletera();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.hash]);

  return (
    <div className="max-w-md mx-auto space-y-6 xs:px-1">
      {/* Encabezado */}
      <div className="text-center space-y-1">
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-wider bg-rojo-claro text-rojo">
          <span className="w-1.5 h-1.5 rounded-full bg-rojo" />
          Llave Digital de Retiro
        </span>
        <h1 className="text-2xl font-black text-azul-noche tracking-tight">
          Billetera Digital Marvisur
        </h1>
        <p className="text-xs text-slate-500">
          Presenta este código en ventanilla para retirar tu encomienda.
        </p>
      </div>

      {/* Cargando */}
      {cargando && <Cargando texto="Verificando llave digital y generando código QR..." />}

      {/* Error / Sin Token */}
      {!cargando && errorMensaje && (
        <div className="space-y-4 animate-fadeIn">
          <Alerta
            tipo={reintentoSegundos ? 'advertencia' : 'error'}
            titulo={reintentoSegundos ? 'Límite de Consultas' : 'Enlace No Válido o No Encontrado'}
            mensaje={errorMensaje}
          />
          <div className="bg-white rounded-2xl p-6 border border-slate-200 text-center space-y-3 shadow-xs">
            <h3 className="text-sm font-bold text-azul-noche">
              ¿Cómo obtengo mi llave digital?
            </h3>
            <p className="text-xs text-slate-600 leading-relaxed">
              El remitente recibe el enlace oficial al momento de despachar la encomienda en ventanilla. Si no cuentas con el enlace, puedes consultar el estado general en la sección de rastreo.
            </p>
            <div className="pt-2">
              <Link
                to="/rastreo"
                className="inline-flex items-center justify-center px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-rojo hover:bg-rojo-profundo transition-colors"
              >
                Ir a Rastreo de Encomiendas
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* Contenido de la Billetera */}
      {!cargando && billetera && (
        <div className="bg-white rounded-3xl border border-slate-200 shadow-lg overflow-hidden animate-fadeIn">
          {/* Cabecera de estado */}
          <div className="bg-gradient-to-br from-azul-noche to-azul-noche-suave p-6 text-white text-center space-y-2">
            <div className="flex items-center justify-center gap-2">
              <span className="text-xs font-mono tracking-widest text-slate-300 uppercase">
                Comprobante
              </span>
              <span className="font-bold text-white tracking-wide text-sm">
                {billetera.codigoComprobante}
              </span>
            </div>

            <div className="pt-1">
              {billetera.estadoToken === 'ACTIVO' && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-400/30">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  Código Activo para Retiro
                </span>
              )}
              {billetera.estadoToken === 'INVALIDADO' && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-slate-500/20 text-slate-300 border border-slate-400/30">
                  Código Invalidado (Ya Entregado)
                </span>
              )}
              {billetera.estadoToken === 'EXPIRADO' && (
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-red-500/20 text-red-300 border border-red-400/30">
                  Código Expirado
                </span>
              )}
            </div>
          </div>

          <div className="p-6 sm:p-8 space-y-6">
            {/* Banner si el pago está PENDIENTE */}
            {billetera.estadoPago === 'PENDIENTE' && billetera.estadoToken === 'ACTIVO' && (
              <Alerta
                tipo="advertencia"
                titulo="Pago Contra Entrega Requerido"
                mensaje={
                  <div className="space-y-1">
                    <p>
                      Monto a pagar en ventanilla antes de retirar:
                    </p>
                    <p className="text-lg font-black text-amber-900">
                      S/. {billetera.montoTotal.toFixed(2)}
                    </p>
                  </div>
                }
              />
            )}

            {/* Mensajes según estado del token */}
            {billetera.estadoToken === 'INVALIDADO' && (
              <Alerta
                tipo="info"
                titulo="Envío Ya Entregado"
                mensaje="Este envío ya fue entregado y el código ya no es válido. La llave digital ha sido inactivada irreversiblemente en el sistema."
              />
            )}

            {billetera.estadoToken === 'EXPIRADO' && (
              <Alerta
                tipo="error"
                titulo="Plazo de Retiro Expirado"
                mensaje="Este código ha expirado. Por favor, acérquese a la ventanilla de la agencia con su documento de identidad físico para validar su encomienda."
              />
            )}

            {/* Código QR2 grande renderizado */}
            <div className="flex flex-col items-center justify-center p-4 bg-slate-50 rounded-2xl border border-slate-100">
              <div className="relative p-3 bg-white rounded-2xl shadow-sm border border-slate-200">
                {billetera.qr2Base64 ? (
                  <img
                    src={`data:image/png;base64,${billetera.qr2Base64}`}
                    alt="Código QR2 Digital de Retiro"
                    className={`w-64 h-64 object-contain ${
                      billetera.estadoToken !== 'ACTIVO' ? 'opacity-30 grayscale' : ''
                    }`}
                  />
                ) : (
                  <div className="w-64 h-64 flex items-center justify-center text-xs text-slate-400">
                    No se pudo generar la imagen del QR
                  </div>
                )}

                {/* Sello de agua si está invalidado */}
                {billetera.estadoToken === 'INVALIDADO' && (
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="px-4 py-2 bg-red-600 text-white font-black text-xs uppercase tracking-widest rounded-lg transform -rotate-12 shadow-lg">
                      INVALIDADO
                    </span>
                  </div>
                )}
              </div>

              {billetera.estadoToken === 'ACTIVO' && (
                <p className="text-xs text-slate-600 font-medium text-center mt-3 max-w-xs">
                  Muestra este código al personal en ventanilla junto a tu documento de identidad para retirar tu paquete.
                </p>
              )}
            </div>

            {/* Advertencia de privacidad y seguridad */}
            <div className="bg-amber-50/70 border border-amber-200 rounded-xl p-3.5 flex items-start gap-2.5 text-xs text-amber-900">
              <svg className="w-4 h-4 text-amber-700 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
              </svg>
              <div>
                <p className="font-bold">No compartas este enlace</p>
                <p className="text-[11px] text-amber-800 mt-0.5">
                  Este enlace contiene tu llave digital de retiro personal. Cualquiera con acceso al código podría intentar reclamar el paquete.
                </p>
              </div>
            </div>

            {/* Resumen de la Encomienda */}
            <div className="border-t border-slate-100 pt-4 space-y-2 text-xs">
              <h4 className="font-bold text-azul-noche uppercase tracking-wider text-[11px]">
                Detalles de Retiro
              </h4>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Agencia Destino:</span>
                <span className="font-semibold text-azul-noche">{billetera.agenciaDestino}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Ciudad Destino:</span>
                <span className="font-semibold text-azul-noche">{billetera.ciudadDestino}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-100">
                <span className="text-slate-500">Estado Logístico:</span>
                <span className="font-semibold text-azul-noche">{billetera.estadoLogistico.replace(/_/g, ' ')}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Estado de Pago:</span>
                <span
                  className={`font-bold ${
                    billetera.estadoPago === 'PAGADO' ? 'text-emerald-600' : 'text-amber-600'
                  }`}
                >
                  {billetera.estadoPago} (S/. {billetera.montoTotal.toFixed(2)})
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
