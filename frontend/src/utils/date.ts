export function formatDateTime(value) {
  if (!value) return '';

  const date = new Date(String(value).replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return value;

  const pad = number => String(number).padStart(2, '0');
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function shortDateTime(value) {
  return formatDateTime(value).replace(' ', '<br />');
}

export function toLocalInput(date) {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16);
}

export function durationTextFromMinutes(minutes) {
  const total = Number(minutes || 0);
  const days = Math.floor(total / 1440);
  const hours = Math.floor((total % 1440) / 60);
  const leftMinutes = total % 60;
  const parts = [];

  if (days > 0) parts.push(`${days}天`);
  if (hours > 0) parts.push(`${hours}小时`);
  if (leftMinutes > 0) parts.push(`${leftMinutes}分钟`);

  return parts.length ? parts.join('') : '0分钟';
}

export function durationParts(startValue, endValue, emptyText = '0小时') {
  const start = new Date(startValue);
  const end = new Date(endValue);

  if (!startValue || !endValue || end <= start) {
    return { minutes: 0, text: emptyText };
  }

  const minutes = Math.round((end - start) / 60000);
  return {
    minutes,
    text: durationTextFromMinutes(minutes).replace('0分钟', emptyText)
  };
}

export function approvalLevelByMinutes(minutes) {
  const days = Math.ceil(Number(minutes || 0) / 1440);
  if (days <= 3) return 1;
  if (days <= 10) return 2;
  return 3;
}
