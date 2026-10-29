import { describe, expect, it } from 'vitest';

import { diaSemana, fechaCorta, hoyEnLima, porcentaje, sumarDias, volumen } from './utilidades';

describe('utilidades de fecha y formato', () => {
  it('hoy se calcula en hora de Lima, no del navegador', () => {
    // 2026-10-30 02:00 UTC todavía es 29 de octubre en Lima (UTC-5).
    expect(hoyEnLima(new Date('2026-10-30T02:00:00Z'))).toBe('2026-10-29');
  });

  it('suma y resta días cruzando meses y años', () => {
    expect(sumarDias('2026-10-29', -29)).toBe('2026-09-30');
    expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('formatea fechas, volúmenes y porcentajes', () => {
    expect(fechaCorta('2026-10-29')).toBe('29/10/2026');
    expect(diaSemana('2026-10-29')).toBe('jueves 29/10');
    expect(volumen(28_000)).toMatch(/^28.000 L$/);
    expect(volumen(1_250_000)).toMatch(/^1.250 m³$/);
    expect(porcentaje(87.5)).toBe('87.5 %'); // es-PE usa punto decimal
    expect(porcentaje(null)).toBe('—');
  });
});
