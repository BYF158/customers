export function todayISO() {
  const date = new Date()
  const offset = date.getTimezoneOffset()
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 10)
}

export function currentMonth() {
  return todayISO().slice(0, 7)
}

export function formatMonth(month) {
  if (!month) return '全部月份'
  const [year, value] = String(month).split('-')
  if (!value) return month
  return `${year} 年 ${Number(value)} 月`
}

export function shortMonth(month) {
  if (!month) return '—'
  const parts = String(month).split('-')
  return parts.length > 1 ? `${Number(parts[1])}月` : month
}
