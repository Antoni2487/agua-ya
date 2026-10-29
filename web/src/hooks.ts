import { useEffect, useState } from 'react';

/** Hora actual que se actualiza cada `intervaloMs`, para textos como "hace 40 s". */
export function useAhora(intervaloMs = 10_000): number {
  const [ahora, setAhora] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setAhora(Date.now()), intervaloMs);
    return () => clearInterval(id);
  }, [intervaloMs]);
  return ahora;
}
