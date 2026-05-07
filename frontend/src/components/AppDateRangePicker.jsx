import { forwardRef } from 'react';
import DatePicker from 'react-datepicker';

function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function formatIsoDate(date) {
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatPrettyDate(value) {
  const date = parseIsoDate(value);
  if (!date) return '';
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

function getRangeDays(startDate, endDate) {
  const start = parseIsoDate(startDate);
  const end = parseIsoDate(endDate);
  if (!start || !end) return null;
  const diffMs = end.getTime() - start.getTime();
  if (diffMs < 0) return null;
  return Math.floor(diffMs / (1000 * 60 * 60 * 24)) + 1;
}

const RangeDisplayInput = forwardRef(function RangeDisplayInput(
  { value, onClick, placeholder },
  ref
) {
  return (
    <input
      ref={ref}
      value={value || ''}
      onClick={onClick}
      placeholder={placeholder}
      readOnly
      className="app-date-picker-input"
    />
  );
});

function AppDateRangePicker({ label, startDate, endDate, onChange, className = '' }) {
  const days = getRangeDays(startDate, endDate);
  const displayValue = startDate && endDate
    ? `${formatPrettyDate(startDate)} - ${formatPrettyDate(endDate)} (${days} days)`
    : startDate
      ? `${formatPrettyDate(startDate)} - ...`
      : '';

  return (
    <label className={className}>
      {label}
      <DatePicker
        selected={parseIsoDate(startDate)}
        startDate={parseIsoDate(startDate)}
        endDate={parseIsoDate(endDate)}
        onChange={(dates) => {
          const [start, end] = dates || [];
          onChange(formatIsoDate(start), formatIsoDate(end));
        }}
        selectsRange
        shouldCloseOnSelect={false}
        dateFormat="yyyy-MM-dd"
        placeholderText="YYYY-MM-DD - YYYY-MM-DD"
        customInput={
          <RangeDisplayInput
            value={displayValue}
            placeholder="YYYY-MM-DD - YYYY-MM-DD"
          />
        }
      />
    </label>
  );
}

export default AppDateRangePicker;
