import dayjs from 'dayjs';

const nf = new Intl.NumberFormat('vi-VN');

export const fmtNum = (n?: number | null) => (n === undefined || n === null ? '—' : nf.format(n));

export const fmtDiff = (cur: number | undefined, next: number) => {
  if (cur === undefined) return '';
  const d = next - cur;
  return `${d > 0 ? '+' : ''}${nf.format(d)}`;
};

export const fmtTime = (s?: string | null) => (s ? dayjs(s).format('HH:mm:ss DD/MM/YYYY') : '—');

export const numberInputProps = {
  formatter: (v?: string | number) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.'),
  parser: (v?: string) => Number((v ?? '').replace(/\./g, '')),
};
